"""Deploy the two hash-pinned startup fixes with verified rollback copies."""
from __future__ import annotations

from datetime import datetime, timedelta, timezone
from pathlib import Path
import hashlib
import json
import os
import shutil
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "outputs" / "crash-fix-20261021"
MODS = (ROOT.parent / "mods").resolve()
OURS = json.loads((OUT / "verification.json").read_text(encoding="utf-8"))
OTHER = json.loads((OUT / "enigmatic-patch-verification.json").read_text(encoding="utf-8"))
NEW_OURS = Path(OURS["jar"]).resolve()
OLD_OURS = Path(OURS["active_jar"]).resolve()
NEW_ENIGMATIC = Path(OTHER["release"]).resolve()
OLD_ENIGMATIC = Path(OTHER["source"]).resolve()
OURS_TARGET = MODS / NEW_OURS.name
EXPECTED = {
    NEW_OURS: OURS["sha256"],
    OLD_OURS: OURS["active_sha256"],
    NEW_ENIGMATIC: OTHER["release_sha256"],
    OLD_ENIGMATIC: OTHER["source_sha256"],
}


def sha(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def ids(path: Path) -> set[str]:
    with zipfile.ZipFile(path) as jar:
        data = tomllib.loads(jar.read("META-INF/mods.toml").decode("utf-8-sig"))
    return {mod["modId"] for mod in data["mods"]}


def matching(modid: str) -> list[Path]:
    found = []
    for path in MODS.glob("*.jar"):
        try:
            if modid in ids(path):
                found.append(path.resolve())
        except (zipfile.BadZipFile, KeyError, tomllib.TOMLDecodeError, UnicodeDecodeError):
            continue
    return found


assert NEW_OURS.parent == OUT.resolve() and NEW_ENIGMATIC.parent == OUT.resolve()
assert OLD_OURS.parent == MODS and OLD_ENIGMATIC.parent == MODS and OURS_TARGET.parent == MODS
assert not OURS_TARGET.exists()
for path, digest in EXPECTED.items():
    assert path.is_file() and sha(path) == digest, f"Changed artifact: {path}"
assert OURS["jar_crc"] == "pass" and OTHER["jar_crc"] == "pass"
assert "alex_caves_spellsbook" in ids(NEW_OURS) and "enigmaticdelicacy" in ids(NEW_ENIGMATIC)
assert matching("alex_caves_spellsbook") == [OLD_OURS]
assert matching("enigmaticdelicacy") == [OLD_ENIGMATIC]

with zipfile.ZipFile(OLD_ENIGMATIC) as before, zipfile.ZipFile(NEW_ENIGMATIC) as after:
    assert before.namelist() == after.namelist()
    changed = [name for name in before.namelist() if before.read(name) != after.read(name)]
    assert changed == [OTHER["class"]], changed
    assert after.testzip() is None

stamp = datetime.now(timezone(timedelta(hours=8))).strftime("%Y%m%d-%H%M%S")
backup_dir = ROOT / "deployment-backups" / f"crash-fix-{stamp}"
backup_dir.mkdir(parents=True, exist_ok=False)
backup_ours = backup_dir / OLD_OURS.name
backup_enigmatic = backup_dir / OLD_ENIGMATIC.name
for source, backup in ((OLD_OURS, backup_ours), (OLD_ENIGMATIC, backup_enigmatic)):
    shutil.copy2(source, backup)
    assert sha(backup) == EXPECTED[source]

stage_ours = MODS / (OURS_TARGET.name + ".crash-fix-stage")
stage_enigmatic = MODS / (OLD_ENIGMATIC.name + ".crash-fix-stage")
assert not stage_ours.exists() and not stage_enigmatic.exists()
shutil.copy2(NEW_OURS, stage_ours)
shutil.copy2(NEW_ENIGMATIC, stage_enigmatic)
assert sha(stage_ours) == EXPECTED[NEW_OURS] and sha(stage_enigmatic) == EXPECTED[NEW_ENIGMATIC]
assert sha(OLD_OURS) == EXPECTED[OLD_OURS] and sha(OLD_ENIGMATIC) == EXPECTED[OLD_ENIGMATIC]

parked_ours = backup_dir / (OLD_OURS.name + ".parked")
try:
    os.replace(OLD_OURS, parked_ours)
    os.replace(stage_ours, OURS_TARGET)
    os.replace(stage_enigmatic, OLD_ENIGMATIC)
except Exception:
    if OURS_TARGET.exists() and sha(OURS_TARGET) == EXPECTED[NEW_OURS]:
        OURS_TARGET.unlink()
    if parked_ours.exists() and not OLD_OURS.exists():
        os.replace(parked_ours, OLD_OURS)
    if OLD_ENIGMATIC.exists() and sha(OLD_ENIGMATIC) == EXPECTED[NEW_ENIGMATIC]:
        os.replace(backup_enigmatic, OLD_ENIGMATIC)
    raise

assert sha(OURS_TARGET) == EXPECTED[NEW_OURS]
assert sha(OLD_ENIGMATIC) == EXPECTED[NEW_ENIGMATIC]
assert matching("alex_caves_spellsbook") == [OURS_TARGET]
assert matching("enigmaticdelicacy") == [OLD_ENIGMATIC]

receipt = {
    "time": datetime.now(timezone(timedelta(hours=8))).isoformat(),
    "our_mod": {"active": str(OURS_TARGET), "sha256": sha(OURS_TARGET), "backup": str(backup_ours), "backup_sha256": sha(backup_ours)},
    "enigmatic_delicacy": {"active": str(OLD_ENIGMATIC), "sha256": sha(OLD_ENIGMATIC), "backup": str(backup_enigmatic), "backup_sha256": sha(backup_enigmatic), "changed_entry": changed[0]},
    "one_active_jar_per_mod_id": True,
    "game_restarted": False,
    "gameplay_verified": False,
}
for path in (OUT / "deployment.json", backup_dir / "deployment.json"):
    path.write_text(json.dumps(receipt, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(receipt, ensure_ascii=False, indent=2))
