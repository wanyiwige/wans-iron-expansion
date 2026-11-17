"""Create closed cuboid spell props through Blockbench MCP; preserve editor exports as evidence."""
from pathlib import Path
import json,base64
from importlib.util import spec_from_file_location,module_from_spec
ROOT=Path(__file__).resolve().parents[1]
loader=spec_from_file_location('bb',ROOT/'tools/blockbench-toxic.py');bb=module_from_spec(loader);loader.loader.exec_module(bb)
OUT=ROOT/'artwork/toxic-models';OUT.mkdir(parents=True,exist_ok=True)
def run(name,args):
    result=bb.call(name,args)
    if 'error' in result or result.get('result',{}).get('isError'):raise RuntimeError(str(result)[:1000])
    return result['result']
def text(result):
    return '\n'.join(c.get('text','') for c in result.get('content',[]) if c['type']=='text')
def save_export(name,codec):
    result=run('export_model',{'codec_id':codec,'max_content_length':1000000})
    (OUT/(name+'-'+codec+'-mcp.json')).write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
    return result
def texture(name,color):
    return run('create_texture',{'name':name,'width':32,'height':32,'fill_color':color,'layer_name':name})
def main():
    run('create_project',{'name':'toxic_waste_cuboid','format':'java_block'})
    run('save_checkpoint',{'name':'empty cuboid waste prop'})
    texture('silver_metal','#D6DEE4');texture('steel_rims','#8796A3');texture('hazard_yellow','#EAD524');texture('warning_base','#202821')
    marker=ROOT/'src/main/resources/assets/alex_caves_spellsbook/textures/entity/toxic/radiation_mark.png'
    run('create_texture',{'name':'radiation_glyph','data':'data:image/png;base64,'+base64.b64encode(marker.read_bytes()).decode()})
    run('create_texture',{'name':'warning_strip','width':96,'height':32,'fill_color':'#202821','layer_name':'warning band'})
    # Composite only the already drawn ImageGen glyph in the editor; no new image generation proxy.
    code="""(()=>{let strip=Texture.all.find(t=>t.name==='warning_strip');let glyph=Texture.all.find(t=>t.name==='radiation_glyph');let c=strip.ctx;c.imageSmoothingEnabled=false;for(let i=0;i<3;i++)c.drawImage(glyph.img,i*32+3,3,26,26);strip.updateChangesAfterEdit();return {strip:strip.uuid,width:strip.width,height:strip.height};})()"""
    print('strip',text(run('risky_eval',{'code':code}))[:300])
    for x,color in [(0,'#EDF1F4'),(3,'#B8C5CF'),(28,'#A7B7C2'),(30,'#F0F4F6')]:
        run('draw_shape_tool',{'texture_id':'silver_metal','shape':'rectangle','start':{'x':x,'y':0},'end':{'x':min(31,x+1),'y':31},'color':color,'opacity':255})
    cubes=[
        ('silver_cuboid_body',[2,0,3],[14,16,13],'silver_metal'),
        ('top_rim',[1.75,15.3,2.75],[14.25,16,13.25],'steel_rims'),
        ('bottom_rim',[1.75,0,2.75],[14.25,.8,13.25],'steel_rims'),
        ('warning_band',[1.85,6.2,2.85],[14.15,9.8,13.15],'warning_base'),
        ('warning_band_upper',[1.8,9.8,2.8],[14.2,10.15,13.2],'hazard_yellow'),
        ('warning_band_lower',[1.8,5.85,2.8],[14.2,6.2,13.2],'hazard_yellow'),
        ('lid_handle_left',[5.5,16,6],[6.5,16.8,10],'steel_rims'),
        ('lid_handle_right',[9.5,16,6],[10.5,16.8,10],'steel_rims'),
        ('lid_handle_bridge',[6.5,16.4,6],[9.5,16.8,7],'silver_metal'),
    ]
    for name,lo,hi,tex in cubes:run('place_cube',{'elements':[{'name':name,'from':lo,'to':hi}],'texture':tex,'faces':True})
    # Each closed band face spans the full repeated strip. All other faces stay fully covered.
    code="""(()=>{Project.texture_width=32;Project.texture_height=32;let strip=Texture.all.find(t=>t.name==='warning_strip');let band=Cube.all.find(c=>c.name==='warning_band');for(let side of ['north','south','east','west']){band.faces[side].texture=strip.uuid;band.faces[side].uv=[0,0,32,32];}for(let c of Cube.all){for(let side of ['up','down'])c.faces[side].uv=[0,0,32,32];}Canvas.updateAll();return {format:Format.id,cubes:Cube.all.length,nonVolumetric:Cube.all.filter(c=>c.to.some((v,i)=>v<=c.from[i])).length};})()"""
    print('geometry',text(run('risky_eval',{'code':code}))[:500])
    run('set_camera_angle',{'position':[35,28,37],'target':[8,8,8],'projection':'perspective'})
    shot=run('capture_screenshot',{})
    (OUT/'cuboid-screenshot-mcp.json').write_text(json.dumps(shot,ensure_ascii=False),encoding='utf-8')
    for c in shot.get('content',[]):
        if c['type']=='image':(OUT/'toxic-waste-cuboid-preview.png').write_bytes(base64.b64decode(c['data']))
    save_export('toxic_waste_cuboid','project');save_export('toxic_waste_cuboid','java_block')
    print('EXPORTS SAVED',OUT)
if __name__=='__main__':main()
