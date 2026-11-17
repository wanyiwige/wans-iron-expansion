"""Plan byte-preserving resource relocation, then update exact references/receipts after native moves."""
from pathlib import Path
import hashlib,json,re,sys
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources'
OUT=ROOT/'outputs/hazen-gecko-path-fix-20261016';OUT.mkdir(exist_ok=True)
NS='alex_caves_spellsbook';RECORD=ROOT/'development-records/hazen-recreation'
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,d):p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
planfile=OUT/'resource-moves.json'
if '--apply-metadata' not in sys.argv:
    assert not planfile.exists(),'Keep the original relocation plan; use --apply-metadata to finish it.'
    rows=[]
    for p in (RES/f'assets/{NS}/hazen').rglob('*.json'):
        name=p.relative_to(RES).as_posix()
        match=re.fullmatch(r'assets/'+NS+r'/hazen/(?:(\w+)/)?(geo|animations)/(.+)',name)
        if not match:continue
        upstream,kind,tail=match.groups();new=f'assets/{NS}/{kind}/hazen/'+(upstream+'/' if upstream else '')+tail
        rows.append({'source':str(p),'target':str(RES/new),'old_relative':name,'new_relative':new,'sha256':sha(p),
          'old_id':NS+':'+name.split(NS+'/',1)[1],'new_id':NS+':'+new.split(NS+'/',1)[1]})
    assert rows and any(r['old_id'].endswith('/volcano.geo.json') for r in rows)
    write(planfile,rows);print('Planned byte-preserving moves:',len(rows))
else:
    rows=read(planfile);urls={r['old_id']:r['new_id'] for r in rows}
    for r in rows:assert sha(Path(r['target']))==r['sha256'],r
    def refs(value):
        if isinstance(value,str):return urls.get(value,value)
        if isinstance(value,list):return [refs(v) for v in value]
        if isinstance(value,dict):return {k:refs(v) for k,v in value.items()}
        return value
    for name in ['hazen-recreated-weapons.json','hazen-spell-visuals.json']:write(RES/name,refs(read(RES/name)))
    paths={str(Path(r['source']).relative_to(ROOT)):str(Path(r['target']).relative_to(ROOT)) for r in rows}
    for name in ['asset-receipts.json','spell-asset-receipts.json']:
        receipts=read(RECORD/name)
        for r in receipts:r['target']=paths.get(r['target'],r['target'])
        write(RECORD/name,receipts)
    print('Updated references and receipts; source image/model/animation bytes unchanged.')
