from pathlib import Path
import json,shutil

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'outputs/radiation-rework-20261022'
OUT.mkdir(parents=True,exist_ok=True)
rows={
 'isotope_brand':('给瞄准目标植入六秒烙印，头顶显示辐射标志。','Brand the aimed enemy for six seconds; a radiation sigil appears above its head.',
 '对二十四格内瞄准的敌人植入烙印，共造成六次衰变伤害。首次成功命中增加一层本人的裂变印记，后续衰变不重复叠层。标志悬浮在目标头顶，下方亮点显示裂变层数。重复施放替换本人的旧烙印。',
 'Brand an aimed enemy within 24 blocks for six decay ticks. Only the first successful hit grants one owned fission mark. A floating head sigil and pips show the mark. Recasting replaces your previous brand.'),
 'neutron_rebound':('中子命中敌人消散；三级与六级解锁反弹。','The neutron dissipates on an enemy; bounces unlock at levels 3 and 6.',
 '发射中子团，沿飞行轨迹检查最近的敌人与方块。三级可反弹一次，六级可反弹两次，每次反弹保留百分之八十五的伤害。成功命中增加一层本人的裂变印记；反弹后的命中另施加三秒辐照。撞击面会出现短促方形闪斑。',
 'Fire a neutron with swept nearest-hit collision. Levels 3 and 6 allow one and two block bounces; each retains 85% damage. A successful enemy hit adds one owned mark; bounced hits also inflict three seconds of radiation.'),
 'moderation_field':('在地面展开慢化区；本人标记目标额外减速。','Deploy a ground field; your marked enemies are slowed further.',
 '瞄准可见地面，展开半径三格的慢化区域。持续时间随等级从四秒增至六秒；四级起基础减速提升。本人的裂变印记使减速再提高一级。效果每五刻刷新，离开后迅速恢复；不会强制定身或打断首领技能。',
 'Aim at a visible floor to create a three-block slowing field for four to six seconds. Level 4 increases the base slow; your own marks increase it one further tier. Refreshes every five ticks and wears off quickly outside the field.'),
 'lead_curtain':('竖起五秒铅幕，仅拦截从正面穿过的普通投射物。','Erect a five-second curtain against ordinary frontal projectiles.',
 '铅幕固定生成在前方两格，可拦截二至五枚箭、投掷物或火球。检查投射物穿过幕面的轨迹，指示灯逐格熄灭，耗尽后收拢消散。不会保护背面、阻挡近战或吸收大型实体光束。',
 'A fixed curtain appears two blocks ahead and intercepts two to five arrows, thrown projectiles or fireballs crossing from the front. Lamps track remaining blocks. It does not cover your rear, melee attacks or boss beams.'),
 'chain_reaction':('先瞄准带有本人裂变印记的目标。','First aim at an enemy carrying your own fission mark.',
 '消耗每名目标本人的一层裂变印记，放电每五刻向四格内可见的下一名标记敌人跳跃。最多命中二至五名敌人，不会回跳；没有可连接的标记目标便结束。每一跳有独立折线、爆闪和音效。',
 'Consume one of your marks per enemy. Every five ticks the arc jumps to another visible marked enemy within four blocks, up to two to five enemies. Targets are never revisited; the chain stops when no marked target remains.'),
 'radiation_film':('记录四张残像，免费重施或三秒后依次曝光。','Record four snapshots; recast free or expose automatically after three seconds.',
 '瞄准敌人，在三秒内记录四个位置。通过法术书或武器施放时，重施可免费提前触发；卷轴直接施放则自动曝光。每张底片向目标当前的位置照射，受方块遮挡，后续射线逐次保留百分之七十伤害。目标死亡时取消余下照射。',
 'Record four positions of an aimed enemy over three seconds. Spellbook or weapon casts allow one free early recast; direct scroll use resolves automatically. Each snapshot fires toward the current target, blocked by terrain. Each later exposure deals 70% of the previous one.'),
 'cloud_chamber_trail':('移动的敌人留下完整径迹；免费重施启动追迹。','Moving enemies leave full trails; recast free to launch tracking particles.',
 '地面云室记录最多八名敌人的移动径迹，每条最多十二个节点。四秒后自动触发，也可通过法术书或武器免费重施。亮点沿各自径迹前进，在末端各引爆一次。五级起记录半径由三格增至四格。',
 'The ground chamber records up to eight enemies with twelve nodes per path. After four seconds, or a free spellbook/weapon recast, particles travel each complete path and burst once at its end. Recording radius grows from three to four blocks at level 5.'),
 'cherenkov_wake':('沿视线方向在地面滑行，随后航迹向两侧放出波纹。','Dash along your ground-facing direction; the wake then emits lateral waves.',
 '必须站在地面且不在水中。短暂滑行期间可转向，记录实际经过的路线；碰墙或离地会结束滑行并提前释放航迹。随后蓝色波纹沿路线向两侧逐段结算，每名敌人最多受到两段伤害，自交另触发一次脉冲。',
 'Requires solid ground and no water. Steer during the brief dash; the actual route is recorded. Walls or leaving the ground stop the dash and release the wake early. Lateral waves resolve along the route, with at most two hits per enemy and one extra self-crossing pulse.'),
 'decay_transposition':('留下可打碎的空壳，四秒内免费重施返回。','Leave a breakable husk and recast free within four seconds to return.',
 '留下四秒辐射空壳，吸引正在追击你的近处敌人。法术书或武器可免费重施，只有二十四格内无遮挡且落点无碰撞、有地面、无液体时才交换位置，并在离开处释放脉冲。空壳被击破时在原地爆散，不会强制传送。到期直接消失。',
 'Leave a four-second husk that draws nearby pursuers. A free spellbook/weapon recast returns you only within 24 blocks, with line of sight, free space, floor support and no liquid. Your departure releases a pulse. A broken husk bursts in place without teleporting you.'),
 'critical_matrix':('从不同方向命中附近敌人，填充九宫格反应阵列。','Hit nearby enemies from different directions to charge a nine-cell reactor.',
 '放置七点五秒反应阵列。使用辐射法术命中阵列八格内敌人时，根据真实命中来源的方向填充一格；同一次施法至多贡献一格，已亮格不重复消耗机会。横排触发扫线，竖列触发三柱喷发与碎片，对角线触发交叉射线。未完成时免费重施或到期回收为弱脉冲。',
 'Deploy a reactor for 7.5 seconds. Radiation hits on enemies within eight blocks fill cells by the actual incoming direction. Each cast contributes at most once; an occupied cell does not consume that cast. Rows sweep, columns erupt with fragments, diagonals fire crossed rays. Recast free or wait to release an incomplete reactor as a weak pulse.')
}
details={
 'isotope':('每次伤害：%s；共六次','Damage per tick: %s; six ticks'),
 'neutron':('直击伤害：%s；反弹次数：%s','Direct damage: %s; bounces: %s'),
 'field':('持续 %s 秒；基础减速 %s%%','Duration: %ss; base slow: %s%%'),
 'lead':('可拦截 %s 次；灯光显示剩余次数','Blocks %s projectiles; lamps show remaining charges'),
 'chain':('最多 %s 名目标；每名伤害 %s','Up to %s targets; %s damage each'),
 'film':('首次曝光伤害：%s；后续逐次保留 70%%','First exposure: %s damage; later shots retain 70%%'),
 'cloud':('记录半径 %s 格；末端爆发伤害 %s','Recording radius: %s blocks; endpoint burst: %s damage'),
 'wake':('每段波纹伤害：%s；同一目标最多两段','Damage per wave: %s; at most two waves per enemy'),
 'decoy':('脉冲伤害：%s；重施不重复扣蓝','Pulse damage: %s; recasting costs no additional mana'),
 'matrix':('横排 %s；竖列基础 %s；交叉 %s','Row: %s; column base: %s; diagonal cross: %s')
}
extra={
 'unsafe_swap':('落点不安全或路径被遮挡，未进行换位。','Unsafe destination or blocked line of sight; transposition cancelled.'),
 'require_ground':('请瞄准近处可见地面，并站在干燥地面上。','Aim at a nearby visible floor and stand on dry ground.'),
 'require_target':('请瞄准二十四格内无遮挡的敌人。','Aim at a visible enemy within 24 blocks.'),
 'require_marked_target':('链式反应需要目标带有你施加的裂变印记。','Chain Reaction requires one of your own fission marks on the target.')
}
for index,lang in enumerate(('zh_cn','en_us')):
    path=ROOT/f'src/main/resources/assets/alex_caves_spellsbook/lang/{lang}.json'
    backup=OUT/f'{lang}-before.json'
    if not backup.exists():shutil.copy2(path,backup)
    data=json.loads(path.read_text(encoding='utf-8'))
    for spell,row in rows.items():
        data[f'ui.alex_caves_spellsbook.toxic.new.{spell}']=row[index]
        data[f'spell.alex_caves_spellsbook.{spell}.guide']=row[index+2]
    for key,row in details.items():data[f'ui.alex_caves_spellsbook.toxic.detail.{key}']=row[index]
    for key,row in extra.items():data[f'ui.alex_caves_spellsbook.radiation.{key}']=row[index]
    data['effect.alex_caves_spellsbook.radiation_mark']=('裂变印记','Fission Mark')[index]
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Updated only the ten radiation spell guides, their detailed values, and their casting feedback in both languages.')
