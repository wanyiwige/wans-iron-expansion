"""Inspect actual artwork, preserve pre-edit mechanics, and bound release changes vs preview.6."""
from pathlib import Path
from PIL import Image
import hashlib,json,re,zipfile
ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'artwork/candy-eleven-fifteen-20261017'
RES=ROOT/'src/main/resources/assets/alex_caves_spellsbook'
SRC=ROOT/'src/main/java/cn/wanyiwige/alexcavesspellsbook/candy'
baseline=json.loads((ART/'baseline.json').read_text(encoding='utf-8'))
changes=[]
for path,digest in baseline['sources'].items():
    p=ROOT.parent/path
    if hashlib.sha256(p.read_bytes()).hexdigest()!=digest:changes.append(p.name)
assert set(changes)=={'CandySpellEntity.java','CandyPixelScenes.java','CandyFieldPixelScenes.java'},changes
before=(ART/'source-before/CandySpellEntity.java').read_text(encoding='utf-8')
after=(SRC/'CandySpellEntity.java').read_text(encoding='utf-8')
def methods(text):
    out={}
    for m in re.finditer(r'\b(?:private|public|protected)\s+(?:static\s+)?[\w<>]+\s+(\w+)\([^;{}]*\)\s*\{',text):
        start=text.index('{',m.start());depth=1;i=start+1
        while depth:depth+=(text[i]=='{')-(text[i]=='}');i+=1
        out[m.group(1)]=text[m.start():i]
    return out
a=methods(before);b=methods(after)
modified=[name for name in a if a[name]!=b[name]]
assert set(modified)=={'pixelSpell','impact'},modified
assert a['contact']==b['contact'] and a['sweep']==b['sweep'] and a['nextPixelColor']==b['nextPixelColor']
names=json.loads((ART/'new-textures.json').read_text(encoding='utf-8'))
for name in names:
    im=Image.open(RES/f'textures/entity/candy/pixel/{name}.png').convert('RGBA')
    assert im.size in [(8,8),(16,16),(16,32),(32,8),(32,32)]
    assert set(im.getchannel('A').getdata())<={0,255}
    assert 0<sum(v!=0 for v in im.getchannel('A').getdata())<im.width*im.height
pools=[Image.open(RES/f'textures/entity/candy/pixel/caramel_pool_{i}.png').convert('RGBA') for i in range(3)]
masks=[im.getchannel('A') for im in pools]
assert len({im.tobytes() for im in masks})==1,'Liquid outline must not pop between frames'
mask=masks[0];assert mask.getpixel((15,15))==255,'Pool must be liquid, not just an outline ring'
assert mask.tobytes()!=mask.transpose(Image.Transpose.FLIP_LEFT_RIGHT).tobytes()
assert mask.tobytes()!=mask.transpose(Image.Transpose.FLIP_TOP_BOTTOM).tobytes()
points={(x,y) for y in range(32) for x in range(32) if mask.getpixel((x,y))}
assert all((x-15.5)**2+(y-15.5)**2<=15.5**2 for x,y in points)
seen={next(iter(points))};todo=list(seen)
while todo:
    x,y=todo.pop()
    for p in [(x+1,y),(x-1,y),(x,y+1),(x,y-1)]:
        if p in points and p not in seen:seen.add(p);todo.append(p)
assert seen==points,'Caramel must form a single connected puddle'
scene=(SRC/'client/CandyDessertPixelScenes.java').read_text(encoding='utf-8')
assert 'anchor("Anchor")' in scene and 'anchor("Second")' in scene and 'rainbow_ribbon.png' in scene
assert 'CandyFieldPixelScenes.nativeSpear' in scene
assert (ART.parent/'candy-ricochet-hit-20261017/build.log').read_text(encoding='utf-8',errors='replace').find('PASS: six target faces')>=0
old=ROOT/'build/libs/wans_iron_expansion-1.2.1-candy-pixel-preview.6.jar'
new=ROOT/'build/libs/wans_iron_expansion-1.2.1-candy-pixel-preview.7.jar'
with zipfile.ZipFile(old) as a,zipfile.ZipFile(new) as b:
    assert b.testzip() is None and not set(a.namelist())-set(b.namelist())
    changed=sorted(n for n in set(a.namelist())&set(b.namelist()) if a.read(n)!=b.read(n))
    allowed={'META-INF/mods.toml','META-INF/MANIFEST.MF'}|{'cn/wanyiwige/alexcavesspellsbook/candy/'+n+'.class' for n in ['CandySpellEntity','client/CandyPixelScenes','client/CandyFieldPixelScenes']}
    assert set(changed)<=allowed,changed
    for name in names:
        for suffix in ['.png','.png.mcmeta']:
            p=RES/f'textures/entity/candy/pixel/{name}{suffix}'
            assert b.read('assets/alex_caves_spellsbook/'+p.relative_to(RES).as_posix())==p.read_bytes()
    field=b.read('cn/wanyiwige/alexcavesspellsbook/candy/client/CandyFieldPixelScenes.class')
    assert b'FrostmintSpearModel' in field and b'frost_boundary' not in field
report={'result':'PASS','newPixelTextures':len(names),'newModels':0,'caramelConnected':True,'caramelAsymmetric':True,'caramelStableOutline':True,
        'caramelPixels':len(points),'modifiedServerMethods':modified,'movementDamageAndTeleportPreserved':True,'ricochetCheckAlreadyPassed':True,
        'nativeSpearPreserved':True,'frostRainGroundCircleAbsent':True,'changedPreviousJarEntries':changed,'runtimeTested':False}
(ART/'scope-validation.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
print(json.dumps(report,indent=2))
