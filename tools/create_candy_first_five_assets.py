"""Native-grid pixel art and runtime models from the Blockbench-built geometry."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json, math, colorsys

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / 'artwork/candy-first-five-20261017'
RES = ROOT / 'src/main/resources/assets/alex_caves_spellsbook'
TEX = RES / 'textures/entity/candy/pixel'
MODELS = RES / 'models/candy'
TEX.mkdir(parents=True, exist_ok=True)
MODELS.mkdir(parents=True, exist_ok=True)

def rgb(s): return tuple(bytes.fromhex(s)) + (255,)
def mix(a,b,t): return tuple(round(x*(1-t)+y*t) for x,y in zip(a,b))
palettes = [
    ['772F63','BA3F79','F2699A','FFB3BF','FFF2CB'],
    ['73401D','C17228','FFB74B','FFE79B','FFF9DA'],
    ['235B7A','428EA7','82D8DB','C6F2ED','FFFFFF'],
    ['542C58','9B3A66','E75D7D','FFA759','FFF0B2'],
]
atlas = Image.new('RGBA',(128,128))
for row, palette in enumerate(palettes):
    p=list(map(rgb,palette))
    for face in range(6):
        tile=Image.new('RGBA',(16,16),p[2]);d=ImageDraw.Draw(tile)
        d.rectangle((0,0,15,15),fill=p[0]);d.rectangle((1,1,14,14),fill=p[1])
        d.rectangle((2,2,13,12),fill=p[2]);d.rectangle((3,2,11,4),fill=p[3])
        d.line([(2,4),(2,10),(3,10)],fill=p[3],width=1)
        d.rectangle((4,3,7,3),fill=p[4]);d.rectangle((3,4,4,5),fill=p[4])
        d.line([(5,13),(12,13),(13,12)],fill=p[0],width=1)
        d.point((12,10),fill=p[3])
        if row==0:
            # A broad diagonal ribbon, clearly subordinate to the shell highlight.
            for x in range(5,12):
                y=15-x
                d.point((x,y),fill=p[3]);d.point((x,y+1),fill=p[1])
        elif row==1:
            d.rectangle((6,6,11,11),fill=p[0]);d.rectangle((7,7,10,10),fill=rgb('E44F7E'))
            d.line((7,7,9,7),fill=rgb('FFD0B5'));d.point((10,10),fill=rgb('982F67'))
        elif row==2:
            d.polygon([(7,5),(12,6),(10,10),(5,12),(4,9)],fill=p[1])
            d.line([(7,5),(8,8),(5,12)],fill=p[4]);d.line([(8,8),(12,6)],fill=p[3])
            d.line([(8,8),(10,10),(12,11)],fill=p[0]);d.point((11,5),fill=p[4])
        else:
            for pts in [[(9,2),(8,5),(10,7),(7,9),(8,13)],[(10,7),(13,6)],[(7,9),(4,8),(2,9)]]:
                d.line(pts,fill=p[0],width=2);d.line(pts,fill=p[3],width=1)
            d.point((8,5),fill=p[4]);d.point((7,9),fill=p[4])
        if face==5: tile=tile.point(lambda x: int(x*.74) if x!=255 else x)
        atlas.paste(tile,(face*16,row*16))

# Peppermint: one pattern extruded through its thickness; no unrelated side stripes.
for face in range(6):
    tile=Image.new('RGBA',(16,16));pix=tile.load()
    for y in range(16):
        for x in range(16):
            if face in (0,2):
                dx=x-7.5;dy=y-7.5;r=math.hypot(dx,dy)
                band=int(((math.atan2(dy,dx)+r*.11)%(2*math.pi))/(math.pi/4))%2
                c=rgb('EF526E' if band else 'FFF4D8')
                if r>7: c=rgb('A42F52' if band else 'CFB8A8')
                if y<5 and x<9 and r<6: c=rgb('FF8F9B' if band else 'FFFFFF')
            else: c=rgb('E54B67' if ((x+y)//3)%2 else 'F8DEC5')
            pix[x,y]=c
    atlas.paste(tile,(face*16,64))
front=atlas.crop((0,64,16,80))
atlas.paste(front.transpose(Image.Transpose.FLIP_LEFT_RIGHT),(32,64))
mint_source=json.loads((ART/'geometry-before-texture.bbmodel').read_text(encoding='utf-8'))
mint_wall_uv={}
def mint_surface(x,y):
    u=max(0,min(15,math.floor((24-x)*2)))
    v=max(0,min(15,math.floor((13-y)*1.6)))
    return front.getpixel((u,v))
wall_index=0
for e in mint_source['elements']:
    if not e['name'].startswith('peppermint_'):continue
    for face in ['east','west','up','down']:
        tx=(wall_index%8)*16;ty=80+(wall_index//8)*16;wall_index+=1
        x0,y0,z0=e['from'];x1,y1,z1=e['to']
        width=4 if face in ['east','west'] else (x1-x0)*2
        height=(y1-y0)*1.6 if face in ['east','west'] else 4
        tile=Image.new('RGBA',(16,16));px=tile.load()
        for v in range(16):
            for u in range(16):
                if face in ['east','west']:
                    x=x1 if face=='east' else x0;y=y1-(v+.5)/1.6
                    depth=(u+.5)/4
                else:
                    x=x0+(u+.5)/2;y=y1 if face=='up' else y0
                    depth=(v+.5)/4
                c=mint_surface(x,y)
                # Same boundary colours at both caps, with a restrained crease in the middle.
                shade=.92 if .25<depth<.75 else 1
                px[u,v]=tuple(round(k*shade) for k in c[:3])+(255,)
        atlas.paste(tile,(tx,ty))
        mint_wall_uv[(e['name'],face)]=[tx/8,ty/8,(tx+width)/8,(ty+height)/8]
atlas.save(ART/'candy_atlas.png');atlas.save(TEX/'candy_atlas.png')

def sprite(name,size,paint):
    im=Image.new('RGBA',(size,size));paint(ImageDraw.Draw(im));im.save(TEX/(name+'.png'))
    return im
spark=sprite('spark',16,lambda d:[d.rectangle((7,2,8,13),fill='#FFF8DE'),d.rectangle((2,7,13,8),fill='#FFF8DE'),d.rectangle((6,6,9,9),fill='#FFFFFF')])
sprite('crumb',8,lambda d:[d.rectangle((2,1,5,6),fill='#FFFFFF'),d.rectangle((1,2,6,5),fill='#FFFFFF'),d.rectangle((2,5,5,6),fill='#A5B1C8')])
sprite('trail',16,lambda d:[d.rectangle((0,6,12,9),fill='#FFFFFF'),d.rectangle((2,5,8,10),fill='#FFFFFF'),d.rectangle((13,7,15,8),fill='#FFFFFF'),d.rectangle((1,6,3,7),fill='#D1D9EE')])
sprite('turn',16,lambda d:[d.line([(3,12),(3,6),(6,3),(11,3)],fill='#FFFFFF',width=2),d.line([(8,1),(11,3),(8,6)],fill='#FFFFFF',width=2)])
sprite('smoke',16,lambda d:[d.rectangle((4,3,10,12),fill='#FFFFFF'),d.rectangle((2,6,13,9),fill='#FFFFFF'),d.rectangle((6,1,8,13),fill='#FFFFFF'),d.rectangle((4,11,6,14),fill='#FFFFFF')])
for frame in range(4):
    im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im);r=[4,9,13,15][frame]
    # Eight pointed, deliberately stepped pixel burst; later frames have a hollow centre.
    pts=[]
    for i in range(16):
        a=i*math.pi/8;rr=r if i%2==0 else r*.48
        pts.append((round(15.5+math.cos(a)*rr),round(15.5+math.sin(a)*rr)))
    d.polygon(pts,fill='#FFFFFF')
    if frame>=2:d.rectangle((12-frame,12-frame,19+frame,19+frame),fill=(0,0,0,0))
    im.save(TEX/f'burst_{frame}.png')
for file in TEX.glob('*.png'):
    file.with_suffix('.png.mcmeta').write_text(json.dumps({'texture':{'blur':False,'clamp':True}}),encoding='utf-8')

# Geometry comes from Blockbench; normalize each prop around model centre (8,8,8).
source=json.loads((ART/'geometry-before-texture.bbmodel').read_text(encoding='utf-8'))
names=['rainbow_hard_candy','sugar_hunt_filled_candy','crystal_split_candy','popping_cluster_candy','peppermint_guard']
for row,name in enumerate(names):
    selected=[e for e in source['elements'] if (e['name']==name or row==4 and e['name'].startswith('peppermint_'))]
    centre=[-17,-8,1,10,20][row]
    result=[]
    for e in selected:
        faces={}
        for i,f in enumerate(['north','east','south','west','up','down']):
            uv=[i*2,row*2,i*2+2,row*2+2] # 128px atlas -> 16 model UV units
            if row==4 and f in ('north','south'):
                left=e['from'][0]-16;right=e['to'][0]-16
                top=13-e['to'][1];bottom=13-e['from'][1]
                uv=[i*2+left/4,8+top/5,i*2+right/4,8+bottom/5]
            elif row==4:
                uv=mint_wall_uv[(e['name'],f)]
            faces[f]={'uv':uv,'texture':'#0'}
        result.append({'name':e['name'],'from':[e['from'][0]-centre+8,e['from'][1],e['from'][2]+8],
                       'to':[e['to'][0]-centre+8,e['to'][1],e['to'][2]+8],'faces':faces})
    model={'credit':'Built in Blockbench via MCP; native-grid candy pixel art','ambientocclusion':False,
           'textures':{'0':'alex_caves_spellsbook:entity/candy/pixel/candy_atlas','particle':'alex_caves_spellsbook:entity/candy/pixel/candy_atlas'},'elements':result}
    (MODELS/(name+'.json')).write_text(json.dumps(model,indent=2),encoding='utf-8')

# Six texture variants reuse the same cube geometry; white highlights keep their luminance.
rainbow_names=['rose','orange','lemon','green','blue','purple']
rainbow_hues=[.96,.07,.14,.37,.57,.75]
rainbow_sheet=Image.new('RGB',(960,190),'#202434');rd=ImageDraw.Draw(rainbow_sheet)
base_model=json.loads((MODELS/'rainbow_hard_candy.json').read_text(encoding='utf-8'))
for i,(name,hue) in enumerate(zip(rainbow_names,rainbow_hues)):
    variant=atlas.copy()
    for y in range(16):
        for x in range(96):
            r,g,b,a=atlas.getpixel((x,y));_,l,s=colorsys.rgb_to_hls(r/255,g/255,b/255)
            rr,gg,bb=colorsys.hls_to_rgb(hue,l,s)
            variant.putpixel((x,y),(round(rr*255),round(gg*255),round(bb*255),a))
    for y in range(64,128):
        for x in range(128):
            r,g,b,a=atlas.getpixel((x,y))
            if r>g*1.15 and r>b*1.05:
                _,l,s=colorsys.rgb_to_hls(r/255,g/255,b/255)
                rr,gg,bb=colorsys.hls_to_rgb(hue,l,s)
                variant.putpixel((x,y),(round(rr*255),round(gg*255),round(bb*255),a))
    texname='rainbow_'+name
    variant.save(TEX/(texname+'.png'))
    (TEX/(texname+'.png.mcmeta')).write_text(json.dumps({'texture':{'blur':False,'clamp':True}}),encoding='utf-8')
    model=json.loads(json.dumps(base_model))
    model['textures']={k:'alex_caves_spellsbook:entity/candy/pixel/'+texname for k in ['0','particle']}
    (MODELS/('rainbow_hard_candy_'+name+'.json')).write_text(json.dumps(model,indent=2),encoding='utf-8')
    mint=json.loads((MODELS/'peppermint_guard.json').read_text(encoding='utf-8'))
    mint['textures']=model['textures'].copy()
    (MODELS/('peppermint_guard_'+name+'.json')).write_text(json.dumps(mint,indent=2),encoding='utf-8')
    rainbow_sheet.paste(variant.crop((0,0,16,16)).resize((112,112),Image.Resampling.NEAREST),(i*160+24,25))
    rd.text((i*160+24,155),name.upper(),fill='#FFF2DB')
rainbow_sheet.save(ART/'rainbow-candy-colors.png')
mint_sheet=Image.new('RGB',(960,190),'#202434');md=ImageDraw.Draw(mint_sheet)
for i,name in enumerate(rainbow_names):
    variant=Image.open(TEX/('rainbow_'+name+'.png'))
    mint_sheet.paste(variant.crop((0,64,16,80)).resize((112,112),Image.Resampling.NEAREST),(i*160+24,25))
    md.text((i*160+24,155),name.upper()+' / CREAM',fill='#FFF2DB')
mint_sheet.save(ART/'mint-candy-colors.png')

# Art sheet uses nearest-neighbour pixels, never smooth filtering.
sheet=Image.new('RGB',(1000,420),'#202434');d=ImageDraw.Draw(sheet)
labels=['RAINBOW','FILLED HUNT','CRYSTAL','POPPING','PEPPERMINT']
for row,label in enumerate(labels):
    x=20+row*196;d.text((x,15),label,fill='#F4E9DB')
    sheet.paste(atlas.crop((0,row*16,16,row*16+16)).resize((128,128),Image.Resampling.NEAREST),(x,40))
    sheet.paste(atlas.crop((16,row*16,32,row*16+16)).resize((64,64),Image.Resampling.NEAREST),(x+65,175))
    d.text((x,250),'16px per face',fill='#A9B4CE')
for i,name in enumerate(['spark','crumb','trail','turn','burst_0','burst_1','burst_2','burst_3']):
    im=Image.open(TEX/(name+'.png')).resize((64,64),Image.Resampling.NEAREST)
    sheet.paste(im,(20+i*120,300),im);d.text((20+i*120,375),name,fill='#D9DDE7')
sheet.save(ART/'pixel-materials-preview.png')
print('Generated atlas, 5 Blockbench-derived models, 9 sprites and nearest-neighbour preview.')
