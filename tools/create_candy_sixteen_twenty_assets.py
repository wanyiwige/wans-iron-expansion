"""Native-grid sheets for overdrive and snare; 18-20 reference Alex's Caves directly."""
from pathlib import Path
from PIL import Image,ImageDraw
import json
root=Path(__file__).resolve().parents[1]
tex=root/'src/main/resources/assets/alex_caves_spellsbook/textures/entity/candy/pixel'
art=root/'artwork/candy-sixteen-twenty-20261017';art.mkdir(exist_ok=True)
made=[]
def save(name,im):
    im.save(tex/(name+'.png'))
    (tex/(name+'.png.mcmeta')).write_text(json.dumps({'texture':{'blur':False,'clamp':True}}))
    made.append(name)
im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
d.polygon([(8,0),(5,5),(6,6),(1,11),(6,10),(5,15),(14,5),(9,6),(11,1)],fill='#AC4C29')
d.polygon([(8,1),(6,5),(8,6),(3,10),(8,8),(7,12),(12,6),(8,8),(10,3)],fill='#FFC854')
d.line([(8,2),(7,4),(8,5)],fill='#FFF2BD');d.line([(5,9),(8,7),(9,7)],fill='#FFF2BD')
save('overdrive_bolt',im)
for frame in range(3):
 im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
 for i in range(3):
  x=2+i*5;yy=1+frame*2+i%2
  d.line([(x,yy),(x,yy+6-i),(x+1,yy+7-i)],fill=['#FFE6A0','#FFAE73','#FF8FAF'][i],width=1)
 d.rectangle((7,12-frame,8,13-frame),fill='#FFF8E2');save('overdrive_speed_'+str(frame),im)
im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
d.polygon([(8,1),(13,5),(14,10),(9,14),(3,12),(1,6)],fill='#744276')
d.polygon([(8,2),(12,5),(10,8),(4,10),(2,6)],fill='#F49CC0')
d.polygon([(4,10),(10,8),(12,5),(13,10),(9,13)],fill='#BD82DD')
d.line([(5,5),(8,3),(10,4)],fill='#FFF0C8',width=1);save('snare_knot',im)
im=Image.new('RGBA',(32,8));d=ImageDraw.Draw(im)
colors=['#FF81A7','#FFB96B','#FFE9A1','#9CDCAC','#81C9EB','#C49BEB']
for y,c in enumerate(colors,1):
 for x in range(32):
  if x<2 and (x+y)%2 or x>29 and (x+y)%2:continue
  d.point((x,y),fill=c)
d.line((3,1,28,1),fill='#FFEAD6');d.line((3,6,28,6),fill='#8769AF')
save('snare_band',im)
sheet=Image.new('RGB',(1000,240),'#252B35');d=ImageDraw.Draw(sheet)
for i,name in enumerate(made):
 im=Image.open(tex/(name+'.png'));scale=min(128//im.width,128//im.height);im=im.resize((im.width*scale,im.height*scale),Image.Resampling.NEAREST)
 sheet.paste(im,(12+i*164,20),im);d.text((12+i*164,166),name,fill='#FFF0DD')
sheet.save(art/'pixel-sheets-16-20.png');(art/'new-textures.json').write_text(json.dumps(made,indent=2))
print('Created',len(made),'pixel textures for spells 16-17; no replacement artwork for 18-20.')
