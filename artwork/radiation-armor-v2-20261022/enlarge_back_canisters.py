"""Enlarge paired back canisters and repaint only their native UV islands."""
import json,base64,io,hashlib
from PIL import Image
from build_v2 import ROOT,init,call,text_result,evaluation,export

OUT=ROOT/'texture-materials-v5-large-canisters';OUT.mkdir(exist_ok=True)
BASE=ROOT/'texture-materials-v4'
original=json.loads((BASE/'critical_warden_materials.bbmodel').read_text(encoding='utf-8'))
P=json.loads((BASE/'palette.json').read_text(encoding='utf-8'))
init()
info=json.loads(text_result(call('get_project_info')))
assert info['format']['id']=='geckolib_model' and info['counts']['cubes']==63
assert evaluation('JSON.stringify(Cube.all.filter(c=>/^back_canister_/.test(c.name)).map(c=>c.name))')==['back_canister_Left','back_canister_Right']
call('save_checkpoint',{'name':'Before enlarging twin back canisters and remapping their UVs'})

changed={}
for side,s in [('Left',1),('Right',-1)]:
    changed['back_canister_'+side]={'from':[3 if s==1 else -6,13,3],'to':[6 if s==1 else -3,21,6]}
    changed['canister_cap_'+side]={'from':[2.5 if s==1 else -6.5,20,2.5],'to':[6.5 if s==1 else -2.5,21,6.5]}

ops={};uvs={};rects_by_name={}
for role,names,size,start in [('body',['back_canister_Left','back_canister_Right'],[3,8,3],[1,70]),('cap',['canister_cap_Left','canister_cap_Right'],[4,1,4],[17,70])]:
    dx,dy,dz=size;u,v=start
    rects={'north':[u+dz,v+dz,dx,dy],'east':[u,v+dz,dz,dy],'west':[u+dz+dx,v+dz,dz,dy],'south':[u+2*dz+dx,v+dz,dx,dy],'up':[u+dz,v,dx,dz],'down':[u+dz+dx,v,dx,dz]}
    for n in names:
        uvs[n]={f:[x,y,x+w,y+h] for f,(x,y,w,h) in rects.items()};rects_by_name[n]=rects
    for face,(x,y,w,h) in rects.items():
        def put(xx,yy,color):ops[x+xx,y+yy]=P[color]
        for yy in range(h):
            for xx in range(w):
                c='steel1'
                if face=='down' or xx==w-1:c='steel0'
                if face=='up':c='steel1' if 0<xx<w-1 and 0<yy<h-1 else 'steel2'
                if role=='body' and face in {'north','south','east','west'}:
                    if yy in {0,h-1}:c='steel0'
                    if yy==3:c='ochre'
                    if xx==0 and 0<yy<h-1 and yy!=3:c='steel2' if face in {'south','north'} else 'steel1'
                if role=='cap' and face not in {'up','down'}:c='steel0' if xx==w-1 else 'steel1'
                put(xx,yy,c)

result=evaluation('const change='+json.dumps(changed)+';const uv='+json.dumps(uvs)+';for(const c of Cube.all)if(change[c.name]){c.from=change[c.name].from;c.to=change[c.name].to;c.box_uv=false;for(const [f,r]of Object.entries(uv[c.name]))c.faces[f].uv=r;}Canvas.updateAll();JSON.stringify({changedParts:Object.keys(change),cubes:Cube.all.length})')
print(json.dumps(result),flush=True)
ops=[{'x':x,'y':y,'c':c} for (x,y),c in sorted(ops.items())]
name='critical_warden_large_canisters.png'
evaluation('const t=Texture.all[0];t.name='+json.dumps(name)+';const ops='+json.dumps(ops)+';t.edit(canvas=>{const ctx=canvas.getContext("2d");ctx.imageSmoothingEnabled=false;for(const p of ops){ctx.fillStyle=p.c;ctx.fillRect(p.x,p.y,1,1);}},{no_undo:true});t.select();Canvas.updateAll();"New canister UV islands painted at one pixel per unit"')
png=next(b for b in call('get_texture',{'texture':name})['content'] if b['type']=='image')
(OUT/name).write_bytes(base64.b64decode(png['data']))
evaluation('const t=Texture.all[0];t.path='+json.dumps(str(OUT/name))+';t.saved=true;t.internal=true;Project.name="Critical Warden - Larger back canisters";Project.save_path='+json.dumps(str(OUT/'critical_warden_large_canisters.bbmodel'))+';"Save paths set"')
call('list_export_formats')
export('project',OUT/'critical_warden_large_canisters.bbmodel')
export('bedrock',OUT/'critical_warden_large_canisters.geo.json')

