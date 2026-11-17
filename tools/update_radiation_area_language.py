"""Descriptions for the area spell revision; leave unrelated translations intact."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
rows={
 'ui.alex_caves_spellsbook.toxic.new.isotope_brand':('给瞄准区域半径五格内的所有可见敌人施加烙印。','Brand all visible enemies within five blocks of the aimed area.'),
 'spell.alex_caves_spellsbook.isotope_brand.guide':('以瞄准的敌人或落点为中心，给半径五格内的可见敌人各施加一层本人的裂变印记，并分别造成六次周期伤害。圆形边界显示施加范围，标志显示在每个目标头顶；离开范围不移除已施加的效果。最多同时记录三十二名敌人，不会透墙或标记盟友。最高八级为传说稀有度。','Apply one owned fission mark and six periodic damage pulses to each visible enemy within five blocks of your aimed enemy or location. A pixel circle shows the initial area; marks appear overhead and persist after leaving it. Up to 32 targets, excluding allies and blocked enemies. Level 8 is Legendary.'),
 'ui.alex_caves_spellsbook.toxic.new.radiation_film':('为半径五格内敌人分别记录四张底片，重施时一同曝光。','Record four snapshots per enemy within five blocks, then expose them together.'),
 'spell.alex_caves_spellsbook.radiation_film.guide':('以瞄准的敌人或落点为中心，同时锁定半径五格内最多三十二名可见敌人，为每名目标分别记录四张底片。法术书或武器可免费重施提前曝光，三秒后也会自动曝光。每张底片向所属目标的当前位置照射，后续伤害逐次保留百分之七十，受方块遮挡。某个目标死亡不会取消其他目标的曝光。','Lock up to 32 visible enemies within five blocks of the aimed enemy or location and record four snapshots for each. Recast free from a spellbook or weapon, or expose automatically after three seconds. Each snapshot fires toward its own target, blocked by terrain; later shots retain 70% damage. One target dying does not cancel the others.'),
 'ui.alex_caves_spellsbook.toxic.new.cloud_chamber_trail':('记录敌人径迹；重施后沿路径扫掠伤害并在末端爆发。','Record enemy trails, then recast to sweep them with damage and endpoint bursts.'),
 'spell.alex_caves_spellsbook.cloud_chamber_trail.guide':('地面云室记录最多八名敌人的移动径迹，每条最多十二个节点，静止敌人也会记录一个节点。四秒后自动触发，法术书或武器可以免费提前重施。追迹粒子沿整条路径逐段扫掠，碰到敌人造成路径伤害，并在末端引爆；同一敌人每次施法至多承受一次路径伤害与一次末端伤害，命中施加本人的印记。五级起记录半径由三格增至四格，圆环显示范围。','Record up to eight enemies with twelve nodes each, including one node for stationary enemies. After four seconds or a free spellbook/weapon recast, particles sweep the full paths and burst at the ends. Each enemy can receive one path hit and one endpoint hit per cast; hits apply your mark. The circle shows the three-block radius, increased to four at level 5.'),
 'ui.alex_caves_spellsbook.toxic.new.decay_transposition':('留下空壳，十二秒内免费重施返回，原版顶部条显示倒计时。','Leave a husk and recast free within twelve seconds; the native top bar tracks the window.'),
 'spell.alex_caves_spellsbook.decay_transposition.guide':('留下十二秒辐射空壳，吸引正在追击你的近处敌人。使用原版顶部重施倒计时；法术书或武器可免费重施返回。只有二十四格内无遮挡且落点无碰撞、有地面、无液体时才换位，并在离开处释放脉冲。空壳被击破时原地爆散；到期直接消失，不会自动传送。直接使用卷轴也显示倒计时，可切换已装备的同名法术重施。','Leave a twelve-second husk drawing nearby pursuers. The native top recast bar tracks the window. Recast free from a spellbook or weapon to return within 24 blocks, requiring line of sight, free space, floor and no liquid. Your departure releases a pulse. Broken husks burst in place; timeout never teleports you. Direct scroll casts also show the bar and can be recalled using the same equipped spell.'),
 'spell.alex_caves_spellsbook.critical_matrix.guide':('放置七点五秒反应阵列，九宫格悬浮在地面上方三点二格，圆形边界表示八格收集范围。使用辐射法术命中圆内敌人，按照真实命中来源的方向填充一格，同一次施法至多贡献一格。横排触发扫线，竖列触发三柱喷发与碎片，对角线触发交叉射线。未完成时免费重施或到期回收为弱脉冲。','Deploy a reactor for 7.5 seconds, with its enlarged grid 3.2 blocks above the ground and a circle showing the eight-block collection radius. Radiation hits inside fill cells by incoming direction, at most once per cast. Rows sweep, columns erupt, diagonals cross. Recast free or wait to release an incomplete grid as a weak pulse.'),
 'ui.alex_caves_spellsbook.toxic.detail.isotope':('每名目标每次伤害 %s；共六次；范围半径 %s 格','Per target: %s damage per pulse, six pulses; radius %s blocks'),
 'ui.alex_caves_spellsbook.toxic.detail.film':('每名目标首次曝光伤害 %s；后续保留 70%%；半径 %s 格','First shot per target: %s damage; later shots retain 70%%; radius %s blocks'),
 'ui.alex_caves_spellsbook.toxic.detail.cloud':('记录半径 %s 格；路径伤害 %s；末端爆发 %s','Radius: %s blocks; path damage: %s; endpoint burst: %s'),
 'ui.alex_caves_spellsbook.toxic.detail.decoy':('脉冲伤害 %s；%s 秒内免费重施','Pulse damage: %s; free recast within %s seconds'),
 'ui.alex_caves_spellsbook.radiation.require_area_targets':('瞄准区域半径五格内没有可见敌人。','No visible enemies within five blocks of the aimed area.')
}
for i,lang in enumerate(('zh_cn','en_us')):
    path=ROOT/f'src/main/resources/assets/alex_caves_spellsbook/lang/{lang}.json'
    data=json.loads(path.read_text(encoding='utf-8'))
    data.update({key:value[i] for key,value in rows.items()})
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Updated area, tracking, recast and Legendary descriptions in both languages.')
