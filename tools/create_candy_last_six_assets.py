"""Original pixel effects and six ice-cream atlases fitted to Blockbench geometry."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,math
root=Path(__file__).resolve().parents[1]
art=root/'artwork/candy-last-six-20261017';res=root/'src/main/resources/assets/alex_caves_spellsbook'
tex=res/'textures/entity/candy/pixel';models=res/'models/candy'
made=[]
flavors=[('vanilla',['#FFF6CC','#EADCA9','#C5A878','#FFFDF0']),('strawberry',['#FFB0BB','#E27B98','#A54C70','#FFE2DB']),('chocolate',['#AC7955','#805235','#573528','#E9BB86']),('mint',['#ADE7CA','#73BBA6','#3F8278','#E8FFF0']),('blueberry',['#A8C9F1','#7798D2','#4B5B98','#E8F5FF']),('grape',['#D3AFEB','#A77DCD','#72508E','#F7E7FF'])]
def save(name,im):
 im.save(tex/(name+'.png'));(tex/(name+'.png.mcmeta')).write_text(json.dumps({'texture':{'blur':False,'clamp':True}}));made.append(name)
geometry=json.loads((art/'icecream_geometry.json').read_text(encoding='utf-8'))
face_tiles={'up':(0,0),'north':(16,0),'east':(32,0),'south':(0,16),'west':(16,16),'down':(32,16)}
for index,(name,pal) in enumerate(flavors):
 atlas=Image.new('RGBA',(64,64));d=ImageDraw.Draw(atlas)
 for face,(ox,oy) in face_tiles.items():
  d.rectangle((ox,oy,ox+15,oy+15),fill=pal[0] if face=='up' else pal[1] if face=='down' else pal[0])
  for y in range(16):
   for x in range(16):
    # Coarse cream folds, irregular two-pixel shade clusters, no gradient or blur.
    noise=((x//2)*13+(y//2)*7+(x//5)*19)%17
    c=pal[1] if noise<5 else pal[0]
    if face=='down' or (face!='up' and y>11 and noise<11):c=pal[2] if noise<4 else pal[1]
    if face=='up' and noise>13 or (face!='down' and x<7 and y<5 and noise>9):c=pal[3]
    d.point((ox+x,oy+y),fill=c)
  if face!='down':
   d.line([(ox+3,oy+5),(ox+5,oy+3),(ox+9,oy+3)],fill=pal[3],width=1)
   d.line([(ox+8,oy+11),(ox+11,oy+10),(ox+12,oy+8)],fill=pal[1],width=1)
  for x,y in [(10,6),(4,12),(13,4)]:
   d.rectangle((ox+x,oy+y,ox+x+1,oy+y),fill=pal[2])
 save('icecream_atlas_'+name,atlas)
 model={'ambientocclusion':False,'texture_size':[64,64],'textures':{'cream':'alex_caves_spellsbook:entity/candy/pixel/icecream_atlas_'+name},'elements':[]}
 for src in geometry['elements']:
  e={'name':src['name'],'from':src['from'],'to':src['to'],'faces':{}}
  x,y,z=e['from'];xx,yy,zz=e['to']
  coords={'up':[x,z,xx,zz],'down':[x,16-zz,xx,16-z],'north':[16-xx,16-yy,16-x,16-y],'south':[x,16-yy,xx,16-y],'east':[16-zz,16-yy,16-z,16-y],'west':[z,16-yy,zz,16-y]}
  for face,uv in coords.items():
   ox,oy=face_tiles[face];e['faces'][face]={'uv':[(uv[0]+ox)/4,(uv[1]+oy)/4,(uv[2]+ox)/4,(uv[3]+oy)/4],'texture':'#cream'}
  model['elements'].append(e)
 (models/('icecream_scoop_'+name+'.json')).write_text(json.dumps(model,indent=2),encoding='utf-8')
 # Each mouthful keeps its flavor in the impact liquid and droplets.
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 d.polygon([(8,5),(14,3),(19,6),(25,5),(28,10),(24,14),(30,18),(25,23),(20,22),(17,28),(10,26),(9,22),(3,23),(2,17),(6,14),(3,10)],fill=pal[2])
 d.polygon([(8,7),(14,5),(19,8),(24,7),(26,10),(22,15),(27,18),(24,21),(19,20),(16,25),(11,24),(11,20),(5,21),(4,17),(8,14),(5,10)],fill=pal[0])
 d.line([(7,11),(11,8),(16,8)],fill=pal[3],width=2);d.line([(15,22),(20,19),(23,19)],fill=pal[1],width=2)
 save('icecream_splat_'+name,im)
 im=Image.new('RGBA',(8,8));d=ImageDraw.Draw(im);d.polygon([(3,0),(5,2),(6,5),(4,7),(1,6),(1,3)],fill=pal[1]);d.rectangle((2,2,3,4),fill=pal[3]);save('icecream_drop_'+name,im)

# Frosting domain: separate radial wedges provide readable bounds and an open center.
im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
d.polygon([(16,16),(3,9),(3,5),(8,2),(15,1),(17,4),(18,9)],fill='#D99FB0')
d.polygon([(16,15),(5,8),(6,5),(10,3),(15,3),(15,8)],fill='#FFF2D9')
d.line([(5,6),(8,4),(12,4)],fill='#FFFFFF',width=1)
for x,y,c in [(7,6,'#FF86AE'),(12,5,'#A8DCE6'),(10,9,'#D0A4E8')]:d.rectangle((x,y,x+1,y+1),fill=c)
for y in range(32):
 for x in range(32):
  distance=math.hypot(x-15.5,y-15.5)
  if distance>15.5 or distance<8:im.putpixel((x,y),(0,0,0,0))
save('banquet_frosting',im)
im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
d.polygon([(2,4),(7,1),(13,3),(15,9),(11,14),(5,15),(1,10)],fill='#502B64')
d.line([(3,9),(5,4),(9,3),(12,5),(11,9),(7,11),(6,8),(8,6),(10,7)],fill='#B66BB7',width=2)
d.line([(5,5),(8,3),(10,4)],fill='#F2B7D9');save('licorice_knot',im)
im=Image.new('RGBA',(32,8));d=ImageDraw.Draw(im)
for x in range(32):
 y=2+((x//3)%3);d.rectangle((x,y,x,y+2),fill='#713A86');d.point((x,y),fill='#DFA1DC')
save('licorice_tendril',im)
im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
d.polygon([(6,1),(10,1),(10,5),(14,5),(14,10),(10,10),(10,14),(6,14),(6,10),(2,10),(2,5),(6,5)],fill='#76C998')
d.rectangle((7,3,8,12),fill='#E5FFE1');d.rectangle((4,6,12,8),fill='#E5FFE1');save('feast_heal',im)
im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
d.polygon([(1,10),(3,5),(7,2),(13,2),(14,5),(9,5),(5,8),(4,12)],fill='#FFB5CE')
d.line([(2,10),(4,6),(8,3),(12,3)],fill='#FFF7DF',width=1);d.rectangle((11,10,13,11),fill='#FFDDA2');save('cane_swipe',im)
im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
d.polygon([(2,4),(5,2),(9,3),(10,7),(7,10),(3,9)],fill='#C4809D')
d.rectangle((3,4,5,5),fill='#FFE5CD');d.rectangle((11,9,13,11),fill='#D5A9BC');d.point((5,13),fill='#FFF0DA');save('worm_crumbs',im)
sheet=Image.new('RGB',(1120,math.ceil(len(made)/7)*160+20),'#252B35');d=ImageDraw.Draw(sheet)
for i,name in enumerate(made):
 im=Image.open(tex/(name+'.png'));scale=max(1,min(120//im.width,120//im.height));im=im.resize((im.width*scale,im.height*scale),Image.Resampling.NEAREST)
 x=8+i%7*160;y=12+i//7*160;sheet.paste(im,(x,y),im);d.text((x,y+126),name,fill='#FFF0DC')
sheet.save(art/'pixel-sheets-21-26.png');(art/'new-textures.json').write_text(json.dumps(made,indent=2))
(art/'flavors.json').write_text(json.dumps([n for n,p in flavors],indent=2))
print('Created',len(made),'textures; six 8-cube ice-cream models from Blockbench geometry.')
