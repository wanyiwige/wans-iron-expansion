import json
from bb_client import call,text_result,ROOT
from review_model import snapshot

neutral = json.loads(text_result(call('risky_eval', {'code':'JSON.stringify(Group.all.map(g=>({name:g.name,rotation:g.rotation.slice()})))'})))
if isinstance(neutral,str):neutral=json.loads(neutral)
call('save_checkpoint',{'name':'Before temporary articulation preview'})
pose={
    'armorHead':[5,25,0], 'armorLeftArm':[-50,0,-8], 'armorRightArm':[25,0,8],
    'armorLeftLeg':[25,0,0], 'armorRightLeg':[-25,0,0],
    'armorLeftBoot':[25,0,0], 'armorRightBoot':[-25,0,0],
    'coatTailLeft':[21,0,3], 'coatTailRight':[-29,0,-3],
    'coatSideLeft':[25,0,4], 'coatSideRight':[-25,0,-4],
}
code='const pose='+json.dumps(pose)+';for(const g of Group.all){if(pose[g.name])g.rotation=pose[g.name];}Canvas.updateAll();"temporary pose only"'
try:
    call('risky_eval',{'code':code})
    snapshot('articulation',[48,35,-65])
finally:
    code='const saved='+json.dumps(neutral)+';for(const v of saved){const g=Group.all.find(g=>g.name===v.name);if(g)g.rotation=v.rotation;}Canvas.updateAll();"neutral restored"'
    call('risky_eval',{'code':code})
(ROOT/'pose-review.json').write_text(json.dumps({'neutral':neutral,'preview_pose':pose,'note':'Editor pose only. Tail and boot rotations were set manually. No gameplay or animation integration is claimed.'},indent=2),encoding='utf-8')
