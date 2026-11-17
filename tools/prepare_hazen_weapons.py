"""Extract declarative weapon specifications and original bitmap/geometry bytes.

This consumes the audited original archive, not the failed backport's Java.
"""
from pathlib import Path
import hashlib
import json
import re
import zipfile
from audit_hazen_recreation import PROJECT, INSTANCE, PACKAGE, OUT, clean, read, balanced, split_args

NAMESPACE = 'alex_caves_spellsbook'
RES = PROJECT / 'src/main/resources'


def uses_glow_layer(text):
    # An unused import or commented-out layer does not enable upstream glow.
    return bool(re.search(r'\baddRenderLayer\s*\(\s*new\s+AutoGlowing\w*\s*(?:<[^>]*>)?\s*\(', clean(text)))


def number(s):
    return float(re.sub(r'[fFdD]$', '', s.strip()))


def attrs(text):
    result = []
    for m in re.finditer(r'new AttributeContainer\s*\(', text):
        args = split_args(balanced(text, text.index('(', m.start())))
        result.append({'source_attribute': args[0], 'amount': number(args[1]),
                       'operation': args[2].rsplit('.', 1)[-1]})
    return result


def tiers(name):
    s = clean(read(PACKAGE / f'HnSUtilities/Item/{name}.java'))
    rows = {}
    for m in re.finditer(r'\b(\w+)\s*=\s*new ' + name + r'\s*\(', s):
        args = split_args(balanced(s, s.index('(', m.start())))
        if name == 'HnSExtendedWeaponsTiers':
            rows[m[1]] = dict(durability=int(args[0]), damage=number(args[1]),
                              speed=number(args[2]), enchantment=int(args[3]), attributes=attrs(','.join(args[6:])))
        else:
            rows[m[1]] = dict(durability=0, damage=number(args[0]), speed=number(args[1]),
                              enchantment=15, attributes=attrs(','.join(args[2:])))
    return rows


