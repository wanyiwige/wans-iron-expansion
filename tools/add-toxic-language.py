"""Append the new school translations while retaining existing hand formatting and entries."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'src/main/resources/assets/alex_caves_spellsbook/lang'
zh={
 'school.alex_caves_spellsbook.toxic':'辐射',
 'attribute.alex_caves_spellsbook.toxic_spell_power':'辐射法术强度',
 'attribute.alex_caves_spellsbook.toxic_magic_resist':'辐射魔法抗性',
 'itemGroup.alex_caves_spellsbook.toxic_spells':'辐射学派法术',
 'item.alex_caves_spellsbook.toxic_waste_prop':'废料桶法术模型',
 'item.alex_caves_spellsbook.toxic_reactor_prop':'反应堆屏障模型',
 'death.attack.toxic_magic':'%1$s 被辐射能量吞噬了',
 'death.attack.toxic_magic.player':'%1$s 被 %2$s 的辐射能量吞噬了',
 'ui.alex_caves_spellsbook.toxic.gamma':'每次脉冲伤害：%s（共3次）',
 'ui.alex_caves_spellsbook.toxic.range':'射程：%s格',
 'ui.alex_caves_spellsbook.toxic.drum.impact':'直击伤害：%s',
 'ui.alex_caves_spellsbook.toxic.drum.field':'区域每秒伤害：%s · 半径3格 · 持续6秒',
 'ui.alex_caves_spellsbook.toxic.cleanse':'清除普通辐照；不清除特殊高阶辐照',
 'ui.alex_caves_spellsbook.toxic.shield':'防护总量：%s · 持续%s秒',
 'ui.alex_caves_spellsbook.toxic.lance':'每次贯穿伤害：%s（最多4次）',
 'ui.alex_caves_spellsbook.toxic.fission':'无印记伤害：%s · 四层印记伤害：%s',
 'ui.alex_caves_spellsbook.toxic.ward':'每次充能释放伤害：%s（最多3次充能）',
}
en={
 'school.alex_caves_spellsbook.toxic':'Radiation',
 'attribute.alex_caves_spellsbook.toxic_spell_power':'Radiation Spell Power',
 'attribute.alex_caves_spellsbook.toxic_magic_resist':'Radiation Magic Resistance',
 'itemGroup.alex_caves_spellsbook.toxic_spells':'Radiation School Spells',
 'item.alex_caves_spellsbook.toxic_waste_prop':'Waste Drum Spell Model',
 'item.alex_caves_spellsbook.toxic_reactor_prop':'Reactor Ward Model',
 'death.attack.toxic_magic':'%1$s was consumed by radiation energy',
 'death.attack.toxic_magic.player':"%1$s was consumed by %2$s's radiation energy",
 'ui.alex_caves_spellsbook.toxic.gamma':'Damage per pulse: %s (3 pulses)',
 'ui.alex_caves_spellsbook.toxic.range':'Range: %s blocks',
 'ui.alex_caves_spellsbook.toxic.drum.impact':'Direct hit damage: %s',
 'ui.alex_caves_spellsbook.toxic.drum.field':'Field damage per second: %s · Radius 3 · Duration 6s',
 'ui.alex_caves_spellsbook.toxic.cleanse':'Removes ordinary irradiation; preserves special higher amplifiers',
 'ui.alex_caves_spellsbook.toxic.shield':'Total protection: %s · Duration: %ss',
 'ui.alex_caves_spellsbook.toxic.lance':'Damage per beam pulse: %s (up to 4)',
 'ui.alex_caves_spellsbook.toxic.fission':'Unmarked damage: %s · Four-mark damage: %s',
 'ui.alex_caves_spellsbook.toxic.ward':'Release damage per charge: %s (up to 3 charges)',
}
spells=[
 ('gamma_jet','伽马喷流','Gamma Jet','短暂蓄力后，向前八格释放三次逐渐展开的辐射喷流。命中施加四秒普通辐照；同一次施放对每个目标只增加一层裂变印记。','Release three widening radiation pulses up to eight blocks ahead. Hits apply four seconds of ordinary irradiation and at most one fission mark per target per cast.'),
 ('waste_drum_cast','废料桶投掷','Waste Drum Cast','投出旋转废料桶，直击敌人后向地面泼洒废液。半径三格的区域每秒伤害一次，共六次；命中施加普通辐照和裂变印记。同一施法者只能保留一个区域，不放置真实酸液方块。','Throw a rotating waste drum. Impact spills a radius-three field with six once-per-second hits, irradiation and fission marks. Each caster can maintain one field; no acid blocks are placed.'),
 ('raycat_purification','射线净化','Raycat Purification','净化十二格内瞄准友方的普通辐照，未选中友方时净化自身。成功清除后给予四秒有限防护；同一目标十秒内只获得一次净化防护。不清除特殊高阶辐照。','Cleanse ordinary irradiation from an aimed ally within twelve blocks, or yourself. A successful cleanse grants four seconds of finite protection, limited to once per target every ten seconds. Special higher amplifiers are preserved.'),
 ('proton_lance','质子贯穿','Proton Lance','短暂蓄力后发出可随瞄准转向的二十四格贯穿光束。方块阻挡射线，每个目标最多承受四次伤害，同一次施放只增加一层裂变印记。','Fire a steerable piercing beam up to twenty-four blocks after a short charge. Blocks stop the beam; each target takes up to four hits and one fission mark per cast.'),
 ('critical_fission','临界裂变','Critical Fission','对二十格内视线可及的敌人引发裂变。仅消耗本人施加的印记，每层额外增加40%基础伤害，最多四层。命中已有辐照的目标时产生半径三格的次级脉冲，不连锁引爆或破坏地形。','Trigger fission on a visible enemy within twenty blocks. Consume only your own marks for 40% extra base damage per mark, up to four. An irradiated primary target releases a secondary radius-three pulse without chain detonations or terrain damage.'),
 ('reactor_ward','反应堆屏障','Reactor Ward','展开六秒有限防护。期间新施加裂变印记可积累充能，最多三次；正常到期时释放一次半径三格的脉冲。护盾被提前击破时只消散；重复施放会替换旧屏障。','Create six seconds of finite protection. Applying new fission marks builds up to three charges, released as a radius-three pulse on normal expiry. A broken ward dissipates without a pulse; recasting replaces the previous ward.'),
]
for id,cn,eng,cnguide,enguide in spells:
    key='spell.alex_caves_spellsbook.'+id
    zh[key]=cn;zh[key+'.guide']=cnguide;en[key]=eng;en[key+'.guide']=enguide
    entity='waste_drum_spell' if id=='waste_drum_cast' else id+'_effect'
    zh['entity.alex_caves_spellsbook.'+entity]=cn;en['entity.alex_caves_spellsbook.'+entity]=eng
for lang,entries in [('zh_cn',zh),('en_us',en)]:
    path=BASE/(lang+'.json');text=path.read_text(encoding='utf-8');current=json.loads(text)
    if lang=='zh_cn':text=text.replace('持续造成自然伤害并施加中毒和腐蚀。','持续造成辐射伤害并施加中毒和腐蚀。')
    else:
        old=current['spell.alex_caves_spellsbook.corrosive_cloud.guide']
        text=text.replace(json.dumps(old,ensure_ascii=False),json.dumps(old.replace('nature damage','radiation damage').replace('Nature damage','Radiation damage'),ensure_ascii=False))
    additions={k:v for k,v in entries.items() if k not in current}
    if additions:text=text[:text.rfind('}')].rstrip()+',\n'+json.dumps(additions,ensure_ascii=False,indent=2)[2:-2]+'\n}\n'
    data=json.loads(text)
    for k,v in entries.items():assert data[k]==v,(k,data[k],v)
    path.write_text(text,encoding='utf-8')
    print(lang,'added',len(additions),'keys')
