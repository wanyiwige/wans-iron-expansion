"""Release/resource checks; deliberately does not claim Minecraft runtime acceptance."""
from pathlib import Path
from PIL import Image
import argparse, base64, hashlib, io, json, math, re, zipfile

parser=argparse.ArgumentParser()
parser.add_argument('--version',default='1.2.1-candy-pixel-preview.3')
parser.add_argument('--report-dir',default='artwork/candy-first-five-20261017')
parser.add_argument('--build-log',default='artwork/candy-first-five-20261017/build.log')
args=parser.parse_args()

root=Path(__file__).resolve().parents[1]
art=root/'artwork/candy-first-five-20261017'
assets=root/'src/main/resources/assets/alex_caves_spellsbook'
jar=root/f'build/libs/wans_iron_expansion-{args.version}.jar'
baseline=json.loads((art/'baseline.json').read_text(encoding='utf-8'))
active=root.parent/baseline['installed']
assert hashlib.sha256(active.read_bytes()).hexdigest()==baseline['sha256'],'Installed baseline changed during work'
final=json.loads((art/'candy_first_five_final.bbmodel').read_text(encoding='utf-8'))
assert len(final['elements'])==9
texture=next(t for t in final['textures'] if t['name']=='candy_atlas.png')
if texture.get('source','').startswith('data:'):
    embedded=Image.open(io.BytesIO(base64.b64decode(texture['source'].split(',',1)[1]))).convert('RGBA')
    external=Image.open(art/'candy_atlas.png').convert('RGBA')
    assert embedded.size==external.size and embedded.tobytes()==external.tobytes()
names=['rainbow_hard_candy','sugar_hunt_filled_candy','crystal_split_candy','popping_cluster_candy','peppermint_guard']
faces_checked=0
for idx,name in enumerate(names):
    model=json.loads((assets/f'models/candy/{name}.json').read_text(encoding='utf-8'))
    assert 'parent' not in model
    assert len(model['elements'])==(5 if idx==4 else 1)
    for e in model['elements']:
        source=next(s for s in final['elements'] if s['name']==e['name'])
        centre=[-17,-8,1,10,20][idx]
        assert e['from']==[source['from'][0]-centre+8,source['from'][1],source['from'][2]+8]
        assert e['to']==[source['to'][0]-centre+8,source['to'][1],source['to'][2]+8]
        assert all(0<=x<=16 for x in e['from']+e['to'])
        assert all(b>a for a,b in zip(e['from'],e['to']))
        if idx<4: assert len(set(b-a for a,b in zip(e['from'],e['to'])))==1,'Candy must be an exact cube'
        assert set(e['faces'])=={'north','south','east','west','up','down'}
        for face,data in e['faces'].items():
            uv=data['uv'];assert uv[2]>uv[0] and uv[3]>uv[1]
            assert all(0<=v<=16 for v in uv)
            expected=[v/8 for v in source['faces'][face]['uv']]
            assert all(abs(a-b)<1e-6 for a,b in zip(uv,expected)),(name,face,uv,expected)
            faces_checked+=1
images=[]
variants=['rose','orange','lemon','green','blue','purple']
seam_samples=0
mint=json.loads((assets/'models/candy/peppermint_guard.json').read_text(encoding='utf-8'))
for texname in ['candy_atlas']+['rainbow_'+v for v in variants]:
    atlas=Image.open(assets/f'textures/entity/candy/pixel/{texname}.png').convert('RGBA')
    front=atlas.crop((0,64,16,80))
    assert atlas.crop((32,64,48,80)).tobytes()==front.transpose(Image.Transpose.FLIP_LEFT_RIGHT).tobytes()
    for e in mint['elements']:
        x0,y0,_=e['from'];x1,y1,_=e['to'];x0+=12;x1+=12
        for face in ['east','west','up','down']:
            uv=e['faces'][face]['uv'];tx=round(uv[0]*8);ty=round(uv[1]*8)
            count=math.ceil((uv[3]-uv[1])*8) if face in ['east','west'] else round((uv[2]-uv[0])*8)
            for i in range(count):
                x=(x1 if face=='east' else x0) if face in ['east','west'] else x0+(i+.5)/2
                y=y1-(i+.5)/1.6 if face in ['east','west'] else y1 if face=='up' else y0
                expected=front.getpixel((max(0,min(15,math.floor((24-x)*2))),max(0,min(15,math.floor((13-y)*1.6)))))
                for depth in [0,3]:
                    at=(tx+depth,ty+i) if face in ['east','west'] else (tx+i,ty+depth)
                    assert atlas.getpixel(at)==expected,(texname,e['name'],face,at)
                    seam_samples+=1
