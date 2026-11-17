"""Check native-module ownership, exact assets and the two texture-path contracts."""
from pathlib import Path
import argparse
import hashlib
import json
import re
import struct
import zipfile

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
RECORDS = ROOT / 'development-records/hazen-recreation'
NS = 'alex_caves_spellsbook'


def read_json(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--jar', type=Path)
    args = parser.parse_args()
    errors = []
    expected = {row['id'] for row in read_json(RECORDS / 'original-inventory.json')['weapons'] if row['new'] and not row['excluded']}
    weapons = read_json(RES / 'hazen-recreated-weapons.json')
    if {w['id'] for w in weapons} != expected: errors.append('Original weapon coverage mismatch')
    if len(weapons) != 47: errors.append('Expected 47 non-mace additions')
    receipts = read_json(RECORDS / 'asset-receipts.json') + read_json(RECORDS / 'spell-asset-receipts.json')
    if (RES / 'hazen-armor-sets.json').is_file() and (RECORDS / 'armor-asset-receipts.json').is_file():
        receipts += read_json(RECORDS / 'armor-asset-receipts.json')
    secondary=read_json(RES/'hazen-secondary-spells.json') if (RES/'hazen-secondary-spells.json').is_file() else []
    if secondary:receipts+=read_json(RECORDS/'secondary-asset-receipts.json')
    for row in receipts:
        dest = ROOT / row['target']
        if not dest.is_file() or hashlib.sha256(dest.read_bytes()).hexdigest() != row['sha256']:
            errors.append('Original asset hash mismatch: ' + row['target'])

    def explicit(resource):
        namespace, path = resource.split(':', 1)
        if namespace == NS and not (RES / 'assets' / namespace / path).is_file():
            errors.append('Missing explicit resource: ' + resource)
    armor_sets = read_json(RES / 'hazen-armor-sets.json') if (RES / 'hazen-armor-sets.json').is_file() else []
    for spec in armor_sets:
        if not spec['model'].split(':', 1)[1].startswith('geo/'):
            errors.append('GeckoLib cannot discover armor outside geo/: ' + spec['id'])
        explicit(spec['model']); explicit(spec['texture'])
    if secondary:
        if len(secondary)!=31:errors.append('Missing original secondary spell definitions')
        for spec in secondary:
            explicit(NS+':textures/gui/spell_icons/hazen_'+spec['id']+'.png')
        for spec in read_json(RES/'hazen-secondary-projectiles.json').values():
            if spec['model']:
                if not spec['model'].split(':',1)[1].startswith('geo/'):errors.append('Secondary model outside Gecko discovery: '+spec['kind'])
                explicit(spec['model']);explicit(spec['animation'])
                animation=RES/f'assets/{NS}'/spec['animation'].split(':',1)[1]
                if spec['animation_name'] not in read_json(animation)['animations']:errors.append('Absent secondary animation: '+spec['kind'])
            for texture in spec['textures']:explicit(texture)
    for w in weapons:
        if not w['model'].split(':', 1)[1].startswith('geo/'):
            errors.append('GeckoLib cannot discover weapon geometry outside geo/: ' + w['id'])
        if w['animation'] and not w['animation'].split(':', 1)[1].startswith('animations/'):
            errors.append('GeckoLib cannot discover animation outside animations/: ' + w['id'])
        explicit(w['model']); explicit(w['texture'])
        if w['animation']: explicit(w['animation'])
        model = read_json(RES / f'assets/{NS}/models/item/hazen_{w["id"]}.json')
        if model.get('parent') != 'builtin/entity': errors.append('GeckoLib model lacks builtin/entity: ' + w['id'])
    models = list((RES / f'assets/{NS}/models/item').glob('hazen_*.json')) + list((RES / f'assets/{NS}/models/item').glob('scroll_hazen_*.json'))
    atlas = read_json(RES / 'assets/minecraft/atlases/blocks.json')
    atlas_singles = {source['resource'] for source in atlas['sources']
                     if source['type'] in ('single', 'minecraft:single')}
    for path in models:
        model = read_json(path)
        if model.get('loader', '').startswith('neoforge:') or 'neoforge_data' in model:
            errors.append('NeoForge-only item model contract: ' + path.name)
        for value in model.get('textures', {}).values():
            if value.startswith('#'): continue
            namespace, texture = value.split(':', 1) if ':' in value else ('minecraft', value)
            if namespace == NS and not (RES / f'assets/{namespace}/textures/{texture}.png').is_file():
                errors.append(f'Missing baked texture: {path.name} -> {value}')
            if namespace == NS and texture.startswith('hazen/') and value not in atlas_singles:
                errors.append(f'Baked texture outside blocks atlas: {path.name} -> {value}')
    for visual in read_json(RES / 'hazen-spell-visuals.json').values():
        if not visual['model'].split(':', 1)[1].startswith('geo/') or not visual['animation'].split(':', 1)[1].startswith('animations/'):
            errors.append('GeckoLib discovery root mismatch: ' + visual['model'])
        explicit(visual['model']); explicit(visual['animation'])
        for texture in visual['textures'] + visual.get('glow_textures', []): explicit(texture)
        if visual['animation'].startswith(NS + ':'):
            path = RES / f'assets/{NS}' / visual['animation'].split(':', 1)[1]
            if visual['animation_name'] not in read_json(path)['animations']:
                errors.append('Missing animation ' + visual['animation_name'])
    for path in (RES / f'assets/{NS}/particles').glob('hazen_*.json'):
        for value in read_json(path)['textures']:
            namespace, texture = value.split(':', 1)
            if namespace == NS and not (RES / f'assets/{namespace}/textures/particle/{texture}.png').is_file():
                errors.append('Missing particle atlas texture: ' + value)
    recipes = list((RES / f'data/{NS}/recipes/hazen').rglob('*.json'))
    for path in recipes:
        recipe = read_json(path)
        if recipe['type'] in ('minecraft:smelting','minecraft:blasting','minecraft:smoking','minecraft:campfire_cooking') and not isinstance(recipe['result'],str):
            errors.append('Forge 1.20.1 furnace result must be a string: '+str(path))
        if '/recipe/' in str(path): errors.append('Wrong 1.21 recipe directory: ' + str(path))
        for condition in recipe.get('conditions', []):
            if condition['type'] == 'forge:item_exists' and ':' not in condition['item']:
                errors.append('Unqualified item-exists condition: ' + str(path))
        if isinstance(recipe.get('result'), dict) and ('components' in recipe['result'] or 'id' in recipe['result']):
            errors.append('1.21 result components leaked: ' + str(path))
    for path in (ROOT / 'src/main/java/cn/wanyiwige/alexcavesspellsbook/hazen').rglob('*.java'):
        if 'import net.neoforged.' in path.read_text(encoding='utf-8'):
            errors.append('NeoForge API leaked into native implementation: ' + str(path))
    result = {'weapon_count': len(weapons), 'school_count': 4, 'exclusive_spell_count': 7,
              'secondary_spell_count':len(secondary),'total_recreated_spell_count':7+len(secondary),
              'armor_set_count': len(armor_sets), 'armor_item_count': len(armor_sets) * 4,
              'material_count': len(read_json(RES / 'hazen-materials.json')), 'recipe_count': len(recipes),
              'exact_asset_receipts': len(receipts), 'errors': errors,
              'runtime_acceptance': 'not complete: standard GameTest startup blocked by external Iron Lib HTTP/Unix-domain loopback failure'}
    if args.jar:
        with zipfile.ZipFile(args.jar) as jar:
            names = set(jar.namelist())
            for row in receipts:
                entry = str(Path(row['target']).relative_to('src/main/resources')).replace('\\', '/')
                if entry not in names or hashlib.sha256(jar.read(entry)).hexdigest() != row['sha256']:
                    errors.append('Packaged asset hash mismatch: ' + entry)
            new_classes = [name for name in names if name.startswith('cn/wanyiwige/alexcavesspellsbook/hazen/') and name.endswith('.class')]
            if not new_classes: errors.append('Native classes missing from release JAR')
            for name in new_classes:
                raw = jar.read(name)
                if struct.unpack('>H', raw[6:8])[0] != 61: errors.append('Native class is not Java 17: ' + name)
                if b'net/neoforged/' in raw: errors.append('Packaged NeoForge dependency: ' + name)
            if any(name.startswith(('net/hazen/hazentouvelib/', 'net/acetheeldritchking/aces_spell_utils/')) for name in names):
                errors.append('Old backport library classes bundled')
            if any('cn/wanyiwige/hazenqa/' in name for name in names): errors.append('QA fixture leaked into production JAR')
            result['native_class_count'] = len(new_classes)
        result['jar'] = str(args.jar.resolve())
        result['jar_sha256'] = hashlib.sha256(args.jar.read_bytes()).hexdigest()
        result['jar_bytes'] = args.jar.stat().st_size
    for log_name in ('survival-native-verified.log','secondary-native-checks.log', 'armor-native-checks.log', 'native-final-checks.log', 'native-checks-materials.log', 'native-checks-configured.log'):
        log = RECORDS / log_name
        if log.is_file():
            passes = re.findall(r'HAZEN_NATIVE_EARLY_PASS (\w+)', log.read_text(encoding='utf-8', errors='replace'))
            if passes:
                result['native_startup_assertions'] = passes
                result['native_startup_assertion_log'] = log_name
                result['native_startup_assertions_completed'] = ('HAZEN_NATIVE_EARLY_CHECKS_COMPLETE ' + str(len(passes))) in log.read_text(encoding='utf-8', errors='replace')
                result['native_startup_assertion_scope'] = 'synchronous assertions in a real isolated Forge server world; not a completed scheduled GameTest run'
                result['secondary_callback_count']=len(re.findall(r'HAZEN_SECONDARY_CALLBACK_PASS ',log.read_text(encoding='utf-8',errors='replace')))
                result['secondary_collision_kinds']=re.findall(r'HAZEN_SECONDARY_COLLISION_PASS (\w+)',log.read_text(encoding='utf-8',errors='replace'))
                break
    for name, marker in [('armor-gecko-bake.log', 'PASS GECKO_ASSET_BAKE'),
                         ('armor-atlas-discovery.log', 'PASS ATLAS_DISCOVERY'),
                         ('secondary-gecko-bake.log','PASS GECKO_ASSET_BAKE'),('secondary-atlas-discovery.log','PASS ATLAS_DISCOVERY'),('survival-atlas-discovery.log','PASS ATLAS_DISCOVERY')]:
        path = RECORDS / name
        if armor_sets and path.is_file():
            lines = path.read_text(encoding='utf-8', errors='replace').splitlines()
            result.setdefault('native_asset_checks', {})[name] = next((line for line in lines if line.startswith(marker)), 'NOT PASSED')
    (RECORDS / 'verification.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, ensure_ascii=False, indent=2))
    if errors: raise SystemExit(1)


if __name__ == '__main__': main()
