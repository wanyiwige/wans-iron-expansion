"""Draw native-grid item art using Iron's blank rune/orb silhouettes as the base."""
from pathlib import Path
from PIL import Image,ImageDraw
import json
root=Path(__file__).resolve().parents[1];art=root/'artwork/candy-rune-orb-20261017'
tex=root/'src/main/resources/assets/alex_caves_spellsbook/textures/item'
im=Image.open(art/'references/blank_rune.png').convert('RGBA');assert im.size==(16,16)
d=ImageDraw.Draw(im)
# Cut-in shadow, luminous wrapped-candy sigil, restrained mint/gold accents.
d.polygon([(5,7),(7,5),(10,6),(11,8),(10,11),(7,12),(5,10),(3,11),(3,7)],fill='#483B48')
d.polygon([(5,8),(7,6),(10,7),(10,10),(7,11),(5,10)],fill='#C9598D')
d.line([(6,8),(7,7),(9,7),(9,9),(7,9),(7,10)],fill='#FFE3B1',width=1)
d.point((6,9),fill='#FFF4DD');d.point((9,10),fill='#F58BB3')
d.polygon([(3,7),(5,8),(5,10),(3,11)],fill='#F096B2');d.point((3,8),fill='#FFE9CC')
d.polygon([(11,7),(12,6),(12,10),(11,10)],fill='#EAA868');d.point((12,7),fill='#FFF0C9')
d.point((6,4),fill='#9AD7BA');d.point((11,12),fill='#9AD7BA')
im.save(tex/'candy_rune.png')

im=Image.open(art/'references/upgrade_orb_base.png').convert('RGBA');assert im.size==(16,16)
d=ImageDraw.Draw(im)
d.polygon([(6,2),(10,2),(13,5),(13,10),(10,13),(6,13),(3,10),(3,5)],fill='#512B50')
d.line([(4,5),(6,3),(10,3),(12,5)],fill='#E899BD',width=1)
d.line([(12,6),(12,10),(10,12),(6,12)],fill='#AD598E',width=1)
d.line([(5,10),(4,8),(4,6)],fill='#FFCB9C',width=1)
# Bright, faceted sugar core with a cream spiral rather than a smooth gradient.
d.polygon([(7,4),(10,5),(11,8),(9,11),(6,11),(5,8),(6,5)],fill='#F495BC')
d.polygon([(7,5),(9,5),(10,7),(9,9),(7,10),(6,8)],fill='#FFD292')
d.line([(7,6),(9,6),(9,8),(7,8),(7,9)],fill='#FFF3D4',width=1)
d.point((10,9),fill='#D6629B');d.point((6,10),fill='#BE628E')
# Small mint sweets sit on opposite sides of the orb's native dark frame.
d.rectangle((2,6,3,7),fill='#9DDDBF');d.point((2,6),fill='#E7FFE2')
d.rectangle((12,10,13,11),fill='#84BBAA');d.point((12,10),fill='#D7F8CE')
d.point((6,2),fill='#FFF2CE');d.point((10,12),fill='#FFE1AC')
im.save(tex/'candy_upgrade_orb.png')
for name in ['candy_rune','candy_upgrade_orb']:
 (tex/(name+'.png.mcmeta')).write_text(json.dumps({'texture':{'blur':False,'clamp':True}}))
sheet=Image.new('RGB',(640,360),'#252B35');d=ImageDraw.Draw(sheet)
for i,name in enumerate(['candy_rune','candy_upgrade_orb']):
 im=Image.open(tex/(name+'.png'));im=im.resize((256,256),Image.Resampling.NEAREST)
 sheet.paste(im,(32+i*320,28),im);d.text((32+i*320,306),name,fill='#FFF1DF')
sheet.save(art/'candy-rune-orb-preview.png')
print('Created two 16x16 item textures and preview.')
