"""Construct the Critical Warden armor in the live Blockbench editor through MCP.

This is geometry with flat color swatches, not the final painted armor atlas.
Coordinates use the existing magnetic armor's GeckoLib humanoid convention.
"""
import json
from collections import defaultdict
from pathlib import Path
from bb_client import call, text_result, ROOT

PALETTE = {
    'cloth': '#15192E', 'cloth_light': '#262E49', 'shadow': '#092D38',
    'metal': '#075A63', 'edge': '#22AD99', 'green': '#27CF62',
    'lime': '#DFF65B', 'steel': '#617480', 'black': '#080F1B',
    'rubber': '#263B42', 'core': '#91F546', 'steel_light': '#9DB8BC',
}
GROUPS = [
    ('armorHead', [0, 24, 0], None),
    ('armorBody', [0, 24, 0], None),
    ('armorLeftArm', [5, 22, 0], None),
    ('armorRightArm', [-5, 22, 0], None),
    ('armorLeggingTorsoLayer', [0, 24, 0], None),
    ('armorLeftLeg', [2, 12, 0], None),
    ('armorRightLeg', [-2, 12, 0], None),
    ('armorLeftBoot', [2, 12, 0], None),
    ('armorRightBoot', [-2, 12, 0], None),
    ('coatTailLeft', [2.5, 12, 2.75], 'armorBody'),
    ('coatTailRight', [-2.5, 12, 2.75], 'armorBody'),
    ('coatSideLeft', [4.25, 12, 0], 'armorBody'),
    ('coatSideRight', [-4.25, 12, 0], 'armorBody'),
]
CUBES = []


def cube(group, material, name, a, b, rotation=None, pivot=None):
    obj = {'name': name, 'from': a, 'to': b}
    if rotation:
        obj['rotation'] = rotation
        obj['origin'] = pivot or [0, 0, 0]
    CUBES.append({'group': group, 'material': material, **obj})


