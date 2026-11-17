"""Verify the three approved equipment items, freeze the release and deploy with rollback."""
from pathlib import Path
from datetime import datetime,timezone,timedelta
import base64,hashlib,json,os,shutil,tomllib,zipfile
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
MODS=ROOT.parent/'mods'
CHECK=ROOT/'development-checks/radiation-equipment-20261022'
ART=ROOT/'artwork/radiation-equipment-release-20261022'
OUT=ROOT/'outputs/radiation-equipment-20261022'
VERSION='1.2.6-radiation-equipment.1'
OLD_HASH='2329df04af2342e078ce703f0e90756c0781ad95e5855b5dd68cca495f7d2822'
old=MODS/'wans_iron_expansion-1.2.5-radiation-area.1.jar'
built=ROOT/f'build/libs/wans_iron_expansion-{VERSION}.jar'
harness=ROOT/f'build/libs/radiation-equipment-verification-{VERSION}.jar'
IDS=('mechanical_radiation_eye','radiation_grimoire','fission_staff')
PREFIX='cn/wanyiwige/alexcavesspellsbook/toxic/'

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def matching():
    found=[]
    for path in MODS.glob('*.jar'):
        try:
            with zipfile.ZipFile(path) as z:meta=tomllib.loads(z.read('META-INF/mods.toml').decode('utf-8-sig'))
        except (KeyError,ValueError,zipfile.BadZipFile):continue
        if any(m['modId']=='alex_caves_spellsbook' for m in meta['mods']):found.append(path)
    return found

assert matching()==[old] and sha(old)==OLD_HASH
log=(CHECK/'build-and-checks.log').read_text(encoding='utf-8-sig')
for marker in ('BUILD SUCCESSFUL',':reobfJar','PASS RADIATION_EQUIPMENT_MODELS cubes=200 faces=1200 layers=1','expected=14 discovered=14 missing=0'):
    assert marker in log,marker
runtime=(CHECK/'isolated-server/console.log').read_text(encoding='utf-8')
assert 'RADIATION_EQUIPMENT_AUDIT_PASS checks=71' in runtime and 'RADIATION_EQUIPMENT_AUDIT_FAILED' not in runtime

changed_allowed={'META-INF/MANIFEST.MF','META-INF/mods.toml',PREFIX+'ToxicRegistry.class',
    'cn/wanyiwige/alexcavesspellsbook/registry/ModCreativeTabs.class',
    'data/curios/tags/items/spellbook.json','data/irons_spellbooks/tags/items/staff.json'}
changed_allowed|={f'assets/alex_caves_spellsbook/lang/{lang}.json' for lang in ('zh_cn','en_us')}
added_expected={PREFIX+'RadiationEquipment'+suffix+'.class' for suffix in ('','$Focus','$FissionStaff')}
for item in IDS:
    added_expected|={f'assets/alex_caves_spellsbook/models/item/{item}.json',f'assets/alex_caves_spellsbook/textures/item/{item}.png',f'data/alex_caves_spellsbook/recipes/{item}.json'}
with zipfile.ZipFile(old) as before,zipfile.ZipFile(built) as after,zipfile.ZipFile(harness) as tested:
    assert after.testzip() is None
    previous=set(before.namelist());names=set(after.namelist());assert previous<=names
    assert names-previous==added_expected,names-previous
    changed={n for n in previous if before.read(n)!=after.read(n)}
    assert changed==changed_allowed,changed
    assert not any(any(word in n for word in ('Harness','Audit','OfflineListenerMixin','OfflinePatreonMixin','EquipmentModelCheck','EquipmentAtlasCheck')) for n in names)
    meta=tomllib.loads(after.read('META-INF/mods.toml').decode('utf-8-sig'))
    assert [(m['modId'],m['version']) for m in meta['mods']]==[('alex_caves_spellsbook',VERSION)]
    for stem in ('RadiationEquipment','RadiationEquipment$Focus','RadiationEquipment$FissionStaff','ToxicRegistry','ToxicSpell'):
        name=PREFIX+stem+'.class';assert after.read(name)==tested.read(name),stem
    for lang in ('zh_cn','en_us'):
        path=f'assets/alex_caves_spellsbook/lang/{lang}.json'
        a=json.loads(before.read(path));b=json.loads(after.read(path))
        assert all(b.get(k)==v for k,v in a.items())
        assert set(b)-set(a)=={'item.alex_caves_spellsbook.'+item for item in IDS}
    for tag,new_ids in [('data/curios/tags/items/spellbook.json',IDS[:2]),('data/irons_spellbooks/tags/items/staff.json',IDS[2:])]:
        a=json.loads(before.read(tag))['values'];b=json.loads(after.read(tag))['values']
        assert set(b)-set(a)=={'alex_caves_spellsbook:'+item for item in new_ids} and set(a)<=set(b)

