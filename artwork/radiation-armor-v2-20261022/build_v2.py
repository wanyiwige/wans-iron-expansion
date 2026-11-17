"""Second, newly authored radiation armor; build geometry in live Blockbench first."""
import sys,json,base64,hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parent
sys.path.insert(0,str(ROOT.parent/'radiation-armor-20261022'))
import bb_client as client
client.ROOT=ROOT
from bb_client import call,text_result,init

GROUPS=[]
PARTS=[]
def group(name,pivot,parent=None,rotation=None):
    GROUPS.append({'name':name,'origin':pivot,'parent':parent,'rotation':rotation or [0,0,0]})
    return name
def cube(g,name,a,b,role='metal',inflate=0):
    assert all(b[i]>a[i] for i in range(3))
    PARTS.append({'name':name,'from':a,'to':b,'group':g,'role':role,'inflate':inflate})

def plan():
    h=group('armorHead',[0,24,0])
    b=group('armorBody',[0,24,0])
    la=group('armorLeftArm',[5,22,0]);ra=group('armorRightArm',[-5,22,0])
    w=group('armorLeggingTorsoLayer',[0,24,0])
    ll=group('armorLeftLeg',[2,12,0]);rl=group('armorRightLeg',[-2,12,0])
    lb=group('armorLeftBoot',[2,12,0]);rb=group('armorRightBoot',[-2,12,0])
    cube(h,'sealed_hood',[-4,24,-4],[4,32,4],'hood',0.2)
    c=group('hood_crown_fold',[0,31,0],h,[-7.5,0,0])
    cube(c,'folded_hood_crown',[-4,31,-4],[4,33,4],'hood_crown',0.05)
    for side,s in [('Left',1),('Right',-1)]:
        c=group('visor'+side,[0,29.5,-4],h,[7.5,-15*s,0])
        xa,xb=(0,4) if s==1 else (-4,0)
        cube(c,'sloped_visor_'+side,[xa,28,-5],[xb,31,-3],'visor',0.025)
    m=group('respirator',[0,26,-4],h,[10,0,0])
    cube(m,'respirator_centre',[-2,24,-6],[2,28,-4],'respirator',0.025)
    for side,s in [('Left',1),('Right',-1)]:
        c=group('jaw'+side,[0,26,-4],m,[0,-22.5*s,0])
        xa,xb=(1,4) if s==1 else (-4,-1)
        cube(c,'angled_jaw_'+side,[xa,24,-5],[xb,28,-3],'jaw',0.025)
    c=group('ocular_socket',[4,28.5,-1],h,[45,0,0])
    cube(c,'ocular_frame',[4,27,-2],[5,30,1],'ocular',0.025)
    cube(h,'right_connector',[-5,26,-1],[-4,28,1],'connector',0.025)
    cube(h,'hood_rear_clasp',[-1,25,4],[1,27,5],'rear_clasp',0.025)

    cube(b,'tailored_torso',[-4,12,-2],[4,24,2],'coat',0.15)
    for side,s in [('Left',1),('Right',-1)]:
        c=group('collar'+side,[0,23,2],b,[0,0,12.5*s])
        xa,xb=(0,5) if s==1 else (-5,0)
        cube(c,'folded_collar_'+side,[xa,22,0],[xb,25,4],'collar',0.025)
    cube(b,'collar_rear_lock',[-2,23,3],[2,25,4],'collar_lock',0.025)
    ch=group('chest_tilt',[0,21,-2],b,[10,0,0])
    for side,s in [('Left',1),('Right',-1)]:
        c=group('chest'+side,[0,21,-2],ch,[0,-18*s,0])
        xa,xb=(1,5) if s==1 else (-5,-1)
        cube(c,'folded_chest_'+side,[xa,18,-4],[xb,24,-1],'chest',0.025)
    core=group('core_diamond',[0,21,-4],ch,[0,0,45])
    cube(core,'diamond_core_socket',[-2,19,-5],[2,23,-3],'core_socket',0.025)
    cube(core,'fissile_core',[-1,20,-6],[1,22,-5],'core',0.025)
    cube(b,'radiation_plaque',[-3.5,11,-3],[3.5,18,-2],'trefoil',0.025)
    cube(b,'reactor_back_mount',[-3,16,2],[3,23,4],'back_mount',0.025)
    cube(b,'reactor_service_panel',[-2.5,17,4],[2.5,22,5],'back_panel',0.025)
    for side,s in [('Left',1),('Right',-1)]:
        c=group('reactor_canister'+side,[3*s,20,3],b,[0,0,-7.5*s])
        xa,xb=(3,5) if s==1 else (-5,-3)
        cube(c,'back_canister_'+side,[xa,14,3],[xb,20,5],'canister',0.025)
        xa,xb=(2.75,5.25) if s==1 else (-5.25,-2.75)
        cube(c,'canister_cap_'+side,[xa,19,2.75],[xb,20,5.25],'cap',0.025)
        xa,xb=(2,4) if s==1 else (-4,-2)
        cube(b,'canister_connection_'+side,[xa,19,1],[xb,21,4],'connector',0.025)
    cube(w,'containment_belt',[-4.5,11,-2.5],[4.5,13,2.5],'belt',0.025)
    for side,s in [('Left',1),('Right',-1)]:
        c=group('rear_tail'+side,[2*s,12,2.5],b,[-10,0,5*s])
        xa,xb=(0,4) if s==1 else (-4,0)
        cube(c,'rear_coat_tail_'+side,[xa,4,2],[xb,12,4],'tail',0.025)
        c=group('front_tail'+side,[2*s,12,-2.5],b,[8,0,8*s])
        xa,xb=(0,4) if s==1 else (-4,0)
        cube(c,'front_coat_panel_'+side,[xa,7,-3],[xb,12,-1],'front_tail',0.025)
        c=group('side_tail'+side,[4*s,12,0],b,[0,0,10*s])
        xa,xb=(3,5) if s==1 else (-5,-3)
        cube(c,'side_tasset_'+side,[xa,7,-1],[xb,13,2],'side_tail',0.025)

    for side,s,arm in [('Left',1,la),('Right',-1,ra)]:
        xa,xb=(4,8) if s==1 else (-8,-4)
        cube(arm,'sealed_sleeve_'+side,[xa,12,-2],[xb,23,2],'sleeve',0.15)
        sh=group('pauldron'+side,[4.5*s,23,0],arm,[0,0,-17.5*s])
        xa,xb=(4,9) if s==1 else (-9,-4)
        cube(sh,'pauldron_upper_'+side,[xa,22,-3],[xb,25,3],'shoulder',0.025)
        sh2=group('pauldron_outer'+side,[7*s,22,0],sh,[0,12.5*s,-10*s])
        xa,xb=(7,10) if s==1 else (-10,-7)
        cube(sh2,'pauldron_overlap_'+side,[xa,20,-3],[xb,23,3],'shoulder_outer',0.025)
        if s==1:
            cube(sh2,'left_shield_drop',[9,18,-2],[10,22,2],'shoulder_shield',0.025)
        fg=group('forearm'+side,[6*s,17,0],arm,[-7.5,0,-7.5*s])
        xa,xb=(4,8) if s==1 else (-8,-4)
        cube(fg,'forearm_guard_'+side,[xa,14,-3],[xb,19,-1],'forearm',0.025)
        xa,xb=(7,9) if s==1 else (-9,-7)
        cube(fg,'forearm_side_'+side,[xa,14,-1],[xb,18,2],'forearm_side',0.025)
        xa,xb=(4,8) if s==1 else (-8,-4)
        cube(arm,'wrist_cuff_'+side,[xa,12,-2],[xb,14,2],'cuff',0.25)

    for side,s,leg,boot in [('Left',1,ll,lb),('Right',-1,rl,rb)]:
        xa,xb=(0,4) if s==1 else (-4,0)
        cube(leg,'reinforced_trouser_'+side,[xa,0,-2],[xb,12,2],'trouser',0.1)
        k=group('knee'+side,[2*s,7,-1],leg,[-12.5,0,0])
        xa,xb=(0.5,3.5) if s==1 else (-3.5,-0.5)
        cube(k,'angled_knee_'+side,[xa,5,-3],[xb,8,-1],'knee',0.025)
        xa,xb=(3,4) if s==1 else (-4,-3)
        cube(leg,'thigh_lead_plate_'+side,[xa,8,-1],[xb,11,2],'thigh',0.2)
        xa,xb=(0,4) if s==1 else (-4,0)
        cube(boot,'boot_shell_'+side,[xa,0,-2],[xb,5,3],'boot',0.15)
        cube(boot,'square_toe_'+side,[xa,0,-4],[xb,2,0],'toe',0.15)
        ins=group('boot_instep'+side,[2*s,3,-1],boot,[-15,0,0])
        xa,xb=(0.5,3.5) if s==1 else (-3.5,-0.5)
        cube(ins,'sloped_instep_'+side,[xa,2,-4],[xb,5,-2],'instep',0.025)
        xa,xb=(0,4) if s==1 else (-4,0)
        cube(boot,'boot_sole_'+side,[xa,-1,-4],[xb,0,3],'sole',0.25)
        xa,xb=(4,5) if s==1 else (-5,-4)
        cube(boot,'ankle_socket_'+side,[xa,1,-1],[xb,4,1],'ankle',0.025)