def geometry():
    h = 'armorHead'
    cube(h, 'cloth', 'hood_crown', [-4.5, 31.25, -4.25], [4.5, 32.75, 4.25])
    cube(h, 'cloth', 'hood_back', [-4.5, 24, 3.75], [4.5, 31.25, 4.75])
    for side, sign in [('left', 1), ('right', -1)]:
        x0, x1 = (3.75, 4.75) if sign == 1 else (-4.75, -3.75)
        cube(h, 'cloth', f'hood_{side}_wall', [x0, 24, -4.25], [x1, 31.25, 3.75])
        x0, x1 = (2.75, 3.75) if sign == 1 else (-3.75, -2.75)
        cube(h, 'metal', f'crown_{side}_short_ridge', [x0, 32.5, -3.5], [x1, 33.25, 2.5])
        x0, x1 = (3.25, 4.5) if sign == 1 else (-4.5, -3.25)
        cube(h, 'metal', f'visor_{side}_cheek', [x0, 25.5, -4.875], [x1, 29.5, -4.125])
        x0, x1 = (0.375, 3.25) if sign == 1 else (-3.25, -0.375)
        cube(h, 'green', f'visor_{side}_green_lens', [x0, 27.625, -4.8125], [x1, 29, -4.6875])
        cube(h, 'core', f'visor_{side}_upper_light', [x0, 28.75, -4.875], [x1, 29, -4.8125])
    cube(h, 'metal', 'hood_forehead_frame', [-4.5, 29.5, -4.75], [4.5, 31.5, -3.875])
    cube(h, 'shadow', 'hood_forehead_step', [-3.5, 31.25, -4.625], [3.5, 32, -4.125])
    cube(h, 'black', 'visor_recess', [-3.5, 27.25, -4.6875], [3.5, 29.5, -4.375])
    cube(h, 'shadow', 'visor_centre_bridge', [-0.375, 27.125, -5], [0.375, 29.5, -4.5])
    cube(h, 'metal', 'lower_jaw_shell', [-3.75, 24, -4.75], [3.75, 26, -3.75])
    cube(h, 'shadow', 'respirator_chamfer_base', [-2.25, 24, -5.25], [2.25, 27.25, -4.5])
    cube(h, 'metal', 'respirator_front_plate', [-1.75, 24.25, -5.5], [1.75, 27, -5.25])
    for i in range(3):
        y = 24.625 + 0.75 * i
        cube(h, 'black', f'respirator_vent_{i+1}', [-1.25, y, -5.5625], [1.25, y+0.25, -5.5])
    cube(h, 'shadow', 'left_ocular_mount', [4.5, 26.75, -3.125], [5.25, 30.25, 0.375])
    cube(h, 'metal', 'left_ocular_vertical_ring', [5.25, 26.75, -2.625], [5.75, 30.25, -0.125])
    cube(h, 'metal', 'left_ocular_horizontal_ring', [5.25, 27.25, -3.125], [5.75, 29.75, 0.375])
    cube(h, 'black', 'left_ocular_recess', [5.75, 27.5, -2.375], [5.875, 29.5, -0.375])
    cube(h, 'green', 'left_ocular_lens', [5.875, 27.875, -2], [6, 29.125, -0.75])
    cube(h, 'lime', 'left_ocular_focus', [6, 28.25, -1.625], [6.0625, 28.75, -1.125])
    for y in (27.125, 29.625):
        cube(h, 'steel', f'ocular_fastener_{y}', [5.75, y, -1.625], [5.9375, y+0.25, -1.125])
    cube(h, 'steel', 'right_sealed_connector', [-5.125, 26.5, -1], [-4.75, 28, 1])
    cube(h, 'cloth_light', 'hood_back_spine', [-0.5, 26, 4.75], [0.5, 31.75, 5])
    for x in (-1.5, 0.75):
        cube(h, 'green', f'hood_rear_status_{x}', [x, 25, 4.75], [x+0.75, 25.5, 4.875])

    b = 'armorBody'
    cube(b, 'cloth', 'sealed_coat_torso', [-4.25, 12, -2.25], [4.25, 24, 2.25])
    cube(b, 'cloth_light', 'front_left_lapel', [2.75, 14, -2.5], [4.25, 22.5, -2.25])
    cube(b, 'cloth_light', 'front_right_lapel', [-4.25, 14, -2.5], [-2.75, 22.5, -2.25])
    cube(b, 'metal', 'high_collar_back', [-4.25, 23, 2.25], [4.25, 25.5, 3.25])
    for side, sign in [('left', 1), ('right', -1)]:
        xa, xb = (3.25, 4.5) if sign == 1 else (-4.5, -3.25)
        cube(b, 'metal', f'high_collar_{side}', [xa, 22.5, -1.5], [xb, 25.25, 2.25])
        xa, xb = (2.25, 3.75) if sign == 1 else (-3.75, -2.25)
        cube(b, 'shadow', f'collar_{side}_front_return', [xa, 22.5, -3], [xb, 23.75, -2])
        xa, xb = (3.125, 3.625) if sign == 1 else (-3.625, -3.125)
        cube(b, 'metal', f'chest_{side}_containment_rail', [xa, 17.5, -3], [xb, 22.5, -2.5])
        xa, xb = (2.25, 2.75) if sign == 1 else (-2.75, -2.25)
        cube(b, 'green', f'core_{side}_short_conduit', [xa, 17.5, -2.625], [xb, 19.5, -2.5])
    cube(b, 'shadow', 'fission_core_mount', [-2.75, 19, -3.125], [2.75, 23.25, -2.25])
    cube(b, 'metal', 'fission_frame_top', [-2.25, 22.5, -3.625], [2.25, 23.25, -3.125])
    cube(b, 'metal', 'fission_frame_bottom', [-2.25, 19, -3.625], [2.25, 19.75, -3.125])
    for x in (-2.25, 1.5):
        cube(b, 'metal', f'fission_frame_side_{x}', [x, 19.75, -3.625], [x+0.75, 22.5, -3.125])
    cube(b, 'black', 'fission_isolation_socket', [-1.5, 19.75, -3.25], [1.5, 22.5, -3.125])
    cube(b, 'green', 'fission_core_outer', [-1.125, 20, -3.625], [1.125, 22.25, -3.25])
    cube(b, 'core', 'fission_core_inner', [-0.75, 20.375, -3.75], [0.75, 21.875, -3.625])
    cube(b, 'lime', 'fission_core_centre', [-0.375, 20.75, -3.8125], [0.375, 21.5, -3.75])
    for x in (-2.5, 2):
        for y in (19.125, 22.625):
            cube(b, 'steel', f'core_clamp_{x}_{y}', [x, y, -3.75], [x+0.5, y+0.5, -3.625])
    cube(b, 'shadow', 'radiation_emblem_plaque', [-1.625, 14.75, -2.75], [1.625, 18, -2.5])
    # Coarse embossed three-blade motif. Final texture painting may simplify it.
    for name, rect in [
        ('top_wide', [-0.875, 17, 0.875, 17.5]),
        ('top_narrow', [-0.625, 16.5, 0.625, 17]),
        ('centre_dot', [-0.25, 16, 0.25, 16.5]),
        ('left_wide', [-1.25, 15.25, -0.5, 16]),
        ('left_tip', [-1, 15, -0.5, 15.25]),
        ('right_wide', [0.5, 15.25, 1.25, 16]),
        ('right_tip', [0.5, 15, 1, 15.25]),
    ]:
        x0,y0,x1,y1 = rect
        cube(b, 'lime', 'emblem_'+name, [x0,y0,-2.8125], [x1,y1,-2.75])
    cube(b, 'shadow', 'reactor_back_mount', [-3.5, 15.5, 2.25], [3.5, 23.25, 3])
    cube(b, 'metal', 'reactor_back_case', [-3, 16.25, 3], [3, 22.75, 4])
    cube(b, 'shadow', 'reactor_back_face', [-2.5, 16.75, 4], [2.5, 22.25, 4.375])
    cube(b, 'green', 'reactor_vertical_window', [-0.5, 17.5, 4.375], [0.5, 21.75, 4.5])
    cube(b, 'core', 'reactor_window_upper_cap', [-0.375, 21.25, 4.5], [0.375, 21.75, 4.5625])
    for x in (-2.25, 1):
        for y in (18.5, 20):
            cube(b, 'black', f'back_vent_{x}_{y}', [x,y,4.375], [x+1.25,y+0.5,4.5])
    cube(b, 'metal', 'reactor_top_overhang', [-3.25, 22.25, 3], [3.25, 23, 4.5])
    cube(b, 'metal', 'reactor_bottom_rail', [-2.75, 16.25, 4], [2.75, 16.75, 4.5])
    cube(b, 'rubber', 'reactor_belt_connection', [-0.75, 13, 2.25], [0.75, 15.5, 3.125])
    for x in (-3.375, 2.875):
        cube(b, 'metal', f'back_vertical_frame_{x}', [x,16,3], [x+0.5,23,3.5])
    # Independent tail pivots keep a useful editing/animation boundary.
    for side, sign in [('Left',1),('Right',-1)]:
        g = 'coatTail'+side
        xa, xb = (0.375,4.25) if sign == 1 else (-4.25,-0.375)
        cube(g,'cloth','rear_'+side+'_tail', [xa,5,2.5], [xb,12.5,3.25])
        cube(g,'metal','rear_'+side+'_hem', [xa,5,2.5], [xb,5.5,3.375])
        xa, xb = (3.75,4.25) if sign == 1 else (-4.25,-3.75)
        cube(g,'shadow','rear_'+side+'_outer_binding', [xa,5.5,3.25], [xb,12.5,3.375])
        g = 'coatSide'+side
        xa, xb = (4.25,5) if sign == 1 else (-5,-4.25)
        cube(g,'cloth','side_'+side+'_tail', [xa,6.5,-1.5], [xb,12.5,2])
        cube(g,'metal','side_'+side+'_hem', [xa,6.5,-1.5], [xb,7,2])
        xa, xb = (2.25,4.25) if sign == 1 else (-4.25,-2.25)
        cube(g,'cloth_light','front_'+side+'_short_panel', [xa,8,-2.875], [xb,12.5,-2.25])
        cube(g,'metal','front_'+side+'_panel_hem', [xa,8,-3], [xb,8.5,-2.875])

    for side, sign in [('Left', 1), ('Right', -1)]:
        g = 'armor'+side+'Arm'
        xa, xb = (4.125,8.125) if sign == 1 else (-8.125,-4.125)
        cube(g,'cloth','sleeve_'+side,[xa,12,-2.125],[xb,23,2.125])
        xa, xb = (4.125,8.625) if sign == 1 else (-8.625,-4.125)
        cube(g,'shadow','pauldron_'+side+'_underlayer',[xa,21.5,-2.5],[xb,24,2.5])
        if sign == 1:
            cube(g,'metal','left_shield_pauldron',[4.25,23,-2.875],[9,24.75,2.875])
            cube(g,'metal','left_pauldron_outer_drop',[8.5,21.25,-2.5],[9.25,23.5,2.5])
            cube(g,'edge','left_pauldron_top_edge',[4.5,24.5,-2.875],[8.75,24.75,-2.5])
            for x in (5,6.25,7.5):
                cube(g,'lime',f'left_shoulder_warning_{x}',[x,23.5,-3],[x+0.5,24.25,-2.875])
        else:
            cube(g,'metal','right_pauldron_upper',[-8.375,23.25,-2.625],[-4.25,24.5,2.625])
            cube(g,'metal','right_pauldron_overlap',[-8.625,21.75,-2.75],[-4.5,23,-2.125])
        xa, xb = (4.5,7.75) if sign == 1 else (-7.75,-4.5)
        cube(g,'metal','forearm_'+side+'_plate',[xa,14.5,-2.875],[xb,18.75,-2.125])
        xa, xb = (5.75,6.5) if sign == 1 else (-6.5,-5.75)
        cube(g,'green','forearm_'+side+'_window',[xa,15.5,-3],[xb,18,-2.875])
        xa, xb = (4.125,8.125) if sign == 1 else (-8.125,-4.125)
        cube(g,'rubber','wrist_'+side+'_seal',[xa,13.75,-2.25],[xb,14.5,2.25])
        cube(g,'cloth_light','glove_'+side+'_back',[xa,12,-2.375],[xb,13.5,-2.125])
        xa, xb = (7.75,8.375) if sign == 1 else (-8.375,-7.75)
        cube(g,'shadow','forearm_'+side+'_outer_rail',[xa,14.5,-1.75],[xb,18.75,1.75])

    w='armorLeggingTorsoLayer'
    cube(w,'rubber','lead_belt_front',[-4.375,12,-2.625],[4.375,13.25,-2.25])
    cube(w,'rubber','lead_belt_back',[-4.375,12,2.25],[4.375,13.25,2.625])
    for x in (-4.625,4.25):
        cube(w,'metal',f'belt_side_lock_{x}',[x,12,-2.25],[x+0.375,13.25,2.25])
    cube(w,'metal','belt_square_buckle',[-1.125,11.75,-3],[1.125,13.5,-2.625])
    for x in (-0.75,-0.125,0.5):
        cube(w,'green',f'belt_three_step_meter_{x}',[x,12.25,-3.0625],[x+0.375,13,-3])
    cube(w,'shadow','right_hip_canister_mount',[-5,9.25,-1.25],[-4.375,12.25,1.25])
    cube(w,'metal','right_hip_sealed_canister',[-5.5,9.5,-1],[-5,12,1])
    cube(w,'steel','right_hip_canister_cap',[-5.625,11.5,-1.125],[-5,12,1.125])
    cube(w,'green','canister_status_window',[-5.5625,10,-0.5],[-5.5,11,0.5])
    cube(w,'steel','left_hip_attachment', [4.375,10.5,-0.625],[4.875,12,0.625])

    for side, sign in [('Left',1),('Right',-1)]:
        g='armor'+side+'Leg'
        xa,xb=(0.125,4.125) if sign==1 else (-4.125,-0.125)
        cube(g,'cloth','trouser_'+side,[xa,0.75,-2.125],[xb,12,2.125])
        xa,xb=(3.75,4.375) if sign==1 else (-4.375,-3.75)
        cube(g,'shadow','thigh_'+side+'_side_strip',[xa,7,-1.5],[xb,11.5,1.5])
        xa,xb=(0.625,3.625) if sign==1 else (-3.625,-0.625)
        cube(g,'metal','knee_'+side+'_shield',[xa,5.25,-2.75],[xb,7.75,-2.125])
        cube(g,'edge','knee_'+side+'_upper_step',[xa,7.25,-2.9375],[xb,7.75,-2.75])
        xa,xb=(3,3.5) if sign==1 else (-3.5,-3)
        cube(g,'green','knee_'+side+'_status',[xa,5.75,-2.8125],[xb,6.25,-2.75])
        xa,xb=(0.625,3.625) if sign==1 else (-3.625,-0.625)
        cube(g,'cloth_light','trouser_'+side+'_rear_fold',[xa,5.5,2.125],[xb,6.25,2.25])
        g='armor'+side+'Boot'
        xa,xb=(0,4.25) if sign==1 else (-4.25,0)
        cube(g,'rubber','boot_'+side+'_upper',[xa,1,-2.375],[xb,4.75,2.375])
        cube(g,'shadow','boot_'+side+'_sole',[xa,0,-3.125],[xb,1,2.625])
        cube(g,'metal','boot_'+side+'_square_toe',[xa,1,-3.125],[xb,2.25,-2.375])
        xa,xb=(0.5,3.75) if sign==1 else (-3.75,-0.5)
        cube(g,'metal','boot_'+side+'_shin',[xa,2.25,-2.875],[xb,5,-2.375])
        xa,xb=(1.75,2.5) if sign==1 else (-2.5,-1.75)
        cube(g,'lime','boot_'+side+'_short_window',[xa,3,-3],[xb,4.5,-2.875])
        xa,xb=(-0.125,4.375) if sign==1 else (-4.375,0.125)
        cube(g,'shadow','boot_'+side+'_sealed_cuff',[xa,4.25,-2.5],[xb,4.875,2.5])
        xa,xb=(4.25,4.75) if sign==1 else (-4.75,-4.25)
        cube(g,'metal','boot_'+side+'_ankle_box',[xa,1.75,-0.75],[xb,3.25,0.75])
        x0,x1=(4.75,4.8125) if sign==1 else (-4.8125,-4.75)
        for z in (-0.5,0.125):
            cube(g,'green',f'boot_{side}_ankle_status_{z}',[x0,2.25,z],[x1,2.75,z+0.375])
        xa,xb=(0.75,3.5) if sign==1 else (-3.5,-0.75)
        cube(g,'steel','boot_'+side+'_heel_latch',[xa,1.75,2.375],[xb,2.25,2.5625])
        xa,xb=(0.125,4.125) if sign==1 else (-4.125,-0.125)
        for i,z in enumerate((-2.5,-0.75,1)):
            cube(g,'rubber',f'boot_{side}_sole_tread_{i}',[xa,-0.125,z],[xb,0,z+0.75])