def main():
    inventory = json.loads(read(OUT / 'original-inventory.json'))
    melee, staff = tiers('HnSExtendedWeaponsTiers'), tiers('HnSStaffTier')
    receipts, weapons = [], []
    with zipfile.ZipFile(inventory['original_jar']) as jar:
        def copy_asset(path):
            if path.startswith('animations/wizard_armor'):
                return 'irons_spellbooks:' + path
            original = 'assets/hazennstuff/' + path
            raw = jar.read(original)
            # GeckoLib discovers only geo/** and animations/** during resource reload.
            target_path = ('geo/hazen/' + path[4:]) if path.startswith('geo/') else ('animations/hazen/' + path[11:]) if path.startswith('animations/') else ('hazen/' + path)
            dest = RES / f'assets/{NAMESPACE}/{target_path}'
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes(raw)
            receipts.append({'original': original, 'target': str(dest.relative_to(PROJECT)),
                             'sha256': hashlib.sha256(raw).hexdigest(), 'bytes': len(raw)})
            if original + '.mcmeta' in jar.namelist():
                copy_asset(path + '.mcmeta')
            if path.endswith('.png'):
                glow = path[:-4] + '_glowmask.png'
                if 'assets/hazennstuff/' + glow in jar.namelist():
                    copy_asset(glow)
            return f'{NAMESPACE}:{target_path}'

        for w in inventory['weapons']:
            if not w['new'] or w['excluded']:
                continue
            s = w['source_definition']
            tm = re.search(r'(HnS\w*Tier\w*)\.([A-Z_0-9]+)', s)
            if tm:
                stats = dict((melee if tm[1] == 'HnSExtendedWeaponsTiers' else staff)[tm[2]])
            else:
                assert 'StaffTier.ARTIFICER' in s, w['id']
                # Resolve this against the installed 1.20.1 StaffTier at runtime.
                stats = dict(durability=0, damage=0, speed=0, enchantment=15, attributes=[], vanilla_staff_tier='ARTIFICER')
            spec = dict(id=w['id'], source=w['source'], source_sha256=w['source_sha256'],
                        kind='staff' if 'Staff' in w['base_class'] else 'sword', **stats)
            paths = w['resource_refs']
            spec['model'] = copy_asset(next(p for p in paths if p.startswith('geo/')))
            spec['texture'] = copy_asset(next(p for p in paths if p.startswith('textures/')))
            anim = next((p for p in paths if p.startswith('animations/')), None)
            spec['animation'] = copy_asset(anim) if anim else ''
            source_dir = INSTANCE / w['source']
            sibling_text = '\n'.join(clean(read(p)) for p in source_dir.parent.glob('*.java'))
            spec['glowing'] = uses_glow_layer(sibling_text)
            spec['animated'] = bool(re.search(r'controllerRegistrar.add\(animationController\)', s)) or 'Animator' in sibling_text
            spec['animation_name'] = 'idle'
            if anim and not anim.startswith('animations/wizard_armor'):
                animations = json.loads(jar.read('assets/hazennstuff/' + anim))['animations']
                spec['animation_names'] = list(animations)
                if 'idle' not in animations:
                    # Read animator constants, never infer by filename similarity.
                    match = re.search(r'thenLoop\("([^"]+)"\)', sibling_text)
                    spec['animation_name'] = match[1] if match else ''
            spec['fire_resistant'] = '.fireResistant()' in s
            rarity = re.search(r'rarity\((?:HLRarities\.|Rarity\.)(\w+)', s)
            spec['rarity'] = rarity[1] if rarity else 'COMMON'
            spec['descriptions'] = list(dict.fromkeys(re.findall(r'Component.translatable\("(item\.hazennstuff\.[^"]+)"', s)))
            spec['presets'] = []
            for field, level in re.findall(r'new SpellDataRegistryHolder\(([^,]+),\s*(\d+)\)', s):
                spec['presets'].append({'source_field': field.strip(), 'level': int(level)})
            spec['affinity'] = []
            for field, bonus in re.findall(r'((?:SpellRegistry|HnSSpellRegistries)\.\w+)\.get\(\)\.getSpellResource\(\),\s*(\d+)', s):
                spec['affinity'].append({'source_field': field, 'bonus': int(bonus)})
            raw_model = jar.read('assets/hazennstuff/models/item/' + w['id'] + '.json')
            item_model = json.loads(raw_model)
            # Preserve every display transform; only repair the particle binding.
            particle_path = next(p for p in paths if p.startswith('textures/'))
            raw_particle = jar.read('assets/hazennstuff/' + particle_path)
            baked_particle_path = RES / f'assets/{NAMESPACE}/textures/hazen' / particle_path.removeprefix('textures/')
            baked_particle_path.parent.mkdir(parents=True, exist_ok=True); baked_particle_path.write_bytes(raw_particle)
            receipts.append({'original': 'assets/hazennstuff/' + particle_path, 'target': str(baked_particle_path.relative_to(PROJECT)),
                             'sha256': hashlib.sha256(raw_particle).hexdigest(), 'bytes': len(raw_particle)})
            item_model.setdefault('textures', {})['particle'] = f'{NAMESPACE}:hazen/' + particle_path.removeprefix('textures/').removesuffix('.png')
            dest = RES / f'assets/{NAMESPACE}/models/item/hazen_{w["id"]}.json'
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_text(json.dumps(item_model, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
            spec['original_item_model_sha256'] = hashlib.sha256(raw_model).hexdigest()
            weapons.append(spec)

        for lang in ('en_us', 'zh_cn'):
            dest = RES / f'assets/{NAMESPACE}/lang/{lang}.json'
            existing = json.loads(read(dest))
            key = f'assets/hazennstuff/lang/{lang}.json'
            original = json.loads(jar.read(key)) if key in jar.namelist() else {}
            english = json.loads(jar.read('assets/hazennstuff/lang/en_us.json'))
            for w in weapons:
                old_key = f'item.hazennstuff.{w["id"]}'
                existing[f'item.{NAMESPACE}.hazen_{w["id"]}'] = original.get(old_key, english.get(old_key, w['id']))
                for desc in w['descriptions']:
                    if desc in original or desc in english:
                        existing[f'hazen_recreated.{desc}'] = original.get(desc, english.get(desc))
            existing[f'itemGroup.{NAMESPACE}.hazen_equipment'] = 'Hazen 装备重制' if lang == 'zh_cn' else 'Hazen Equipment'
            dest.write_text(json.dumps(existing, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    data = RES / 'hazen-recreated-weapons.json'
    data.write_text(json.dumps(weapons, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    (OUT / 'asset-receipts.json').write_text(json.dumps(receipts, indent=2), encoding='utf-8')
    print(json.dumps({'weapons': len(weapons), 'copied_assets': len(receipts),
                      'attribute_fields': sorted({a['source_attribute'] for w in weapons for a in w['attributes']}),
                      'spell_fields': sorted({p['source_field'] for w in weapons for p in w['presets']})}, indent=2))


if __name__ == '__main__':
    main()
