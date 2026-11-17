"""Apply image-drawn materials and export the live Blockbench project through MCP."""
from pathlib import Path
import json,base64,importlib.util
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('bb',ROOT/'tools/blockbench-toxic.py')
bb=importlib.util.module_from_spec(spec);spec.loader.exec_module(bb)
OUT=ROOT/'artwork/toxic-models'
RES=ROOT/'src/main/resources/assets/alex_caves_spellsbook'
def call(name,args):
    r=bb.call(name,args)
    if 'error' in r or r.get('result',{}).get('isError'):raise RuntimeError(str(r)[:600])
    return r['result']
def decode(result):
    meta=json.loads(next(c['text'] for c in result['content'] if c['type']=='text'))
    raw=meta['content']
    try:return json.loads(raw)
    except json.JSONDecodeError:return json.loads(raw.replace('\\n','\n').replace('\\t','\t').replace('\\"','"'))
def export(name,prop):
    for codec,ext in [('project','bbmodel'),('java_block','json')]:
        result=call('export_model',{'codec_id':codec,'max_content_length':1000000})
        (OUT/(name+'-'+codec+'-mcp.json')).write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
        model=decode(result)
        (OUT/(name+'.'+ext)).write_text(json.dumps(model,ensure_ascii=False,indent=2),encoding='utf-8')
        if codec=='java_block':
            model.pop('format_version',None)
            model['textures']={k:v if v.startswith('#') else 'alex_caves_spellsbook:entity/toxic/'+v.rsplit('/',1)[-1].removesuffix('.png') for k,v in model['textures'].items()}
            (RES/'models/item'/f'{prop}.json').write_text(json.dumps(model,ensure_ascii=False,indent=2),encoding='utf-8')
def main():
    for name in ['emitter_shell','emitter_frame','emitter_cell']:
        data='data:image/png;base64,'+base64.b64encode((RES/'textures/entity/toxic'/f'{name}.png').read_bytes()).decode()
        literal=json.dumps(data).replace('/', '\\u002f')
        call('risky_eval',{'code':"(()=>{let t=Texture.all.find(t=>t.name==="+json.dumps(name)+");t.fromDataURL("+literal+");return {name:t.name};})()"})
    code="""(()=>{Project.texture_width=128;Project.texture_height=128;for(let t of Texture.all){t.uv_width=128;t.uv_height=128;}let shell=Texture.all.find(t=>t.name==='emitter_shell'),frame=Texture.all.find(t=>t.name==='emitter_frame'),cell=Texture.all.find(t=>t.name==='emitter_cell');for(let c of Cube.all){for(let [side,f] of Object.entries(c.faces)){let horizontal=side==='up'||side==='down';f.texture=shell.uuid;f.uv=[0,0,128,128];if(c.name==='solid_core'&&horizontal){f.texture=frame.uuid;}if(c.name.startsWith('cap_')){f.texture=horizontal?frame.uuid:shell.uuid;f.uv=horizontal?[0,0,128,128]:[0,0,128,16];}if(c.name.startsWith('front_frame_')){f.texture=shell.uuid;f.uv=horizontal?[0,0,20,20]:[0,0,20,128];}if(c.name.endsWith('energy_window')){let outside=(c.name.startsWith('front')&&side==='north')||(c.name.startsWith('back')&&side==='south')||(c.name.startsWith('left')&&side==='west')||(c.name.startsWith('right')&&side==='east');f.texture=outside?cell.uuid:shell.uuid;f.uv=outside?[18,32,110,98]:horizontal?[0,0,80,12]:[0,0,12,80];}}}Canvas.updateAll();return {cubes:Cube.all.length,textures:Texture.all.map(t=>({name:t.name,width:t.width,height:t.height})),unmapped:Cube.all.flatMap(c=>Object.values(c.faces)).filter(f=>!f.texture).length};})()"""
    print(call('risky_eval',{'code':code}))
    export('toxic_reactor_emitter_detailed','toxic_reactor_prop')
    shot=call('set_camera_angle',{'position':[29,22,31],'target':[8,8,8],'projection':'perspective'})
    shot=call('capture_screenshot',{})
    for c in shot.get('content',[]):
        if c['type']=='image':(OUT/'toxic-reactor-detailed-preview.png').write_bytes(base64.b64decode(c['data']))
    print('Detailed reactor exported into source resources.')
if __name__=='__main__':main()
