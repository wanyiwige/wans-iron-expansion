import json
from bb_client import call,text_result,ROOT
import build_model as model

call('save_checkpoint',{'name':'Before geometry seam and coat flare refinement'})
old={c['name']:c for c in json.loads((ROOT/'geometry-plan.json').read_text(encoding='utf-8'))['cubes']}
model.geometry()
model.refine_geometry()
changed=[]
for c in model.CUBES:
    name=c['name']
    if name in old:
        if old[name]['from']!=c['from'] or old[name]['to']!=c['to']:
            call('modify_cube',{'id':name,'from':c['from'],'to':c['to']})
            changed.append(name)
    else:
        i=list(model.PALETTE).index(c['material'])
        x=(i%4)*4;y=(i//4)*4
        call('place_cube',{'elements':[{k:v for k,v in c.items() if k not in ('group','material')}],
             'group':c['group'],'texture':'critical_warden_block_colors.png',
             'faces':[{'face':f,'uv':[x+1,y+1,x+3,y+3]} for f in ('north','south','east','west','up','down')]})
        changed.append(name)
call('risky_eval',{'code':"for(const g of Group.all){if(g.name.startsWith('coatTail'))g.rotation=[-4,0,g.name.endsWith('Left')?3:-3];if(g.name.startsWith('coatSide'))g.rotation=[0,0,g.name.endsWith('Left')?4:-4];}Canvas.updateAll();'coats flare outward'"})
(ROOT/'geometry-plan.json').write_text(json.dumps({'groups':model.GROUPS,'palette':model.PALETTE,'cubes':model.CUBES},indent=2),encoding='utf-8')
print(json.dumps({'refined':changed,'cubes':len(model.CUBES)},ensure_ascii=False))
call('save_checkpoint',{'name':'Refined Critical Warden geometry complete'})
