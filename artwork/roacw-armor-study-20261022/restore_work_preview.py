import sys,json,base64
from pathlib import Path
ROOT=Path(__file__).resolve().parent
ARMOR=ROOT.parent/'radiation-armor-20261022'
sys.path.insert(0,str(ARMOR))
from bb_client import call,text_result

def open_and_preview(source,output,name):
    data=json.loads(source.read_text(encoding='utf-8'))
    assert len(data['elements'])==169
    print(text_result(call('create_project',{'name':name,'format':'geckolib_model'})),flush=True)
    code="const model=JSON.parse(Buffer.from('"+json.dumps(data).encode().hex()+"','hex').toString('utf8'));Codecs.project.parse(model);Project.name="+json.dumps(name)+";Project.save_path="+json.dumps(str(source))+";Canvas.updateAll();if(Cube.all.length!==169)throw Error('Model count mismatch');JSON.stringify({uuid:Project.uuid,cubes:Cube.all.length,bones:Group.all.length,textures:Texture.all.length})"
    print(text_result(call('risky_eval',{'code':code})),flush=True)
    call('set_camera_angle',{'position':[48,35,-65],'target':[0,16.5,0],'projection':'orthographic'})
    call('risky_eval',{'code':'Preview.selected.camera.zoom=0.34;Preview.selected.camera.updateProjectionMatrix();Canvas.updateAll();"camera fitted"'})
    result=call('capture_screenshot')
    for b in result.get('content',[]):
        if b['type']=='image':output.write_bytes(base64.b64decode(b['data']))
    print(text_result(call('get_project_info'))[:850],flush=True)

open_and_preview(ARMOR/'critical_warden_armor.bbmodel',ARMOR/'preview-front-three-quarter.png','Critical Warden - Geometry Preserved')
open_and_preview(ARMOR/'texture-v1/critical_warden_textured_v1.bbmodel',ARMOR/'texture-v1/preview-front-three-quarter.png','Critical Warden - Pixel Texture v1 Preserved')
