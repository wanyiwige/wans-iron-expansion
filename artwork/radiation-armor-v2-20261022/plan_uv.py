import json,math
from pathlib import Path
ROOT=Path(__file__).resolve().parent
plan=json.loads((ROOT/'geometry-plan.json').read_text(encoding='utf-8'))
model=json.loads((ROOT/'critical_warden_v2_geometry.bbmodel').read_text(encoding='utf-8'))
ids={c['name']:c['uuid'] for c in model['elements']}
blocks={}
for p in plan['parts']:
    size=tuple(p['to'][i]-p['from'][i] for i in range(3))
    role=p['role']
    if role=='shoulder' and p['name'].endswith('Left'):role='shoulder_warning'
    key=(role,size)
    p['paint_role']=role
    if key not in blocks:
        blocks[key]={'role':role,'size':size,'width':math.ceil(2*(size[0]+size[2])),
                     'height':math.ceil(size[1]+size[2]),'parts':[]}
    blocks[key]['parts'].append(p['name'])
ordered=sorted(blocks.values(),key=lambda b:(-b['height'],-b['width'],b['role']))
x=y=row_h=0
for block in ordered:
    w,h=block['width']+2,block['height']+2
    if x+w>128:x=0;y+=row_h;row_h=0
    block['u']=x+1;block['v']=y+1
    x+=w;row_h=max(row_h,h)
height=2**math.ceil(math.log2(y+row_h))
by_name={n:b for b in ordered for n in b['parts']}
faces=[]
for p in plan['parts']:
    b=by_name[p['name']];u,v=b['u'],b['v'];dx,dy,dz=b['size']
    # The standard box-unwrapping arrangement; each face retains one px/model unit.
    rects={'north':[u+dz,v+dz,dx,dy], 'east':[u,v+dz,dz,dy],
           'west':[u+dz+dx,v+dz,dz,dy], 'south':[u+2*dz+dx,v+dz,dx,dy],
           'up':[u+dz,v,dx,dz], 'down':[u+dz+dx,v,dx,dz]}
    for f,(fx,fy,fw,fh) in rects.items():
        faces.append({'name':p['name'],'uuid':ids[p['name']],'role':p['paint_role'],'face':f,
                      'rect':[fx,fy,fw,fh],'uv':[fx,fy,fx+fw,fy+fh],
                      'pixels':[int(fx),int(fy),max(1,math.ceil(fw)),max(1,math.ceil(fh))]})
result={'width':128,'height':height,'density':1,'blocks':ordered,'faces':faces}
(ROOT/'uv-plan.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print(json.dumps({'atlas':[128,height],'unique_painted_boxes':len(ordered),'faces':len(faces),'density':1,'used_height':y+row_h}))
