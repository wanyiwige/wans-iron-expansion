"""Fit a low-density pixel atlas to the completed Blockbench wizard armor geometry."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,math
root=Path(__file__).resolve().parents[1];art=root/'artwork/candy-wizard-armor-20261017'
bb=json.loads((art/'candy_wizard_geometry.bbmodel').read_text(encoding='utf-8'))
mats=json.loads((art/'materials.json').read_text())
palette={
 'hat':['#392443','#24182E','#57365D','#C386B8'],
 'robe':['#4A2E5B','#30203D','#64416F','#9B648C'],
 'berry':['#9B456F','#66314F','#BC658C','#E9A0AC'],
 'cream':['#F4DDB6','#BA986F','#FFF0CF','#FFFFE6'],
 'mint':['#92C7B0','#557F79','#CDE7C3','#FFF1CF'],
 'gold':['#C49356','#88603D','#F3CE88','#FFF0BD'],
 'candy':['#E087A9','#A04E7A','#FFCDAC','#FFF4D5'],
 'boot':['#392B43','#231B2E','#65475F','#A86B7B'],
 'caramel':['#9F6B46','#5A382C','#CF9C68','#E7BB8C'],
 'glass':['#77B8AF','#315F67','#B8E6D4','#FFF1D5']}
rects=[]
elements=[e for e in bb['elements'] if e.get('export',True)]
for e in elements:
 x,y,z=[abs(b-a) for a,b in zip(e['from'],e['to'])]
 for face,(w,h) in {'north':(x,y),'south':(x,y),'east':(z,y),'west':(z,y),'up':(x,z),'down':(x,z)}.items():
  rects.append({'name':e['name'],'face':face,'w':2*max(1,math.ceil(w)),'h':2*max(1,math.ceil(h))})
rects.extend([{'name':'_preview_face','face':'north','w':8,'h':8},{'name':'_preview_skin','face':'all','w':2,'h':2}])
# Two native pixels per model unit. Each face is redrawn at this density.
rects.sort(key=lambda a:(-a['h'],-a['w'],a['name'],a['face']))
shelves=[]
for r in rects:
 found=next((s for s in shelves if s['h']>=r['h']+2 and s['x']+r['w']+2<=512),None)
 if found is None:
  y=sum(s['h'] for s in shelves);found={'y':y,'h':r['h']+2,'x':0};shelves.append(found)
 r['x']=found['x']+1;r['y']=found['y']+1;found['x']+=r['w']+2
assert sum(s['h'] for s in shelves)<=256,('Atlas overflow',sum(s['h'] for s in shelves))
atlas=Image.new('RGBA',(512,256));d=ImageDraw.Draw(atlas)
uvs={}
for r in rects:
 name,face,x,y,w,h=[r[k] for k in ['name','face','x','y','w','h']]
 if name.startswith('_preview'):
  d.rectangle((x-1,y-1,x+w,y+h),fill='#B8AC98')
  if name=='_preview_face':
   d.rectangle((x,y,x+7,y+1),fill='#A39A89');d.point((x+2,y+4),fill='#635D64');d.point((x+5,y+4),fill='#635D64');d.point((x+4,y+6),fill='#998777')
  uvs.setdefault(name,{})[face]=[x,y,x+w,y+h];continue
 mat=mats[name];base,shadow,light,hot=palette[mat]
 d.rectangle((x-1,y-1,x+w,y+h),fill=base)
 for v in range(h):
  for u in range(w):
   color=base
   if mat in ['hat','robe','berry','boot']:
    if face=='down':color=shadow
    elif face=='up':color=light
    # Broad, irregular cloth planes; no repeating stripe or noise pattern.
    elif u==w-1 and v>h//3:color=shadow
    elif u<w//3 and h//4<=v<max(h//4+1,2*h//3) and face in ['north','west']:color=light
    elif u>max(w//2,w-3) and v>max(1,h//2):color=shadow
    if v==h-1 and h>3:color=shadow
   else:
    color=shadow if face=='down' or v==h-1 else light if face=='up' or v==0 else base
   if mat=='mint' and u==0 and v==0:color=hot
   if mat=='candy':color=hot if u==0 and v<max(1,h//2) else light if u<w//2 and v<h//2 else shadow if v==h-1 else base
   if mat=='glass':color=hot if u==1 else light if u==0 else shadow if u==w-1 else base
   d.point((x+u,y+v),fill=color)
 if face in ['north','south'] and w>=8 and h>=6 and name in ['hat_crown_base','left_robe_front_panel']:
  # A single wrapped-sweet crest, rather than identical crosses on every face.
  cx=x+w//2;cy=y+h//2
  d.polygon([(cx-2,cy),(cx,cy-2),(cx+2,cy),(cx,cy+2)],fill='#D4A27F')
  d.point((cx,cy-1),fill='#FFF0CF');d.point((cx-3,cy-1),fill='#92C7B0');d.point((cx+3,cy+1),fill='#92C7B0')
 if mat in ['robe','berry'] and h>=7 and face in ['north','south']:
  d.line((x,y+h-2,x+w-1,y+h-2),fill='#AD6C88')
 if name=='robe_tunic' and face=='north':
  cx=x+w//2
  for v in range(2,h-2,3):d.point((cx,y+v),fill='#E6BE8C')
 # Individual stitched hems and stepped icing piping, never smooth gradients.
 if ('icing' in name or 'collar' in name) and face in ['north','south','east','west'] and w>=3:
  d.line((x,y,x+w-1,y),fill='#FFF7E1')
  if h>=4:
   d.line((x+1,y+h-2,x+w-2,y+h-2),fill='#D2AF88')
   d.point((x+w//3,y+1),fill='#D797A5')
 if name=='robe_tunic' and face=='north' and w>=7 and h>=9:
  # One small sugar-star embroidery, with the rest left as readable cloth.
  d.point((x+1,y+3),fill='#E6BE8C');d.point((x+2,y+2),fill='#F4DDB6');d.point((x+3,y+3),fill='#B87394')
 if 'back_mantle' in name and face=='south' and w>=7 and h>=5:
  cx=x+w//2;cy=y+h//2
  d.line([(cx-2,cy),(cx-1,cy-1),(cx+1,cy-1),(cx+2,cy),(cx+1,cy+1),(cx-1,cy+1),(cx-2,cy)],fill='#F4DDB6',width=1)
  d.point((cx,cy),fill='#B8DBBF')
 if face in ['north','south','east','west']:
  if mat in ['robe','hat'] and w>=8 and h>=10:
   # Short seam shadows taper into broad cloth, with no tiled weave.
   sx=x+w-3
   d.line([(sx,y+h-3),(sx,y+2*h//3),(sx-1,y+h//2)],fill=shadow)
   d.point((sx-1,y+h-3),fill=light)
  if 'sleeve' in name and w>=6 and h>=12:
   d.line([(x+1,y+h-5),(x+2,y+h-6),(x+4,y+h-6)],fill=light)
   d.line([(x+w-3,y+2),(x+w-2,y+3),(x+w-2,y+5)],fill=shadow)
  if 'band' in name and mat=='berry' and w>=6 and h>=3:
   d.line((x+1,y+1,x+w-2,y+1),fill='#B75E87')
   d.point((x+2,y+1),fill='#DF9AAA')
  if mat=='candy' and w>=4 and h>=4:
   d.line((x+1,y+1,x+min(w-2,3),y+1),fill=hot)
   d.point((x+1,y+2),fill=light)
   d.point((x+w-2,y+h-2),fill=shadow)
  if mat=='mint' and 'wrapper' in name and w>=4 and h>=3:
   d.line([(x,y+h-1),(x+w//2,y+h//2),(x+w-1,y+h-1)],fill=shadow)
   d.point((x+w//2,y),fill=hot)
  if mat=='glass' and w>=5 and h>=6:
   d.rectangle((x+2,y+h//2,x+w-2,y+h-2),fill='#BD6791')
   d.line((x+2,y+h//2,x+w-2,y+h//2),fill='#F2A9B7')
   d.line((x+1,y+1,x+1,y+h-3),fill=hot)
   d.point((x+2,y+2),fill='#B8E6D4')
  if 'pouch' in name and w>=6 and h>=5:
   d.line([(x+1,y+1),(x+1,y+h-2),(x+w-2,y+h-2)],fill='#C0809A')
  if 'boot_toe' in name and w>=5 and h>=3:
   d.line((x+1,y+1,x+w//2,y+1),fill='#795269')
  if mat=='gold' and w>=3 and h>=3:
   d.point((x+1,y+1),fill=hot);d.point((x+w-1,y+h-1),fill=shadow)
 uvs.setdefault(name,{})[face]=[x,y,x+w,y+h]
path=root/'src/main/resources/assets/alex_caves_spellsbook/textures/models/armor/candy_wizard.png';path.parent.mkdir(parents=True,exist_ok=True)
atlas.save(path);atlas.save(art/'candy_wizard_atlas.png')
path.with_suffix('.png.mcmeta').write_text(json.dumps({'texture':{'blur':False,'clamp':True}}))
(art/'uv-map.json').write_text(json.dumps(uvs,indent=2));atlas.resize((1536,768),Image.Resampling.NEAREST).save(art/'atlas-preview.png')
print(json.dumps({'cubes':len(elements),'faces':len(elements)*6,'atlas':[512,256],'usedRows':sum(s['h'] for s in shelves)}))
