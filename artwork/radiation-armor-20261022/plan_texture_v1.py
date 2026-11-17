"""Pack real cube faces at a uniform two texture pixels per model unit.

This script creates UV data only; painted material pixels come from imagegen.
"""
import json, math
from pathlib import Path

ROOT=Path(__file__).resolve().parent
OUT=ROOT/'texture-v1'
OUT.mkdir(exist_ok=True)
model=json.loads((ROOT/'critical_warden_armor.bbmodel').read_text(encoding='utf-8'))
plan=json.loads((ROOT/'geometry-plan.json').read_text(encoding='utf-8'))
materials={c['name']:c['material'] for c in plan['cubes']}
material_tiles={m:i for i,m in enumerate(plan['palette'])}
faces=[]
for cube in model['elements']:
    dx,dy,dz=[cube['to'][i]-cube['from'][i] for i in range(3)]
    for face in ['north','east','south','west','up','down']:
        w,h={'north':(dx,dy),'south':(dx,dy),'east':(dz,dy),'west':(dz,dy),'up':(dx,dz),'down':(dx,dz)}[face]
        tile=material_tiles[materials[cube['name']]]
        if materials[cube['name']]=='cloth':
            if 'tail' in cube['name'] or 'panel' in cube['name']:tile=14
            elif cube['name']=='hood_back':tile=12
        if cube['name'] in ('reactor_back_case','reactor_back_face'):tile=13
        if cube['name'].startswith('fission_frame'):tile=15
        faces.append({'cube':cube['name'],'uuid':cube['uuid'],'face':face,'material':materials[cube['name']],
                      'tile':tile,'width':w*2,'height':h*2,'pixel_width':max(1,math.ceil(w*2)),
                      'pixel_height':max(1,math.ceil(h*2))})
# Shelf packing; one-pixel gutters are extruded from each painted patch.
faces.sort(key=lambda f:(-f['pixel_height'],-f['pixel_width'],f['cube'],f['face']))
atlas_width=256
x=y=0;row_h=0
for f in faces:
    pw,ph=f['pixel_width']+2,f['pixel_height']+2
    if x+pw>atlas_width:
        x=0;y+=row_h;row_h=0
    f['rect']=[x+1,y+1,f['pixel_width'],f['pixel_height']]
    f['uv']=[x+1,y+1,x+1+f['width'],y+1+f['height']]
    x+=pw;row_h=max(row_h,ph)
used_height=y+row_h
atlas_height=2**math.ceil(math.log2(used_height))
result={'width':atlas_width,'height':atlas_height,'used_height':used_height,
        'pixels_per_model_unit':2,'geometry_source':'../critical_warden_armor.bbmodel',
        'source_material_library':{'columns':4,'rows':4,'native_tile_size':32},'faces':faces}
(OUT/'uv-plan.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print(json.dumps({'atlas':[atlas_width,atlas_height],'used_height':used_height,'faces':len(faces),'native_painted_area':sum(f['pixel_width']*f['pixel_height'] for f in faces),'density':2}))
