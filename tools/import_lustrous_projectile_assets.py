"""Import existing gem geometry and palettes; no mod compilation or deployment."""
import json
import shutil
from pathlib import Path

project = Path(__file__).resolve().parents[1]
source = project.parent / '.codex_staging/gem-projectiles-v01'
assets = project / 'src/main/resources/assets/alex_caves_spellsbook'
gems = json.loads((source / 'geometry_spec.json').read_text(encoding='utf8'))
geometry = assets / 'models/projectile/lustrous_gems.json'
geometry.parent.mkdir(parents=True, exist_ok=True)
shutil.copy2(source / 'geometry_spec.json', geometry)
textures = assets / 'textures/entity/lustrous_gems'
textures.mkdir(parents=True, exist_ok=True)
for gem in gems:
    shutil.copy2(source / 'textures' / (gem['file'] + '.png'), textures / (gem['file'] + '.png'))
(assets / 'textures/spell').mkdir(parents=True, exist_ok=True)
spell_icon = assets / 'textures/spell/land_of_the_lustrous.png'
if not spell_icon.exists():
    shutil.copy2(assets / 'textures/item/land_of_the_lustrous.png', spell_icon)
translations = {
    'zh_cn': {
        'spell.alex_caves_spellsbook.land_of_the_lustrous': '宝石之国',
        'entity.alex_caves_spellsbook.lustrous_gem_projectile': '宝石之国射弹',
        'tooltip.alex_caves_spellsbook.land_of_the_lustrous': '12槽法术书 · 内置传说III级宝石之国 · 装备时悬浮于肩侧',
        'ui.alex_caves_spellsbook.lustrous.damage': '每次穿透伤害：%s',
        'ui.alex_caves_spellsbook.lustrous.gems': '射弹数量：%s（3波）',
        'ui.alex_caves_spellsbook.lustrous.lock_range': '锁定距离：%s格',
        'ui.alex_caves_spellsbook.lustrous.piercing': '追踪单一目标并盘旋收拢；穿透实体；无目标时直线飞行',
    },
    'en_us': {
        'spell.alex_caves_spellsbook.land_of_the_lustrous': 'Land of the Lustrous',
        'entity.alex_caves_spellsbook.lustrous_gem_projectile': 'Lustrous Gem',
        'tooltip.alex_caves_spellsbook.land_of_the_lustrous': '12 slots · Built-in Legendary III Land of the Lustrous · Floats beside your shoulder',
        'ui.alex_caves_spellsbook.lustrous.damage': 'Damage per piercing hit: %s',
        'ui.alex_caves_spellsbook.lustrous.gems': 'Projectiles: %s (3 waves)',
        'ui.alex_caves_spellsbook.lustrous.lock_range': 'Lock range: %s blocks',
        'ui.alex_caves_spellsbook.lustrous.piercing': 'Tracks one target and spirals inward; pierces entities; flies straight without a target',
    }
}
for locale, entries in translations.items():
    path = assets / 'lang' / (locale + '.json')
    data = json.loads(path.read_text(encoding='utf8'))
    data.update(entries)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
print('Imported nine existing solid gem meshes, palette textures and spell translations.')
