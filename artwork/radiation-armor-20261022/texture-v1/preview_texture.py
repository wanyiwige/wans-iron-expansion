import base64,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parent
sys.path.insert(0,str(ROOT.parent))
from bb_client import call

VIEWS={'front-three-quarter':[48,35,-65],'front':[0,22,-70],'back-three-quarter':[-48,35,65],'back':[0,22,70],'left':[70,22,0]}
for name,pos in VIEWS.items():
    call('set_camera_angle',{'position':pos,'target':[0,16.5,0],'projection':'orthographic'})
    call('risky_eval',{'code':'Preview.selected.camera.zoom=0.34;Preview.selected.camera.updateProjectionMatrix();Canvas.updateAll();"camera fitted"'})
    result=call('capture_screenshot')
    for block in result.get('content',[]):
        if block['type']=='image':
            target=ROOT/('preview-'+name+'.png')
            target.write_bytes(base64.b64decode(block['data']))
            print(target.name)
