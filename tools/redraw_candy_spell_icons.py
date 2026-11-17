"""26 native-grid 16px spell icons: hand-placed forms and hard pixel light clusters."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import math,json,re,hashlib
root=Path(__file__).resolve().parents[1];art=root/'artwork/candy-spell-icons-redraw-20261018'
dest=root/'src/main/resources/assets/alex_caves_spellsbook/textures/gui/spell_icons'
# shadow, body, light, specular; each material has a short, readable ramp.
P={
 'pink':('#612248','#BD4779','#F58EAE','#FFE3C8'),
 'mint':('#164F53','#3C9E91','#91E0BE','#E9FFE0'),
 'ice':('#17567D','#42ACD4','#9DF2EE','#F4FFFF'),
 'amber':('#64301B','#B96B27','#F0B85E','#FFF1AD'),
 'cream':('#805A55','#C69F83','#F2D9AF','#FFF4DB'),
 'violet':('#3C1956','#7D329D','#C374E0','#FFE0FF'),
 'licorice':('#160F28','#38213D','#72416D','#DB7AA9'),
 'blue':('#263D70','#477EBB','#9BCDEB','#E2FAFF')}
def new():return Image.new('RGBA',(16,16))
def draw(im):return ImageDraw.Draw(im)
def dot(d,x,y,c):d.point((x,y),fill=c)
def line(d,pts,c,w=1):d.line(pts,fill=c,width=w)
def star(d,x,y,c):
 line(d,[(x-1,y),(x+1,y)],c);line(d,[(x,y-1),(x,y+1)],c)
def cube(d,x,y,r,mat):
 s,b,l,h=P[mat]
 d.polygon([(x-r,y-r+1),(x-r+1,y-r),(x+r-1,y-r),(x+r,y-r+1),(x+r,y+r-1),(x+r-1,y+r),(x-r+1,y+r),(x-r,y+r-1)],fill=b)
 line(d,[(x-r+1,y-r),(x+r-1,y-r),(x+r,y-r+1)],l)
 line(d,[(x-r,y-r+1),(x-r,y+r-1)],l)
 line(d,[(x+r,y-r+2),(x+r,y+r-1),(x+r-1,y+r),(x-r+1,y+r)],s)
 dot(d,x-r+1,y-r+1,h)
def wrapped(d,x,y,mat='amber'):
 s,b,l,h=P[mat]
 d.polygon([(x-5,y-2),(x-3,y),(x-5,y+2)],fill=l)
 d.polygon([(x+5,y-2),(x+3,y),(x+5,y+2)],fill=s)
 cube(d,x,y,2,mat)
def mint(d,x,y,r=3,mat='pink'):
 s,b,l,h=P[mat];d.ellipse((x-r,y-r,x+r,y+r),fill=P['cream'][2])
 line(d,[(x-r+1,y+r-1),(x,y+r),(x+r-1,y+r-1)],P['cream'][0])
 for dx,dy in [(-1,-2),(0,-2),(1,-1),(1,0),(0,1),(-1,1),(-2,0)]:
  if abs(dx)<=r and abs(dy)<=r:dot(d,x+dx,y+dy,b)
 dot(d,x,y,h);dot(d,x-r+1,y-r+1,P['cream'][3])
def person(d,x,y,mat='licorice',scale=1):
 s,b,l,h=P[mat];d.rectangle((x,y,x+1,y+1),fill=l)
 d.polygon([(x-1,y+3),(x+2,y+3),(x+3,y+6),(x+1,y+6),(x,y+5),(x-1,y+6),(x-2,y+6)],fill=b)
 dot(d,x,y+3,l)
def spear(d,x,y,length=7):
 line(d,[(x,y),(x,y+length-2)],P['mint'][1]);line(d,[(x-1,y),(x-1,y+length-3)],P['ice'][2])
 d.polygon([(x-2,y+length-3),(x+2,y+length-3),(x,y+length)],fill=P['cream'][2]);dot(d,x,y+length-2,P['cream'][3])
def frost(d,x,y):
 for dx,dy in [(0,-3),(2,-2),(3,0),(2,2),(0,3),(-2,2),(-3,0),(-2,-2)]:line(d,[(x,y),(x+dx,y+dy)],P['ice'][1])
 star(d,x,y,P['ice'][3])
def bear(d,x,y,r=3):
 s,b,l,h=P['amber'];d.rectangle((x-r,y-r-1,x-r+1,y-r),fill=l);d.rectangle((x+r-1,y-r-1,x+r,y-r),fill=b)
 d.polygon([(x-r,y-r),(x+r,y-r),(x+r,y+r),(x-r,y+r)],fill=b)
 line(d,[(x-r,y-r+1),(x-r,y+r-1)],l);d.rectangle((x-1,y+1,x+1,y+2),fill=l)
 dot(d,x-1,y-1,s);dot(d,x+1,y-1,s);dot(d,x,y+1,s);dot(d,x-1,y-r,h)
def ginger(d,x,y,small=False):
 s,b,l,h=P['amber'];d.rectangle((x,y,x+1,y+1),fill=l)
 line(d,[(x-1,y+3),(x+2,y+3)],b,2);line(d,[(x,y+2),(x,y+5)],b,2)
 line(d,[(x,y+5),(x-1,y+6)],b);line(d,[(x+1,y+5),(x+2,y+6)],b)
 dot(d,x,y,P['cream'][3]);dot(d,x,y+3,P['pink'][2]);dot(d,x+1,y+4,P['mint'][2])
def scoop(d,x,y,mat):
 cube(d,x,y,2,mat);s,b,l,h=P[mat]
 line(d,[(x-1,y-2),(x+1,y-2)],h);dot(d,x-1,y+2,b);dot(d,x+1,y+3,l)
 dot(d,x,y-1,P['pink'][1] if mat!='pink' else P['amber'][2])

def icon_01():
 im=new();d=draw(im)
 line(d,[(1,12),(5,13),(3,8),(8,6),(12,3)],P['violet'][0],3)
 line(d,[(1,12),(5,13),(3,8)],P['pink'][2]);line(d,[(3,8),(8,6)],P['mint'][2]);line(d,[(7,6),(11,3)],P['amber'][2])
 cube(d,11,4,3,'pink');dot(d,9,2,P['cream'][3]);line(d,[(0,5),(0,10),(2,10)],P['ice'][0]);return im
def icon_02():
 im=new();d=draw(im);line(d,[(1,13),(4,11),(5,8),(8,7)],P['amber'][0],2)
 cube(d,2,12,1,'pink');cube(d,5,9,1,'mint');wrapped(d,10,5)
 line(d,[(1,7),(1,5),(3,5)],P['amber'][2]);return im
def icon_03():
 im=new();d=draw(im);s,b,l,h=P['ice']
 d.polygon([(6,2),(9,5),(9,10),(6,13),(3,10),(3,6)],fill=b)
 d.polygon([(6,2),(6,12),(4,9),(4,6)],fill=l);line(d,[(6,3),(6,6)],h)
 line(d,[(6,7),(8,8),(6,10)],s)
 for x,y in [(12,3),(13,9),(11,13)]:d.polygon([(x,y-2),(x+1,y),(x,y+2),(x-1,y)],fill=l)
 dot(d,1,11,P['pink'][1]);return im
def icon_04():
 im=new();d=draw(im);cube(d,4,10,2,'pink');cube(d,10,11,2,'amber');cube(d,8,5,3,'pink')
 line(d,[(9,2),(10,1),(11,2)],P['amber'][1]);star(d,12,3,P['amber'][3]);star(d,6,8,P['cream'][3])
 for x,y in [(2,4),(13,8),(5,14)]:dot(d,x,y,P['amber'][2])
 return im
def icon_05():
 im=new();d=draw(im);d.arc((1,1,14,14),0,300,fill=P['mint'][0],width=2);person(d,7,5)
 mint(d,4,4,3,'mint');mint(d,12,7,2,'pink');mint(d,6,12,2,'amber');return im
def icon_06():
 im=new();d=draw(im);d.arc((0,0,15,15),220,348,fill=P['mint'][1],width=3);d.arc((2,1,15,14),225,330,fill=P['cream'][3],width=1)
 line(d,[(1,13),(6,9),(10,4)],P['pink'][1],2);mint(d,10,5,4)
 line(d,[(1,14),(5,11)],P['mint'][2]);return im
def icon_07():
 im=new();d=draw(im);line(d,[(1,13),(14,13)],P['mint'][0]);line(d,[(0,9),(3,9)],P['mint'][1]);line(d,[(3,5),(6,5)],P['pink'][1])
 mint(d,3,10,2,'mint');mint(d,8,8,2,'pink');mint(d,12,5,3,'amber');dot(d,13,12,P['cream'][2]);return im
def icon_08():
 im=new();d=draw(im);s,b,l,h=P['licorice']
 d.polygon([(1,13),(3,9),(6,8),(7,4),(11,2),(14,3),(12,5),(10,6),(10,10),(6,11),(5,14)],fill=b)
 line(d,[(2,12),(4,10),(7,10),(8,6),(11,4),(14,4)],l,2)
 line(d,[(4,13),(6,12),(10,12),(12,10),(14,10)],P['violet'][1],2);line(d,[(7,12),(10,12),(12,10)],l)
 line(d,[(9,4),(11,3),(13,3)],h);dot(d,5,10,P['pink'][2]);return im
def icon_09():
 im=new();d=draw(im);d.ellipse((1,7,14,14),outline=P['pink'][0]);d.ellipse((3,8,12,12),outline=P['pink'][2])
 for x,y in [(7,3),(4,5),(10,5),(4,8),(10,8)]:cube(d,x,y,2,'pink')
 star(d,7,7,P['cream'][3]);dot(d,7,2,P['cream'][2]);dot(d,1,10,P['mint'][1]);return im
def icon_10():
 im=new();d=draw(im);spear(d,3,1,9);spear(d,8,0,12);spear(d,13,3,10)
 for x,y in [(1,4),(6,5),(11,1),(10,14)]:dot(d,x,y,P['ice'][2])
 return im
def icon_11():
 im=new();d=draw(im);line(d,[(1,14),(6,9)],P['violet'][1],3);line(d,[(2,14),(8,8)],P['pink'][2]);star(d,2,11,P['ice'][2])
 d.polygon([(6,6),(10,2),(13,5),(9,10)],fill=P['violet'][1]);line(d,[(7,5),(10,3),(11,4)],P['violet'][2]);line(d,[(8,8),(10,6)],P['cream'][2],2)
 d.rectangle((11,1,12,2),fill=P['mint'][2]);dot(d,4,8,P['violet'][2]);dot(d,1,7,P['cream'][2]);return im
def icon_12():
 im=new();d=draw(im);d.ellipse((1,9,14,14),fill=P['violet'][0],outline=P['violet'][2]);frost(d,8,9)
 for x,y,r in [(4,5,2),(9,3,2),(12,7,1)]:
  d.ellipse((x-r,y-r,x+r,y+r),fill=P['violet'][1]);dot(d,x-r,y-r,P['violet'][3])
 line(d,[(2,12),(5,11),(6,13),(9,12),(12,13)],P['ice'][2]);return im
def icon_13():
 im=new();d=draw(im);s,b,l,h=P['amber']
 pts=[(1,10),(4,9),(3,7),(6,7),(8,8),(11,7),(13,9),(15,10),(13,12),(14,14),(10,14),(8,13),(5,14),(1,13),(0,11)]
 d.polygon(pts,fill=s);d.polygon([(1,10),(5,9),(7,10),(11,8),(14,10),(11,12),(7,11),(5,13),(1,12)],fill=b)
 line(d,[(2,10),(4,10),(5,9)],h);line(d,[(8,10),(11,9),(12,10)],l);line(d,[(7,12),(9,12)],h)
 d.polygon([(7,2),(9,5),(9,6),(7,7),(6,5)],fill=l);dot(d,7,4,h);return im
def icon_14():
 im=new();d=draw(im);line(d,[(1,14),(7,8),(13,2)],P['ice'][0],2)
 for x,y,mat in [(5,10,'ice'),(11,4,'pink'),(11,12,'mint')]:
  s,b,l,h=P[mat];d.polygon([(x,y-3),(x+1,y-1),(x+3,y),(x+1,y+1),(x,y+3),(x-1,y+1),(x-2,y),(x-1,y-1)],fill=b);line(d,[(x,y-2),(x,y+1)],h);dot(d,x-1,y,l)
 dot(d,2,6,P['cream'][2]);return im
def icon_15():
 im=new();d=draw(im);person(d,2,8,'violet');person(d,12,1,'cream')
 for off,mat in enumerate(['pink','amber','mint','ice']):line(d,[(3,13-off),(7,12-off),(9,8-off),(12,7-off)],P[mat][1])
 star(d,13,2,P['cream'][3]);dot(d,1,13,P['violet'][2]);return im
def icon_16():
 im=new();d=draw(im);person(d,7,2,'pink');d.polygon([(6,6),(10,6),(10,10),(8,11),(5,9)],fill=P['pink'][1])
 line(d,[(1,6),(3,3),(4,3)],P['pink'][2]);line(d,[(12,7),(14,5),(14,2)],P['mint'][1]);line(d,[(3,13),(5,14),(6,12)],P['amber'][1])
 d.polygon([(9,4),(6,8),(8,8),(6,12),(11,7),(9,7)],fill=P['amber'][3]);return im
def icon_17():
 im=new();d=draw(im);person(d,7,4);d.rectangle((1,9,3,13),fill=P['cream'][1]);d.rectangle((12,8,14,12),fill=P['cream'][1])
 line(d,[(2,5),(4,4),(7,5),(11,5),(13,4)],P['pink'][2]);line(d,[(2,8),(5,7),(9,8),(13,7)],P['amber'][2]);line(d,[(2,11),(5,10),(10,11),(13,10)],P['mint'][2])
 dot(d,7,7,P['pink'][3]);return im
def icon_18():
 im=new();d=draw(im)
 for y,mat in [(8,'pink'),(10,'amber'),(12,'mint')]:line(d,[(0,y),(3,y+1),(5,y)],P[mat][1])
 s,b,l,h=P['cream'];d.polygon([(4,8),(8,7),(9,4),(12,4),(14,6),(12,8),(10,8),(10,10),(8,11),(7,10),(5,10)],fill=l)
 line(d,[(4,9),(7,9),(8,10),(10,9)],b);line(d,[(5,10),(4,13)],l,2);line(d,[(9,10),(12,12)],l,2)
 line(d,[(10,4),(12,1)],P['ice'][2]);line(d,[(8,5),(8,7),(6,8)],P['pink'][2]);dot(d,12,5,s);dot(d,9,4,h);return im
def icon_19():
 im=new();d=draw(im);d.rectangle((4,7,11,13),fill=P['amber'][1]);d.rectangle((2,8,4,11),fill=P['amber'][2]);d.rectangle((11,8,13,11),fill=P['amber'][0])
 d.rectangle((5,10,9,13),fill=P['amber'][2]);bear(d,8,5,3);dot(d,6,11,P['amber'][3]);d.rectangle((4,14,6,14),fill=P['amber'][0]);d.rectangle((10,14,12,14),fill=P['amber'][0]);return im
def icon_20():
 im=new();d=draw(im);ginger(d,2,2);ginger(d,11,2);ginger(d,6,7)
 line(d,[(0,12),(3,12)],P['amber'][0]);line(d,[(10,14),(14,14)],P['cream'][1]);dot(d,8,1,P['cream'][2]);return im
def icon_21():
 im=new();d=draw(im);d.arc((0,0,15,15),35,278,fill=P['pink'][1],width=2);d.arc((2,1,14,14),65,245,fill=P['cream'][2],width=1)
 d.polygon([(3,11),(6,7),(9,6),(11,8),(7,12),(5,14)],fill=P['pink'][1]);line(d,[(4,11),(6,8),(9,7)],P['pink'][2],2)
 d.rectangle((8,4,13,8),fill=P['cream'][1]);d.rectangle((9,3,13,5),fill=P['cream'][2]);line(d,[(9,6),(13,6)],P['pink'][1]);dot(d,11,4,P['cream'][3]);return im
def icon_22():
 im=new();d=draw(im);d.rectangle((4,5,10,11),fill=P['mint'][0]);d.rectangle((5,5,9,9),fill=P['ice'][1]);line(d,[(5,5),(5,8)],P['ice'][3]);d.rectangle((4,3,10,4),fill=P['pink'][1]);dot(d,7,2,P['pink'][2])
 for x,y,mat in [(7,6,'pink'),(8,8,'amber'),(6,8,'mint')]:dot(d,x,y,P[mat][2])
 d.rectangle((3,11,11,13),fill=P['pink'][0]);d.rectangle((7,10,10,12),fill=P['cream'][1]);d.rectangle((10,8,13,10),fill=P['cream'][2]);d.rectangle((13,8,14,10),fill=P['cream'][0]);cube(d,14,5,1,'pink');return im
def icon_23():
 im=new();d=draw(im);s,b,l,h=P['pink']
 line(d,[(1,14),(3,10),(6,9),(7,6),(10,5)],s,5);line(d,[(2,13),(4,10),(7,9),(8,6)],b,3)
 for x,y in [(3,11),(6,9),(8,6)]:dot(d,x-1,y,l)
 d.ellipse((8,1,15,8),fill=l);d.ellipse((10,2,15,7),fill=P['licorice'][0])
 for x,y in [(11,2),(14,2),(10,5),(14,7)]:dot(d,x,y,P['cream'][3])
 line(d,[(0,15),(4,14),(6,15),(8,13),(11,14)],P['amber'][0]);return im
def icon_24():
 im=new();d=draw(im);d.ellipse((1,8,14,14),fill=P['amber'][0]);d.arc((1,7,14,14),0,360,fill=P['cream'][2])
 d.ellipse((3,9,12,12),fill=P['amber'][1]);line(d,[(4,11),(7,12),(10,11)],P['cream'][2])
 cube(d,3,10,1,'pink');cube(d,12,10,1,'mint');cube(d,8,12,1,'ice')
 d.polygon([(7,5),(9,7),(7,9),(5,7)],fill=P['cream'][2]);line(d,[(7,5),(7,8)],P['cream'][3])
 for x,y in [(2,3),(12,3)]:line(d,[(x,y+3),(x,y),(x+1,y-1),(x+2,y)],P['pink'][2]);dot(d,x,y+1,P['cream'][3])
 star(d,7,4,P['cream'][3]);dot(d,7,12,P['mint'][2]);return im
def icon_25():
 im=new();d=draw(im)
 for x,y in [(3,1),(9,4),(14,0)]:line(d,[(x,y),(x-1,y+2)],P['ice'][1])
 scoop(d,4,5,'pink');scoop(d,11,3,'mint');scoop(d,9,11,'cream')
 line(d,[(0,13),(3,14),(4,13)],P['pink'][1]);line(d,[(11,15),(14,14)],P['mint'][1]);dot(d,14,8,P['cream'][2]);return im
def icon_26():
 im=new();d=draw(im)
 for pts in [[(7,13),(3,11),(2,8),(4,6)],[(8,13),(12,11),(13,8),(11,6)],[(7,12),(7,8),(9,6),(8,3)]]:
  line(d,pts,P['licorice'][1],3);line(d,pts,P['licorice'][2])
 d.ellipse((4,8,11,14),fill=P['violet'][0]);d.arc((4,7,11,14),0,300,fill=P['pink'][2]);star(d,8,11,P['violet'][3])
 bear(d,3,4,2);ginger(d,11,1);dot(d,1,10,P['mint'][2]);dot(d,14,11,P['mint'][1]);return im

def finish(fg,mat):
 # Pixel-distance glow built from the actual painted silhouette, no blur or antialiasing.
 colors=P[mat];pts=[(x,y,fg.getpixel((x,y))) for y in range(16) for x in range(16) if fg.getpixel((x,y))[3]]
 hue=tuple(bytes.fromhex(colors[1][1:]));background=Image.new('RGBA',(16,16))
 for y in range(16):
  for x in range(16):
   distance=min(max(abs(x-px),abs(y-py)) for px,py,c in pts)
   amount={0:.23,1:.16,2:.085,3:.035}.get(distance,.008)
   # Near-black tinted field; stepped glow is deliberately much darker than the subject.
   background.putpixel((x,y),tuple(int(3+channel*amount) for channel in hue)+(255,))
 background.alpha_composite(fg);return background

def main():
 rows=re.findall(r'new Spec\("([^"]+)", "([^"]+)"',(root/'src/main/java/cn/wanyiwige/alexcavesspellsbook/candy/CandySpecs.java').read_text(encoding='utf-8'))
 mats=['pink','amber','ice','pink','mint','mint','mint','violet','pink','ice','violet','ice','amber','ice','violet','pink','mint','pink','amber','amber','pink','mint','pink','amber','ice','violet']
 assert len(rows)==26
 records=[];sheet=Image.new('RGB',(5*208,6*174+40),'#29272E');d=draw(sheet);font=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',18);d.text((12,8),'糖果学派 · 16×16 原生像素网格重绘',font=font,fill='#E9DECC')
 for i,((id,name),mat) in enumerate(zip(rows,mats),1):
  im=finish(globals()[f'icon_{i:02d}'](),mat);path=dest/(id+'.png');im.save(path);im.save(art/(id+'.png'))
  # Static textures use the existing icon resource IDs. No resource or model registration changes.
  preview=im.resize((112,112),Image.Resampling.NEAREST);x=((i-1)%5)*208;y=((i-1)//5)*174+40;sheet.paste(preview,(x+48,y+4));label=f'{i:02d} {name}';w=d.textbbox((0,0),label,font=font)[2];d.text((x+(208-w)//2,y+126),label,font=font,fill='#E9DECC')
  records.append({'number':i,'id':id,'name':name,'size':[16,16],'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
 sheet.save(art/'overview.png');(art/'icons.json').write_text(json.dumps(records,ensure_ascii=False,indent=2),encoding='utf-8');print('Redrew 26 original icons at 16x16; paths preserved.')
if __name__=='__main__':main()
