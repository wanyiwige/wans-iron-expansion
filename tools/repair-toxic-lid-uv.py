"""Repair degenerate lid side UVs without changing any accepted geometry or painted image."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'artwork/toxic-models'
model=json.loads((OUT/'toxic_waste_cuboid_approved.json').read_text(encoding='utf-8'))
project=json.loads((OUT/'toxic_waste_cuboid_approved.bbmodel').read_text(encoding='utf-8'))
fixed=[]
for element in model['elements']:
    for side,face in element['faces'].items():
        uv=face['uv']
        if uv[1]==uv[3]:
            assert element['name'].startswith('lid_') and side not in ['up','down']
            uv[1]=0;uv[3]=max(.5,(element['to'][1]-element['from'][1])*16/12.3)
            face['texture']='#10'
            native=next(e for e in project['elements'] if e['name']==element['name'])['faces'][side]
            native['uv']=[v*8 for v in uv];native['texture']=10
            fixed.append({'element':element['name'],'face':side,'uv':uv})
for filename,data in [('toxic_waste_cuboid_final.json',model),('toxic_waste_cuboid_final.bbmodel',project)]:
    (OUT/filename).write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
model.pop('format_version',None)
model['textures']={k:'alex_caves_spellsbook:entity/toxic/'+v for k,v in model['textures'].items()}
(ROOT/'src/main/resources/assets/alex_caves_spellsbook/models/item/toxic_waste_prop.json').write_text(json.dumps(model,ensure_ascii=False,indent=2),encoding='utf-8')
(OUT/'lid-uv-repair.json').write_text(json.dumps({'geometry_changed':False,'image_bytes_changed':False,'fixed_faces':fixed,'note':'Post-export UV repair. Editor MCP timed out after the approved export; repaired BBModel remains reopenable. Final UV revision has not been visually rechecked in the live editor.'},ensure_ascii=False,indent=2),encoding='utf-8')
for name in ['waste_front','waste_back','waste_left','waste_right','waste_top','waste_bottom','emitter_shell','emitter_frame','emitter_cell']:
    (ROOT/'src/main/resources/assets/alex_caves_spellsbook/textures/entity/toxic'/f'{name}.png.mcmeta').write_text('{"texture":{"blur":false,"clamp":false}}\n',encoding='utf-8')
print('Repaired',len(fixed),'lid side UVs; accepted geometry unchanged.')
