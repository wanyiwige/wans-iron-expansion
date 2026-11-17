"""Check the production archive before deploying it."""
import hashlib
import json
import tomllib
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
release = root / 'build/libs/wans_iron_expansion-1.0.31.jar'
old = root.parent / 'mods/wans_iron_expansion-1.0.30.jar'
if not old.exists():
    deployment = json.loads((root / 'outputs/land-of-the-lustrous-source/deployment-1.0.31.json').read_text(encoding='utf-8-sig'))
    old = Path(deployment['backup'])
prefix = 'cn/wanyiwige/alexcavesspellsbook/'
required = [prefix + entry for entry in [
    'spells/LandOfTheLustrousSpell.class', 'entities/LustrousGemProjectile.class',
    'client/LustrousGemModels.class', 'client/render/LustrousGemRenderer.class',
    'client/render/LandOfTheLustrousCurioRenderer.class', 'item/LandOfTheLustrousItem.class']]
required += ['assets/alex_caves_spellsbook/' + entry for entry in [
    'models/projectile/lustrous_gems.json', 'textures/spell/land_of_the_lustrous.png',
    'models/item/land_of_the_lustrous.json', 'geo/land_of_the_lustrous_caster.geo.json',
    'textures/curios/land_of_the_lustrous_caster.png']]
with zipfile.ZipFile(release) as archive, zipfile.ZipFile(old) as previous:
    assert archive.testzip() is None
    names = set(archive.namelist())
    assert all(entry in names for entry in required)
    missing = sorted(set(previous.namelist()) - names)
    assert not missing, missing
    metadata = tomllib.loads(archive.read('META-INF/mods.toml').decode('utf8'))
    mods = {mod['modId']: mod['version'] for mod in metadata['mods']}
    assert mods['alex_caves_spellsbook'] == '1.0.31' and 'roacw' in mods
    vendor_classes = list((root / 'vendor/roacw-1.1.1-unpacked/net/kamaarion/roacw').rglob('*.class'))
    assert len(vendor_classes) == 98
    for path in vendor_classes:
        entry = path.relative_to(root / 'vendor/roacw-1.1.1-unpacked').as_posix()
        assert archive.read(entry) == path.read_bytes(), entry
    gems = json.loads(archive.read('assets/alex_caves_spellsbook/models/projectile/lustrous_gems.json'))
    assert len(gems) == 9
    for gem in gems:
        required.append('assets/alex_caves_spellsbook/textures/entity/lustrous_gems/' + gem['file'] + '.png')
    for entry in required:
        if entry.startswith('assets/'):
            assert archive.read(entry) == (root / 'src/main/resources' / entry).read_bytes(), entry
result = {'checks': 'passed', 'release': str(release), 'size_bytes': release.stat().st_size,
          'sha256': hashlib.sha256(release.read_bytes()).hexdigest(), 'mod_versions': mods,
          'required_entries': required, 'old_entries_missing': missing,
          'roacw_vendor_classes_identical': 98, 'gameplay_verified': False}
(root / 'outputs/land-of-the-lustrous-source/release-validation.json').write_text(
    json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
print(json.dumps(result, ensure_ascii=False, indent=2))
