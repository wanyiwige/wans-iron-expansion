"""One native 16px sprite: a frosting crescent catches a suspended rainbow sweet."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import json
root=Path(__file__).resolve().parents[1];art=root/'artwork/candy-spellbook-20261018'
palette={'.':(0,0,0,0),'o':'#342639','d':'#684652','s':'#B48862','c':'#E8CD9D','w':'#FFF0CC',
         'y':'#CC9A54','p':'#9B456F','r':'#DA88A1','g':'#A7D4B3','t':'#537F79','b':'#729BB4'}
rows=[
'......scco......',
'....scwwcso.....',
'...scw....so....',
'...cw..rr.......',
'..sc...prg......',
'..cw..rwggt.....',
'..cw...pbt......',
'..sc....t.......',
'...cs.....ys....',
'....ssccssy.....',
'.....oyoyo......',
'.....y.y.y......',
'....pr.c.gt.....',
'....rp.cs.tg....',
'.......s........',
'................']
assert len(rows)==16 and all(len(r)==16 for r in rows)
im=Image.new('RGBA',(16,16))
for y,row in enumerate(rows):
 for x,k in enumerate(row):im.putpixel((x,y),palette[k] if k=='.' else tuple(bytes.fromhex(palette[k][1:]))+(255,))
path=root/'src/main/resources/assets/alex_caves_spellsbook/textures/item/candy_starcatcher.png'
im.save(path);im.save(art/'candy_starcatcher.png')
path.with_suffix('.png.mcmeta').write_text(json.dumps({'texture':{'blur':False,'clamp':True}}))
preview=Image.new('RGB',(430,400),'#35343C');large=im.resize((320,320),Image.Resampling.NEAREST)
preview.paste(large,(55,15),large);d=ImageDraw.Draw(preview);font=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',22)
d.text((142,350),'糖梦捕星环',font=font,fill='#E8CD9D');preview.save(art/'preview.png')
print('16x16 native transparent sprite and preview written.')
