"""Verify shipped resources and bytecode scope against preview.8; no runtime claim."""
from pathlib import Path
from zipfile import ZipFile
from PIL import Image
import hashlib,json,re,subprocess
root=Path(__file__).resolve().parents[1]
art=root/'artwork/candy-sixteen-twenty-20261017'
version='1.2.1-candy-pixel-preview.9'
jar=root/f'build/libs/wans_iron_expansion-{version}.jar'
old=root/'build/libs/wans_iron_expansion-1.2.1-candy-pixel-preview.8.jar'
base='cn/wanyiwige/alexcavesspellsbook/candy/'
tex='assets/alex_caves_spellsbook/textures/entity/candy/pixel/'
names=json.loads((art/'new-textures.json').read_text())
baseline=json.loads((art/'source-baseline.json').read_text())
changed_sources=[p for p,digest in baseline.items() if hashlib.sha256((root/p).read_bytes()).hexdigest()!=digest]
assert {Path(p).name for p in changed_sources}=={'CandyLastSpellEntity.java','CandyPixelScenes.java'}
with ZipFile(old) as a,ZipFile(jar) as b:
    before=set(a.namelist());after=set(b.namelist());assert not before-after
    changed=sorted(n for n in before&after if a.read(n)!=b.read(n))
    assert set(changed)<={'META-INF/mods.toml','META-INF/MANIFEST.MF',base+'CandyLastSpellEntity.class',base+'client/CandyPixelScenes.class'},changed
    added=sorted(after-before)
    expected={tex+name+suffix for name in names for suffix in ('.png','.png.mcmeta')}
    assert set(added)==expected|{base+'client/CandySupportPixelScenes.class',base+'client/CandySupportPixelScenes$ChargePose.class'},added
    for name in names:
        p=root/'src/main/resources'/tex/(name+'.png');im=Image.open(p).convert('RGBA')
        assert im.size in [(16,16),(32,8)] and set(im.getchannel('A').getdata())=={0,255}
        assert len(set(im.getdata()))<=12
        for suffix in ('.png','.png.mcmeta'):
            assert b.read(tex+name+suffix)==(root/'src/main/resources'/tex/(name+suffix)).read_bytes()
    native=json.loads((art/'native-assets.json').read_text())
    with ZipFile(native['jar']) as n:
        refs=[f'assets/alexscaves/textures/entity/candicorn_{i}.png' for i in range(5)]
        refs += [f'assets/alexscaves/textures/particle/sugar_flake_{i}.png' for i in range(5)]
        refs += [f'assets/alexscaves/textures/particle/witch_cookie_{i}.png' for i in range(8)]
        refs += ['assets/alexscaves/textures/particle/rainbow.png']
        for ref in refs:assert ref in n.namelist()
    for name in ['SummonedCandyBear','SummonedCandyGinger','CandySummonBrain','client/CandyClientEvents','CandySpellEntity','client/CandyDessertPixelScenes']:
        assert a.read(base+name+'.class')==b.read(base+name+'.class')

javap=Path('C:/Program Files/Microsoft/jdk-17.0.15.6-hotspot/bin/javap.exe')
def methods(path):
    result=subprocess.run([str(javap),'-p','-c','-classpath',str(path),'cn.wanyiwige.alexcavesspellsbook.candy.CandyLastSpellEntity'],capture_output=True,text=True,check=True).stdout
    (art/(path.stem+'-server-bytecode.txt')).write_text(result,encoding='utf-8')
    chunks=re.split(r'(?=^  (?:public|private|protected).+\(.*\);$)',result,flags=re.M)
    return {chunk.splitlines()[0].strip():re.sub(r'#\d+','#CP',chunk).strip() for chunk in chunks[1:]}
a=methods(old);b=methods(jar)
modified=[k for k in a if a[k]!=b[k]]
assert len(modified)==3 and all(any(name+'(' in k for name in ['snare','summonBear','summonGinger']) for k in modified),modified
assert len(set(b)-set(a))==1 and 'summonCue(' in next(iter(set(b)-set(a)))
report={'result':'PASS','version':version,'newTextures':len(names),'newModels':0,'nativeTextureReferences':len(refs),
    'nativeBearAndGingerRenderersUnchanged':True,'serverMethodsWithVisualAdditions':modified,
    'otherServerMethodBytecodeUnchanged':True,'previousCandyAssetsAndRandomColorsAndRicochetUnchanged':True,
    'changedEntries':changed,'addedEntries':added,'jarBytes':jar.stat().st_size,
    'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'deployed':False,'runtimeTested':False}
(art/'scope-validation.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
print(json.dumps(report,indent=2))
