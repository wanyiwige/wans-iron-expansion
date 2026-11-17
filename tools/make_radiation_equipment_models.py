"""Create a one-voxel-thick native item model and matching editable Blockbench project."""
from pathlib import Path
import base64,json,uuid
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'artwork/radiation-equipment-release-20261022'
MODELS=ROOT/'src/main/resources/assets/alex_caves_spellsbook/models/item'
TEX=ROOT/'src/main/resources/assets/alex_caves_spellsbook/textures/item'
namespace='alex_caves_spellsbook'
rows=json.loads((ART/'texture-export.json').read_text(encoding='utf-8-sig'))

def write(path,data):path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for name,scale in [('mechanical_radiation_eye',1.55),('radiation_grimoire',1.55)]:
    write(MODELS/(name+'.json'),{
        'parent':'minecraft:item/generated','textures':{'layer0':f'{namespace}:item/{name}'},
        'display':{'gui':{'scale':[scale]*3},'fixed':{'scale':[scale]*3},'ground':{'scale':[.65]*3}}})

row=next(r for r in rows if r['id']=='fission_staff')
native=Image.open(ROOT/row['source']).convert('RGBA')
width,height=native.size;assert width==height==37
texture_size=row['textureSize'];ox,oy=row['offset'];unit=16/width
front=8-unit/2;back=8+unit/2
elements=[];bb_elements=[]
directions=('north','south','east','west','up','down')
for y in range(height):
    for x in range(width):
        if native.getpixel((x,y))[3]==0:continue
        lo=[x*unit,(height-y-1)*unit,front];hi=[(x+1)*unit,(height-y)*unit,back]
        uv=[(ox+x)*16/texture_size,(oy+y)*16/texture_size,(ox+x+1)*16/texture_size,(oy+y+1)*16/texture_size]
        name=f'pixel_{x}_{y}'
        elements.append({'name':name,'from':lo,'to':hi,'faces':{side:{'uv':uv,'texture':'#0'} for side in directions}})
        bb_elements.append({'name':name,'type':'cube','uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,'wans:fission_staff:'+name)),
                            'from':lo,'to':hi,'origin':[8,8,8],'box_uv':False,'autouv':0,'export':True,
                            'faces':{side:{'uv':[ox+x,oy+y,ox+x+1,oy+y+1],'texture':0} for side in directions}})
display={
 'gui':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[.95]*3},
 'ground':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.55]*3},
 'fixed':{'rotation':[0,180,0],'scale':[1]*3},
 'thirdperson_righthand':{'rotation':[0,-90,55],'translation':[0,4,.5],'scale':[1.4]*3},
 'thirdperson_lefthand':{'rotation':[0,90,-55],'translation':[0,4,.5],'scale':[1.4]*3},
 'firstperson_righthand':{'rotation':[0,-90,25],'translation':[1.13,3.2,1.13],'scale':[1.05]*3},
 'firstperson_lefthand':{'rotation':[0,90,-25],'translation':[1.13,3.2,1.13],'scale':[1.05]*3}}
model={'credit':'Single voxel layer generated from the approved pixel texture; no Blockbench MCP was available.',
       'ambientocclusion':False,'gui_light':'front','textures':{'0':f'{namespace}:item/fission_staff','particle':f'{namespace}:item/fission_staff'},
       'elements':elements,'display':display}
# Do not inherit builtin/generated: it replaces explicit geometry with item-layer extrusion.
write(MODELS/'fission_staff.json',model)
texture_path=TEX/'fission_staff.png'
bb={'meta':{'format_version':'4.10','model_format':'java_block','box_uv':False},'name':'fission_staff_single_layer',
    'ambientocclusion':False,'front_gui_light':True,'resolution':{'width':64,'height':64},'elements':bb_elements,
    'outliner':[{'name':'single_voxel_layer','origin':[8,8,8],'uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,'wans:fission_staff:layer')),
                 'children':[e['uuid'] for e in bb_elements]}],
    'textures':[{'name':'fission_staff.png','id':'0','namespace':namespace,'folder':'item','width':64,'height':64,'uv_width':64,'uv_height':64,
                 'uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,'wans:fission_staff:texture')),'particle':True,
                 'source':'data:image/png;base64,'+base64.b64encode(texture_path.read_bytes()).decode()}],
    'display':display}
write(ART/'fission_staff_single_layer.bbmodel',bb)
write(ART/'model-report.json',{'cubes':len(elements),'native_grid':[width,height],'one_cube_per_opaque_pixel':True,
      'layer_count':1,'thickness_model_units':unit,'z_bounds':[front,back],'all_six_faces_textured':True,'model_parent':None,
      'blockbench_project_method':'authored JSON, not a connected MCP session'})
print('Generated',len(elements),'cubes in exactly one layer; six textured faces per cube; editable bbmodel saved.')
