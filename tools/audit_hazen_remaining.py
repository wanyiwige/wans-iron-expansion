"""Audit content gaps without modifying gameplay sources or the installed mods."""
from pathlib import Path
from collections import Counter
import hashlib
import json
import re
import zipfile
from audit_hazen_recreation import clean, balanced

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'development-records/hazen-recreation'
SOURCE = OUT / 'reference-1.4.0.14/src/main/java/net/hazen/hazennstuff'
RES = ROOT / 'src/main/resources'
LEGACY = ROOT.parent / 'mod-backports-1.20.1/reference-decompiled/hazennstuff_legacy/net/hazen/hazennstuff'


def load(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def category(cls):
    if '.Armor.' in cls: return '护甲与外观'
    if '.Weapons.' in cls or '.Staves.' in cls: return '武器与法杖'
    if '.Curios.' in cls: return '饰品与法术书'
    if '.Util.' in cls: return '工具'
    return '其他物品与材料'


def main():
    inv = load(OUT / 'original-inventory.json')
    originals = Path(inv['original_jar'])
    legacy_jar = Path(inv['legacy_jar'])
    for path, expected in [(originals, inv['original_sha256']), (legacy_jar, inv['legacy_sha256'])]:
        if hashlib.sha256(path.read_bytes()).hexdigest() != expected:
            raise SystemExit('Reference changed: ' + str(path))
    with zipfile.ZipFile(originals) as jar:
        names = set(jar.namelist())
        en = json.loads(jar.read('assets/hazennstuff/lang/en_us.json'))
        zh = json.loads(jar.read('assets/hazennstuff/lang/zh_cn.json')) if 'assets/hazennstuff/lang/zh_cn.json' in names else {}
        recipes = sorted(n for n in names if re.match(r'data/[^/]+/recipe/.+\.json$', n))
        data_counts = Counter(n.split('/')[2] for n in names if n.startswith('data/hazennstuff/') and n.endswith('.json'))
        enchantments = sorted(n.split('/')[-1][:-5] for n in names if n.startswith('data/hazennstuff/enchantment/') and n.endswith('.json'))
    def title(kind, name):
        key = f'{kind}.hazennstuff.{name}'
        return zh.get(key, en.get(key, name))
    weapons = load(RES / 'hazen-recreated-weapons.json')
    materials = load(RES / 'hazen-materials.json')
    native_ids = {i['id'] for i in weapons + materials}
    armor_map_path = OUT / 'armor-recreation-map.json'
    armor_map = load(armor_map_path) if armor_map_path.is_file() and (RES / 'hazen-armor-sets.json').is_file() else {'original_aliases': {}, 'recipes': []}
    native_ids.update(armor_map['original_aliases'])
    item_rows = []
    for row in inv['items']:
        ident = row['id']
        state = '本次重做' if ident in native_ids else '旧版已有' if row['legacy'] else '按历史要求排除' if ident == 'skyscorcher' else '待重做'
        item_rows.append(dict(id=ident, name=title('item', ident), category=category(row['class']), status=state, source_class=row['class']))
    original_ids = {i['id'] for i in inv['items']}
    support = [i['id'] for i in materials if i['id'] not in original_ids]
    registry = clean((SOURCE / 'Spells/HnSSpellRegistries.java').read_text(encoding='utf-8'))
    # Resolve the exact imported spell, not an unrelated same-named abstract class.
    imports = {fq.rsplit('.', 1)[-1]: fq for fq in re.findall(r'import ([\w.]+);', registry)}
    native_spell_source = (ROOT / 'src/main/java/cn/wanyiwige/alexcavesspellsbook/hazen/HazenSpells.java').read_text(encoding='utf-8')
    native_array = re.search(r'for \(String name : new String\[\]\{([^}]+)\}', native_spell_source, re.S).group(1)
    new_spell_ids = set(re.findall(r'"([^"]+)"', native_array))
    secondary_path=RES/'hazen-secondary-spells.json'
    secondary=load(secondary_path) if secondary_path.is_file() else []
    new_spell_ids.update(s['id']for s in secondary)
    spells = []
    for cls in re.findall(r'registerSpell\(new (\w+)\(', registry):
        if cls in imports:
            path = SOURCE.parents[2] / (imports[cls].replace('.', '/') + '.java')
        else:
            candidates = list((SOURCE / 'Spells/Schools').rglob(cls + '.java'))
            if len(candidates) != 1: raise ValueError('Ambiguous wildcard import: ' + cls)
            path = candidates[0]
        source = clean(path.read_text(encoding='utf-8'))
        match = re.search(r'spellId\s*=.*?[;,]', source)
        if match:
            # Stop at the semicolon rather than the comma between resource arguments.
            match = re.search(r'spellId\s*=([^;]+);', source)
        ident = re.findall(r'"([a-z_]+)"', match.group(1))[-1] if match else None
        if not ident: raise ValueError('Unresolved spell: ' + cls)
        school = re.search(r'setSchoolResource\(([^)]+)\)', source)
        if not school: raise ValueError('Unresolved school: ' + cls)
        spells.append(dict(id=ident, name=title('spell', ident), school=school.group(1).split('.')[-1].removesuffix('_RESOURCE').lower(), status='本次重做' if ident in new_spell_ids else '待重做', source=str(path.relative_to(ROOT))))
    assert len(spells) == 38 and sum(s['status'] == '本次重做' for s in spells) == 7+len(secondary)
    with zipfile.ZipFile(legacy_jar) as jar:
        assert not any('/Spells/' in n and n.endswith('Spell.class') for n in jar.namelist()), 'Legacy spells require deeper comparison'
    block_source = clean((SOURCE / 'Item/Block/HnSBlocks.java').read_text(encoding='utf-8'))
    legacy_blocks = set(re.findall(r'registerBlock\(\s*"([^"]+)"', clean((LEGACY / 'Blocks/HnSBlockRegistry.java').read_text(encoding='utf-8'))))
    old_ore_aliases = {'zenalite_stone_ore': 'stone_zenalite_ore', 'zenalite_deepslate_ore': 'deepslate_zenalite_ore', 'zenalite_end_stone_ore': 'end_stone_zenalite_ore'}
    blocks = []
    survival_path=OUT/'survival-map.json'
    survival=load(survival_path) if survival_path.exists() else {}
    native_blocks=set(survival.get('ore_blocks',[])+survival.get('wood_blocks',[]))
    for ident in re.findall(r'registerBlock\(\s*"([^"]+)"', block_source):
        state = '本次原生重做' if ident in native_blocks else '旧版同 ID 已有' if ident in legacy_blocks else '旧版矿石有不同 ID 对应，待核对' if ident in old_ore_aliases else '待重做'
        blocks.append(dict(id=ident, name=title('block', ident), status=state, legacy_alias=old_ore_aliases.get(ident)))
    entity_source = clean((SOURCE / 'Registries/HnSEntityRegistry.java').read_text(encoding='utf-8'))
    mapped = {'thorn_chakram', 'shooting_star', 'spectral_axe', 'nights_edge_after_slash', 'terraprisma_projectile', 'lifesteal_knife', 'terraprisma'}
    if secondary:mapped.update(load(RES/'hazen-secondary-projectiles.json'))
    entities = []
    for match in re.finditer(r'ENTITIES\.register\(\s*"', entity_source):
        expr = balanced(entity_source, entity_source.index('(', match.start()))
        ident = re.match(r'\s*"([^"]+)"', expr).group(1)
        mob = re.search(r'MobCategory\.(\w+)', expr).group(1)
        entities.append(dict(id=ident, name=title('entity', ident), category=mob, status='本次重做基础实现' if ident in mapped else '待重做'))
    done_effects={'harmonized','chaos_state','swordmaster_set_bonus'}
    if secondary:done_effects.update({'flame_kissed','moonkissed','ichor','infested','spores','electrocuted','gluttonous_hunger'})
    effects = [dict(id=i, name=title('effect', i), status='本次重做' if i in done_effects else '待重做') for i in re.findall(r'MOB_EFFECTS.register\(\s*"([^"]+)"', clean((SOURCE / 'Registries/HnSEffects.java').read_text(encoding='utf-8')))]
    recipes_map = load(OUT / 'recipe-map.json')
    summary = {}
    for row in item_rows:
        summary.setdefault(row['category'], Counter())[row['status']] += 1
    data = dict(original_jar=str(originals), original_sha256=inv['original_sha256'], legacy_jar=str(legacy_jar), legacy_sha256=inv['legacy_sha256'],
                scope='Source registration and resource presence audit; same ID is not proof of complete native behavior; no installed JAR changed',
                explicit_item_total=len(item_rows), explicit_item_summary=summary, items=item_rows, spells=spells, blocks=blocks, entities=entities, effects=effects,
                school_support_items_outside_hazen_item_registry=support, enchantments=enchantments,
                original_recipe_count=len(recipes), recreated_recipe_count=len(recipes_map['original_recipes'])+(3 if survival else 0),
                original_recipe_paths=recipes, original_data_file_counts=data_counts,
                unresolved_recipe_dependencies=recipes_map['unresolved_libraries'])
    (OUT / 'remaining-content.json').write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    md = ['# Hazen N Stuff 重做缺项统计（2026-10-16）', '',
          '基准：用户提供的 1.4.0.14 原 JAR、当前 1.1.2 精简版、Wan’s Iron Expansion 试做源码。此次只读审计，没有替换正式 JAR。', '',
          '“旧版已有”仅指注册 ID 存在，完整属性、套装技能、外观和兼容性仍需比对。“本次重做”指已进入试做包，尚未完成客户端与联机验收。', '',
          '## 物品总表', '', '|类别|原版显式注册|旧版已有|本次重做|待重做|明确排除|', '|---|---:|---:|---:|---:|---:|']
    for cat, counts in summary.items():
        md.append(f'|{cat}|{sum(counts.values())}|{counts["旧版已有"]}|{counts["本次重做"]}|{counts["待重做"]}|{counts["按历史要求排除"]}|')
    md += ['', '482 项是物品主注册表，不包含方块工厂自动注册的物品。30 项新增材料中有 6 项宇宙／辉光／暗影符文和升级珠来自学派支持层，单列，不重复塞进这张原模组物品表。护甲数是件数，包含 GeckoLib／AzureLib 分别注册的版本，并非套数。', '',
           f'## 法术：38 个中还缺 {sum(s["status"]=="待重做"for s in spells)} 个（指源码清单，非完整实战验收）', '']
    for school in sorted({s['school'] for s in spells}):
        md += ['### ' + school, '', '|法术|原 ID|状态|', '|---|---|---|']
        md += [f'|{s["name"]}|`{s["id"]}`|{s["status"]}|' for s in spells if s['school'] == school]
        md.append('')
    md += ['## 其他系统缺项', '',
           f'- 方块共 {len(blocks)} 种：本次原生重做 {sum(b["status"]=="本次原生重做" for b in blocks)} 种；同 ID 旧版已有 1 种；3 种禅金矿存在旧版不同 ID 对应；其余 {sum(b["status"]=="待重做" for b in blocks)} 种待重做。铁魔法的智木木板只有资源而无物品注册，现已原生重做智木材料链。',
           f'- 实体共 {len(entities)} 种：本次覆盖 {sum(e["status"]!="待重做"for e in entities)} 种基础实现，其余 {sum(e["status"]=="待重做"for e in entities)} 种未重做。其中普通生物／召唤体分类 MONSTER 有 {sum(e["category"] == "MONSTER" for e in entities)} 种，只有泰拉棱镜召唤体已做，其余 15 种待做；不能全部称为 Boss。',
           f'- 状态效果共 {len(effects)} 种：本次做 {len(done_effects)} 种原模组效果，还缺 {len(effects)-len(done_effects)} 种。额外的邪咒和魂火支持实现不计入原模组 22 个效果。',
           f'- 原包附魔数据 {len(enchantments)} 份尚未原生重做，原有饰品、法术联动机制也未补齐。',
           f'- 原 JAR 配方数据共 {len(recipes)} 份，本次按原路径重建 {data["recreated_recipe_count"]} 份。旧版也有配方；不能把两者相减直接称为缺失数。配方产物、依赖与材料获取链仍需逐条审核。',
           '- 原命名空间另有 14 份世界生成、100 份掉落表、31 份战利品修改器、269 份进度数据。生存批次已接入三套矿物特征、14 条掉落修改器与 83 条配方解锁；原生物、树木、饰品及附魔依赖仍有缺项，不能宣称全内容生存闭环。详见 docs/hazen-survival-batch1.md。',
           '- 铁魔法珠宝、Curios 槽位、Create 等跨模组数据和全部联动适配尚未覆盖。',
           '- 四个新增学派与武器所需资源已注册，但不能算学派的法术、材料和生存内容已经完整。', '',
           '## 待重做物品逐项清单', '']
    for cat in summary:
        pending = [i for i in item_rows if i['category'] == cat and i['status'] == '待重做']
        if not pending: continue
        md += ['### ' + cat + f'（{len(pending)} 项）', '', '|名称|原 ID|', '|---|---|']
        md += [f'|{i["name"]}|`{i["id"]}`|' for i in pending]
        md.append('')
    for label, rows in [('方块逐项', blocks), ('实体逐项', entities), ('状态效果逐项', effects)]:
        md += ['## ' + label, '', '|名称|原 ID|状态|', '|---|---|---|']
        md += [f'|{i["name"]}|`{i["id"]}`|{i["status"]}|' for i in rows]
        md.append('')
    md += ['## 第一批护甲', '', '五套实际 20 件护甲，覆盖原版 28 个注册 ID；宇宙学者和辉光水晶的双渲染版本合并为同一件原生物品。表中按原版 ID 计数，因此护甲缺项由 269 降为 241 件。新增 21 份护甲及近战符文配方，另见 armor-recreation-map.json。', '',
           '## 验收边界', '', '用户已反馈此前武器／法术试做版验证没有太大问题；本次新护甲仍需独立客户端验收。服务端与打包结果以最新 verification.json 为准。完整 GameTest 仍受本机 loopback 故障影响。当前整合包使用 preview.4，本次源码修改尚未替换该 JAR。', '',
           '证据：`original-inventory.json`、`recipe-map.json`、`verification.json`、匹配原包的参考源码和原 JAR。可机读的逐项结果保存在同目录 `remaining-content.json`。', '']
    (OUT / 'remaining-content.md').write_text('\n'.join(md), encoding='utf-8')
    print(json.dumps(dict(items=summary, spells=dict(total=len(spells), pending=sum(s['status'] == '待重做' for s in spells)), blocks=Counter(b['status'] for b in blocks),
                          entities=dict(total=len(entities), pending=sum(e['status'] == '待重做' for e in entities)), effects=Counter(e['status'] for e in effects), original_recipes=len(recipes)), ensure_ascii=False, indent=2))


if __name__ == '__main__': main()
