"""Deploy only the hash-pinned progression release after backing up the active mod."""
from pathlib import Path
from datetime import datetime,timezone,timedelta
import hashlib,json,os,shutil,tomllib,zipfile
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'outputs/radiation-progression-20261016'
validation=json.loads((OUT/'release-validation.json').read_text(encoding='utf-8'))
source=Path(validation['artifact']).resolve()
mods=(ROOT.parent/'mods').resolve()
target=(mods/source.name).resolve()
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
assert source.is_relative_to(OUT.resolve()) and target.parent==mods
assert sha(source)==validation['sha256'] and source.stat().st_size==validation['bytes']
assert validation['progression_checks_passed']==201
def modids(path):
    with zipfile.ZipFile(path) as z:
        if 'META-INF/mods.toml' not in z.namelist():return set()
        d=tomllib.loads(z.read('META-INF/mods.toml').decode('utf-8-sig'))
        return {m['modId'] for m in d.get('mods',[])}
with zipfile.ZipFile(source) as z:
    assert z.testzip() is None
    assert 'cn/wanyiwige/alexcavesspellsbook/toxic/ToxicSpec.class' in z.namelist()
assert {'alex_caves_spellsbook','roacw'}<=modids(source)
def matching():
    found=[]
    for p in mods.glob('*.jar'):
        try:ids=modids(p)
        except (zipfile.BadZipFile,KeyError,tomllib.TOMLDecodeError,UnicodeDecodeError):continue
        if ids&{'alex_caves_spellsbook','roacw'}:found.append(p.resolve())
    return found
assert matching()==[target],'Unexpected same-mod-ID JARs; refusing to replace unverified files'
before=sha(target)
assert before in {validation['active_sha256'],validation['sha256']},'Active JAR changed since validation'
stamp=datetime.now(timezone(timedelta(hours=8))).strftime('%Y%m%d-%H%M%S')
backup_dir=ROOT/'deployment-backups'/('radiation-progression-'+stamp)
backup_dir.mkdir(parents=True,exist_ok=False)
backup=backup_dir/target.name
shutil.copy2(target,backup)
assert sha(backup)==before,'Backup verification failed'
stage=mods/(target.name+'.radiation-staged')
assert not stage.exists(),'Staging file already exists'
shutil.copy2(source,stage)
assert sha(stage)==validation['sha256'],'Staged JAR verification failed'
assert sha(target)==before,'Active JAR changed during backup'
os.replace(stage,target)
assert sha(target)==validation['sha256'] and matching()==[target]
receipt={'time':datetime.now(timezone(timedelta(hours=8))).isoformat(),'source':str(source),'deployed':str(target),
 'sha256':sha(target),'previous_sha256':before,'rollback_backup':str(backup),'backup_sha256':sha(backup),
 'backup_verified':True,'one_active_same_mod_id_jar':True,'game_restarted':False,'gameplay_verified':False}
for path in [OUT/'deployment.json',backup_dir/'deployment.json']:
    path.write_text(json.dumps(receipt,ensure_ascii=False,indent=2),encoding='utf-8')
validation['deployed']=True;validation['deployment_receipt']=str(OUT/'deployment.json')
(OUT/'release-validation.json').write_text(json.dumps(validation,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(receipt,ensure_ascii=False,indent=2))
