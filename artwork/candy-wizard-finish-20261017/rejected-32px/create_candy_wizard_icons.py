"""Native 32px candy-wizard inventory sprites, drawn without smooth scaling."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json
root=Path(__file__).resolve().parents[1]
tex=root/'src/main/resources/assets/alex_caves_spellsbook/textures/item'
art=root/'artwork/candy-wizard-finish-20261017';art.mkdir(exist_ok=True)
P={'ink':'#21182B','shade':'#30203D','cloth':'#4A2E5B','light':'#76517F','berry':'#9B456F','pink':'#CF82A0','cream':'#F4DDB6','ivory':'#FFF0CF','tan':'#BA986F','mint':'#92C7B0','teal':'#557F79','gold':'#C49356','goldlight':'#F3CE88','sole':'#9F6B46'}
items=['hat','robe','leggings','boots'];ims=[]
def candy(d,cx,cy):
 d.polygon([(cx-5,cy-2),(cx-3,cy),(cx-5,cy+2)],fill=P['mint'])
 d.polygon([(cx+5,cy-2),(cx+3,cy),(cx+5,cy+2)],fill=P['mint'])
 d.polygon([(cx,cy-4),(cx+4,cy),(cx,cy+4),(cx-4,cy)],fill=P['berry'],outline=P['ink'])
 d.polygon([(cx,cy-2),(cx+2,cy),(cx,cy+2),(cx-2,cy)],fill=P['pink'])
 d.line([(cx-1,cy-1),(cx,cy-2),(cx+1,cy-1)],fill=P['ivory'])
for kind in items:
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 if kind=='hat':
  d.polygon([(2,24),(6,22),(10,22),(11,17),(13,13),(14,8),(17,3),(22,1),(26,3),(26,5),(22,5),(21,7),(22,12),(24,16),(25,22),(29,23),(31,26),(27,29),(6,29),(1,27)],fill=P['ink'])
  d.polygon([(4,25),(10,23),(24,23),(29,25),(26,27),(6,27)],fill=P['shade'])
  d.line((5,28,26,28),fill=P['cream'])
  d.polygon([(11,22),(12,17),(14,13),(15,8),(18,4),(22,2),(24,3),(21,4),(19,8),(20,13),(23,18),(24,22)],fill=P['cloth'])
  d.polygon([(12,18),(14,14),(16,13),(17,16),(16,19)],fill=P['light'])
  d.line([(16,9),(17,7),(18,7)],fill=P['light'])
  d.line([(20,14),(22,17),(22,19)],fill=P['shade'])
  d.polygon([(10,22),(11,20),(24,20),(25,23),(11,24)],fill=P['berry'])
  d.line((12,21,22,21),fill=P['pink'])
  candy(d,17,22)
  d.point((23,6),fill=P['gold']);d.rectangle((24,7,25,8),fill=P['pink'])
 elif kind=='robe':
  d.polygon([(10,2),(21,2),(29,7),(31,18),(25,20),(23,15),(23,29),(8,29),(8,15),(6,20),(0,18),(2,7)],fill=P['ink'])
  d.polygon([(3,8),(10,4),(12,7),(8,15),(5,18),(2,17)],fill=P['cloth'])
  d.polygon([(21,4),(28,8),(29,17),(26,18),(23,12)],fill=P['cloth'])
  d.polygon([(9,8),(22,8),(22,28),(9,28)],fill=P['cloth'])
  d.polygon([(3,8),(9,5),(10,8),(4,11)],fill=P['berry'])
  d.polygon([(22,5),(27,8),(28,11),(22,9)],fill=P['berry'])
  d.polygon([(10,3),(14,4),(15,9),(12,12),(10,8)],fill=P['cream'])
  d.polygon([(21,3),(17,4),(16,9),(19,12),(21,8)],fill=P['cream'])
  d.line((11,4,13,8),fill=P['ivory']);d.line((20,4,18,8),fill=P['ivory'])
  candy(d,16,10)
  d.rectangle((10,13,12,23),fill=P['berry']);d.rectangle((20,13,21,23),fill=P['berry'])
  d.point((16,16),fill=P['goldlight']);d.point((16,19),fill=P['gold'])
  d.polygon([(10,24),(12,23),(13,27),(10,27)],fill=P['light'])
  d.line([(19,24),(20,25),(20,27)],fill=P['shade'])
  d.line((9,22,22,22),fill=P['berry'],width=2);d.rectangle((15,22,17,24),fill=P['gold']);d.point((16,23),fill=P['goldlight'])
  d.rectangle((2,16,6,18),fill=P['cream']);d.rectangle((25,16,29,18),fill=P['cream'])
  d.line((2,15,6,15),fill=P['mint']);d.line((25,15,29,15),fill=P['mint'])
  d.line((9,28,22,28),fill=P['cream']);d.point((11,27),fill=P['ivory'])
 elif kind=='leggings':
  d.polygon([(7,2),(24,2),(25,12),(24,29),(18,30),(16,15),(14,30),(7,29),(6,12)],fill=P['ink'])
  d.rectangle((8,3,23,8),fill=P['berry']);d.line((9,4,22,4),fill=P['pink'])
  d.rectangle((14,4,17,7),fill=P['gold']);d.rectangle((15,5,16,6),fill=P['goldlight'])
  d.polygon([(8,9),(14,9),(14,18),(12,28),(8,28)],fill=P['cloth'])
  d.polygon([(17,9),(23,9),(23,28),(19,28),(17,18)],fill=P['cloth'])
  d.polygon([(9,13),(11,12),(12,18),(11,23),(9,24)],fill=P['light'])
  d.line([(21,17),(22,20),(21,26)],fill=P['shade'])
  d.rectangle((8,27,12,28),fill=P['cream']);d.rectangle((19,27,23,28),fill=P['cream'])
  d.line((9,26,11,26),fill=P['tan']);d.line((20,26,22,26),fill=P['tan'])
  # Waist-mounted syrup vial and the opposite folded candy pouch.
  d.rectangle((3,8,6,10),fill=P['gold']);d.rectangle((2,11,7,18),fill=P['teal'])
  d.rectangle((3,11,6,16),fill=P['mint']);d.rectangle((3,14,6,17),fill=P['berry']);d.line((3,11,3,15),fill=P['ivory'])
  d.rectangle((24,10,28,16),fill=P['berry']);d.rectangle((24,9,28,11),fill=P['cream']);d.point((26,11),fill=P['pink'])
  d.point((20,12),fill=P['goldlight']);d.point((21,13),fill=P['gold'])
 else:
  for x,dy in [(2,1),(17,0)]:
   d.polygon([(x+3,4+dy),(x+11,4+dy),(x+11,21+dy),(x+12,25+dy),(x+10,29+dy),(x,29+dy),(x,24+dy),(x+3,20+dy)],fill=P['ink'])
   d.rectangle((x+4,6+dy,x+10,21+dy),fill=P['cloth'])
   d.rectangle((x+4,6+dy,x+10,8+dy),fill=P['cream']);d.line((x+4,6+dy,x+10,6+dy),fill=P['ivory'])
   d.rectangle((x+4,9+dy,x+10,10+dy),fill=P['berry'])
   d.rectangle((x+6,13+dy,x+9,16+dy),fill=P['gold']);d.rectangle((x+7,14+dy,x+8,15+dy),fill=P['goldlight'])
   d.polygon([(x+3,22+dy),(x+8,21+dy),(x+10,24+dy),(x+10,26+dy),(x+1,26+dy),(x+1,24+dy)],fill=P['cloth'])
   d.line((x+2,24+dy,x+6,23+dy),fill=P['light']);d.point((x+2,25+dy),fill=P['pink'])
   d.line((x+1,27+dy,x+10,27+dy),fill=P['sole']);d.line((x+2,28+dy,x+9,28+dy),fill=P['tan'])
 name='candy_wizard_'+kind
 im.save(tex/(name+'.png'));im.save(art/(name+'.png'))
 (tex/(name+'.png.mcmeta')).write_text(json.dumps({'texture':{'blur':False,'clamp':True}}))
 ims.append((kind,im))
sheet=Image.new('RGB',(800,240),'#211F2B');d=ImageDraw.Draw(sheet);font=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',20)
for i,((kind,im),label) in enumerate(zip(ims,['糖霜巫师帽','糖霜巫师袍','糖霜巫师长裤','糖霜巫师靴'])):
 large=im.resize((160,160),Image.Resampling.NEAREST);sheet.paste(large,(i*200+20,20),large)
 d.text((i*200+35,197),label,font=font,fill=P['cream'])
sheet.save(art/'inventory-icons.png')
print('Four native 32x32 pixel icons and contact sheet written.')
