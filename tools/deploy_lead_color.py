"""Verify the narrow Lead Curtain color release and retain a hashed rollback copy."""
from pathlib import Path
from datetime import datetime
import hashlib, json, os, shutil, tomllib, zipfile

ROOT = Path(__file__).resolve().parents[1]
MODS = ROOT.parent / 'mods'
OUT = ROOT / 'outputs/lead-curtain-color-20261022'
VERSION = '1.2.4-radiation-rework.3'
OLD_HASH = '1a3f0289f6024e7cdc9335c7d28b783b839bca0baad0d10e28ac0b5955f7871c'
old = MODS / 'wans_iron_expansion-1.2.4-radiation-rework.2.jar'
built = ROOT / f'build/libs/wans_iron_expansion-{VERSION}.jar'

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def matching():
    result = []
    for path in MODS.glob('*.jar'):
        try:
            with zipfile.ZipFile(path) as archive:
                mods = tomllib.loads(archive.read('META-INF/mods.toml').decode('utf-8-sig'))['mods']
        except (zipfile.BadZipFile, KeyError, tomllib.TOMLDecodeError, UnicodeDecodeError):
            continue
        if any(mod['modId'] == 'alex_caves_spellsbook' for mod in mods):
            result.append(path)
    return result

assert matching() == [old] and sha(old) == OLD_HASH
allowed = {'META-INF/MANIFEST.MF', 'META-INF/mods.toml'} | {
    'cn/wanyiwige/alexcavesspellsbook/toxic/' + name + '.class'
    for name in ('client/RadiationEffectRenderer',)
}
with zipfile.ZipFile(old) as before, zipfile.ZipFile(built) as after:
    assert after.testzip() is None
    assert set(before.namelist()) == set(after.namelist())
    changed = {name for name in after.namelist() if before.read(name) != after.read(name)}
    assert changed == allowed, changed
    metadata = tomllib.loads(after.read('META-INF/mods.toml').decode('utf-8-sig'))
    assert [(m['modId'], m['version']) for m in metadata['mods']] == [('alex_caves_spellsbook', VERSION)]

OUT.mkdir(parents=True, exist_ok=True)
source = OUT / built.name
assert not source.exists()
shutil.copy2(built, source)
new_hash = sha(source)
assert new_hash == sha(built)
backup_dir = ROOT / 'deployment-backups' / ('lead-curtain-color-' + datetime.now().strftime('%Y%m%d-%H%M%S'))
backup_dir.mkdir(parents=True, exist_ok=False)
backup = backup_dir / old.name
shutil.copy2(old, backup)
assert sha(backup) == OLD_HASH
target = MODS / source.name
stage = MODS / (source.name + '.stage')
assert not target.exists() and not stage.exists()
shutil.copy2(source, stage)
assert sha(stage) == new_hash and sha(old) == OLD_HASH
parked = backup_dir / (old.name + '.parked')
try:
    os.replace(old, parked)
    os.replace(stage, target)
    assert sha(target) == new_hash and matching() == [target]
except Exception:
    if target.exists() and sha(target) == new_hash:
        target.unlink()
    if parked.exists() and not old.exists():
        os.replace(parked, old)
    raise
receipt = dict(version=VERSION, active=str(target), sha256=new_hash,
               rollback_backup=str(backup), previous_sha256=OLD_HASH, backup_verified=True,
               changed_entries=sorted(changed), assets_unchanged=True, jar_crc='pass',
               build_reobfuscated=True, curtain_alpha_before=100, curtain_alpha_after=210,
               curtain_tint='A6FF35', impact_tint='C0FF40', impact_lifetime_ticks=8,
               one_active_same_mod_id_jar=True, game_started=False, gpu_render_verified=False)
for dest in (OUT / 'deployment.json', backup_dir / 'deployment.json'):
    dest.write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps(receipt, ensure_ascii=False, indent=2))
