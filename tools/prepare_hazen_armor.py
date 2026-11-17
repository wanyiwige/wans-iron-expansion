"""Read five evidenced sets and reuse their original assets in native armor."""
from pathlib import Path
import hashlib
import json
import re
import zipfile
from audit_hazen_recreation import PROJECT, PACKAGE, OUT, read

RES = PROJECT / 'src/main/resources'
NS = 'alex_caves_spellsbook'
SETS = {
    'cosmic_scholar': ('SchoolTier/CosmicScholar/GeckolibCosmicScholarArmorModel.java', 'cosmic', 'COSMIC_RARITY', 32),
    'radiant_crystal': ('SchoolTier/RadiantCrystal/GeckolibRadiantCrystalArmorModel.java', 'radiance', 'RADIANCE_RARITY', 32),
    'nautilus_knight': ('SchoolTier/NautilusKnight/NautilusKnightArmorModel.java', 'hydro', 'RADIANCE_RARITY', 32),
    'frostbite_knight': ('Melee/FrostbiteKnight/FrostbiteKnightArmorModel.java', None, 'ICE_RARITY', 64),
    'dreadsteel_knight': ('Melee/DreadsteelKnight/DreadsteelArmorModel.java', None, 'EPIC', 64),
}


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def main():
    inventory = json.loads(read(OUT / 'original-inventory.json'))
    jar_path = Path(inventory['original_jar'])
    assert hashlib.sha256(jar_path.read_bytes()).hexdigest() == inventory['original_sha256']
    receipts, specs, aliases = [], [], {}
    # Remove only this script's earlier explicit-path staging, after hash verification.
    old_receipts = OUT / 'armor-asset-receipts.json'
    if old_receipts.is_file():
        for row in json.loads(read(old_receipts)):
            if f'assets/{NS}/hazen/armor/' not in row['target'].replace('\\', '/'):
                continue
            path = PROJECT / row['target']
            if path.is_file():
                assert hashlib.sha256(path.read_bytes()).hexdigest() == row['sha256']
                path.unlink()
    mapping = dict(json.loads(read(OUT / 'recipe-map.json'))['id_map'])
    parts = ('helmet', 'chestplate', 'leggings', 'boots')
    with zipfile.ZipFile(jar_path) as jar:
        def copy(original, target):
            raw = jar.read(original)
            path = RES / target
            path.parent.mkdir(parents=True, exist_ok=True); path.write_bytes(raw)
            receipts.append(dict(archive=str(jar_path), original=original, target=str(path.relative_to(PROJECT)), sha256=hashlib.sha256(raw).hexdigest()))
        for name, (model_file, school, rarity, durability) in SETS.items():
            source = read(PACKAGE / 'Item/Armor' / model_file)
            geom = re.search(r'"(geo/[^\"]+)"', source)[1]
            texture = re.search(r'"(textures/[^\"]+)"', source)[1]
            geo_target = 'geo/hazen/armor/' + geom.split('/')[-1]
            texture_target = 'textures/hazen/armor/worn/' + texture.split('/')[-1]
            copy('assets/hazennstuff/' + geom, f'assets/{NS}/{geo_target}')
            copy('assets/hazennstuff/' + texture, f'assets/{NS}/{texture_target}')
            spec = dict(id=name, school=school, rarity=rarity, durability_multiplier=durability,
                        model=f'{NS}:{geo_target}', texture=f'{NS}:{texture_target}',
                        animation='irons_spellbooks:animations/wizard_armor_animation.json',
                        source_model=model_file, source_model_sha256=hashlib.sha256(source.encode()).hexdigest())
            specs.append(spec)
            for part in parts:
                ident = name + '_' + part
                mapping['hazennstuff:' + ident] = f'{NS}:hazen_{ident}'
                aliases[ident] = ident
                if name in ('cosmic_scholar', 'radiant_crystal'):
                    mapping['hazennstuff:geckolib_' + ident] = f'{NS}:hazen_{ident}'
                    aliases['geckolib_' + ident] = ident
                model = json.loads(jar.read(f'assets/hazennstuff/models/item/{ident}.json'))
                for key, value in model.get('textures', {}).items():
                    assert value.startswith('hazennstuff:')
                    path = value.split(':', 1)[1]
                    copy(f'assets/hazennstuff/textures/{path}.png', f'assets/{NS}/textures/hazen/armor/{path}.png')
                    model['textures'][key] = f'{NS}:hazen/armor/{path}'
                write(RES / f'assets/{NS}/models/item/hazen_{ident}.json', model)
        # One missing original ingredient is needed by the melee armor recipes.
        mapping['hazennstuff:melee_rune'] = f'{NS}:hazen_melee_rune'
        materials = json.loads(read(RES / 'hazen-materials.json'))
        if not any(i['id'] == 'melee_rune' for i in materials):
            materials.append(dict(id='melee_rune', kind='material', rarity='COMMON', fire_resistant=False))
        write(RES / 'hazen-materials.json', materials)
        model = json.loads(jar.read('assets/hazennstuff/models/item/melee_rune.json'))
        for key, value in model['textures'].items():
            path = value.split(':', 1)[1]
            copy(f'assets/hazennstuff/textures/{path}.png', f'assets/{NS}/textures/hazen/armor/{path}.png')
            model['textures'][key] = f'{NS}:hazen/armor/{path}'
        write(RES / f'assets/{NS}/models/item/hazen_melee_rune.json', model)
        def remap(value):
            if isinstance(value, str): return mapping.get(value, value)
            if isinstance(value, list): return [remap(v) for v in value]
            if isinstance(value, dict): return {k: remap(v) for k, v in value.items()}
            return value
        recipe_receipts = []
        for path in sorted(jar.namelist()):
            if '/recipe/' not in path or not path.endswith('.json'): continue
            data = json.loads(jar.read(path))
            result = data.get('result', {})
            original_result = result.get('id') if isinstance(result, dict) else result
            if original_result not in {f'hazennstuff:{s}_{part}' for s in SETS for part in parts} | {'hazennstuff:melee_rune'}: continue
            result_short = original_result.split(':')[1]
            data['result'] = dict(item=mapping[original_result], count=result.get('count', 1))
            if data['type'] == 'minecraft:smithing_transform': data['type'] = f'{NS}:hazen_smithing'
            # Only the result is changed structurally; source ingredient tags are remapped explicitly.
            data = remap(data)
            for value in ['hazennstuff:items/hydro_runes', 'hazennstuff:focus/melee_focus']:
                new_tag = f'{NS}:hazen/' + value.split(':')[1]
                def tags(obj):
                    if isinstance(obj, dict):
                        return {k: new_tag if k == 'tag' and v == value else tags(v) for k,v in obj.items()}
                    if isinstance(obj, list): return [tags(v) for v in obj]
                    return obj
                data = tags(data)
            required = set()
            def dependencies(obj):
                if isinstance(obj, dict):
                    for k, v in obj.items():
                        if k == 'item' and isinstance(v, str): required.add(v)
                        else: dependencies(v)
                elif isinstance(obj, list):
                    for v in obj: dependencies(v)
            dependencies(data)
            data['conditions'] = [dict(type='forge:item_exists', item=i) for i in sorted(required)] + [dict(type=f'{NS}:hazen_enabled', item=result_short)]
            dest = RES / f'data/{NS}/recipes/hazen/armor_batch1/{result_short}.json'
            write(dest, data)
            recipe_receipts.append(dict(original=path, target=str(dest.relative_to(PROJECT))))
        assert len(recipe_receipts) == 21, len(recipe_receipts)
        for tag in ('items/hydro_runes', 'focus/melee_focus'):
            data = json.loads(jar.read('data/hazennstuff/tags/item/' + tag + '.json'))
            data = remap(data)
            write(RES / f'data/{NS}/tags/items/hazen/{tag}.json', data)
        # Original equip sound and suite-effect icon, with no additional library classes.
        sounds = json.loads(read(RES / f'assets/{NS}/sounds.json'))
        source_sound = json.loads(jar.read('assets/hazennstuff/sounds.json'))['cryogenic_ruler_equip']
        for i, value in enumerate(source_sound['sounds']):
            sound_path = value.split(':')[1]
            copy(f'assets/hazennstuff/sounds/{sound_path}.ogg', f'assets/{NS}/sounds/hazen/armor/{sound_path}.ogg')
            source_sound['sounds'][i] = f'{NS}:hazen/armor/{sound_path}'
        sounds['hazen_cryogenic_ruler_equip'] = source_sound
        write(RES / f'assets/{NS}/sounds.json', sounds)
        sound_ids = json.loads(read(RES / 'hazen-sound-ids.json'))
        if 'cryogenic_ruler_equip' not in sound_ids: sound_ids.append('cryogenic_ruler_equip')
        write(RES / 'hazen-sound-ids.json', sound_ids)
        copy('assets/hazennstuff/textures/mob_effect/swordmaster_set_bonus.png', f'assets/{NS}/textures/mob_effect/hazen_swordmaster_set_bonus.png')
        for lang in ('en_us', 'zh_cn'):
            source = json.loads(jar.read(f'assets/hazennstuff/lang/{lang}.json'))
            path = RES / f'assets/{NS}/lang/{lang}.json'; target = json.loads(read(path))
            for ident in [f'{s}_{part}' for s in SETS for part in parts] + ['melee_rune']:
                key = 'item.hazennstuff.' + ident
                target[f'item.{NS}.hazen_{ident}'] = source.get(key, ident)
            target[f'effect.{NS}.hazen_swordmaster_set_bonus'] = source.get('effect.hazennstuff.swordmaster_set_bonus', 'Swordmaster set bonus')
            target[f'ui.{NS}.hazen_armor_disabled'] = '此护甲已禁用' if lang == 'zh_cn' else 'This armor is disabled'
            target[f'ui.{NS}.hazen_armor_affinity'] = '穿戴时：冰霜噬咬等级 +1' if lang == 'zh_cn' else 'While equipped: Frostbite level +1'
            target[f'ui.{NS}.hazen_armor_set'] = '同套四件：攻击速度 +15%%' if lang == 'zh_cn' else 'Four matching pieces: +15%% attack speed'
            write(path, target)
        atlas_path = RES / 'assets/minecraft/atlases/blocks.json'
        atlas = json.loads(read(atlas_path))
        singles = {v.get('resource') for v in atlas['sources'] if v['type'] in ('single', 'minecraft:single')}
        for model_path in (RES / f'assets/{NS}/models/item').glob('hazen_*.json'):
            for resource in json.loads(read(model_path)).get('textures', {}).values():
                if resource.startswith(f'{NS}:hazen/armor/') and resource not in singles:
                    atlas['sources'].append(dict(type='minecraft:single', resource=resource)); singles.add(resource)
        write(atlas_path, atlas)
    write(RES / 'hazen-armor-sets.json', specs)
    write(OUT / 'armor-asset-receipts.json', receipts)
    write(OUT / 'armor-recreation-map.json', dict(sets=list(SETS), original_aliases=aliases, recipes=recipe_receipts, actual_item_count=20, data_source_sha256=inventory['original_sha256']))
    print(json.dumps(dict(sets=len(specs), armor_items=20, recipes=len(recipe_receipts), exact_assets=len(receipts)), indent=2))


if __name__ == '__main__': main()