rows=json.loads((ART/'texture-export.json').read_text(encoding='utf-8-sig'))
for row in rows:
    native=Image.open(ROOT/row['source']).convert('RGBA')
    path=ROOT/f'src/main/resources/assets/alex_caves_spellsbook/textures/item/{row["id"]}.png'
    game=Image.open(path).convert('RGBA');ox,oy=row['offset']
    assert game.size==(row['textureSize'],row['textureSize'])
    assert game.crop((ox,oy,ox+native.width,oy+native.height)).tobytes()==native.tobytes()
    assert set(game.getchannel('A').getdata())=={0,255}
model=json.loads((ROOT/'src/main/resources/assets/alex_caves_spellsbook/models/item/fission_staff.json').read_text(encoding='utf-8'))
project=json.loads((ART/'fission_staff_single_layer.bbmodel').read_text(encoding='utf-8'))
assert 'parent' not in model and len(model['elements'])==len(project['elements'])==200
assert all(len(e['faces'])==6 and abs(e['to'][2]-e['from'][2]-16/37)<1e-6 for e in model['elements'])
assert all(a['from']==b['from'] and a['to']==b['to'] for a,b in zip(model['elements'],project['elements']))
texture=ROOT/'src/main/resources/assets/alex_caves_spellsbook/textures/item/fission_staff.png'
assert base64.b64decode(project['textures'][0]['source'].split(',',1)[1])==texture.read_bytes()

OUT.mkdir(parents=True,exist_ok=True);source=OUT/built.name;assert not source.exists()
shutil.copy2(built,source);new_hash=sha(source);assert new_hash==sha(built)
shutil.copy2(CHECK/'build-and-checks.log',OUT/'build-and-checks.log')
shutil.copy2(CHECK/'isolated-server/console.log',OUT/'equipment-check.log')
shutil.copy2(ART/'fission_staff_single_layer.bbmodel',OUT/'fission_staff_single_layer.bbmodel')
stamp=datetime.now(timezone(timedelta(hours=8))).strftime('%Y%m%d-%H%M%S')
backup_dir=ROOT/'deployment-backups'/('radiation-equipment-'+stamp);backup_dir.mkdir(parents=True,exist_ok=False)
backup=backup_dir/old.name;shutil.copy2(old,backup);assert sha(backup)==OLD_HASH
target=MODS/source.name;stage=MODS/(source.name+'.stage');assert not target.exists() and not stage.exists()
shutil.copy2(source,stage);assert sha(stage)==new_hash and sha(old)==OLD_HASH
parked=backup_dir/(old.name+'.parked')
try:
    os.replace(old,parked);os.replace(stage,target)
    assert sha(target)==new_hash and matching()==[target]
except Exception:
    if target.exists() and sha(target)==new_hash:target.unlink()
    if parked.exists() and not old.exists():os.replace(parked,old)
    raise
receipt=dict(version=VERSION,active=str(target),sha256=new_hash,bytes=target.stat().st_size,rollback_backup=str(backup),
             previous_sha256=OLD_HASH,backup_verified=True,one_active_same_mod_id_jar=True,
             added_entries=sorted(added_expected),changed_entries=sorted(changed),jar_crc='pass',
             isolated_world_checks=71,atlas='14/14',staff_model=dict(cubes=200,faces=1200,layers=1),
             tested_equipment_classes_match_release=True,approved_native_pixels_preserved=True,
             main_instance_started=False,gpu_render_verified=False,multiplayer_verified=False,
             isolated_world_network_listener_disabled=True)
for path in (OUT/'deployment.json',backup_dir/'deployment.json'):
    path.write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(receipt,ensure_ascii=False,indent=2))
