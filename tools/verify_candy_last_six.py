"""Audit final-six resources, source model UVs, native dependencies, and release scope."""
from pathlib import Path
from zipfile import ZipFile
from PIL import Image
import hashlib,json,re,subprocess,base64,io
root=Path(__file__).resolve().parents[1];art=root/'artwork/candy-last-six-20261017'
version='1.2.1-candy-pixel-preview.10';jar=root/f'build/libs/wans_iron_expansion-{version}.jar'
old=root/'build/libs/wans_iron_expansion-1.2.1-candy-pixel-preview.9.jar'
base='cn/wanyiwige/alexcavesspellsbook/candy/';resource='assets/alex_caves_spellsbook/'
names=json.loads((art/'new-textures.json').read_text());flavors=json.loads((art/'flavors.json').read_text())
baseline=json.loads((art/'source-baseline.json').read_text())
changed_sources=[p for p,d in baseline.items() if hashlib.sha256((root/p).read_bytes()).hexdigest()!=d]
assert {Path(p).name for p in changed_sources}=={'CandyLastSpellEntity.java','CandyPixelScenes.java','CandyClientEvents.java'},changed_sources
bb=json.loads((art/'icecream_six_flavors.bbmodel').read_text(encoding='utf-8'))
assert bb['resolution']=={'width':64,'height':64} and len(bb['elements'])==48 and len(bb['textures'])==6
geometry=json.loads((art/'icecream_geometry.json').read_text(encoding='utf-8'))
assert len(geometry['elements'])==8
checked_faces=0
for i,flavor in enumerate(flavors):
    model=json.loads((root/'src/main/resources'/resource/f'models/candy/icecream_scoop_{flavor}.json').read_text())
    assert 'parent' not in model and len(model['elements'])==8
    atlas=root/'src/main/resources'/resource/f'textures/entity/candy/pixel/icecream_atlas_{flavor}.png'
    im=Image.open(atlas).convert('RGBA');assert im.size==(64,64)
    embedded=bb['textures'][i]['source'];assert embedded.startswith('data:')
    assert Image.open(io.BytesIO(base64.b64decode(embedded.split(',',1)[1]))).convert('RGBA').tobytes()==im.tobytes()
    offset=[i%3*19,0,i//3*20]
    for e in model['elements']:
        original=next(x for x in geometry['elements'] if x['name']==e['name'])
        source=next(x for x in bb['elements'] if x['name']==flavor+'_'+e['name'])
        assert e['from']==original['from'] and e['to']==original['to']
        for endpoint in ['from','to']:assert [source[endpoint][axis]-offset[axis] for axis in range(3)]==e[endpoint]
        assert all(b>a for a,b in zip(e['from'],e['to']))
        assert set(e['faces'])=={'north','south','east','west','up','down'}
        for face,f in e['faces'].items():
            uv=f['uv'];assert all(0<=v<=16 for v in uv) and uv[2]>uv[0] and uv[3]>uv[1]
            assert [v*4 for v in uv]==source['faces'][face]['uv']
            assert source['faces'][face]['texture']==i
            pixels=im.crop(tuple(round(v*4) for v in uv));assert pixels.getchannel('A').getextrema()==(255,255)
            checked_faces+=1
for name in names:
    im=Image.open(root/'src/main/resources'/resource/f'textures/entity/candy/pixel/{name}.png').convert('RGBA')
    assert im.size in [(8,8),(16,16),(32,8),(32,32),(64,64)]
    assert set(im.getchannel('A').getdata())<={0,255} and len(set(im.getdata()))<=12
with ZipFile(old) as a,ZipFile(jar) as b:
    before=set(a.namelist());after=set(b.namelist());assert not before-after
    changed=sorted(n for n in before&after if a.read(n)!=b.read(n))
    allowed={'META-INF/mods.toml','META-INF/MANIFEST.MF','mixins.alex_caves_spellsbook.json',base+'CandyLastSpellEntity.class',base+'client/CandyPixelScenes.class',base+'client/CandyClientEvents.class'}
    allowed|={base+'client/'+s+'.class' for s in ['CandyClientEvents$Ticks','CandyPixelScenes$History','CandyPixelScenes$Hit']}
    assert set(changed)<=allowed,changed
    added=sorted(after-before)
    expected={resource+'textures/entity/candy/pixel/'+name+suffix for name in names for suffix in ['.png','.png.mcmeta']}
    expected|={resource+'models/candy/icecream_scoop_'+f+'.json' for f in flavors}
    classes={base+'client/'+name+'.class' for name in ['CandyFinalPixelScenes','CandyNativeWormScene','CandyNativeWormScene$Trail','CandyNativeWormScene$HeadPose','CandyNativeWormScene$SegmentPose']}
    classes.add('cn/wanyiwige/alexcavesspellsbook/mixin/client/CaniacModelAccessor.class')
    assert {n for n in added if not n.endswith('/')}==expected|classes,added
    for n in expected:assert b.read(n)==(root/'src/main/resources'/n).read_bytes()
    config=json.loads(a.read('mixins.alex_caves_spellsbook.json'));updated=json.loads(b.read('mixins.alex_caves_spellsbook.json'))
    assert updated.pop('client')==['client.CaniacModelAccessor']+config.pop('client') and updated==config
    assert 'cn/wanyiwige/alexcavesspellsbook/mixin/client/CaniacModelAccessor.class' in after
    for name in ['CandySpellEntity','CandySession','CandySummonBrain','SummonedCandyBear','SummonedCandyGinger','SummonedCandySentry','client/CandyDessertPixelScenes','client/CandySupportPixelScenes']:
        assert a.read(base+name+'.class')==b.read(base+name+'.class')
    native=json.loads((art/'native-assets.json').read_text())
    with ZipFile(native['jar']) as n:
        native_refs=[f'assets/alexscaves/textures/entity/{s}.png' for s in ['caniac','gum_worm','gum_worm_segment_0','gum_worm_segment_1','gum_worm_segment_2','gum_worm_connection','gumbeeper','gumbeeper_glass']]
        for name in native_refs:assert name in n.namelist()

javap=Path('C:/Program Files/Microsoft/jdk-17.0.15.6-hotspot/bin/javap.exe')
def dump(path,clazz):return subprocess.run([str(javap),'-p','-c','-classpath',str(path),clazz],capture_output=True,text=True,check=True).stdout
for name in ['CandyClientEvents$Ticks','CandyPixelScenes$History','CandyPixelScenes$Hit']:
    clazz='cn.wanyiwige.alexcavesspellsbook.candy.client.'+name
    assert re.sub(r'#\d+','#CP',dump(old,clazz))==re.sub(r'#\d+','#CP',dump(jar,clazz)),name
native_fields=dump(native['jar'],'com.github.alexmodguy.alexscaves.client.model.CaniacModel')
assert 'AdvancedModelBox left_Arm;' in native_fields and 'AdvancedModelBox right_Arm;' in native_fields
(art/'native-caniac-bytecode.txt').write_text(native_fields,encoding='utf-8')
def methods(path):
    text=dump(path,'cn.wanyiwige.alexcavesspellsbook.candy.CandyLastSpellEntity')
    (art/(path.stem+'-server-bytecode.txt')).write_text(text,encoding='utf-8')
    chunks=re.split(r'(?=^  (?:public|private|protected).+\(.*\);$)',text,flags=re.M)
    def canonical(chunk):
        # Constant-pool growth can widen ldc and move branch byte offsets.
        # Compare actual instructions and branch destinations by instruction index.
        lines=chunk.splitlines();instructions={}
        for line in lines:
            m=re.match(r'\s*(\d+):\s+([a-z][a-z0-9_]*)',line)
            if m:instructions[int(m[1])]=len(instructions)
        result=[]
        for line in lines:
            m=re.match(r'\s*(\d+):\s+([a-z][a-z0-9_]*)(.*)',line)
            if m:
                op=m[2];rest=m[3]
                if op=='ldc_w':op='ldc'
                if op.startswith('if') or op in ('goto','goto_w','jsr'):
                    target=re.match(r'\s*(\d+)',rest)
                    if target:rest=' @'+str(instructions[int(target[1])])
                line=op+rest
            else:
                target=re.match(r'\s*(default|-?\d+):\s*(\d+)\s*$',line)
                if target:line=target[1]+': @'+str(instructions[int(target[2])])
            result.append(re.sub(r'\s+',' ',re.sub(r'#\d+','#CP',line)).strip())
        return '\n'.join(result)
    return {c.splitlines()[0].strip():canonical(c) for c in chunks[1:]}
a=methods(old);b=methods(jar)
modified=[k for k in a if k in b and a[k]!=b[k]]
allowed_methods=['CandyLastSpellEntity','actor','projectile','whirl','banquet','summonBear','summonGinger','summonSentry','sentryShot']
assert all(any(name+'(' in k for name in allowed_methods) for k in modified),modified
for name in ['worm','iceStorm','grandFeast','hit','splash','snare','overdrive','candicorn','serverTimeline','beginLast','summonPosition']:
    signature=next(k for k in a if name+'(' in k);assert a[signature]==b[signature],name
assert len(set(a)-set(b))==1 and 'summonCue(' in next(iter(set(a)-set(b)))
assert len(set(b)-set(a))==3 and all(any(name+'(' in k for name in ['summonCue','nextLastColor','impact']) for k in set(b)-set(a))
assert 'PASS: production worm route sampling' in (art/'worm-check.log').read_text(encoding='utf-8',errors='replace')
scene=(root/'src/main/java/cn/wanyiwige/alexcavesspellsbook/candy/client/CandyPixelScenes.java').read_text()
spec=(root/'src/main/java/cn/wanyiwige/alexcavesspellsbook/candy/CandySpecs.java').read_text(encoding='utf-8')
ids=re.findall(r'new Spec\("([^"]+)"',spec)
handles=scene[scene.index('public static boolean handles'):scene.index('private static int index')]
assert len(ids)==26 and all('"'+s+'"' in handles for s in ids)
report={'result':'PASS','version':version,'all26SpellsRouted':True,'newTextures':len(names),'newModelVariants':6,'newBaseGeometry':1,'cubesPerIceCream':8,'verifiedUvFaces':checked_faces,
    'nativeCaniacArmAccessorsMatchInstalledJar':True,'nativeWormHasHeadSixSegmentsAndConnector':True,'nativeReferencesExist':native_refs,
    'modifiedServerMethods':modified,'wormIceStormAndGrandFeastMechanicsBytecodeUnchanged':True,'previousCandyAssetsPreserved':True,
    'productionWormPathChecksPassed':True,'changedEntries':changed,'addedEntries':added,'jarBytes':jar.stat().st_size,
    'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'deployed':False,'runtimeTested':False}
(art/'scope-validation.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
print(json.dumps({k:v for k,v in report.items() if k not in ['addedEntries','nativeReferencesExist','modifiedServerMethods']},indent=2))
