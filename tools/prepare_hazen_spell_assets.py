"""Stage exact original effects, school materials and dependency recipe graph."""
from pathlib import Path
import collections
import hashlib
import json
import re
import zipfile
from audit_hazen_recreation import PROJECT, INSTANCE, PACKAGE, OUT, read

NS = 'alex_caves_spellsbook'
RES = PROJECT / 'src/main/resources'
SPELLS = ['thorn_chakram', 'shooting_star', 'spectral_axe', 'nights_edge_strike',
          'terraprismic_barrage', 'call_forth_terraprisma', 'prismatic_shift']
ENTITY_DIR = {'thorn_chakram': 'Spells/Nature/ThornChakram', 'shooting_star': 'Spells/Cosmic/ShootingStar',
              'spectral_axe': 'Spells/Evocation/SpectralAxe', 'nights_edge_after_slash': 'Spells/Shadow/NightsEdgeAfterSlash',
              'terraprisma': 'Spells/Radiance/Terraprisma',
              'vampire_knife': 'Spells/Blood/LifestealKnife'}


def main():
    inputs = json.loads(read(INSTANCE / 'mod-backports-1.20.1/inputs.json'))
    inventory = json.loads(read(OUT / 'original-inventory.json'))
    specs = json.loads(read(RES / 'hazen-recreated-weapons.json'))
    receipts, visuals, models, materials, recipes = [], {}, {}, {}, {}
    jars = {name: zipfile.ZipFile(inputs[name]) for name in ('hazennstuff', 'hazentouvelib', 'aces_spell_utils')}

    def write_json(path, obj):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

    def raw_asset(namespace, path):
        key = f'assets/{namespace}/{path}'
        raw = jars[namespace].read(key)
        dest_path = ('sounds/hazen/' + namespace + '/' + path[7:]) if path.startswith('sounds/') else ('geo/hazen/' + namespace + '/' + path[4:]) if path.startswith('geo/') else ('animations/hazen/' + namespace + '/' + path[11:]) if path.startswith('animations/') else ('hazen/' + namespace + '/' + path)
        dest = RES / f'assets/{NS}' / dest_path
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(raw)
        receipts.append({'archive': inputs[namespace], 'original': key, 'target': str(dest.relative_to(PROJECT)),
                         'sha256': hashlib.sha256(raw).hexdigest()})
        return f'{NS}:{dest_path}'

    def model_asset(namespace, model_path):
        key = f'assets/{namespace}/models/{model_path}.json'
        d = json.loads(jars[namespace].read(key))
        # The upstream resources target NeoForge; preserve layer data using
        # Forge 1.20.1's corresponding loader and face-data key.
        if d.get('loader') == 'neoforge:item_layers':
            d['loader'] = 'forge:item_layers'
            if 'neoforge_data' in d:
                d['forge_data'] = d.pop('neoforge_data')
        for layer, texture in d.get('textures', {}).items():
            if texture.startswith('#'): continue
            tns, tpath = texture.split(':') if ':' in texture else ('minecraft', texture)
            if tns in jars:
                # Baked models resolve texture IDs below textures/, whereas GeckoLib
                # resource getters use explicit paths. Keep those two contracts distinct.
                for suffix in ('', '.mcmeta'):
                    key = f'assets/{tns}/textures/{tpath}.png{suffix}'
                    if key not in jars[tns].namelist(): continue
                    raw = jars[tns].read(key)
                    dest = RES / f'assets/{NS}/textures/hazen/{tns}/{tpath}.png{suffix}'
                    dest.parent.mkdir(parents=True, exist_ok=True); dest.write_bytes(raw)
                    receipts.append({'archive': inputs[tns], 'original': key, 'target': str(dest.relative_to(PROJECT)),
                                     'sha256': hashlib.sha256(raw).hexdigest()})
                d['textures'][layer] = f'{NS}:hazen/{tns}/{tpath}'
        return d

    for kind, directory in ENTITY_DIR.items():
        text = '\n'.join(read(p) for p in (PACKAGE / 'Entity' / directory).glob('*Model.java'))
        geom = re.search(r'"(geo/[^"]+)"', text)[1]
        anim_match = re.search(r'"(animations/[^"]+)"', text)
        anim = anim_match[1] if anim_match else ''
        if '"textures/spells/shooting_star_"' in text:
            textures = [f'textures/spells/shooting_star_{i}.png' for i in range(3)]
        elif '"textures/spells/nights_edge_after_slash_"' in text:
            textures = [f'textures/spells/nights_edge_after_slash_{i}.png' for i in range(8)]
        else:
            textures = [re.search(r'"(textures/[^"+]+\.png)"', text)[1]]
        animation_id = raw_asset('hazennstuff', anim) if anim and not anim.startswith('animations/wizard_armor') else 'irons_spellbooks:animations/wizard_armor_animation.json'
        original_entity = '\n'.join(read(p) for p in (PACKAGE / 'Entity' / directory).glob('*.java'))
        am = re.search(r'thenLoop\("([^"]+)"\)', original_entity)
        animation_name = am[1] if am else ''
        if anim and not anim.startswith('animations/wizard_armor'):
            available = json.loads(jars['hazennstuff'].read('assets/hazennstuff/' + anim))['animations']
            if animation_name not in available:
                # Upstream NightsEdge uses a nonexistent spectral-axe animation.
                # The supplied file's single animation is the authoritative visual.
                assert len(available) == 1, (kind, animation_name, list(available))
                animation_name = next(iter(available))
        visuals[kind] = {'model': raw_asset('hazennstuff', geom), 'textures': [raw_asset('hazennstuff', t) for t in textures],
                         'animation': animation_id, 'animation_name': animation_name}
    visuals['terraprisma']['glow_textures'] = [raw_asset('hazennstuff', f'textures/spells/terraprisma/terraprisma_glowmask_{i}.png') for i in range(20)]
    for spell in SPELLS:
        path = f'textures/gui/spell_icons/{spell}.png'
        raw_asset('hazennstuff', path)
        # Iron's AbstractSpell expects the icon under this conventional path.
        raw = jars['hazennstuff'].read('assets/hazennstuff/' + path)
        dest = RES / f'assets/{NS}/textures/gui/spell_icons/hazen_{spell}.png'
        dest.parent.mkdir(parents=True, exist_ok=True); dest.write_bytes(raw)
    write_json(RES / 'hazen-spell-visuals.json', visuals)

    for particle in ['star_impact_particle', 'cosmic_ember_particle', 'nights_edge_particle', 'rod_of_discord_particle', 'leaf_particle', 'nights_edge_strike']:
        original_name = particle if particle != 'nights_edge_strike' else 'nights_edge_strike'
        key = f'assets/hazennstuff/particles/{original_name}.json'
        if key not in jars['hazennstuff'].namelist():
            candidates = [n for n in jars['hazennstuff'].namelist() if n.startswith('assets/hazennstuff/particles/') and 'nights_edge' in n]
            if particle == 'nights_edge_strike': key = next(n for n in candidates if 'strike' in n)
            else: raise KeyError(key)
        d = json.loads(jars['hazennstuff'].read(key))
        tex = []
        for t in d['textures']:
            tns, path = t.split(':') if ':' in t else ('minecraft', t)
            if tns in jars:
                original_path = 'textures/particle/' + path + '.png'
                raw = jars[tns].read(f'assets/{tns}/' + original_path)
                target = RES / f'assets/{NS}/textures/particle/hazen/{tns}/{path}.png'
                target.parent.mkdir(parents=True, exist_ok=True); target.write_bytes(raw)
                receipts.append({'archive': inputs[tns], 'original': f'assets/{tns}/' + original_path,
                                 'target': str(target.relative_to(PROJECT)), 'sha256': hashlib.sha256(raw).hexdigest()})
                tex.append(f'{NS}:hazen/{tns}/{path}')
            else: tex.append(t)
        write_json(RES / f'assets/{NS}/particles/hazen_{particle}.json', {'textures': tex})

    sound_defs = {}
    for namespace in ('hazennstuff', 'hazentouvelib'):
        jar = jars[namespace]
        d = json.loads(jar.read(f'assets/{namespace}/sounds.json'))
        for name, definition in d.items():
            if namespace == 'hazentouvelib' and name != 'cosmic_cast_instant': continue
            definition = json.loads(json.dumps(definition))
            for i, sound in enumerate(definition.get('sounds', [])):
                key = sound if isinstance(sound, str) else sound['name']
                sns, path = key.split(':') if ':' in key else (namespace, key)
                if sns in jars:
                    target = raw_asset(sns, 'sounds/' + path + '.ogg').replace('sounds/', '', 1)[:-4]
                    if isinstance(sound, str): definition['sounds'][i] = target
                    else: sound['name'] = target
            sound_defs['hazen_' + name] = definition
    write_json(RES / f'assets/{NS}/sounds.json', sound_defs)
    write_json(RES / 'hazen-sound-ids.json', [k.removeprefix('hazen_') for k in sound_defs])

    # All recipes are read first so dependencies can be resolved without replacements.
    original_recipes = []
    for namespace, jar in jars.items():
        for name in jar.namelist():
            if '/recipe/' not in name or not name.endswith('.json'): continue
            d = json.loads(jar.read(name)); result = d.get('result', {})
            result_id = result.get('id', result.get('item', '')) if isinstance(result, dict) else result
            if result_id: original_recipes.append((namespace, name, result_id, d))
    registered = {x['id']: x for x in inventory['items']}
    legacy = {x['id'] for x in inventory['items'] if x['legacy']}
    weapon_ids = {w['id'] for w in specs}
    # Recreate original rune/orb materials independently of the unported libraries.
    for school in ('cosmic', 'radiance', 'shadow', 'hydro'):
        namespace = 'hazentouvelib' if school != 'hydro' else 'hazennstuff'
        for suffix in ('rune', 'upgrade_orb'):
            mid = school + '_' + suffix
            materials[f'{namespace}:{mid}'] = {'id': mid, 'kind': suffix, 'school': school}
        source_ns = 'hazentouvelib' if school != 'hydro' else 'aces_spell_utils'
        write_json(RES / f'assets/{NS}/models/item/scroll_hazen_{school}.json', model_asset(source_ns, 'item/scroll_' + school))
    dependencies = set()
    def ingredient_ids(d):
        if isinstance(d, dict):
            for k, v in d.items():
                if k == 'item' and isinstance(v, str): dependencies.add(v)
                elif k not in ('result',): ingredient_ids(v)
        elif isinstance(d, list):
            for v in d: ingredient_ids(v)
    wanted = {'hazennstuff:' + x for x in weapon_ids} | set(materials)
    for school in ('cosmic', 'radiance', 'shadow'):
        t = json.loads(jars['hazennstuff'].read(f'data/hazentouvelib/tags/item/focus/{school}_focus.json'))
        dependencies.update(x for x in t['values'] if isinstance(x, str) and not x.startswith('#'))
    previous = None
    while previous != (len(wanted), len(dependencies)):
        previous = (len(wanted), len(dependencies))
        for namespace, path, result, data in original_recipes:
            if result in wanted:
                recipes[path] = data
                ingredient_ids(data)
        for key in list(dependencies):
            namespace, mid = key.split(':')
            if namespace != 'hazennstuff' or mid in weapon_ids or mid in legacy: continue
            row = registered.get(mid)
            if row and row['class'] in ('Item', 'net.minecraft.world.item.Item'):
                rarity = re.search(r'rarity\((?:HLRarities\.|Rarity\.)(\w+)', row['registration'])
                materials[key] = {'id': mid, 'kind': 'material', 'fire_resistant': '.fireResistant()' in row['registration'],
                                  'rarity': rarity[1] if rarity else 'COMMON'}
                wanted.add(key)
    # The tags are also original data; remap them after the full dependency graph is known.
    id_map = {f'hazennstuff:{x}': f'{NS}:hazen_{x}' for x in weapon_ids}
    id_map.update({key: f'{NS}:hazen_{row["id"]}' for key, row in materials.items()})
    id_map['minecraft:short_grass'] = 'minecraft:grass'
    # The original duplicates Iron's own wisewood planks using the identical
    # irons_spellbooks:block/wisewood_planks texture. Use the installed native block.
    id_map['hazennstuff:wisewood_planks'] = 'irons_spellbooks:wisewood_planks'
    tag_map = {f'hazentouvelib:focus/{s}_focus': f'{NS}:hazen/{s}_focus' for s in ('cosmic', 'radiance', 'shadow')}
    tag_map.update({'hazennstuff:items/hydro_runes': f'{NS}:hazen/hydro_runes', 'hazennstuff:staves/artificer_cane': f'{NS}:hazen/artificer_cane'})
    tag_map['aces_spell_utils:hydro_focus'] = f'{NS}:hazen/hydro_focus'
    def remap(obj):
        if isinstance(obj, str): return id_map.get(obj, tag_map.get(obj, obj))
        if isinstance(obj, list): return [remap(v) for v in obj]
        if isinstance(obj, dict): return {k: remap(v) for k, v in obj.items()}
        return obj
    for key, row in materials.items():
        namespace, mid = key.split(':')
        d = model_asset(namespace, 'item/' + mid)
        write_json(RES / f'assets/{NS}/models/item/hazen_{mid}.json', d)
    for school in ('cosmic', 'radiance', 'shadow'):
        focus = json.loads(jars['hazennstuff'].read(f'data/hazentouvelib/tags/item/focus/{school}_focus.json'))
        write_json(RES / f'data/{NS}/tags/items/hazen/{school}_focus.json', remap(focus))
    hydro_key = 'data/aces_spell_utils/tags/item/hydro_focus.json'
    hydro = {'replace': False, 'values': []}
    for namespace in ('aces_spell_utils', 'hazennstuff'):
        if hydro_key in jars[namespace].namelist():
            for value in json.loads(jars[namespace].read(hydro_key)).get('values', []):
                if value not in hydro['values']: hydro['values'].append(value)
    write_json(RES / f'data/{NS}/tags/items/hazen/hydro_focus.json', remap(hydro))
    write_json(RES / f'data/{NS}/tags/items/hazen/hydro_runes.json', {'values': [f'{NS}:hazen_hydro_rune']})
    original_tag = json.loads(jars['hazennstuff'].read('data/hazennstuff/tags/item/staves/artificer_cane.json'))
    write_json(RES / f'data/{NS}/tags/items/hazen/artificer_cane.json', remap(original_tag))
    pending = []
    for original_path, data in recipes.items():
        d = remap(data)
        result = d['result']
        if isinstance(result, dict):
            result['item'] = result.pop('id', result.get('item'))
            result.pop('components', None)  # defaults are initialized by the native item, not dropped silently
        if d['type'] == 'minecraft:smithing_transform':
            d['type'] = f'{NS}:hazen_smithing'
        required_items = sorted(set(re.findall(r'"item":\s*"([^"]+)"', json.dumps(d))))
        result_id = result.get('item', '') if isinstance(result, dict) else result
        d['conditions'] = [{'type': f'{NS}:hazen_enabled', 'item': result_id.removeprefix(f'{NS}:hazen_')}]
        for item in required_items:
            d['conditions'].append({'type': 'forge:item_exists', 'item': item})
        dest = original_path.split('/recipe/', 1)[1]
        write_json(RES / f'data/{NS}/recipes/hazen' / dest, d)
        remaining = [x for x in re.findall(r'"item":\s*"([^"]+)"', json.dumps(d)) if x.startswith(('hazentouvelib:', 'aces_spell_utils:'))]
        if remaining: pending.append({'recipe': original_path, 'dependencies': sorted(set(remaining))})
    # A previous preparation included the redundant Hazen plank recipe.
    stale_planks = RES / f'data/{NS}/recipes/hazen/crafting/blocks/wisewood_planks.json'
    if stale_planks.exists() and stale_planks.resolve().is_relative_to(RES.resolve()): stale_planks.unlink()
    for school in ('cosmic', 'radiance', 'shadow', 'hydro', 'corrupt'):
        write_json(RES / f'data/{NS}/damage_type/hazen/{school}_magic.json', {
            'exhaustion': 0, 'message_id': 'hazen_' + school + '_magic', 'scaling': 'when_caused_by_living_non_player'})
        if school != 'corrupt':
            write_json(RES / f'data/{NS}/irons_spellbooks/upgrade_orb_type/hazen/{school}_power.json', {
                'amount': .05, 'attribute': f'{NS}:hazen_{school}_spell_power',
                'containerItem': {'Count': 1, 'id': f'{NS}:hazen_{school}_upgrade_orb'}, 'operation': 'MULTIPLY_BASE'})
    write_json(RES / f'data/{NS}/tags/items/hazen/harmonious_equipment.json', {'values': [{'id': 'hazennstuff:pendant_of_harmony', 'required': False}]})
    for tag in ('bypasses_armor', 'bypasses_effects', 'bypasses_enchantments', 'bypasses_resistance', 'bypasses_shield', 'no_knockback'):
        dest = RES / f'data/minecraft/tags/damage_type/{tag}.json'
        d = json.loads(read(dest)) if dest.exists() else {'replace': False, 'values': []}
        value = f'{NS}:hazen/corrupt_magic'
        if value not in d['values']: d['values'].append(value)
        write_json(dest, d)
    write_json(RES / 'hazen-materials.json', list(materials.values()))
    # Better Combat profiles are optional data, with the original hitboxes,
    # combo animations, two-handed flags and multipliers retained.
    combat_profiles = []
    for spec in specs:
        original_path = f'data/hazennstuff/weapon_attributes/{spec["id"]}.json'
        if original_path not in jars['hazennstuff'].namelist(): continue
        profile = json.loads(jars['hazennstuff'].read(original_path))
        def combat_remap(obj):
            if isinstance(obj, str) and obj.startswith('hazennstuff:'):
                suffix = obj.split(':', 1)[1]
                return f'{NS}:hazen_{suffix}'
            if isinstance(obj, dict): return {k: combat_remap(v) for k, v in obj.items()}
            if isinstance(obj, list): return [combat_remap(v) for v in obj]
            return obj
        write_json(RES / f'data/{NS}/weapon_attributes/hazen_{spec["id"]}.json', combat_remap(profile))
        combat_profiles.append(original_path)
    for effect in ('chaos_state', 'harmonized'):
        source = f'assets/hazennstuff/textures/mob_effect/{effect}.png'
        for suffix in ('', '.mcmeta'):
            if source + suffix in jars['hazennstuff'].namelist():
                raw = jars['hazennstuff'].read(source + suffix)
                target = RES / f'assets/{NS}/textures/mob_effect/hazen_{effect}.png{suffix}'
                target.parent.mkdir(parents=True, exist_ok=True); target.write_bytes(raw)
                receipts.append({'archive': inputs['hazennstuff'], 'original': source + suffix,
                                 'target': str(target.relative_to(PROJECT)), 'sha256': hashlib.sha256(raw).hexdigest()})
    write_json(OUT / 'spell-asset-receipts.json', receipts)
    write_json(OUT / 'recipe-map.json', {'id_map': id_map, 'tag_map': tag_map, 'original_recipes': list(recipes), 'unresolved_libraries': pending,
                                      'weapons_without_original_recipe': sorted(weapon_ids - {r.split(':')[-1] for _, _, r, _ in original_recipes})})
    for lang in ('en_us', 'zh_cn'):
        dest = RES / f'assets/{NS}/lang/{lang}.json'; existing = json.loads(read(dest))
        original_langs = {}
        for namespace, jar in jars.items():
            langs = {}
            for l in ('en_us', lang):
                key = f'assets/{namespace}/lang/{l}.json'
                if key in jar.namelist(): langs.update(json.loads(jar.read(key)))
            original_langs[namespace] = langs
        for key, row in materials.items():
            namespace, mid = key.split(':')
            existing[f'item.{NS}.hazen_{mid}'] = original_langs[namespace].get(f'item.{namespace}.{mid}', mid)
        for spell in SPELLS:
            for prefix in ('spell', 'spell_description'):
                old = f'{prefix}.hazennstuff.{spell}'
                if old in original_langs['hazennstuff']: existing[f'{prefix}.{NS}.hazen_{spell}'] = original_langs['hazennstuff'][old]
        for school in ('cosmic', 'radiance', 'shadow', 'hydro'):
            oldns = 'hazentouvelib' if school != 'hydro' else 'aces_spell_utils'
            existing[f'school.{NS}.hazen_{school}'] = original_langs[oldns].get(f'school.{oldns}.{school}', school)
            for suffix in ('spell_power', 'magic_resist'):
                old_key = f'attribute.{oldns}.{school}_{suffix}'
                existing[f'attribute.{NS}.hazen_{school}_{suffix}'] = original_langs[oldns].get(old_key,
                        (existing[f'school.{NS}.hazen_{school}'] + (' 法术强度' if suffix == 'spell_power' else ' 法术抗性')) if lang == 'zh_cn'
                        else school.title() + (' Spell Power' if suffix == 'spell_power' else ' Magic Resistance'))
        for name, chinese in {'mana_rend':'法力撕裂', 'spell_res_penetration':'法术抗性穿透',
                              'magic_projectile_crit_chance':'法术投射物暴击率', 'armor_pierce':'护甲穿透', 'crit_damage':'暴击伤害'}.items():
            existing[f'attribute.{NS}.hazen_{name}'] = chinese if lang == 'zh_cn' else original_langs['aces_spell_utils'].get(f'attribute.aces_spell_utils.{name}', name.replace('_',' ').title())
        existing[f'tooltip.{NS}.hazen_chaos'] = '混沌状态下再次传送会损失最大生命值的 50%' if lang == 'zh_cn' else 'Teleporting during Chaos State costs 50% of maximum health'
        for effect in ('chaos_state', 'harmonized'):
            existing[f'effect.{NS}.hazen_{effect}'] = original_langs['hazennstuff'].get(f'effect.hazennstuff.{effect}', effect.replace('_',' ').title())
        existing[f'tooltip.{NS}.hazen_disabled'] = '该内容已在 Hazen 配置中禁用' if lang == 'zh_cn' else 'Disabled in Hazen configuration'
        write_json(dest, existing)
    for jar in jars.values(): jar.close()
    print(json.dumps({'visuals': len(visuals), 'materials': len(materials), 'recipes': len(recipes), 'assets': len(receipts), 'unresolved_libraries': pending}, ensure_ascii=False, indent=2))


if __name__ == '__main__': main()
