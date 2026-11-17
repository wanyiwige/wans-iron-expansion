"""Original native-grid sprite artwork informed by the local Alex's Caves reference sheet."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFilter,ImageChops
import json,math
ROOT=Path(__file__).resolve().parents[1]
TEX=ROOT/'src/main/resources/assets/alex_caves_spellsbook/textures/entity/candy/pixel'
ART=ROOT/'artwork/candy-eleven-fifteen-20261017'
made=[]
def save(name,im):
    im.save(TEX/(name+'.png'))
    (TEX/(name+'.png.mcmeta')).write_text(json.dumps({'texture':{'blur':False,'clamp':True}}),encoding='utf-8')
    made.append(name)

im=Image.new('RGBA',(16,32));d=ImageDraw.Draw(im)
d.rectangle((5,1,10,3),fill='#B8DFDF');d.rectangle((6,0,9,1),fill='#F1FFFF')
d.rectangle((5,4,10,7),fill='#4D758F');d.rectangle((6,4,8,6),fill='#CAF7F2')
d.polygon([(5,7),(10,7),(13,11),(13,22),(11,25),(4,25),(2,22),(2,11)],fill='#34556F')
d.rectangle((4,10,11,22),fill='#4D1D78');d.rectangle((5,10,10,12),fill='#A74CE1')
d.rectangle((4,13,6,19),fill='#8636C1');d.rectangle((7,16,10,21),fill='#6C20A5')
d.line([(3,12),(3,20),(5,23)],fill='#7EBAC9',width=1)
for x,y in [(8,13),(6,21),(10,18)]:d.rectangle((x,y,x+1,y+1),fill='#DBA4FF')
d.rectangle((5,25,10,27),fill='#A56A44');d.rectangle((6,27,9,29),fill='#E3A16E')
d.rectangle((6,30,7,31),fill='#E9D3FF');d.point((9,30),fill='#9D71DE')
save('soda_rocket',im)
im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
d.polygon([(5,1),(10,1),(14,5),(14,10),(10,14),(5,14),(1,10),(1,5)],outline='#C876F0')
d.line([(3,6),(3,4),(6,3)],fill='#F2D5FF',width=1);d.point((11,11),fill='#7139A4')
save('soda_bubble',im)
for frame in range(4):
    im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im);r=[5,9,13,15][frame]
    points=[]
    for i in range(16):
        a=i*math.pi/8;rr=r if i%2==0 else r*.60
        points.append((round(15.5+math.cos(a)*rr),round(15.5+math.sin(a)*rr)))
    d.polygon(points,fill='#9A46CF')
    d.polygon([(round(15.5+(x-15.5)*.60),round(15.5+(y-15.5)*.60)) for x,y in points],fill='#E8C1FF')
    if frame>1:d.rectangle((11-frame,11-frame,20+frame,20+frame),fill=(0,0,0,0))
    save('soda_burst_'+str(frame),im)
im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
d.polygon([(4,9),(8,4),(19,3),(26,8),(29,17),(24,25),(12,29),(4,22)],fill='#53277C')
for pts in [[(8,9),(15,7),(22,10)],[(5,17),(11,15),(16,18),(24,16)],[(11,24),(19,23),(23,20)]]:
    d.line(pts,fill='#9249B5',width=2)
save('soda_reaction_pool',im)

# A connected, asymmetric liquid mask; jagged lobes and notches stay on the 32px grid.
mask=Image.new('L',(32,32));md=ImageDraw.Draw(mask)
outline=[(8,3),(12,2),(17,3),(19,6),(23,5),(27,6),(28,9),(25,12),(27,14),(30,16),(29,21),(26,23),(23,22),(21,25),(18,25),(17,29),(12,29),(9,26),(10,23),(7,21),(3,22),(1,19),(2,15),(5,14),(5,11),(3,9),(4,6),(7,7)]
outlines=[outline,
 [(5,6),(11,3),(15,4),(17,8),(23,4),(27,7),(26,12),(29,16),(26,20),(28,24),(23,27),(18,25),(14,29),(9,26),(10,22),(4,24),(2,20),(5,16),(2,12)],
 [(10,2),(17,3),(18,7),(24,7),(28,11),(25,15),(29,20),(26,24),(20,23),(19,28),(13,30),(10,26),(5,27),(3,22),(6,18),(3,14),(5,10),(9,11)],
 [(4,10),(7,5),(12,6),(15,3),(21,5),(22,10),(28,9),(30,15),(26,18),(27,23),(22,27),(16,25),(12,28),(7,25),(8,20),(2,19),(2,14),(6,14)],
 [(7,4),(13,2),(18,5),(17,9),(24,6),(28,10),(26,14),(29,18),(25,22),(20,21),(22,27),(17,30),(12,27),(12,23),(6,24),(3,20),(5,16),(2,11),(7,10)],
 [(11,3),(18,2),(21,6),(19,10),(25,8),(29,12),(27,17),(30,21),(25,25),(20,24),(17,29),(11,28),(8,24),(4,25),(2,20),(5,17),(3,12),(7,8),(10,9)]]
# Hand-drawn contours, not rotated copies. Each has three stable-mask shimmer frames.
for shape,outline in enumerate(outlines):
 mask=Image.new('L',(32,32));md=ImageDraw.Draw(mask);md.polygon(outline,fill=255)
# Keep the visible pool within the existing circular damage radius, without drawing a circle.
 for y in range(32):
    for x in range(32):
        if math.hypot(x-15.5,y-15.5)>15.5:mask.putpixel((x,y),0)
 edge=ImageChops.subtract(mask,mask.filter(ImageFilter.MinFilter(3)))
 for frame in range(3):
    im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
    d.bitmap((0,0),mask,fill='#AC5017')
    d.polygon([(6,10),(14,4),(23,7),(26,13),(20,18),(10,19),(5,15)],fill='#DB8526')
    d.polygon([(7,19),(15,18),(22,16),(26,22),(20,27),(10,28),(5,23)],fill='#74300F')
    d.line([(9,20),(14,22),(20,21),(24,19)],fill='#C76A19',width=2)
    # Broad stepped golden reflections with a narrow cream-hot core and dark adjacent liquid.
    dx=[0,1,-1,2,-2,0][shape];dy=[0,1,2,0,1,-1][shape]
    def poly(points,color):d.polygon([(x+dx,y+dy) for x,y in points],fill=color)
    poly([(6,11),(9,7),(13,6),(18,6),(21,8),(19,10),(13,9),(10,11),(9,15),(7,14)],'#F6B54B')
    poly([(8,10),(10,8),(14,7),(18,7),(19,8),(14,8),(11,9),(10,11),(9,13),(8,13)],'#FFE4A0')
    poly([(10,8),(13,7),(16,7),(16,8),(12,8),(11,9),(10,9)],'#FFF7D9')
    poly([(16,23),(20,22),(23,20),(24,20),(23,23),(20,25),(16,25)],'#EFA039')
    poly([(18,23),(21,22),(22,22),(21,23),(18,24)],'#FFE4A0')
    for i,(x,y) in enumerate([(14+dx,12+dy),(22,15),(11,23)]):
        d.rectangle((x,y,x+2,y+1),fill='#F6B54B' if i==frame else '#D47B23')
        if i==frame:d.line((x,y,x+1,y),fill='#FFF7D9')
    d.bitmap((0,0),edge,fill='#502008')
    im.putalpha(mask);save('caramel_pool_'+('' if shape==0 else 'shape'+str(shape)+'_')+str(frame),im)
im=Image.new('RGBA',(8,8));d=ImageDraw.Draw(im)
d.polygon([(3,0),(5,3),(6,5),(4,7),(1,6),(1,4)],fill='#BC7027');d.point((2,4),fill='#F0BA64')
save('caramel_drip',im)

im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
d.polygon([(7,0),(10,5),(15,7),(10,10),(8,15),(4,10),(0,8),(5,5)],fill='#9AD3EA')
d.polygon([(7,1),(8,7),(14,7),(8,9),(8,14),(6,8),(1,8),(6,6)],fill='#E4F6FF')
d.line([(7,2),(7,7),(10,9)],fill='#FFFFFF',width=1);d.line([(4,9),(7,8)],fill='#5A91BA',width=1)
save('sugar_glass_star',im)
im=Image.new('RGBA',(32,8));d=ImageDraw.Draw(im)
colors=['#FF83AF','#FFBB67','#FFEAAF','#9EE0B3','#83CFF1','#C3A0EC']
for y in range(1,7):
    for x in range(32):
        if x<3 and (x+y)%3==0 or x>26 and (x+y)%4==0:continue
        d.point((x,y),fill=colors[y-1])
for x in range(4,29,7):d.point((x,1),fill='#FFF3ED')
save('rainbow_ribbon',im)
im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
for i,c in enumerate(colors):d.rectangle((1+i*2,5+(i%2)*2,2+i*2,6+(i%2)*2),fill=c)
save('rainbow_motes',im)
im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
for i,c in enumerate(colors):
    a=i*math.pi/3;x=round(15.5+math.cos(a)*12);y=round(15.5+math.sin(a)*12)
    d.line([(15,15),(x,y)],fill=c,width=2)
d.rectangle((13,13,17,17),fill='#FFF6EA')
save('leap_flash',im)

sheet=Image.new('RGB',(1120,math.ceil(len(made)/7)*180+20),'#202434');sd=ImageDraw.Draw(sheet)
for i,name in enumerate(made):
    im=Image.open(TEX/(name+'.png'));scale=max(1,min(120//im.width,120//im.height))
    im=im.resize((im.width*scale,im.height*scale),Image.Resampling.NEAREST)
    x=10+(i%7)*160;y=20+(i//7)*180
    sheet.paste(im,(x+(120-im.width)//2,y),im);sd.text((x,y+130),name,fill='#F5E8E2')
sheet.save(ART/'pixel-sheets-11-15.png')
(ART/'new-textures.json').write_text(json.dumps(made,indent=2),encoding='utf-8')
print('Created',len(made),'native-pixel textures; six caramel outlines, each stable across three shimmer frames.')
