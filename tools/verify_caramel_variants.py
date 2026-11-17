"""Check shipped artwork, stable animation masks, and the scope of preview.8."""
from pathlib import Path
from zipfile import ZipFile
from PIL import Image
from io import BytesIO
import hashlib,json

root=Path(__file__).resolve().parents[1]
art=root/'artwork/candy-caramel-reflection-20261017'
prefix='assets/alex_caves_spellsbook/textures/entity/candy/pixel/'
jar=root/'build/libs/wans_iron_expansion-1.2.1-candy-pixel-preview.8.jar'
with ZipFile(root/'build/libs/wans_iron_expansion-1.2.1-candy-pixel-preview.7.jar') as old,ZipFile(jar) as new:
    before=set(old.namelist());after=set(new.namelist())
    assert not before-after
    changed=sorted(n for n in before&after if old.read(n)!=new.read(n))
    allowed={'META-INF/mods.toml','META-INF/MANIFEST.MF',
        'cn/wanyiwige/alexcavesspellsbook/candy/CandySpellEntity.class',
        'cn/wanyiwige/alexcavesspellsbook/candy/client/CandyDessertPixelScenes.class'}
    allowed.update(prefix+'caramel_pool_'+str(i)+'.png' for i in range(3))
    assert set(changed)<=allowed,changed
    added=sorted(after-before)
    assert all(n.startswith(prefix+'caramel_pool_shape') and n.endswith(('.png','.png.mcmeta')) for n in added)
    masks=[];counts=[]
    for shape in range(6):
        frames=[]
        for frame in range(3):
            name='caramel_pool_'+('' if shape==0 else 'shape'+str(shape)+'_')+str(frame)+'.png'
            data=new.read(prefix+name)
            assert data==(root/'src/main/resources'/prefix/name).read_bytes()
            im=Image.open(BytesIO(data)).convert('RGBA');assert im.size==(32,32)
            alpha=im.getchannel('A');assert set(alpha.getdata())=={0,255}
            frames.append(alpha.tobytes())
            pixels=[p for p in im.getdata() if p[3]]
            assert len(set(pixels))<=12
            if frame==0:counts.append(sum(min(p[:3])>=150 for p in pixels))
        assert frames[0]==frames[1]==frames[2]
        masks.append(frames[0])
        points={(x,y) for y in range(32) for x in range(32) if frames[0][y*32+x]}
        todo=[next(iter(points))];seen=set(todo)
        while todo:
            x,y=todo.pop()
            for p in ((x-1,y),(x+1,y),(x,y-1),(x,y+1)):
                if p in points and p not in seen:seen.add(p);todo.append(p)
        assert seen==points
    assert len(set(masks))==6
    original=Image.open(BytesIO(old.read(prefix+'caramel_pool_0.png'))).convert('RGBA')
    assert masks[0]==original.getchannel('A').tobytes()
    def stats(im):
        colors=[p for p in im.getdata() if p[3]]
        lum=[.2126*p[0]+.7152*p[1]+.0722*p[2] for p in colors]
        return {'luminanceRange':round(max(lum)-min(lum),2),'brightPixels':sum(min(p[:3])>=150 for p in colors)}
    prior=stats(original)
    updated=stats(Image.open(BytesIO(new.read(prefix+'caramel_pool_0.png'))).convert('RGBA'))
    assert updated['luminanceRange']>prior['luminanceRange']
    assert updated['brightPixels']>prior['brightPixels']
report={'result':'PASS','distinctConnectedShapes':6,'framesPerShape':3,'stableAnimationMasks':True,
    'originalFirstMaskPreserved':True,'before':prior,'after':updated,'brightPixelsPerShape':counts,
    'changedEntries':changed,'addedEntries':added,'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),
    'runtimeTested':False,'deployed':False}
(art/'caramel-validation.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
print(json.dumps(report,indent=2))
