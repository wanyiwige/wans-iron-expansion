"""New native Blockbench atlas: broad quiet materials, sparse focal accents."""
import json,base64,hashlib
from pathlib import Path
from build_v2 import ROOT,call,text_result,evaluation,export

OUT=ROOT/'texture-clean-v3'
OUT.mkdir(exist_ok=True)
plan=json.loads((ROOT/'uv-plan.json').read_text(encoding='utf-8'))
P={'ink':'#0B1520','cloth0':'#151C30','cloth1':'#202941','cloth2':'#303C56',
   'metal0':'#1B343D','metal1':'#2C4953','metal2':'#44656C',
   'rubber':'#11252C','green':'#3CCA64','lime':'#CBEC58','yellow':'#C5C95D'}
cloth={'hood','hood_crown','coat','sleeve','tail','front_tail','side_tail','trouser','collar'}
rubber={'belt','sole','cuff','cap','connector'}
pixels={}
for f in plan['faces']:
    role,face=f['role'],f['face'];x0,y0,w,h=f['pixels']
    def put(x,y,c):
        if 0<=x<w and 0<=y<h:pixels[x0+x,y0+y]=P[c]
    def row(y,c):
        for x in range(w):put(x,y,c)
    def fill(c):
        for y in range(h):row(y,c)
    if role in cloth:
        fill('cloth2' if face=='up' else 'cloth0' if face in ('east','west','down') else 'cloth1')
        # One continuous seam; no scattered highlights or mottling.
        if role in {'coat','tail','front_tail'} and face in {'north','south'} and w>=4:
            for y in range(h):put(w-1,y,'cloth0')
    elif role in rubber:
        fill('rubber' if face!='down' else 'ink')
    else:
        fill('metal2' if face=='up' else 'metal0' if face in {'east','west','down'} else 'metal1')
    if role=='visor' and face=='north':
        fill('ink')
        for x in range(1,w-1):put(x,1,'green')
    if role=='respirator' and face=='north':
        row(1,'ink');row(h-1,'ink')
    if role=='ocular' and face in {'east','west'}:
        fill('metal0');put(w//2,h//2,'green')
    if role=='core_socket' and face=='north':fill('metal0')
    if role=='core':fill('lime' if face=='north' else 'green')
    if role=='trefoil':
        fill('ink' if face=='north' else 'metal0')
        if face=='north':
            motif=['..YYY..','...Y...','.......','...Y...','YY...YY','YYY.YYY','.YY.YY.']
            for y,line in enumerate(motif):
                for x,c in enumerate(line):
                    if c=='Y':put(x,y,'yellow')
    if role=='back_panel' and face=='south':
        for y in range(1,h-1):put(w//2,y,'green')
    if role=='canister' and face in {'north','south','east','west'}:
        row(0,'rubber');row(h-1,'rubber')
    if role=='sole' and face=='down':
        row(0,'rubber');row(h-1,'rubber')

info=json.loads(text_result(call('get_project_info')))
assert info['format']['id']=='geckolib_model' and info['counts']['cubes']==63
call('save_checkpoint',{'name':'Preserve busy V2 before clean native repaint'})
textures=json.loads(text_result(call('list_textures')))
name='critical_warden_clean.png'
if not any(t['name']==name for t in textures):
    call('create_texture',{'name':name,'width':128,'height':128,'fill_color':[0,0,0,0],'layer_name':'Quiet materials'})
ops=[{'x':x,'y':y,'c':c} for (x,y),c in sorted(pixels.items())]
code='const t=Texture.all.find(t=>t.name==='+json.dumps(name)+');t.width=128;t.height=128;t.uv_width=128;t.uv_height=128;t.select();const ops='+json.dumps(ops)+';t.edit(canvas=>{canvas.width=128;canvas.height=128;const ctx=canvas.getContext("2d");ctx.clearRect(0,0,128,128);ctx.imageSmoothingEnabled=false;for(const p of ops){ctx.fillStyle=p.c;ctx.fillRect(p.x,p.y,1,1);}},{no_undo:true});for(const c of Cube.all)for(const f of Object.values(c.faces))f.texture=t.uuid;for(const old of Texture.all.slice())if(old!==t)old.remove();Project.name="Critical Warden - Clean materials";Canvas.updateAll();JSON.stringify({nativePixels:ops.length,textures:Texture.all.length})'
print(evaluation(code),flush=True)
result=call('get_texture',{'texture':name})
png=next(b for b in result['content'] if b['type']=='image')
(OUT/name).write_bytes(base64.b64decode(png['data']))
evaluation('const t=Texture.all.find(t=>t.name==='+json.dumps(name)+');t.path='+json.dumps(str(OUT/name))+';t.saved=true;t.internal=true;Project.save_path='+json.dumps(str(OUT/'critical_warden_clean.bbmodel'))+';"paths set"')
call('list_export_formats')
export('project',OUT/'critical_warden_clean.bbmodel')
export('bedrock',OUT/'critical_warden_clean.geo.json')

def screenshot(label,pos):
    call('set_camera_angle',{'position':pos,'target':[0,16,0],'projection':'orthographic'})
    evaluation('const cam=Preview.selected.camera;cam.zoom=1;cam.updateProjectionMatrix();cam.updateMatrixWorld(true);const bb=new THREE.Box3().setFromObject(Project.model_3d);let x0=Infinity,y0=Infinity,x1=-Infinity,y1=-Infinity;for(const x of[bb.min.x,bb.max.x])for(const y of[bb.min.y,bb.max.y])for(const z of[bb.min.z,bb.max.z]){const p=new THREE.Vector3(x,y,z).project(cam);x0=Math.min(x0,p.x);x1=Math.max(x1,p.x);y0=Math.min(y0,p.y);y1=Math.max(y1,p.y);}cam.zoom=Math.min(2/(x1-x0),2/(y1-y0))*0.82;cam.updateProjectionMatrix();Canvas.updateAll();"framed"')
    result=call('capture_screenshot')
    img=next(b for b in result['content'] if b['type']=='image')
    (OUT/(label+'.png')).write_bytes(base64.b64decode(img['data']))

for label,pos in [('front-three-quarter',[48,32,-70]),('front',[0,16,-70]),('back-three-quarter',[-48,32,70]),('back',[0,16,70])]:screenshot(label,pos)
screenshot('front-three-quarter',[48,32,-70])
(OUT/'palette.json').write_text(json.dumps(P,indent=2),encoding='utf-8')
(OUT/'paint-operations.json').write_text(json.dumps(ops),encoding='utf-8')
(OUT/'README.md').write_text('''# 辐射护甲：简化配色试稿\n\n2026-10-23。保持上一版几何与 UV，重新在 Blockbench 原生画布绘制贴图。\n\n- 11 色，128×128，沿用每模型单位 1 像素的 UV 密度。\n- 深靛布料、低饱和金属；去掉零散斑纹、重复亮边与四肢指示灯。\n- 明亮颜色集中于目镜、胸口核心、背部能量窗；腹部保留一个哑黄色辐射标识。\n- 上一版文件保留于父目录，未覆盖。\n- 仅编辑器美术试稿，未注册游戏护甲、未构建或部署。\n''',encoding='utf-8')
print(str(OUT),flush=True)
