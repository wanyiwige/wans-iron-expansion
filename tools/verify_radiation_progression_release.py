"""Verify the progression release and isolate its changes from the active preview binary."""
from pathlib import Path
import hashlib,json,zipfile,tomllib,shutil
ROOT=Path(__file__).resolve().parents[1]
props=dict(line.split('=',1) for line in (ROOT/'gradle.properties').read_text().splitlines() if '=' in line and not line.lstrip().startswith('#'))
version=props['mod_version'];name=f'wans_iron_expansion-{version}.jar'
jar=ROOT/'build/libs'/name;active=ROOT.parent/'mods'/name
out=ROOT/'outputs/radiation-progression-20261016';out.mkdir(exist_ok=True)
allowed={'ToxicSpec','ToxicSpell','ToxicEffectEntity','RaycatPurificationEntity'}
with zipfile.ZipFile(jar) as z,zipfile.ZipFile(active) as base:
    assert z.testzip() is None;assert len(z.namelist())==len(set(z.namelist()))
    names=set(z.namelist());assert names==set(base.namelist()),'Unexpected new/removed packaged entries'
    metadata=tomllib.loads(z.read('META-INF/mods.toml').decode());assert next(m for m in metadata['mods'] if m['modId']=='alex_caves_spellsbook')['version']==version
    changed=[n for n in sorted(names) if z.read(n)!=base.read(n)]
    for n in changed:
        if n=='META-INF/MANIFEST.MF':continue
        assert n.startswith('cn/wanyiwige/alexcavesspellsbook/toxic/') and n.endswith('.class'),n
        assert n.rsplit('/',1)[-1].split('$',1)[0].removesuffix('.class') in allowed,n
    assert sum(n.startswith('net/kamaarion/roacw/') and n.endswith('.class') for n in names)==98
    for n in names:
        if n.endswith('.json'):json.loads(z.read(n).decode('utf-8-sig'))
    for lang in ['zh_cn','en_us']:
        d=json.loads(z.read(f'assets/alex_caves_spellsbook/lang/{lang}.json'))
        assert d['school.alex_caves_spellsbook.toxic']==('辐射' if lang=='zh_cn' else 'Radiation')
    assert not any('RadiationProgressionCheck' in n for n in names),'Development harness leaked into runtime'
log=(ROOT/'development-checks/toxic-audit/build.log').read_text(encoding='utf-8-sig')
assert 'PASS: 201 progression checks' in log and 'BUILD SUCCESSFUL' in log and '> Task :reobfJar' in log
frozen=out/name;shutil.copy2(jar,frozen);shutil.copy2(ROOT/'development-checks/toxic-audit/build.log',out/'build-and-checks.log')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
assert sha(jar)==sha(frozen)
report={'version':version,'artifact':str(frozen),'sha256':sha(frozen),'bytes':frozen.stat().st_size,
 'active_artifact':str(active),'active_sha256':sha(active),'changed_jar_entries':changed,'roacw_classes_preserved':98,
 'progression_checks_passed':201,'levels':40,'rarities':5,'different_maxima':6,'deployed':False,'gameplay_verified':False}
(out/'release-validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
(out/'SHA256SUMS.txt').write_text(sha(frozen)+'  '+frozen.name+'\n',encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
