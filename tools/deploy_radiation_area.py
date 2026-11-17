"""Freeze, verify and deploy the tested area revision with an intact rollback copy."""
from pathlib import Path
from datetime import datetime, timezone, timedelta
import hashlib,json,os,shutil,tomllib,zipfile,re
ROOT=Path(__file__).resolve().parents[1]
MODS=ROOT.parent/'mods'
CHECK=ROOT/'development-checks/radiation-area-20261022'
OUT=ROOT/'outputs/radiation-area-20261022'
VERSION='1.2.5-radiation-area.1'
OLD_HASH='147826072435436c481706cac8acb97ee7675abd3014e18d5c0a2aeb9e9f9460'
old=MODS/'wans_iron_expansion-1.2.4-radiation-rework.3.jar'
built=ROOT/f'build/libs/wans_iron_expansion-{VERSION}.jar'
harness=ROOT/f'build/libs/radiation-area-verification-{VERSION}.jar'

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def matching():
    result=[]
    for path in MODS.glob('*.jar'):
        try:
            with zipfile.ZipFile(path) as z:meta=tomllib.loads(z.read('META-INF/mods.toml').decode('utf-8-sig'))
        except (KeyError,ValueError,zipfile.BadZipFile):continue
        if any(m['modId']=='alex_caves_spellsbook' for m in meta['mods']):result.append(path)
    return result

assert matching()==[old] and sha(old)==OLD_HASH,'The active mod changed since verification'
build_log=(CHECK/'final-build-and-checks.log').read_text(encoding='utf-8-sig')
for marker in ('BUILD SUCCESSFUL',':reobfJar','PASS RADIATION_RULES 836','501 progression checks','RADIATION_REVISION_RULES 543','expected=11 discovered=11 missing=0'):
    assert marker in build_log,marker
runtime_log=(CHECK/'isolated-server/console.log').read_text(encoding='utf-8')
assert 'RADIATION_AREA_AUDIT_PASS checks=30' in runtime_log and 'RADIATION_AREA_AUDIT_FAILED' not in runtime_log
prefix='cn/wanyiwige/alexcavesspellsbook/toxic/'
stems=('RadiationNewEffectEntity','RadiationRules','ToxicSpec','ToxicSpell','client/RadiationEffectRenderer','client/RadiationHeadMarks','client/ToxicEffectRenderer','client/RadiationVisualStyle')
allowed={'META-INF/MANIFEST.MF','META-INF/mods.toml','assets/alex_caves_spellsbook/lang/zh_cn.json','assets/alex_caves_spellsbook/lang/en_us.json'}
with zipfile.ZipFile(old) as before,zipfile.ZipFile(built) as after,zipfile.ZipFile(harness) as test:
    assert after.testzip() is None
    names=set(after.namelist());previous=set(before.namelist())
    assert previous<=names,'Unexpected removed resource'
    changed={n for n in names if n not in previous or before.read(n)!=after.read(n)}
    for n in changed:
        assert n in allowed or any(re.fullmatch(re.escape(prefix+stem)+r'(\$[^/]+)?\.class',n) for stem in stems),n
    assert not any(any(word in n for word in ('Harness','Audit','OfflineListener','OfflinePatreon','RevisionRulesCheck','radiation-verification')) for n in names)
    meta=tomllib.loads(after.read('META-INF/mods.toml').decode('utf-8-sig'))
    assert [(m['modId'],m['version']) for m in meta['mods']]==[('alex_caves_spellsbook',VERSION)]
    for stem in ('RadiationNewEffectEntity','RadiationRules','ToxicSpec','ToxicSpell','ToxicCombat','ToxicEffectEntity'):
        name=prefix+stem+'.class'
        assert test.read(name)==after.read(name),'Runtime test class differs from release: '+stem
    for n in previous:
        if n.endswith('.png') or '/models/' in n:assert before.read(n)==after.read(n),n
    for lang in ('zh_cn','en_us'):
        path=f'assets/alex_caves_spellsbook/lang/{lang}.json'
        previous_lang=json.loads(before.read(path));new_lang=json.loads(after.read(path))
        keys={k for k in new_lang if previous_lang.get(k)!=new_lang[k]}
        assert len(keys)==14 and all('alex_caves_spellsbook' in k for k in keys),keys

OUT.mkdir(parents=True,exist_ok=True)
source=OUT/built.name;assert not source.exists()
shutil.copy2(built,source);new_hash=sha(source);assert new_hash==sha(built)
shutil.copy2(CHECK/'final-build-and-checks.log',OUT/'build-and-checks.log')
shutil.copy2(CHECK/'isolated-server/console.log',OUT/'mechanics-check.log')
stamp=datetime.now(timezone(timedelta(hours=8))).strftime('%Y%m%d-%H%M%S')
backup_dir=ROOT/'deployment-backups'/('radiation-area-'+stamp);backup_dir.mkdir(parents=True,exist_ok=False)
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
receipt=dict(version=VERSION,active=str(target),sha256=new_hash,bytes=target.stat().st_size,
             rollback_backup=str(backup),previous_sha256=OLD_HASH,backup_verified=True,
             changed_entries=sorted(changed),png_and_model_assets_unchanged=True,jar_crc='pass',
             checks=dict(existing_rules=836,progression=501,revision_rules=543,isolated_world=30,atlas='11/11'),
             tested_server_classes_match_release=True,one_active_same_mod_id_jar=True,
             main_instance_started=False,gpu_render_verified=False,multiplayer_verified=False,
             isolated_world_network_listener_disabled=True)
for path in (OUT/'deployment.json',backup_dir/'deployment.json'):
    path.write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(receipt,ensure_ascii=False,indent=2))