def screenshot(label,pos):
    call('set_camera_angle',{'position':pos,'target':[0,16,0],'projection':'orthographic'})
    evaluation('const cam=Preview.selected.camera;cam.zoom=1;cam.updateProjectionMatrix();cam.updateMatrixWorld(true);const bb=new THREE.Box3().setFromObject(Project.model_3d);let x0=Infinity,y0=Infinity,x1=-Infinity,y1=-Infinity;for(const x of[bb.min.x,bb.max.x])for(const y of[bb.min.y,bb.max.y])for(const z of[bb.min.z,bb.max.z]){const p=new THREE.Vector3(x,y,z).project(cam);x0=Math.min(x0,p.x);x1=Math.max(x1,p.x);y0=Math.min(y0,p.y);y1=Math.max(y1,p.y);}cam.zoom=Math.min(2/(x1-x0),2/(y1-y0))*0.82;cam.updateProjectionMatrix();Canvas.updateAll();"Fitted"')
    png=next(b for b in call('capture_screenshot')['content'] if b['type']=='image')
    (OUT/(label+'.png')).write_bytes(base64.b64decode(png['data']))
for label,pos in [('back-three-quarter',[-48,32,70]),('back',[0,16,70]),('side',[70,20,0]),('front-three-quarter',[48,32,-70])]:screenshot(label,pos)
screenshot('back-three-quarter',[-48,32,70])

model=json.loads((OUT/'critical_warden_large_canisters.bbmodel').read_text(encoding='utf-8'))
im=Image.open(OUT/name).convert('RGBA');oldim=Image.open(BASE/'critical_warden_materials.png').convert('RGBA')
embedded=Image.open(io.BytesIO(base64.b64decode(model['textures'][0]['source'].split(',',1)[1]))).convert('RGBA')
assert im.size==(128,128) and im.tobytes()==embedded.tobytes()
assert im.crop((0,0,128,70)).tobytes()==oldim.crop((0,0,128,70)).tobytes()
prior={e['name']:e for e in original['elements']}
for e in model['elements']:
    if e['name'] not in changed:assert e==prior[e['name']],e['name']
    else:
        size=[e['to'][i]-e['from'][i] for i in range(3)]
        for f,uv in uvs[e['name']].items():
            expected=[size[0],size[2]] if f in {'up','down'} else [size[0],size[1]] if f in {'north','south'} else [size[2],size[1]]
            assert [uv[2]-uv[0],uv[3]-uv[1]]==expected
            assert im.crop(tuple(uv)).getextrema()[3]==(255,255)
report={'body_before':[2,6,2],'body_after':[3,8,3],'width_depth_increase_percent':50,'height_increase_percent':33.33,'other_elements_unchanged':True,'existing_texture_pixels_unchanged':True,'new_uv_density':1,'mapped_faces_opaque':True,'embedded_png_matches':True,'deployed':False}
(OUT/'validation.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
(OUT/'canister-change.json').write_text(json.dumps({'geometry':changed,'uv':uvs,'pixels':ops},indent=2),encoding='utf-8')
(OUT/'README.md').write_text('辐射护甲 V5：背罐加大\n\n罐体从 2×6×2 调至 3×8×3，罐盖同步加宽；保留倾斜角度与铅灰、黄色警示带。为罐体与罐盖新增 UV 区域，保持每模型单位 1 像素。其他部件及原贴图区域保持不变。仅美术修改，未部署游戏。\n',encoding='utf-8')
print(json.dumps(report),flush=True)