def refine_geometry():
    by_name = {c['name']: c for c in CUBES}
    def set_axis(name, axis, lo=None, hi=None):
        c=by_name[name]
        if lo is not None:c['from'][axis]=lo
        if hi is not None:c['to'][axis]=hi
    set_axis('hood_forehead_frame',0,-4.5625,4.5625)
    set_axis('hood_forehead_step',1,31.1875)
    set_axis('visor_left_cheek',1,hi=29.5625)
    set_axis('visor_right_cheek',1,hi=29.5625)
    set_axis('visor_centre_bridge',1,hi=29.625)
    set_axis('respirator_chamfer_base',1,24.125)
    set_axis('left_ocular_horizontal_ring',2,hi=-2.625)
    cube('armorHead','metal','left_ocular_rear_ring_segment',[5.25,27.25,-0.125],[5.75,29.75,0.375])
    set_axis('sealed_coat_torso',0,-4.0625,4.0625)
    set_axis('front_left_lapel',0,hi=4.0625)
    set_axis('front_right_lapel',0,-4.0625)
    set_axis('high_collar_left',2,hi=2.375)
    set_axis('high_collar_right',2,hi=2.375)
    set_axis('pauldron_Left_underlayer',0,4.0625)
    set_axis('pauldron_Right_underlayer',0,hi=-4.0625)
    set_axis('left_pauldron_outer_drop',2,-2.5625,2.5625)
    set_axis('left_pauldron_top_edge',1,hi=24.8125)
    set_axis('left_pauldron_top_edge',2,-2.9375)
    set_axis('right_pauldron_overlap',0,-8.6875)
    set_axis('wrist_Left_seal',0,4,8.25)
    set_axis('wrist_Right_seal',0,-8.25,-4)
    for side in ('Left','Right'):
        set_axis('glove_'+side+'_back',1,11.9375)
        for name in ('rear_'+side+'_hem','side_'+side+'_hem'):
            c=by_name[name]
            c['from']=[v-0.0625 for v in c['from']]
            c['to']=[v+0.0625 for v in c['to']]
    set_axis('right_hip_canister_cap',0,hi=-4.9375)
    set_axis('right_hip_canister_cap',1,hi=12.0625)
    set_axis('boot_Left_sealed_cuff',0,0.125)
    set_axis('boot_Right_sealed_cuff',0,hi=-0.125)
    set_axis('core_left_short_conduit',0,hi=2.8125)
    set_axis('core_right_short_conduit',0,-2.8125)


