"""Native pixel sheets for candy spells 5-10; no new model geometry."""
from pathlib import Path
from PIL import Image,ImageDraw
import math,json
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'src/main/resources/assets/alex_caves_spellsbook/textures/entity/candy/pixel'
ART=ROOT/'artwork/candy-five-ten-20261017'
OUT.mkdir(parents=True,exist_ok=True);ART.mkdir(parents=True,exist_ok=True)
made=[]
def save(name,im):
    im.save(OUT/(name+'.png'))
    (OUT/(name+'.png.mcmeta')).write_text(json.dumps({'texture':{'blur':False,'clamp':True}}),encoding='utf-8')
    made.append(name)

im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
d.arc((2,2,29,29),200,335,fill='#FFF9DF',width=2)
d.arc((5,5,26,26),215,325,fill='#E788AC',width=2)
d.line([(26,20),(29,16),(28,12)],fill='#FFFFFF',width=2)
save('mint_slash',im)
im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
for y in [1,5,9,13]:
    d.rectangle((4,y,6,y+1),fill='#FFFFFF');d.rectangle((9,y,11,y+1),fill='#E7F4D6')
save('wheel_track',im)

for frame in range(3):
    im=Image.new('RGBA',(64,64));px=im.load()
    for y in range(64):
        for x in range(64):
            dx=x-31.5;dy=y-31.5;r=math.hypot(dx,dy);a=math.atan2(dy,dx)
            if r>30:continue
            if 27<r<29 and (int((a+math.pi)*15)+frame)%5<3:px[x,y]=(155,78,151,255)
            elif r>9 and abs((r+2*math.sin(a*6+frame*.5))%9-4.5)<1 and (x+y+frame)%4:
                px[x,y]=(91,43,112,255)
            elif r>11 and abs(math.sin(a*4+r*.09+frame*.4))>.97:px[x,y]=(215,139,184,255)
    d=ImageDraw.Draw(im)
    for i in range(6):
        a=i*math.pi/3+frame*.13;cx=round(31.5+math.cos(a)*22);cy=round(31.5+math.sin(a)*22)
        d.arc((cx-3,cy-3,cx+3,cy+3),30+frame*35,290+frame*35,fill='#D093C7',width=1)
    save('licorice_wave_'+str(frame),im)

im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
for i in range(12):
    a=i*math.pi/6
    def p(r,offset=0):return (round(31.5+math.cos(a+offset)*r),round(31.5+math.sin(a+offset)*r))
    d.polygon([p(8),p(21,-.11),p(29),p(21,.11)],outline='#EAB6D0')
    d.line([p(11),p(25)],fill='#94619B',width=1)
    d.rectangle((p(29)[0]-1,p(29)[1]-1,p(29)[0]+1,p(29)[1]+1),fill='#FFF3D2')
d.ellipse((25,25,38,38),outline='#FFE5B2',width=1)
d.polygon([(31,24),(38,31),(31,38),(24,31)],outline='#FFD1DB')
save('sugar_flower',im)
for band in range(3):
    im=Image.new('RGBA',(64,64));px=im.load()
    inner=band*10;outer=(band+1)*10
    for y in range(64):
        for x in range(64):
            r=math.hypot(x-31.5,y-31.5);a=math.atan2(y-31.5,x-31.5)
            if not(inner<=r<=outer):continue
            if abs(r-outer)<1.2 and int((a+math.pi)*outer)%4<3:px[x,y]=(255,240,204,255)
            elif r>inner+1 and abs(math.sin(a*12+r*.2))>.94:px[x,y]=(255,168,203,255)
    save('seal_band_'+str(band),im)

im=Image.new('RGBA',(16,32));d=ImageDraw.Draw(im)
d.line((7,1,7,21),fill='#F3FFFF',width=2)
for y in range(3,21,4):d.rectangle((6,y,9,y+1),fill='#73CFC7')
d.polygon([(3,17),(12,17),(11,24),(7,31),(4,24)],fill='#77D8DB')
d.line([(3,17),(7,31),(7,18)],fill='#F7FFFF',width=1)
d.line([(9,19),(8,25)],fill='#C9FFF0',width=1)
d.line([(5,2),(4,5),(7,7),(10,5),(10,2)],fill='#CEFFF2',width=1)
save('frost_spear',im)
im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
for a in [0,math.pi/3,math.pi*2/3]:
    x=math.cos(a)*6;y=math.sin(a)*6
    d.line((round(7.5-x),round(7.5-y),round(7.5+x),round(7.5+y)),fill='#E1FFF9',width=1)
for x,y in [(4,4),(11,4),(4,11),(11,11)]:d.point((x,y),fill='#92DCEB')
save('frost_snow',im)
im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
for i in range(12):
    a=i*math.pi/6
    if i%2==0:
        for r in range(25,30):
            x=round(31.5+math.cos(a)*r);y=round(31.5+math.sin(a)*r);d.point((x,y),fill='#A8E6E4')
    x=round(31.5+math.cos(a)*29);y=round(31.5+math.sin(a)*29)
    d.rectangle((x-1,y-1,x+1,y+1),outline='#F1FFFF')
save('frost_boundary',im)
im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
for i in range(6):
    a=i*math.pi/3
    pts=[(15,15),(round(15+math.cos(a+.12)*6),round(15+math.sin(a+.12)*6)),(round(15+math.cos(a)*14),round(15+math.sin(a)*14))]
    d.line(pts,fill='#DAFFF8',width=1)
    d.line((pts[1][0],pts[1][1],round(15+math.cos(a+.25)*10),round(15+math.sin(a+.25)*10)),fill='#80C8DD',width=1)
save('frost_crack',im)

sheet=Image.new('RGB',(1100,520),'#202434');d=ImageDraw.Draw(sheet)
for i,name in enumerate(made):
    im=Image.open(OUT/(name+'.png'));scale=min(128//im.width,128//im.height)
    im=im.resize((im.width*scale,im.height*scale),Image.Resampling.NEAREST)
    x=15+(i%7)*155;y=25+(i//7)*245
    sheet.paste(im,(x+(128-im.width)//2,y),im);d.text((x,y+145),name,fill='#EEE4EF')
sheet.save(ART/'pixel-sheets-5-10.png')
(ART/'new-textures.json').write_text(json.dumps(made,indent=2),encoding='utf-8')
print('Created',len(made),'pixel sheets; reused existing mint models and common sprites.')
