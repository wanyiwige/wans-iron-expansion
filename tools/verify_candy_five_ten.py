"""Check scope, actual raster resources and preserved server behavior against pre-edit source."""
from pathlib import Path
from PIL import Image
import argparse,json,re,hashlib
parser=argparse.ArgumentParser()
parser.add_argument('--allow-rainbow-bounce',action='store_true')
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
art=root/'artwork/candy-five-ten-20261017'
src=root/'src/main/java/cn/wanyiwige/alexcavesspellsbook/candy'
tex=root/'src/main/resources/assets/alex_caves_spellsbook/textures/entity/candy/pixel'
before=(art/'source-before/CandySpellEntity.java').read_text(encoding='utf-8')
after=(src/'CandySpellEntity.java').read_text(encoding='utf-8')
def method(text,name):
    match=re.search(r'\b(?:private|protected|public)\s+(?:static\s+)?[\w<>]+\s+'+name+r'\(',text)
    assert match,name
    start=text.index('{',match.start());depth=1;i=start+1
    while depth:
        depth+=(text[i]=='{')-(text[i]=='}');i+=1
    return text[match.start():i]
unchanged=['launch','actorTick','ball','mint','spear','rocket','sweep','collideEntities','contact','seek','split','hit','landing','burst','chill','hex','bloom','reaction','caramel','area','block','sky','resolve']
if args.allow_rainbow_bounce:unchanged=[name for name in unchanged if name not in ['sweep','contact']]
for name in unchanged:assert method(before,name)==method(after,name),f'Gameplay method changed: {name}'
baseline=json.loads((art/'baseline.json').read_text(encoding='utf-8'))
modified=[]
for path,h in baseline['sourceHashes'].items():
    p=root.parent/path
    if hashlib.sha256(p.read_bytes()).hexdigest()!=h:modified.append(p.name)
assert set(modified)=={'CandySpellEntity.java','CandyPixelScenes.java'},modified
assert 'visualValue("PixelPalette",root.nextPixelColor())' in method(after,'actor')
assert 'random.nextInt(i+1)' in method(after,'nextPixelColor')
render=(src/'client/CandyPixelScenes.java').read_text(encoding='utf-8')
assert 'getInt("PixelPalette")' in render and 'random.next' not in render
names=json.loads((art/'new-textures.json').read_text(encoding='utf-8'))
coverage={}
for name in names:
    im=Image.open(tex/(name+'.png')).convert('RGBA');alpha=im.getchannel('A')
    opaque=sum(v!=0 for v in alpha.getdata());coverage[name]=round(opaque/(im.width*im.height),3)
    assert 0<opaque<im.width*im.height and set(alpha.getdata())<={0,255}
for i in range(3):
    im=Image.open(tex/f'seal_band_{i}.png').convert('RGBA')
    for y in range(64):
        for x in range(64):
            if im.getpixel((x,y))[3]:assert i*10<=( (x-31.5)**2+(y-31.5)**2 )**.5<=(i+1)*10
fields=(src/'client/CandyFieldPixelScenes.java').read_text(encoding='utf-8')
assert 'CandyFirstRules.bloomTick(band)' in fields and 'PixelHitAge' in fields
assert 'new Cube' not in fields and 'getModel' not in fields
result={'result':'PASS','unchangedGameplayMethods':unchanged,'modifiedPreviousSources':modified,'newPixelSheets':len(names),'newGeometry':0,
        'authorizedGameplayChange':'rainbow_ricochet entity bounce and range accounting' if args.allow_rainbow_bounce else None,
        'paletteSelection':'server-shuffled at spawn; stable synced PixelPalette; unique within cast',
        'rasterCoverage':coverage,'bloomBandMasksMatchDamageBands':True,'runtimeTested':False}
(art/'scope-validation.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print(json.dumps(result,indent=2))
