"""Deploy the validated rework, retaining both copied and parked rollback bytes."""
from pathlib import Path
from datetime import datetime,timezone,timedelta
import hashlib,json,os,shutil,tomllib,zipfile

ROOT=Path(__file__).resolve().parents[1]
OUT=(ROOT/'outputs/radiation-rework-20261022').resolve()
MODS=(ROOT.parent/'mods').resolve()
validation=json.loads((OUT/'verification.json').read_text(encoding='utf-8'))
source=Path(validation['jar']).resolve();target=(MODS/source.name).resolve()

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def matching():
    found=[]
    for path in MODS.glob('*.jar'):
        try:
            with zipfile.ZipFile(path) as z:mods=tomllib.loads(z.read('META-INF/mods.toml').decode('utf-8-sig'))['mods']
        except (zipfile.BadZipFile,KeyError,tomllib.TOMLDecodeError,UnicodeDecodeError):continue
        if any(mod['modId']=='alex_caves_spellsbook' for mod in mods):found.append(path.resolve())
    return found

assert source.parent==OUT and target.parent==MODS
assert sha(source)==validation['sha256'] and source.stat().st_size==validation['bytes']
assert validation['native_atlas_after']=={'expected':11,'discovered':11,'missing':0}
assert validation['rules_checks']==836 and validation['progression_checks']==501 and validation['jar_crc']=='pass'
active=matching();assert len(active)==1,active
old=active[0];before=sha(old)
if old==target and before==validation['sha256']:
    print('Verified release already active.');raise SystemExit(0)
assert not target.exists() and old.parent==MODS
assert before==sha(OUT/'before-preview.2.jar'),'Active mod changed since baseline capture'
stamp=datetime.now(timezone(timedelta(hours=8))).strftime('%Y%m%d-%H%M%S')
backup_dir=ROOT/'deployment-backups'/('radiation-rework-'+stamp)
backup_dir.mkdir(parents=True,exist_ok=False)
backup=backup_dir/old.name;shutil.copy2(old,backup);assert sha(backup)==before
stage=MODS/(target.name+'.radiation-stage');assert not stage.exists()
shutil.copy2(source,stage);assert sha(stage)==validation['sha256'] and sha(old)==before
parked=backup_dir/(old.name+'.parked')
try:
    os.replace(old,parked);os.replace(stage,target)
    assert sha(target)==validation['sha256'] and matching()==[target]
except Exception:
    if target.exists() and sha(target)==validation['sha256']:target.unlink()
    if parked.exists() and not old.exists():os.replace(parked,old)
    raise
receipt={'time':datetime.now(timezone(timedelta(hours=8))).isoformat(),'source':str(source),
         'active':str(target),'sha256':sha(target),'previous_active':str(old),'previous_sha256':before,
         'rollback_backup':str(backup),'backup_sha256':sha(backup),'backup_verified':True,
         'one_active_same_mod_id_jar':True,'game_started':False,'gameplay_verified':False,'gpu_render_verified':False}
for path in (OUT/'deployment.json',backup_dir/'deployment.json'):
    path.write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
validation.update(deployed=True,active_jar=str(target),active_sha256=sha(target),deployment_receipt=str(OUT/'deployment.json'))
(OUT/'verification.json').write_text(json.dumps(validation,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(receipt,ensure_ascii=False,indent=2))