def evaluation(code):
    text=text_result(call('risky_eval',{'code':code}))
    if text.startswith('Error executing code:'):raise RuntimeError(text)
    result=json.loads(text)
    if isinstance(result,str) and result.startswith(('{','[')):result=json.loads(result)
    return result

def capture(label,pos):
    call('set_camera_angle',{'position':pos,'target':[0,17,0],'projection':'orthographic'})
    evaluation('Preview.selected.camera.zoom=0.31;Preview.selected.camera.updateProjectionMatrix();Canvas.updateAll();"camera fitted"')
    result=call('capture_screenshot')
    for block in result.get('content',[]):
        if block['type']=='image':(ROOT/(label+'.png')).write_bytes(base64.b64decode(block['data']))

def export(codec,path):
    result=json.loads(text_result(call('export_model',{'codec_id':codec,'max_content_length':2000000})))
    assert not result.get('truncated')
    content=result['content']
    (ROOT/path).write_text(content if isinstance(content,str) else json.dumps(content,ensure_ascii=False,indent=2),encoding='utf-8')

def build():
    init()
    print(text_result(call('get_project_info'))[:700],flush=True)
    print(text_result(call('list_textures'))[:500],flush=True)
    plan()
    (ROOT/'geometry-plan.json').write_text(json.dumps({'groups':GROUPS,'parts':PARTS},indent=2),encoding='utf-8')
    print(text_result(call('create_project',{'name':'Critical Warden V2 - Geometry','format':'geckolib_model'})),flush=True)
    call('save_checkpoint',{'name':'V2 blank model before new geometry'})
    evaluation("Project.geckolib_model_type='armor';Project.geckolib_modid='alex_caves_spellsbook';Project.model_identifier='armor.critical_warden_v2';Project.visible_box=[3,3.5,1.25];Project.texture_width=16;Project.texture_height=16;Project.box_uv=false;'configured'")
    call('create_texture',{'name':'v2_geometry_clay.png','width':16,'height':16,'fill_color':'#A0ADB2','layer_name':'Geometry proof'})
    for g in GROUPS:
        args={k:v for k,v in g.items() if v is not None}
        args['shade']=True
        call('add_group',args)
    for g in GROUPS:
        parts=[{k:v for k,v in p.items() if k in ('name','from','to')} for p in PARTS if p['group']==g['name']]
        if parts:call('place_cube',{'elements':parts,'group':g['name'],'texture':'v2_geometry_clay.png','faces':[{'face':f,'uv':[0,0,16,16]} for f in ('north','south','east','west','up','down')]})
    inflates={p['name']:p['inflate'] for p in PARTS}
    result=evaluation('const inflation='+json.dumps(inflates)+';for(const c of Cube.all)c.inflate=inflation[c.name]||0;Canvas.updateAll();JSON.stringify({uuid:Project.uuid,cubes:Cube.all.length,bones:Group.all.length,rotatedBones:Group.all.filter(g=>g.rotation.some(n=>n)).length})')
    (ROOT/'geometry-result.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(json.dumps(result),flush=True)
    call('save_checkpoint',{'name':'V2 complete naked geometry before texture painting'})
    call('list_export_formats',{})
    export('project','critical_warden_v2_geometry.bbmodel')
    export('bedrock','critical_warden_v2_geometry.geo.json')
    for label,pos in [('bare-front-three-quarter',[48,36,-65]),('bare-front',[0,22,-70]),('bare-back-three-quarter',[-48,36,65]),('bare-left',[70,22,0])]:capture(label,pos)
    capture('bare-front-three-quarter',[48,36,-65])

if __name__=='__main__':build()
