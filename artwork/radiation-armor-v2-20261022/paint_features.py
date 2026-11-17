"""Paint authored, part-specific native pixels in Blockbench after UV image painting."""
import json,base64
from collections import defaultdict
from build_v2 import ROOT,call,text_result,evaluation,export,capture

plan=json.loads((ROOT/'uv-plan.json').read_text(encoding='utf-8'))
palette=json.loads((ROOT/'palette.json').read_text(encoding='utf-8-sig'))
pixels={};glows=set()
cloth={'hood','hood_crown','coat','sleeve','tail','front_tail','side_tail','trouser','collar'}
metal={'visor','jaw','respirator','ocular','chest','core_socket','back_mount','back_panel','canister','shoulder','shoulder_warning','shoulder_outer','shoulder_shield','forearm','forearm_side','cuff','knee','thigh','boot','toe','instep','ankle','rear_clasp','collar_lock'}

for f in plan['faces']:
    role,face=f['role'],f['face']
    x0,y0,w,h=f['pixels']
    def put(x,y,color,glow=False):
        if 0<=x<w and 0<=y<h:
            key=(x0+x,y0+y)
            pixels[key]=palette.get(color,color)
            if glow:glows.add(key)
            else:glows.discard(key)
    def row(y,color,a=0,b=None):
        for x in range(a,w if b is None else b):put(x,y,color)
    def fill(color):
        for yy in range(h):row(yy,color)
    if role in metal and w>=3 and h>=3 and face in ('north','south'):
        row(0,'t3')
        row(h-1,'t0')
        for yy in range(1,h-1):put(w-1,yy,'t0')
    if role in {'tail','front_tail','side_tail'} and face in ('north','south','east','west'):
        row(h-1,'t2')
        if w>=3:
            for yy in range(1,h-2,3):put(0,yy,'n3')
    if role=='sleeve' and face in ('north','south'):
        row(h-1,'ink');row(h-2,'r0')
    if role=='visor' and face=='north':
        fill('t0');row(0,'t3');row(h-1,'t1')
        for x in range(1,w-1):put(x,1,'g1',True)
    if role=='respirator' and face=='north':
        fill('t2')
        row(0,'t3');row(1,'ink');row(2,'t1');row(3,'ink')
    if role=='jaw' and face=='north':
        row(0,'s0');put(0,h-1,'t0')
    if role=='ocular' and face in ('east','west'):
        fill('t0')
        for x,y in [(1,0),(0,1),(2,1),(1,2)]:put(x,y,'g1',True)
        put(1,1,'y1',True)
    if role=='rear_clasp' and face=='south':
        fill('t0');put(0,0,'g1',True);put(1,0,'s1')
    if role=='core_socket' and face=='north':
        fill('t2')
        for xx,yy in [(0,0),(w-1,0),(0,h-1),(w-1,h-1)]:put(xx,yy,'s1')
        for yy in range(1,h-1):
            for xx in range(1,w-1):put(xx,yy,'ink')
    if role=='core':
        fill('g1')
        for yy in range(h):
            for xx in range(w):
                put(xx,yy,'y1' if face=='north' else 'g2',True)
        if face=='north':put(0,0,'y2',True);put(w-1,h-1,'g2',True)
    if role=='trefoil':
        fill('ink' if face=='north' else 't0')
        if face=='north':
            motif=['..YYY..','..YYY..','.......','...Y...','YY...YY','YYY.YYY','.YY.YY.']
            for yy,line in enumerate(motif):
                for xx,char in enumerate(line):
                    if char=='Y':put(xx,yy,'y1')
    if role=='back_panel' and face=='south':
        fill('t1');row(0,'t3');row(h-1,'t0')
        for yy in range(1,h-1):put(w//2,yy,'g1',True)
        put(w//2,0,'g2',True)
        for yy in (1,3):put(0,yy,'ink');put(w-1,yy,'ink')
    if role=='canister' and face in ('north','south','east','west'):
        row(0,'s0');row(h-1,'s0');row(2,'y0')
        put(w-1,4,'g1')
    if role=='shoulder_warning' and face=='north':
        for xx in (1,3):put(xx,1,'y1')
    if role=='forearm' and face=='north':
        for yy in range(1,h-1):put(1,yy,'g1',True)
        put(1,1,'g2',True)
    if role=='knee' and face=='north':
        put(w-1,1,'g1')
    if role=='instep' and face=='north':
        put(w//2,1,'g1',True);put(w//2,2,'g2',True)
    if role=='ankle' and face in ('east','west'):
        put(w//2,h//2,'g1',True)
    if role=='boot' and face=='south':
        for xx in range(1,w-1):put(xx,h-2,'s1')
    if role=='sole':
        fill('r0')
        if face=='down':
            for yy in range(1,h,2):row(yy,'r1')
        elif face in ('north','south'):row(0,'ink')

ops=[{'x':x,'y':y,'color':color} for (x,y),color in sorted(pixels.items())]
(ROOT/'authored-pixel-operations.json').write_text(json.dumps({'operations':ops,'glow_pixels':[list(p) for p in sorted(glows)]},indent=2),encoding='utf-8')
info=json.loads(text_result(call('get_project_info')))
assert info['counts']['cubes']==63 and info['format']['id']=='geckolib_model'
call('save_checkpoint',{'name':'Before V2 UV mapping and authored pixel details'})
textures=json.loads(text_result(call('list_textures')))
if not any(t['name'] in ('critical_warden_v2.png','critical_warden_v2_painted_base.png') for t in textures):
    call('create_texture',{'name':'critical_warden_v2.png','width':128,'height':128,'data':str(ROOT/'critical_warden_v2_painted_base.png')})
evaluation('const texture=Texture.all.find(t=>t.name==="critical_warden_v2.png"||t.name==="critical_warden_v2_painted_base.png");if(!texture)throw Error("Painted texture absent");texture.name="critical_warden_v2.png";"texture named"')
mapping=defaultdict(dict)
for f in plan['faces']:mapping[f['uuid']][f['face']]=f['uv']
code='const tex=Texture.all.find(t=>t.name==="critical_warden_v2.png");if(!tex)throw Error("Atlas missing");tex.select();Project.texture_width=128;Project.texture_height=128;Project.box_uv=false;const uv='+json.dumps(mapping)+';for(const c of Cube.all){if(!uv[c.uuid])throw Error("Unmapped cube "+c.name);c.box_uv=false;c.autouv=0;for(const [face,rect] of Object.entries(uv[c.uuid])){c.faces[face].uv=rect;c.faces[face].texture=tex.uuid;}}Canvas.updateAll();"UV mapped"'
evaluation(code)
code='const texture=Texture.all.find(t=>t.name==="critical_warden_v2.png");const ops='+json.dumps(ops)+';texture.edit(canvas=>{const ctx=canvas.getContext("2d");ctx.imageSmoothingEnabled=false;for(const op of ops){ctx.fillStyle=op.color;ctx.fillRect(op.x,op.y,1,1);}},{no_undo:true});Canvas.updateAll();JSON.stringify({paintedNativePixels:ops.length,cubes:Cube.all.length})'
print(json.dumps(evaluation(code)),flush=True)
result=call('get_texture',{'texture':'critical_warden_v2.png'})
image=next(b for b in result['content'] if b['type']=='image')
(ROOT/'critical_warden_v2.png').write_bytes(base64.b64decode(image['data']))
code='const texture=Texture.all.find(t=>t.name==="critical_warden_v2.png");texture.path='+json.dumps(str(ROOT/'critical_warden_v2.png'))+';texture.saved=true;texture.internal=true;const gray=Texture.all.find(t=>t.name==="v2_geometry_clay.png");if(gray)gray.remove();Project.name="Critical Warden V2 - Painted";Project.save_path='+json.dumps(str(ROOT/'critical_warden_v2.bbmodel'))+';Canvas.updateAll();"ready"'
evaluation(code)
call('save_checkpoint',{'name':'V2 geometry and per-part pixel painting complete'})
call('list_export_formats',{})
export('project','critical_warden_v2.bbmodel')
export('bedrock','critical_warden_v2.geo.json')
for label,pos in [('painted-front-three-quarter',[48,36,-65]),('painted-front',[0,22,-70]),('painted-back-three-quarter',[-48,36,65]),('painted-back',[0,22,70]),('painted-left',[70,22,0])]:capture(label,pos)
capture('painted-front-three-quarter',[48,36,-65])
print('V2_TEXTURED_EXPORTED',flush=True)
