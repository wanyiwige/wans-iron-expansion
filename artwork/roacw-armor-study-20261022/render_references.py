"""Read-only source study: import exact JAR geometry into a scratch Blockbench project.

Uses the configured local MCP directly because the app tool bridge did not retain
project selection across requests. Source JAR and radiation files are never written.
"""
import sys,json,base64,math
from pathlib import Path
ROOT=Path(__file__).resolve().parent
sys.path.insert(0,str(ROOT.parent/'radiation-armor-20261022'))
from bb_client import call,text_result

def evaluate(code):
    result=text_result(call('risky_eval',{'code':code}))
    try:
        value=json.loads(result)
        return json.loads(value) if isinstance(value,str) and value.startswith(('{','[')) else value
    except json.JSONDecodeError:
        return result

FIT="const cam=Preview.selected.camera;cam.zoom=1;cam.updateProjectionMatrix();cam.updateMatrixWorld(true);const bb=new THREE.Box3().setFromObject(Project.model_3d);let x0=Infinity,y0=Infinity,x1=-Infinity,y1=-Infinity;for(const x of [bb.min.x,bb.max.x])for(const y of [bb.min.y,bb.max.y])for(const z of [bb.min.z,bb.max.z]){const p=new THREE.Vector3(x,y,z).project(cam);x0=Math.min(x0,p.x);x1=Math.max(x1,p.x);y0=Math.min(y0,p.y);y1=Math.max(y1,p.y);}cam.zoom=Math.min(2/(x1-x0),2/(y1-y0))*0.84;cam.updateProjectionMatrix();Canvas.updateAll();'fit'"

def capture(folder,label,mid,position,texture):
    evaluate('const tex=Texture.all.find(t=>t.name==='+json.dumps(texture)+');if(!tex)throw Error("Missing study material");tex.select();for(const c of Cube.all)for(const f of Object.values(c.faces))f.texture=tex.uuid;Canvas.updateAll();"reference material"')
    call('set_camera_angle',{'position':[position[i]+mid[i] for i in range(3)],'target':mid,'projection':'orthographic'})
    evaluate(FIT)
    result=call('capture_screenshot')
    images=[b for b in result.get('content',[]) if b['type']=='image']
    assert images,label
    (folder/(label+'.png')).write_bytes(base64.b64decode(images[0]['data']))

def render(name):
    folder=ROOT/name
    model=json.loads((folder/(name+'.geo.json')).read_text(encoding='utf-8'))
    native=model['minecraft:geometry'][0]
    expected=sum(len(b.get('cubes',[])) for b in native['bones'])
    filename=str(folder/(name+'.geo.json'))
    code="const scratch=ModelProject.all.find(p=>p.uuid==='61ecfbb0-b75c-a096-955b-4fac9812a708');if(!scratch||!scratch.select())throw Error('Scratch unavailable');if(Project.uuid!==scratch.uuid)throw Error('Selection failed');for(const el of Outliner.root.slice())el.remove();for(const t of Texture.all.slice())t.remove();if(!Project.BedrockEntityManager)Formats.bedrock.onSetup(Project);Codecs.bedrock.parse("+json.dumps(model)+","+json.dumps(filename)+",{import_to_current_project:true});Project.name="+json.dumps('ROACW Reference - '+name)+";Project.save_path='';Canvas.updateAll();JSON.stringify({name:Project.name,uuid:Project.uuid,cubes:Cube.all.length,bones:Group.all.length})"
    parsed=evaluate(code)
    assert isinstance(parsed,dict) and parsed.get('cubes')==expected,parsed
    assert parsed['bones']==len(native['bones']),parsed
    d=native['description']
    call('create_texture',{'name':name+'.png','width':d['texture_width'],'height':d['texture_height'],'data':str(folder/(name+'.png'))})
    call('create_texture',{'name':'study-bare-clay.png','width':16,'height':16,'fill_color':'#A0ADB2','layer_name':'Neutral clay reference'})
    bbox=evaluate('Canvas.updateAll();const box=new THREE.Box3().setFromObject(Project.model_3d);JSON.stringify({min:box.min.toArray(),max:box.max.toArray()})')
    assert all(isinstance(n,(int,float)) and math.isfinite(n) for n in bbox['min']+bbox['max']),bbox
    mid=[(bbox['min'][i]+bbox['max'][i])/2 for i in range(3)]
    capture(folder,'bare-front-three-quarter',mid,[48,24,-65],'study-bare-clay.png')
    capture(folder,'textured-front-three-quarter',mid,[48,24,-65],name+'.png')
    capture(folder,'textured-back-three-quarter',mid,[-48,24,65],name+'.png')
    call('list_export_formats',{'only_current_format':True})
    exported=json.loads(text_result(call('export_model',{'codec_id':'project','max_content_length':2000000})))
    assert not exported.get('truncated')
    (folder/(name+'-reference.bbmodel')).write_text(exported['content'],encoding='utf-8')
    record={'name':name,**parsed,'bounds':bbox,'note':'Exact native geometry and base color texture; bare view uses temporary opaque clay, no runtime glow layer.'}
    (folder/'study-render.json').write_text(json.dumps(record,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(record,ensure_ascii=False),flush=True)

if __name__=='__main__':
    for name in sys.argv[1:] or ['plaguebringer_armor','mars_armor','fearmonger_armor','auric_tesla_armor','arsenal_t1_armor','earthen_paladin_armor']:
        render(name)
    restored=evaluate("const original=ModelProject.all.find(p=>p.uuid==='741d619a-a4e7-abd1-7687-560ae87d66e4');if(!original||!original.select())throw Error('Original project unavailable');if(Cube.all.length!==169)throw Error('Original project changed');Canvas.updateAll();JSON.stringify({originalCubes:Cube.all.length,originalBones:Group.all.length,format:Format.id})")
    print('ORIGINAL_PRESERVED',json.dumps(restored),flush=True)