def build():
    geometry()
    refine_geometry()
    (ROOT/'geometry-plan.json').write_text(json.dumps({'groups':GROUPS,'palette':PALETTE,'cubes':CUBES},indent=2),encoding='utf-8')
    print(text_result(call('save_checkpoint',{'name':'Before full Critical Warden geometry'})),flush=True)
    print(text_result(call('risky_eval',{'code':"Project.texture_width=16;Project.texture_height=16;Project.box_uv=false;Project.model_identifier='armor.critical_warden';Project.geckolib_model_type='armor';Project.geckolib_modid='alex_caves_spellsbook';JSON.stringify({name:Project.name,format:Format.id})"})),flush=True)
    call('create_texture',{'name':'critical_warden_block_colors.png','width':16,'height':16,'fill_color':'#15192E','layer_name':'Geometry color swatches'})
    slots={}
    for i,(mat,color) in enumerate(PALETTE.items()):
        x=(i%4)*4;y=(i//4)*4
        slots[mat]=[x+1,y+1,x+3,y+3]
        call('draw_shape_tool',{'texture_id':'critical_warden_block_colors.png','shape':'rectangle','start':{'x':x,'y':y},'end':{'x':x+3,'y':y+3},'color':color,'opacity':255})
    for name,pivot,parent in GROUPS:
        args={'name':name,'origin':pivot,'shade':True}
        if parent:
            args['parent']=parent
        if name.startswith('coatTail'):
            args['rotation']=[-4,0,3 if name.endswith('Left') else -3]
        if name.startswith('coatSide'):
            args['rotation']=[0,0,4 if name.endswith('Left') else -4]
        call('add_group',args)
    batches=defaultdict(list)
    for c in CUBES:
        batches[(c['group'],c['material'])].append({k:v for k,v in c.items() if k not in ('group','material')})
    for (group,mat),items in batches.items():
        call('place_cube',{'elements':items,'group':group,'texture':'critical_warden_block_colors.png','faces':[{'face':f,'uv':slots[mat]} for f in ('north','south','east','west','up','down')]})
        print(f'{group}: {mat}: {len(items)} cubes',flush=True)
    print(text_result(call('save_checkpoint',{'name':'Critical Warden complete geometry and block colors'})),flush=True)
    print(text_result(call('get_project_info')),flush=True)
    call('set_camera_angle',{'position':[48,35,-65],'target':[0,16.5,0],'projection':'orthographic'})
    result=call('capture_screenshot')
    import base64
    for block in result.get('content',[]):
        if block['type']=='image':
            (ROOT/'preview-front-three-quarter.png').write_bytes(base64.b64decode(block['data']))
    print('BUILD_COMPLETE',len(CUBES),flush=True)


if __name__=='__main__':
    build()
