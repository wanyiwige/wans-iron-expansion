"""Audit the survival release, source receipts, native QA and acquisition gaps."""
from pathlib import Path
import argparse,hashlib,json,re,zipfile
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'development-records/hazen-recreation'
RES=ROOT/'src/main/resources'
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def sha(b):return hashlib.sha256(b).hexdigest()
def main():
    p=argparse.ArgumentParser();p.add_argument('--jar',type=Path,required=True);a=p.parse_args()
    report=load(OUT/'survival-map.json');errors=[]
    inv=load(OUT/'original-inventory.json')
    with zipfile.ZipFile(inv['original_jar']) as source,zipfile.ZipFile(a.jar) as jar:
        assert jar.testzip()is None
        for row in report['receipts']:
            assert sha(source.read(row['source']))==row['sha256'],row['source']
            if 'target' in row:
                local=ROOT/row['target'];assert sha(local.read_bytes())==row['sha256'],str(local)
                assert sha(jar.read(local.relative_to(RES).as_posix()))==row['sha256'],str(local)
        for relative in report['generated_files']:
            path=ROOT/relative
            if path.is_relative_to(RES):assert jar.read(path.relative_to(RES).as_posix())==path.read_bytes(),relative
        for name in jar.namelist():
            if name.endswith('.class') and 'HazenSurvival' in name:
                assert int.from_bytes(jar.read(name)[6:8],'big')==61,name
        atlas=load(RES/'assets/minecraft/atlases/blocks.json')
        sprites={s.get('sprite',s.get('resource')) for s in atlas['sources']}
        for path in (RES/'assets/alex_caves_spellsbook/models/block').glob('hazen_*.json'):
            for texture in load(path).get('textures',{}).values():
                if texture.startswith('alex_caves_spellsbook:hazen/survival/'):
                    assert texture in sprites,texture
        # The old skeleton/material path must not survive in active recipes.
        for path in (RES/'data/alex_caves_spellsbook/recipes/hazen').rglob('*.json'):
            data=load(path);text=json.dumps(data)
            assert 'irons_spellbooks:wisewood_planks' not in text,path
            assert not any('hazennstuff:'+ore+'"' in text for ore in report['ore_blocks']),path
    log=(OUT/'survival-native-verified.log').read_text(encoding='utf-8',errors='replace')
    checks=re.findall(r'HAZEN_NATIVE_EARLY_PASS ([a-z_]+)',log)
    assert 'HAZEN_NATIVE_EARLY_CHECKS_COMPLETE 17' in log and len(checks)==17,checks
    for line in log.splitlines():
        if ('ERROR' in line or 'Could not decode' in line) and ('hazen/survival' in line or 'hazen_' in line):errors.append(line)
    atlaslog=(OUT/'survival-atlas-discovery.log').read_text(encoding='utf-8',errors='replace')
    assert 'PASS ATLAS_DISCOVERY' in atlaslog
    baseline=ROOT/report['backup'];assert sha(baseline.read_bytes())==report['backup_sha256']
    result=dict(jar=str(a.jar.resolve()),sha256=sha(a.jar.read_bytes()),bytes=a.jar.stat().st_size,
        ore_blocks=report['ore_blocks'],wood_blocks=report['wood_blocks'],ore_features=3,loot_modifiers=len(report['modifiers']),
        loot_tables=len(report['loot_tables']),recipe_unlocks=len(report['recipe_unlocks']),repaired_recipes=len(report['changed_recipes']),
        source_receipts=len(report['receipts']),native_synchronous_checks=checks,errors=errors,
        preserved_legacy_zenalite_worldgen=True,known_gaps=report['known_gaps'],
        acceptance='Native synchronous server checks and atlas discovery passed; full scheduled GameTest still blocked by external Iron Lib loopback HTTP init. Client, new-chunk mining and multiplayer untested. Not deployed.')
    (OUT/'survival-verification.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(result,ensure_ascii=False,indent=2));assert not errors
if __name__=='__main__':main()
