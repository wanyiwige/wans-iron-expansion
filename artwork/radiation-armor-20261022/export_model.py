import base64
import hashlib
import json
from bb_client import call,text_result,ROOT
from review_model import snapshot


def export(codec,path):
    obj=json.loads(text_result(call('export_model',{'codec_id':codec,'max_content_length':2000000})))
    assert not obj.get('truncated'),obj
    content=obj['content']
    data=content if isinstance(content,str) else json.dumps(content,ensure_ascii=False,indent=2)
    (ROOT/path).write_text(data,encoding='utf-8')
    return json.loads(data)


call('risky_eval',{'code':"Project.visible_box=[3,3,1];Canvas.updateAll();'bounds updated'"})
call('save_checkpoint',{'name':'Critical Warden final geometry, before detailed pixel painting'})
model=export('project','critical_warden_armor.bbmodel')
geo=export('bedrock','critical_warden_armor.geo.json')
texture=model['textures'][0]
png=base64.b64decode(texture['source'].split(',',1)[1])
(ROOT/'critical_warden_block_colors.png').write_bytes(png)

elements=model['elements']
groups=model['groups']
uuids=[e['uuid'] for e in elements]+[g['uuid'] for g in groups]
assert len(uuids)==len(set(uuids))
assert len(elements)==169
assert len(groups)==13
assert len(model['textures'])==1
assert model['meta']['model_format']=='geckolib_model'
assert model['geckolib_model_type']=='armor'
assert all(all(e['to'][i]>e['from'][i] for i in range(3)) for e in elements)
assert all(set(e['faces'])=={'north','south','east','west','up','down'} for e in elements)
assert all(f['texture']==0 and len(f['uv'])==4 and all(0<=v<=16 for v in f['uv']) for e in elements for f in e['faces'].values())
members=[]
def walk(items):
    for item in items:
        if isinstance(item,str):members.append(item)
        else:walk(item.get('children',[]))
walk(model['outliner'])
assert sorted(members)==sorted(e['uuid'] for e in elements)
# Blockbench 5 serializes groups separately and its outliner stores UUIDs.
by_id={g['uuid']:g['name'] for g in groups}
root_names={g.get('name',by_id[g['uuid']]) for g in model['outliner'] if isinstance(g,dict)}
required={'armorHead','armorBody','armorLeftArm','armorRightArm','armorLeggingTorsoLayer','armorLeftLeg','armorRightLeg','armorLeftBoot','armorRightBoot'}
assert root_names==required,(root_names,required)
native=geo['minecraft:geometry'][0]
assert sum(len(b.get('cubes',[])) for b in native['bones'])==169
assert len(native['bones'])==13

manifest={
 'stage':'Completed geometry with temporary flat color swatches; detailed pixel texture not painted.',
 'tool':'Live Blockbench MCP 1.7.0 over the configured localhost endpoint',
 'project_uuid':'741d619a-a4e7-abd1-7687-560ae87d66e4',
 'cubes':len(elements),'bones':len(groups),'faces':sum(len(e['faces']) for e in elements),
 'slot_roots':sorted(required),'native_format':geo['format_version'],
 'checks':['Nonzero three-axis volume for every cube','All six faces have a valid color texture reference','No duplicated UUIDs or orphaned elements','All nine humanoid armor roots retained','Native geometry export retains every cube and bone','Editor articulation preview returned to neutral pose'],
 'limitations':['Flat color swatch UVs must be repacked before detailed pixel painting','Boot and coat motion in the editor preview were manually posed; runtime bone synchronization has not been implemented','Not registered, built, deployed or tested in Minecraft'],
 'sha256':{},
}
for name in ('critical_warden_armor.bbmodel','critical_warden_armor.geo.json','critical_warden_block_colors.png'):
    manifest['sha256'][name]=hashlib.sha256((ROOT/name).read_bytes()).hexdigest()
(ROOT/'model-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
snapshot('front-three-quarter',[48,35,-65])
snapshot('front',[0,22,-70])
snapshot('back-three-quarter',[-48,35,65])
snapshot('back',[0,22,70])
snapshot('left',[70,22,0])
snapshot('front-three-quarter',[48,35,-65])
call('risky_eval',{'code':'Project.save_path='+json.dumps(str(ROOT/'critical_warden_armor.bbmodel'))+';Project.saved=true;"project saved to exported file"'})
print(json.dumps(manifest,ensure_ascii=False,indent=2))