for family,band in [('rainbow_hard_candy',(0,0,96,16)),('peppermint_guard',(0,64,96,80))]:
    signatures=set()
    original=json.loads((assets/f'models/candy/{family}.json').read_text(encoding='utf-8'))
    for variant in variants:
        m=json.loads((assets/f'models/candy/{family}_{variant}.json').read_text(encoding='utf-8'))
        assert m['elements']==original['elements'],'Color variants must reuse existing geometry'
        tex=assets/f'textures/entity/candy/pixel/rainbow_{variant}.png'
        im=Image.open(tex).convert('RGBA');signatures.add(im.crop(band).tobytes())
        assert m['textures']['0'].endswith('rainbow_'+variant)
    assert len(signatures)==6,f'{family} needs six distinct visible textures'
for cast in range(6):
    assert len({(cast+i*2)%6 for i in range(3)})==3
    assert len({(cast+i)%6 for i in range(5)})==5
for p in (assets/'textures/entity/candy/pixel').glob('*.png'):
    im=Image.open(p).convert('RGBA');assert set(im.getchannel('A').getdata())<={0,255}
    assert im.size in [(8,8),(16,16),(16,32),(32,8),(32,32),(64,64),(128,128)]
    meta=json.loads(p.with_suffix('.png.mcmeta').read_text(encoding='utf-8'))
    assert meta['texture']['blur'] is False
    images.append({'name':p.name,'size':im.size})
log=(root/args.build_log).read_text(encoding='utf-8',errors='replace')
assert 'BUILD SUCCESSFUL' in log and '> Task :reobfJar' in log
with zipfile.ZipFile(active) as old,zipfile.ZipFile(jar) as new:
    assert new.testzip() is None
    old_names=set(old.namelist());new_names=set(new.namelist())
    assert len(new.namelist())==len(new_names)
    assert not old_names-new_names
    changed=sorted(n for n in old_names&new_names if old.read(n)!=new.read(n))
    allowed={'CandyEffectEntity','CandySpellEntity','CandyLastSpellEntity','CandyRicochetMath','client/CandyClientEvents','client/CandyEffectRenderer','client/CandyVisuals'}
    def allowed_class(n):
        if n=='cn/wanyiwige/alexcavesspellsbook/mixin/client/CaniacModelAccessor.class':return True
        if not n.startswith('cn/wanyiwige/alexcavesspellsbook/candy/'):return False
        relative=n.removeprefix('cn/wanyiwige/alexcavesspellsbook/candy/').removesuffix('.class').split('$')[0]
        return relative in allowed or relative in ('client/CandyPixelScenes','client/CandyFieldPixelScenes','client/CandyDessertPixelScenes','client/CandySupportPixelScenes','client/CandyFinalPixelScenes','client/CandyNativeWormScene')
    assert all(n in ('META-INF/mods.toml','META-INF/MANIFEST.MF','mixins.alex_caves_spellsbook.json') or allowed_class(n) for n in changed),changed
    added=sorted(new_names-old_names)
    assert all(n.endswith('/') or n.startswith('assets/alex_caves_spellsbook/models/candy/') or n.startswith('assets/alex_caves_spellsbook/textures/entity/candy/pixel/') or allowed_class(n) for n in added),added
    for p in list((assets/'models/candy').glob('*.json'))+list((assets/'textures/entity/candy/pixel').glob('*')):
        entry='assets/alex_caves_spellsbook/'+p.relative_to(assets).as_posix()
        assert new.read(entry)==p.read_bytes(),f'Stale resource in JAR: {entry}'
    for n in new_names:
        if n.endswith('.class') and allowed_class(n):assert int.from_bytes(new.read(n)[6:8],'big')==61
    assert args.version in new.read('META-INF/mods.toml').decode()
report={'result':'PASS','models':17,'baseModels':5,'rainbowColors':6,'mintColors':6,'mintSeamSamples':seam_samples,'geometryReused':True,'blockbenchCubes':9,'nondegenerateFaces':faces_checked,'images':images,
        'jar':str(jar),'jarBytes':jar.stat().st_size,'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),
        'installedBaseline':str(active),'installedUnchanged':True,'missingPreviousEntries':[],
        'changedEntries':changed,'addedEntries':added,'minecraftRuntimeTested':False,'deployed':False}
(root/args.report_dir/'verification.json').write_text(json.dumps(report,indent=2,ensure_ascii=False),encoding='utf-8')
print(json.dumps({k:report[k] for k in ['result','models','blockbenchCubes','nondegenerateFaces','jarBytes','sha256','installedUnchanged','minecraftRuntimeTested','deployed']},indent=2))
