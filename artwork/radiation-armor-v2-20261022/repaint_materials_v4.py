"""Hand-authored UV pixel clusters in the live Blockbench canvas, material by material."""
import json,base64,io,hashlib
from PIL import Image
from build_v2 import ROOT,call,text_result,evaluation,export

OUT=ROOT/'texture-materials-v4';OUT.mkdir(exist_ok=True)
plan=json.loads((ROOT/'uv-plan.json').read_text(encoding='utf-8'))
P={'ink':'#0C1722','cloth0':'#151F34','cloth1':'#26344D','cloth2':'#3A4A64',
   'teal0':'#16353F','teal1':'#295964','teal2':'#47838A','teal3':'#73A5A5',
   'steel0':'#344650','steel1':'#607987','steel2':'#99AEB3',
   'strap0':'#24292F','strap1':'#45494B',
   'green0':'#176646','green1':'#3ABB65','lime':'#C6ED6C',
   'ochre':'#9C8B3F','yellow':'#E1CC66'}
cloth={'hood','hood_crown','coat','sleeve','tail','front_tail','side_tail','trouser','collar'}
steel={'chest','shoulder_outer','back_mount','canister','cap','thigh','instep'}
rubber={'belt','sole','connector','cuff'}
pixels={}
for f in plan['faces']:
    role,face=f['role'],f['face'];x0,y0,w,h=f['pixels']
    def put(x,y,c):
        if 0<=x<w and 0<=y<h:pixels[x0+x,y0+y]=P[c]
    def rect(x,y,ww,hh,c):
        for yy in range(y,y+hh):
            for xx in range(x,x+ww):put(xx,yy,c)
    def row(y,c,a=0,b=None):rect(a,y,(w if b is None else b)-a,1,c)
    def fill(c):rect(0,0,w,h,c)
    # Each material has continuous clusters. No noise, random wear or checkerboard shading.
    if role in cloth:
        fill('cloth1' if face in {'north','south','up'} else 'cloth0')
        if role in {'hood','hood_crown'} and w>=6:
            if face=='up':
                rect(1,1,2,max(1,h-2),'cloth2');rect(w-2,1,2,max(1,h-1),'cloth0')
            elif face in {'east','west','south'}:
                rect(1,1,2,max(1,h-2),'cloth1');rect(2,1,1,max(1,h-3),'cloth2')
                row(h-1,'cloth0')
        elif face in {'north','south'} and w>=3 and h>=5:
            rect(0,1,1,h-1,'cloth0')
            rect(max(1,w//2-1),1,1,h-2,'cloth2')
            if h>=8:rect(max(1,w//2-1),h//2,2,h-h//2-1,'cloth1')
            row(h-1,'cloth0')
        if role=='collar' and face in {'north','south'}:row(0,'cloth2')
    elif role in rubber:
        fill('strap0')
        if face=='up':fill('strap1')
        if role=='sole' and face=='down':
            for y in range(1,h,3):row(y,'ink')
        if role=='belt' and face=='north':
            rect(w//2-1,0,3,h,'ochre');put(w//2,0,'yellow');put(w//2,h-1,'strap0')
    else:
        issteel=role in steel
        low,mid,high=('steel0','steel1','steel2') if issteel else ('teal0','teal1','teal2')
        fill(high if face=='up' else low if face=='down' else mid)
        if w>=3 and h>=3:
            # A shaded rim surrounds one broad panel, rather than a mosaic of small patches.
            rect(w-1,0,1,h,low);row(h-1,low)
            if face in {'north','south'}:
                row(0,high,0,w-1)
                if h>=5:rect(0,1,1,h-2,low)
            elif face=='up':
                rect(1,1,max(1,w-2),max(1,h-2),mid)
        if face in {'east','west'} and w>=3:
            rect(0,0,1,h,low)

    if role=='visor' and face=='north':
        fill('teal0');row(0,'teal2')
        for x in range(1,w-1):put(x,1,'lime' if x==1 else 'green1')
        row(h-1,'ink')
    if role=='respirator' and face=='north':
        fill('steel0');row(0,'steel1');row(1,'ink');row(h-1,'ink')
        put(0,2,'steel2')
    if role=='jaw' and face=='north':
        row(0,'steel1');row(h-1,'teal0')
    if role=='ocular' and face in {'east','west'}:
        fill('teal0');put(1,0,'teal2');put(0,1,'teal2');put(1,1,'lime');put(2,1,'green1');put(1,2,'green0')
    if role=='core_socket' and face=='north':
        fill('teal0');row(0,'steel1');rect(0,0,1,h,'teal2');row(h-1,'ink')
    if role=='core':
        fill('green1' if face=='north' else 'green0')
        if face=='north':put(0,0,'lime');put(1,0,'lime');put(0,1,'green1')
    if role=='trefoil':
        fill('teal0')
        if face=='north':
            fill('ink')
            motif=['..YYY..','..YYY..','.......','Y..Y..Y','YY...YY','YYY.YYY','.YY.YY.']
            for y,line in enumerate(motif):
                for x,c in enumerate(line):
                    if c=='Y':put(x,y,'yellow')
    if role=='shoulder' and face=='up':
        rect(1,1,max(1,w-2),max(1,h-2),'teal1')
        rect(1,1,max(1,w-2),1,'teal2')
    if role=='shoulder_shield' and face in {'east','west'}:
        rect(0,1,w,1,'ochre')
    if role=='forearm' and face=='north':
        rect(1,1,max(1,w-2),max(1,h-2),'steel0')
        rect(1,1,max(1,w-2),1,'steel1')
        rect(1,h-2,max(1,w-2),1,'ink')
    if role=='canister' and face in {'north','south','east','west'}:
        row(0,'steel0');row(h-1,'steel0');row(2,'ochre')
    if role=='back_panel' and face=='south':
        rect(1,1,max(1,w-2),max(1,h-2),'ink')
        for y in range(1,h-1):put(w//2,y,'lime' if y==1 else 'green1')
    if role in {'tail','front_tail','side_tail'} and face in {'north','south','east','west'}:
        row(h-1,'teal0')
    if role=='toe' and face=='north':row(h-1,'steel0')
    if role=='boot' and face=='south' and w>=4:
        rect(1,h-2,w-2,1,'strap1')

info=json.loads(text_result(call('get_project_info')))
assert info['format']['id']=='geckolib_model' and info['counts']['cubes']==63
call('save_checkpoint',{'name':'Preserve flat V3 before material painting V4'})
name='critical_warden_materials.png'
textures=json.loads(text_result(call('list_textures')))
if not any(t['name']==name for t in textures):
    call('create_texture',{'name':name,'width':128,'height':128,'fill_color':[0,0,0,0],'layer_name':'Material pixel clusters'})
ops=[{'x':x,'y':y,'c':c} for (x,y),c in sorted(pixels.items())]
code='const t=Texture.all.find(t=>t.name==='+json.dumps(name)+');t.width=128;t.height=128;t.uv_width=128;t.uv_height=128;t.select();const ops='+json.dumps(ops)+';t.edit(canvas=>{canvas.width=128;canvas.height=128;const ctx=canvas.getContext("2d");ctx.imageSmoothingEnabled=false;for(const p of ops){ctx.fillStyle=p.c;ctx.fillRect(p.x,p.y,1,1);}},{no_undo:true});for(const c of Cube.all)for(const f of Object.values(c.faces))f.texture=t.uuid;for(const old of Texture.all.slice())if(old!==t)old.remove();Project.name="Critical Warden - Material painting V4";Canvas.updateAll();JSON.stringify({pixels:ops.length,cubes:Cube.all.length})'
print(evaluation(code),flush=True)
# Separate a few coplanar surfaces; retain every part's dimensions and its UV density.
geometry=json.loads((ROOT/'geometry-plan.json').read_text(encoding='utf-8'))
offsets={}
for part in geometry['parts']:
    n=part['name'];delta=None
    if n in {'folded_collar_Left','folded_collar_Right'}:delta=[0,0,0.0625 if n.endswith('Left') else -0.0625]
    if n=='left_shield_drop':delta=[0.0625,0,0]
    if any(n.startswith(s) for s in ['boot_shell_','square_toe_','sloped_instep_','boot_sole_','ankle_socket_']):delta=[0.25 if n.endswith('Left') else -0.25,0,0]
    if delta:offsets[n]={'from':[v+delta[i] for i,v in enumerate(part['from'])],'to':[v+delta[i] for i,v in enumerate(part['to'])]}
evaluation('const offsets='+json.dumps(offsets)+';for(const c of Cube.all)if(offsets[c.name]){c.from=offsets[c.name].from;c.to=offsets[c.name].to;}for(const g of Group.all){if(g.name==="boot_instepLeft")g.origin=[2.25,3,-1];if(g.name==="boot_instepRight")g.origin=[-2.25,3,-1];}Canvas.updateAll();"Separated coplanar surfaces"')
result=call('get_texture',{'texture':name});png=next(b for b in result['content'] if b['type']=='image')
(OUT/name).write_bytes(base64.b64decode(png['data']))
evaluation('const t=Texture.all.find(t=>t.name==='+json.dumps(name)+');t.path='+json.dumps(str(OUT/name))+';t.saved=true;t.internal=true;Project.save_path='+json.dumps(str(OUT/'critical_warden_materials.bbmodel'))+';"saved paths"')
call('list_export_formats')
export('project',OUT/'critical_warden_materials.bbmodel');export('bedrock',OUT/'critical_warden_materials.geo.json')

def screenshot(label,pos):
    call('set_camera_angle',{'position':pos,'target':[0,16,0],'projection':'orthographic'})
    evaluation('const cam=Preview.selected.camera;cam.zoom=1;cam.updateProjectionMatrix();cam.updateMatrixWorld(true);const bb=new THREE.Box3().setFromObject(Project.model_3d);let x0=Infinity,y0=Infinity,x1=-Infinity,y1=-Infinity;for(const x of[bb.min.x,bb.max.x])for(const y of[bb.min.y,bb.max.y])for(const z of[bb.min.z,bb.max.z]){const p=new THREE.Vector3(x,y,z).project(cam);x0=Math.min(x0,p.x);x1=Math.max(x1,p.x);y0=Math.min(y0,p.y);y1=Math.max(y1,p.y);}cam.zoom=Math.min(2/(x1-x0),2/(y1-y0))*0.82;cam.updateProjectionMatrix();Canvas.updateAll();"framed"')
    result=call('capture_screenshot');img=next(b for b in result['content'] if b['type']=='image')
    (OUT/(label+'.png')).write_bytes(base64.b64decode(img['data']))
for label,pos in [('front-three-quarter',[48,32,-70]),('front',[0,16,-70]),('back-three-quarter',[-48,32,70]),('back',[0,16,70])]:screenshot(label,pos)
screenshot('front-three-quarter',[48,32,-70])
(OUT/'palette.json').write_text(json.dumps(P,indent=2),encoding='utf-8')
(OUT/'paint-operations.json').write_text(json.dumps(ops),encoding='utf-8')
m=json.loads((OUT/'critical_warden_materials.bbmodel').read_text(encoding='utf-8'))
im=Image.open(OUT/name).convert('RGBA');embedded=Image.open(io.BytesIO(base64.b64decode(m['textures'][0]['source'].split(',',1)[1]))).convert('RGBA')
assert im.size==(128,128) and im.tobytes()==embedded.tobytes()
assert len(m['elements'])==63
for f in plan['faces']:
    x,y,w,h=f['pixels'];assert im.crop((x,y,x+w,y+h)).getextrema()[3]==(255,255)
assert all(f['texture']==0 for e in m['elements'] for f in e['faces'].values())
report={'native_resolution':[128,128],'opaque_palette_colors':len({v for v in im.get_flattened_data() if v[3]}),'cube_count':63,'mapped_faces':378,'embedded_equals_external':True,'geometry_changes':'Small collar/shield/boot offsets to separate coplanar faces; dimensions and UVs unchanged','deployed':False}
(OUT/'validation.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
(OUT/'README.md').write_text('''# 辐射护甲 V4：逐面材质重画\n\n2026-10-23。使用 Blockbench 原生像素画布，128×128 贴图，沿用已有模型与 UV 密度。\n\n深靛布料绘制连续褶皱；蓝绿金属绘制完整的凹面和定向亮边；胸片、外肩和背部罐体使用铅灰金属。面罩、皮带、护腕、靴底分别处理。亮绿集中于目镜、核心与背部能量窗。未使用随机噪点和棋盘格碎纹。\n\n保留 V2 和 V3 原文件。仅美术试稿，未注册、构建或部署。\n''',encoding='utf-8')
print(json.dumps(report),flush=True)
