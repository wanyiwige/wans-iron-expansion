"""Hash-verified backup and atomic replacement of the one active mod JAR."""
import hashlib,json,os,shutil,tomllib,zipfile
from datetime import datetime,timezone,timedelta
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
CHECK=ROOT/'development-checks/radiation-armor-20261023'
verification=json.loads((CHECK/'verification.json').read_text(encoding='utf-8'))
assert verification['status']=='PASS'
release=Path(verification['jar']);mods=(ROOT.parent/'mods').resolve()
before=json.loads((CHECK/'active-before.json').read_text(encoding='utf-8'))[0]
old=Path(before['path']).resolve();target=mods/release.name
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
assert old.parent==mods and target.parent==mods
assert sha(old)==before['sha256'] and sha(release)==verification['sha256']
assert not target.exists(),target
stamp=datetime.now(timezone(timedelta(hours=8))).strftime('%H%M%S')
backup=ROOT/'deployment-backups'/('critical-warden-20261023-'+stamp)
backup.mkdir(parents=True,exist_ok=False)
saved=backup/old.name;shutil.copy2(old,saved)
assert sha(saved)==before['sha256']
stage=mods/(release.name+'.pending');assert not stage.exists()
shutil.copy2(release,stage);assert sha(stage)==verification['sha256']
try:
    os.replace(stage,old)
    old.rename(target)
except Exception:
    if old.exists() and sha(old)!=before['sha256']:shutil.copy2(saved,old)
    if stage.exists():stage.unlink()
    raise
assert sha(target)==verification['sha256']
active=[]
for p in mods.glob('*.jar'):
    try:
        with zipfile.ZipFile(p) as z:
            if 'META-INF/mods.toml' not in z.namelist():continue
            data=tomllib.loads(z.read('META-INF/mods.toml').decode('utf-8-sig'))
            if any(m.get('modId')=='alex_caves_spellsbook' for m in data.get('mods',[])):active.append(p)
    except (zipfile.BadZipFile,tomllib.TOMLDecodeError,UnicodeDecodeError):continue
assert active==[target],active
report={'status':'DEPLOYED','version':verification['version'],'deployed_jar':str(target),'sha256':sha(target),'old_jar_backup':str(saved),'backup_sha256':sha(saved),'one_active_mod_id':True,'game_restarted':False,'game_runtime_tested':False}
for p in [CHECK/'deployment.json',backup/'deployment.json',ROOT/'artwork/radiation-armor-release-20261023/deployment.json']:
    p.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
