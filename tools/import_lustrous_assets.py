"""Import the pinned Coralite artwork and the existing local Blockbench caster."""
import hashlib
import json
import shutil
import subprocess
from pathlib import Path

PROJECT = Path(__file__).resolve().parents[1]
SOURCE = PROJECT.parent / '.codex_staging/land-of-the-lustrous-model'
ASSETS = PROJECT / 'src/main/resources/assets/alex_caves_spellsbook'
COMMIT = '21001e126002f5808b90a31cc7e89bd51536f362'
UPSTREAM = f'https://raw.githubusercontent.com/CoraIite/Coralite-Mod/{COMMIT}'
ICON_PATH = 'Assets/Items/LandOfTheLustrousSeries/LandOfTheLustrous.png'

def fetch(relative):
    # Windows curl uses the working system TLS/network configuration.
    return subprocess.run(['curl.exe','--fail','--silent','--show-error','--location',
                           '--connect-timeout','10','--max-time','45',
                           f'{UPSTREAM}/{relative}'],check=True,capture_output=True).stdout

icon = fetch(ICON_PATH)
assert icon.startswith(b'\x89PNG\r\n\x1a\n'), 'Expected PNG icon'
icon_dest = ASSETS / 'textures/item/land_of_the_lustrous.png'
icon_dest.parent.mkdir(parents=True, exist_ok=True)
icon_dest.write_bytes(icon)
license_text = fetch('LICENSE')
license_dest = PROJECT / 'src/main/resources/licenses/Coralite-GPL-3.0.txt'
license_dest.parent.mkdir(parents=True, exist_ok=True)
license_dest.write_bytes(license_text)

caster = SOURCE / 'reference/caster_frame_00.png'
caster_dest = ASSETS / 'textures/curios/land_of_the_lustrous_caster.png'
caster_dest.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile(caster, caster_dest)

geo = json.loads((SOURCE/'land_of_the_lustrous_caster_v01.geo.json').read_text(encoding='utf8'))
geometry = geo['minecraft:geometry'][0]
geometry['description']['identifier'] = 'geometry.land_of_the_lustrous_caster'
# Bedrock pivots/origins are absolute. Shift every one by the same amount so the
# existing caster_root pivot [0,12,0] becomes the shoulder attachment origin.
for bone in geometry['bones']:
    if 'pivot' in bone:
        bone['pivot'][1] -= 12
    for cube in bone.get('cubes', []):
        cube['origin'][1] -= 12
        if 'pivot' in cube:
            cube['pivot'][1] -= 12
geometry['description']['visible_bounds_offset'] = [0, 0, 0]
geo_dest = ASSETS/'geo/land_of_the_lustrous_caster.geo.json'
geo_dest.parent.mkdir(parents=True, exist_ok=True)
geo_dest.write_text(json.dumps(geo,ensure_ascii=False,indent=2)+'\n',encoding='utf8')

for locale, name, tooltip in [
    ('zh_cn', '宝石之国', '装备于法术书槽后，施法器悬浮在右肩外侧。'),
    ('en_us', 'Land of the Lustrous', 'The caster floats beside your right shoulder while equipped in the spellbook slot.')
]:
    file=ASSETS/'lang'/f'{locale}.json'
    lang=json.loads(file.read_text(encoding='utf8'))
    lang['item.alex_caves_spellsbook.land_of_the_lustrous']=name
    lang['tooltip.alex_caves_spellsbook.land_of_the_lustrous']=tooltip
    file.write_text(json.dumps(lang,ensure_ascii=False,indent=2)+'\n',encoding='utf8')

manifest={
    'upstream':UPSTREAM,'commit':COMMIT,'original_item_icon':ICON_PATH,
    'icon_sha256':hashlib.sha256(icon).hexdigest(),
    'caster_frame_sha256':hashlib.sha256(caster_dest.read_bytes()).hexdigest(),
    'source_geo':str(SOURCE/'land_of_the_lustrous_caster_v01.geo.json'),
    'geometry_center_shift':[0,-12,0],
    'cube_count':sum(len(b.get('cubes',[])) for b in geometry['bones'])
}
audit=PROJECT/'outputs/land-of-the-lustrous-source'
audit.mkdir(parents=True,exist_ok=True)
(audit/'asset-provenance.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps(manifest,ensure_ascii=False,indent=2))
