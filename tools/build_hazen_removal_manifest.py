from pathlib import Path
import hashlib
import json
import zipfile

ROOT=Path(__file__).resolve().parents[1]
RECORDS=ROOT/'development-records/hazen-recreation'
NS='alex_caves_spellsbook'
patterns=[
    'src/main/java/cn/wanyiwige/alexcavesspellsbook/hazen/**/*.java',
    'src/main/resources/hazen-*.json',
    f'src/main/resources/assets/{NS}/hazen/**/*',
    f'src/main/resources/assets/{NS}/geo/hazen/**/*',
    f'src/main/resources/assets/{NS}/animations/hazen/**/*',
    f'src/main/resources/assets/{NS}/textures/hazen/**/*',
    f'src/main/resources/assets/{NS}/textures/particle/hazen/**/*',
    f'src/main/resources/assets/{NS}/sounds/hazen/**/*',
    f'src/main/resources/assets/{NS}/sounds.json',
    f'src/main/resources/assets/{NS}/models/item/hazen_*.json',
    f'src/main/resources/assets/{NS}/models/item/scroll_hazen_*.json',
    f'src/main/resources/assets/{NS}/models/block/hazen_*.json',
    f'src/main/resources/assets/{NS}/blockstates/hazen_*.json',
    f'src/main/resources/assets/{NS}/particles/hazen_*.json',
    f'src/main/resources/assets/{NS}/textures/gui/spell_icons/hazen_*.png',
    f'src/main/resources/assets/{NS}/textures/mob_effect/hazen_*',
    f'src/main/resources/assets/{NS}/player_animations/cast_animation.json',
    f'src/main/resources/data/{NS}/recipes/hazen/**/*.json',
    f'src/main/resources/data/{NS}/weapon_attributes/hazen_*.json',
    f'src/main/resources/data/{NS}/tags/items/hazen/**/*.json',
    f'src/main/resources/data/{NS}/damage_type/hazen/*.json',
    f'src/main/resources/data/{NS}/irons_spellbooks/upgrade_orb_type/hazen/*.json',
    f'src/main/resources/data/{NS}/worldgen/*/hazen/**/*.json',
    f'src/main/resources/data/{NS}/forge/biome_modifier/hazen/**/*.json',
    f'src/main/resources/data/{NS}/loot_tables/hazen/**/*.json',
    f'src/main/resources/data/{NS}/loot_tables/blocks/hazen_*.json',
    f'src/main/resources/data/{NS}/loot_modifiers/hazen/**/*.json',
    f'src/main/resources/data/{NS}/advancements/hazen/survival/**/*.json',
]
paths={p for pattern in patterns for p in ROOT.glob(pattern) if p.is_file()}
for p in (ROOT/'src/main/resources/data/minecraft/tags/damage_type').glob('*.json'):
    data=json.loads(p.read_text(encoding='utf-8'))
    if data.get('values')==[f'{NS}:hazen/corrupt_magic']: paths.add(p)
files=[{'path':p.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted(paths)]
hooks={
    'src/main/java/cn/wanyiwige/alexcavesspellsbook/AlexCavesSpellsbook.java':'        cn.wanyiwige.alexcavesspellsbook.hazen.HazenEquipment.register(context, modEventBus);\n',
    'src/main/java/cn/wanyiwige/alexcavesspellsbook/registry/ModCreativeTabs.java':'                cn.wanyiwige.alexcavesspellsbook.hazen.HazenEquipment.addWeapons(output);\n',
}
languages={}
original=ROOT/'development-backups/hazen-recreation-20261015-205400/original-files.zip'
with zipfile.ZipFile(original) as backup:
    for lang in ('zh_cn','en_us'):
        relative=f'src/main/resources/assets/{NS}/lang/{lang}.json'
        before=json.loads(backup.read(relative).decode('utf-8-sig'))
        after=json.loads((ROOT/relative).read_text(encoding='utf-8'))
        languages[relative]={k:v for k,v in after.items() if k not in before}
manifest={'added_files':files,'integration_hooks':hooks,'language_additions':languages,
          'original_backup':str(original.relative_to(ROOT))}
atlas_relative='src/main/resources/assets/minecraft/atlases/blocks.json'
atlas=json.loads((ROOT/atlas_relative).read_text(encoding='utf-8'))
manifest['atlas_additions']={atlas_relative:[s for s in atlas['sources'] if s.get('resource','').startswith(NS+':hazen/')]}
shared_lists={}
for p in list((ROOT/'src/main/resources/data/minecraft/tags').rglob('*.json'))+list((ROOT/'src/main/resources/data/forge/tags').rglob('*.json')):
    data=json.loads(p.read_text(encoding='utf-8'))
    values=[v for v in data.get('values',[]) if isinstance(v,str) and v.startswith(NS+':hazen_')]
    if values:shared_lists[p.relative_to(ROOT).as_posix()]={'key':'values','entries':values}
global_path=ROOT/'src/main/resources/data/forge/loot_modifiers/global_loot_modifiers.json'
if global_path.exists():
    data=json.loads(global_path.read_text(encoding='utf-8'))
    shared_lists[global_path.relative_to(ROOT).as_posix()]={'key':'entries','entries':[v for v in data.get('entries',[]) if v.startswith(NS+':hazen/survival/')]}
manifest['shared_list_additions']=shared_lists
(RECORDS/'removal-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Managed production files:',len(files))
