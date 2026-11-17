"""Read-only preflight for the Radiation + Hazen release, producing an exact deployment plan."""
from pathlib import Path
import hashlib,json,tomllib,zipfile
ROOT=Path(__file__).resolve().parents[1];INSTANCE=ROOT.parent
OUT=ROOT/'outputs/radiation-hazen-deploy-20261016';OUT.mkdir(exist_ok=True)
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
hazen=read(ROOT/'development-records/hazen-recreation/verification.json')
release=Path(hazen['jar']);assert release.name=='wans_iron_expansion-1.2.1-hazen-preview.1.jar'
assert sha(release)==hazen['jar_sha256'] and not hazen['errors']
radiation=ROOT/'outputs/toxic-1.2-20261015/wans_iron_expansion-1.2.jar'
radreport=read(radiation.parent/'release-validation.json');assert sha(radiation)==radreport['sha256']
for row in read(ROOT/'development-checks/toxic-audit/migration-inputs.json')['inputs']:
    assert sha(INSTANCE/'mods'/row['jar'])==row['sha256'],row['jar']
ids={};scan_errors=[];encoding_fallbacks=[]
for jar in (INSTANCE/'mods').glob('*.jar'):
    try:
        with zipfile.ZipFile(jar) as z:
            if 'META-INF/mods.toml' not in z.namelist():continue
            raw=z.read('META-INF/mods.toml')
            try:text=raw.decode('utf-8-sig')
            except UnicodeDecodeError:
                text=raw.decode('utf-8-sig',errors='replace');encoding_fallbacks.append(jar.name)
            metadata=tomllib.loads(text)
            for m in metadata.get('mods',[]):ids.setdefault(m['modId'],[]).append({'path':str(jar),'version':m.get('version'),'sha256':sha(jar) if m['modId'] in ['alex_caves_spellsbook','roacw'] else None})
    except Exception as ex:scan_errors.append({'jar':jar.name,'error':str(ex)})
active=ids.get('alex_caves_spellsbook',[]);assert len(active)==1,active
assert active[0]['version']=='1.1.17',active
assert active[0]['sha256']==radreport['active_instance_jars'][0]['sha256'],active
assert ids.get('roacw',[{}])[0].get('path')==active[0]['path'] and len(ids['roacw'])==1
with zipfile.ZipFile(release) as new,zipfile.ZipFile(radiation) as rad:
    assert new.testzip() is None
    names=new.namelist();assert len(names)==len(set(names))
    metadata=tomllib.loads(new.read('META-INF/mods.toml').decode('utf-8-sig'))
    mod={m['modId']:m for m in metadata['mods']};assert mod['alex_caves_spellsbook']['version']=='1.2.1-hazen-preview.1'
    checked=[]
    for name in rad.namelist():
        if name.endswith('/'):continue
        assert name in names,('Radiation release entry missing',name)
        if (name.startswith('cn/wanyiwige/alexcavesspellsbook/toxic/') or '/entity/toxic/' in name
            or name.endswith('/scroll_toxic.json') or name.endswith('/scroll_toxic.png')
            or name.endswith('/toxic_waste_prop.json') or name.endswith('/toxic_reactor_prop.json')
            or '/mixin/Toxic' in name):
            assert new.read(name)==rad.read(name),('Radiation implementation changed',name);checked.append(name)
    for lang,wanted in [('zh_cn','辐射'),('en_us','Radiation')]:
        path='assets/alex_caves_spellsbook/lang/'+lang+'.json'
        assert json.loads(new.read(path))['school.alex_caves_spellsbook.toxic']==wanted
    for row in read(ROOT/'development-records/hazen-recreation/asset-receipts.json')+read(ROOT/'development-records/hazen-recreation/spell-asset-receipts.json'):
        name=Path(row['target']).relative_to('src/main/resources').as_posix()
        assert hashlib.sha256(new.read(name)).hexdigest()==row['sha256'],name
    assert len(json.loads(new.read('hazen-recreated-weapons.json')))==47
    assert len(json.loads(new.read('hazen-materials.json')))==30
    assert sum(n.startswith('cn/wanyiwige/alexcavesspellsbook/hazen/') and n.endswith('.class') for n in names)==39
    assert sum(n.startswith('net/kamaarion/roacw/') and n.endswith('.class') for n in names)==98
    for name in names:
        if name.endswith('.json'):json.loads(new.read(name).decode('utf-8-sig'))
    ownclasses=new.read('cn/wanyiwige/alexcavesspellsbook/AlexCavesSpellsbook.class')
    assert b'toxic/ToxicRegistry' in ownclasses and b'hazen/HazenEquipment' in ownclasses
    required=[]
    for owner,deps in metadata.get('dependencies',{}).items():
        for dep in deps:
            if dep.get('mandatory') and dep['modId'] not in ['forge','minecraft']:
                assert dep['modId'] in ids or dep['modId'] in mod,('Missing dependency',dep['modId'])
                required.append(dep)
    vendor=ROOT/'vendor/roacw-1.1.1-unpacked'
    for p in (vendor/'net/kamaarion/roacw').rglob('*.class'):assert new.read(p.relative_to(vendor).as_posix())==p.read_bytes()
config=INSTANCE/'config/wans-hazen-recreation-common.toml'
config_text=config.read_text(encoding='utf-8-sig') if config.exists() else None
plan={'source':str(release),'sha256':sha(release),'bytes':release.stat().st_size,'version':'1.2.1-hazen-preview.1',
 'target':str(INSTANCE/'mods'/release.name),'previous':active[0],
 'radiation_entries_identical':len(checked),'hazen_counts':{k:hazen[k] for k in ['weapon_count','school_count','exclusive_spell_count','material_count','recipe_count','exact_asset_receipts']},
 'mandatory_dependencies_present':required,'metadata_scan_errors':scan_errors,'metadata_non_utf8_text':encoding_fallbacks,
 'hazen_config_existing':config_text,'gameplay_visual_multiplayer_verified':False,
 'hazen_assertions_from_existing_record':hazen['native_startup_assertions'],
 'other_conversation':'01a13f96-c607-7db3-bf1b-775d7e7d3117','deployed':False}
(OUT/'preflight.json').write_text(json.dumps(plan,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({k:plan[k] for k in ['version','sha256','radiation_entries_identical','hazen_counts','previous','hazen_config_existing','metadata_scan_errors']},ensure_ascii=False,indent=2))
