import json,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parent
sys.path.insert(0,str(ROOT.parent))
from bb_client import call,text_result

plan=json.loads((ROOT/'uv-plan.json').read_text(encoding='utf-8'))
call('save_checkpoint',{'name':'Before applying hand-painted pixel armor atlas v1'})
call('create_texture',{'name':'critical_warden_armor_v1.png','width':256,'height':256,'data':str(ROOT/'critical_warden_armor_v1.png'),'render_mode':'default','render_sides':'auto'})
mapping={}
for f in plan['faces']:
    mapping.setdefault(f['uuid'],{})[f['face']]=f['uv']
code='const atlas=Texture.all.find(t=>t.name==="critical_warden_armor_v1.png");if(!atlas)throw Error("painted atlas missing");const mapped='+json.dumps(mapping)+';let count=0;Project.texture_width=256;Project.texture_height=256;Project.box_uv=false;for(const c of Cube.all){if(!mapped[c.uuid])throw Error("unmapped cube "+c.name);c.box_uv=false;c.autouv=0;for(const [f,uv] of Object.entries(mapped[c.uuid])){c.faces[f].uv=uv;c.faces[f].texture=atlas.uuid;count++;}}Project.name="Critical Warden - Pixel Texture v1";Project.save_path='+json.dumps(str(ROOT/'critical_warden_textured_v1.bbmodel'))+';Project.saved=false;Canvas.updateAll();JSON.stringify({mappedFaces:count,cubes:Cube.all.length,texture:atlas.name})'
print(text_result(call('risky_eval',{'code':code})))
call('risky_eval',{'code':'const old=Texture.all.find(t=>t.name==="critical_warden_block_colors.png");if(old)old.remove();Canvas.updateAll();JSON.stringify({textures:Texture.all.map(t=>({name:t.name,width:t.width,height:t.height})),untextured:Cube.all.filter(c=>Object.values(c.faces).some(f=>!f.texture)).map(c=>c.name)})'})
call('save_checkpoint',{'name':'Painted pixel armor atlas v1 mapped to all faces'})

