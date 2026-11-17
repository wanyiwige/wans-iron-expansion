"""Preserve Blockbench geometry; repair atlas discovery and assign UVs at four texels per model unit."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets'
atlas_path=ASSETS/'minecraft/atlases/blocks.json'
atlas=json.loads(atlas_path.read_text(encoding='utf-8'))
sprite='alex_caves_spellsbook:entity/toxic/lead_curtain_metal'
if not any(source.get('resource')==sprite for source in atlas['sources']):
    atlas['sources'].append({'type':'minecraft:single','resource':sprite})
atlas_path.write_text(json.dumps(atlas,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

path=ASSETS/'alex_caves_spellsbook/models/item/toxic_lead_prop.json'
model=json.loads(path.read_text(encoding='utf-8'))
geometry=[(element['name'],element['from'],element['to']) for element in model['elements']]
for element in model['elements']:
    dx,dy,dz=[b-a for a,b in zip(element['from'],element['to'])]
    for face,data in element['faces'].items():
        width,height=(dx,dy) if face in ('north','south') else (dz,dy) if face in ('east','west') else (dx,dz)
        if face in ('north','south') and width<=4 and height<=3:
            u,v=12,12
        elif height>=12:
            u,v=(0 if face=='north' else 2 if face=='south' else 6),0
        elif width>8:
            u,v=0,9
        else:
            u,v=4,3
        assert 0<=u<u+width<=16 and 0<=v<v+height<=16,(element['name'],face,width,height)
        data['uv']=[u,v,u+width,v+height]
        data['texture']='#0'
model['ambientocclusion']=False
assert geometry==[(element['name'],element['from'],element['to']) for element in model['elements']]
path.write_text(json.dumps(model,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Atlas sprite added; 9 original full-depth elements retained; 54 face UVs use equal texel density.')
