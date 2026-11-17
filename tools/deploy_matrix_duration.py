"""Verify and deploy the narrow Critical Matrix collection-window adjustment."""
import hashlib,json,os,shutil,tomllib,zipfile
from datetime import datetime,timezone,timedelta
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
CHECK=ROOT/'development-checks/matrix-duration-20261023'
VERSION='1.2.8-radiation-matrix.1'
MODS=(ROOT.parent/'mods').resolve()
before=json.loads((CHECK/'before.json').read_text(encoding='utf-8'))
old=Path(before['path']);built=ROOT/'build/libs'/f'wans_iron_expansion-{VERSION}.jar'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
log=(CHECK/'build-and-checks.log').read_text(encoding='utf-8-sig')
for marker in ['BUILD SUCCESSFUL',':reobfJar','PASS MATRIX_DURATION','PASS RADIATION_RULES 836','501 progression checks','PASS RADIATION_REVISION_RULES 543']:
    assert marker in log,marker
assert sha(old)==before['sha256']
prefix='cn/wanyiwige/alexcavesspellsbook/toxic/'
allowed={'META-INF/MANIFEST.MF','META-INF/mods.toml',*[f'assets/alex_caves_spellsbook/lang/{lang}.json' for lang in ['zh_cn','en_us']],*[prefix+n+'.class' for n in ['RadiationRules','RadiationNewEffectEntity','ToxicSpec','ToxicSpell']]}
# javac shifts the synthetic enum-switch line table when the shared constant is added.
assert (CHECK/'synthetic-switch-before.txt').read_bytes()==(CHECK/'synthetic-switch-after.txt').read_bytes()
allowed.add(prefix+'RadiationRules$1.class')
with zipfile.ZipFile(old) as a,zipfile.ZipFile(built) as b:
    assert b.testzip() is None
    assert set(a.namelist())==set(b.namelist())
    changed={n for n in a.namelist() if a.read(n)!=b.read(n)}
    assert changed==allowed,changed
    meta=tomllib.loads(b.read('META-INF/mods.toml').decode('utf-8'))
    assert [(m['modId'],m['version']) for m in meta['mods']]==[('alex_caves_spellsbook',VERSION)]
    for lang,phrase in [('zh_cn','三十秒'),('en_us','30 seconds')]:
        path=f'assets/alex_caves_spellsbook/lang/{lang}.json';x=json.loads(a.read(path));y=json.loads(b.read(path))
        keys={k for k in x.keys()|y.keys() if x.get(k)!=y.get(k)}
        assert keys=={'spell.alex_caves_spellsbook.critical_matrix.guide','ui.alex_caves_spellsbook.toxic.detail.matrix_window'},keys
        assert phrase in y['spell.alex_caves_spellsbook.critical_matrix.guide']
    for n in a.namelist():
        if n.endswith('.png') or '/models/' in n or '/geo/' in n or 'CriticalWarden' in n:
            assert a.read(n)==b.read(n),'Unrelated art or armor changed: '+n

def matching():
    matches=[]
    for p in MODS.glob('*.jar'):
        try:
            with zipfile.ZipFile(p) as z:
                if 'META-INF/mods.toml' not in z.namelist():continue
                meta=tomllib.loads(z.read('META-INF/mods.toml').decode('utf-8-sig'))
                if any(m.get('modId')=='alex_caves_spellsbook' for m in meta.get('mods',[])):matches.append(p)
        except (zipfile.BadZipFile,tomllib.TOMLDecodeError,UnicodeDecodeError):continue
    return matches

assert matching()==[old] and old.parent==MODS
stamp=datetime.now(timezone(timedelta(hours=8))).strftime('%H%M%S')
backup=ROOT/'deployment-backups'/('matrix-duration-20261023-'+stamp);backup.mkdir(parents=True,exist_ok=False)
saved=backup/old.name;shutil.copy2(old,saved);assert sha(saved)==before['sha256']
target=MODS/built.name;stage=MODS/(built.name+'.pending')
assert not target.exists() and not stage.exists()
shutil.copy2(built,stage);newhash=sha(built);assert sha(stage)==newhash
try:
    os.replace(stage,old);old.rename(target)
except Exception:
    if old.exists() and sha(old)!=before['sha256']:shutil.copy2(saved,old)
    if stage.exists():stage.unlink()
    raise
assert sha(target)==newhash and matching()==[target]
report={'status':'DEPLOYED','version':VERSION,'old_collection_seconds':7.5,'collection_seconds':30,'native_recast_ticks':600,'initial_entity_lifetime_ticks':610,'native_recast_tooltip_and_auto_trigger_synchronized':True,'textures_models_and_armor_unchanged':True,'changed_jar_entries':sorted(changed),'active_jar':str(target),'sha256':newhash,'backup':str(saved),'backup_sha256':sha(saved),'one_active_mod_id':True,'checks':['MATRIX_DURATION','RADIATION_RULES:836','PROGRESSION:501','RADIATION_REVISION_RULES:543'],'game_restarted':False,'in_game_combo_tested':False}
for p in [CHECK/'deployment.json',backup/'deployment.json']:
    p.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
