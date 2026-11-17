"""Resource-only patch of the active, verified mod; all other entry bytes must be identical."""
from pathlib import Path
from datetime import datetime,timezone,timedelta
from io import BytesIO
import hashlib,json,os,shutil,tomllib,zipfile
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
MODS=(ROOT.parent/'mods').resolve()
ART=ROOT/'artwork/radiation-scroll-v5-trefoil'
ENTRY='assets/alex_caves_spellsbook/textures/item/scroll_toxic.png'
texture=(ART/'scroll_radiation-black-16.png').read_bytes()
assert texture==(ROOT/'src/main/resources'/ENTRY).read_bytes()
im=Image.open(BytesIO(texture)).convert('RGBA')
assert im.size==(16,16) and set(im.getchannel('A').get_flattened_data())=={0,255}
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def matching():
    found=[]
    for p in MODS.glob('*.jar'):
        try:
            with zipfile.ZipFile(p) as z:
                if 'META-INF/mods.toml' not in z.namelist():continue
                ids={m['modId'] for m in tomllib.loads(z.read('META-INF/mods.toml').decode('utf-8-sig')).get('mods',[])}
            if ids&{'alex_caves_spellsbook','roacw'}:found.append(p.resolve())
        except (zipfile.BadZipFile,tomllib.TOMLDecodeError,UnicodeDecodeError):continue
    return found
targets=matching();assert len(targets)==1,targets
target=targets[0];assert target.parent==MODS
previous=json.loads((ROOT/'outputs/radiation-progression-20261016/deployment.json').read_text(encoding='utf-8'))
assert sha(target)==previous['sha256'],'Active JAR changed since the progression deployment'
oldhash=sha(target)
stamp=datetime.now(timezone(timedelta(hours=8))).strftime('%Y%m%d-%H%M%S')
backupdir=ROOT/'deployment-backups'/('radiation-scroll-v5-'+stamp);backupdir.mkdir(parents=True,exist_ok=False)
backup=backupdir/target.name;shutil.copy2(target,backup);assert sha(backup)==oldhash
out=ROOT/'outputs/radiation-scroll-v5-20261016';out.mkdir(exist_ok=True)
release=out/target.name
with zipfile.ZipFile(backup) as base,zipfile.ZipFile(release,'w') as new:
    assert base.testzip() is None
    assert json.loads(base.read(ENTRY+'.mcmeta'))['texture']['blur'] is False
    assert not any(n.startswith('META-INF/') and n.endswith(('.SF','.RSA','.DSA')) for n in base.namelist())
    new.comment=base.comment
    for info in base.infolist():new.writestr(info,texture if info.filename==ENTRY else base.read(info.filename))
with zipfile.ZipFile(backup) as base,zipfile.ZipFile(release) as new:
    assert new.testzip() is None and base.namelist()==new.namelist()
    changed=[n for n in base.namelist() if base.read(n)!=new.read(n)]
    assert changed==[ENTRY],changed
    classes=sum(n.endswith('.class') for n in new.namelist())
stage=MODS/(target.name+'.scroll-staged');assert not stage.exists()
shutil.copy2(release,stage);assert sha(stage)==sha(release)
assert sha(target)==oldhash,'Active JAR changed during packaging'
os.replace(stage,target)
assert sha(target)==sha(release) and matching()==[target]
receipt={'time':datetime.now(timezone(timedelta(hours=8))).isoformat(),'deployed':str(target),
 'artifact':str(release),'sha256':sha(target),'backup':str(backup),'backup_sha256':sha(backup),
 'previous_sha256':oldhash,'changed_entries':changed,'unchanged_classes':classes,
 'texture_size':[16,16],'same_mod_id_jar_count':1,'game_restarted':False,'game_visual_verified':False}
for p in [out/'deployment.json',backupdir/'deployment.json']:p.write_text(json.dumps(receipt,ensure_ascii=False,indent=2),encoding='utf-8')
(out/'SHA256SUMS.txt').write_text(sha(release)+'  '+release.name+'\n',encoding='utf-8')
verification=json.loads((ART/'verification.json').read_text(encoding='utf-8'));verification['deployed']=True;verification['deployment_receipt']=str(out/'deployment.json')
(ART/'verification.json').write_text(json.dumps(verification,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(receipt,ensure_ascii=False,indent=2))
