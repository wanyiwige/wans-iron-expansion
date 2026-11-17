"""Deploy the verified ten-spell release, retaining a hash-checked rollback copy."""
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
OUT = ROOT / "outputs" / "radiation-ten-spells-20261021"
validation = json.loads((OUT / "verification.json").read_text(encoding="utf-8"))
source = Path(validation["jar"]).resolve()
mods = (ROOT.parent / "mods").resolve()
target = (mods / source.name).resolve()
expected = validation["sha256"]
previous = validation["active_sha256"]


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def modids(path: Path) -> set[str]:
    with zipfile.ZipFile(path) as jar:
        if "META-INF/mods.toml" not in jar.namelist():
            return set()
        data = tomllib.loads(jar.read("META-INF/mods.toml").decode("utf-8-sig"))
        return {mod["modId"] for mod in data.get("mods", [])}


def matching() -> list[Path]:
    found = []
    for path in mods.glob("*.jar"):
        try:
            ids = modids(path)
        except (zipfile.BadZipFile, KeyError, tomllib.TOMLDecodeError, UnicodeDecodeError):
            continue
        if "alex_caves_spellsbook" in ids:
            found.append(path.resolve())
    return found


assert source.is_relative_to(OUT.resolve()) and target.parent == mods
assert source.is_file() and target.is_file()
assert source.stat().st_size == validation["bytes"] and sha256(source) == expected
assert validation["jar_crc"] == "pass" and validation["spell_count"] == 10
with zipfile.ZipFile(source) as jar:
    assert jar.testzip() is None
assert "alex_caves_spellsbook" in modids(source)
assert matching() == [target], "Unexpected same-mod-ID JAR; refusing deployment"
assert sha256(target) == previous, "Active JAR changed since release verification"

stamp = datetime.now(timezone(timedelta(hours=8))).strftime("%Y%m%d-%H%M%S")
backup_dir = ROOT / "deployment-backups" / f"radiation-ten-spells-{stamp}"
backup_dir.mkdir(parents=True, exist_ok=False)
backup = backup_dir / target.name
shutil.copy2(target, backup)
assert sha256(backup) == previous, "Backup hash mismatch"

stage = mods / (target.name + ".radiation-staged")
assert not stage.exists(), "Staging file already exists"
shutil.copy2(source, stage)
assert sha256(stage) == expected, "Staged JAR hash mismatch"
assert sha256(target) == previous, "Active JAR changed during staging"
os.replace(stage, target)
assert sha256(target) == expected and matching() == [target]

receipt = {
    "time": datetime.now(timezone(timedelta(hours=8))).isoformat(),
    "source": str(source),
    "deployed": str(target),
    "sha256": expected,
    "previous_sha256": previous,
    "rollback_backup": str(backup),
    "backup_sha256": sha256(backup),
    "backup_verified": True,
    "one_active_same_mod_id_jar": True,
    "game_restarted": False,
    "gameplay_verified": False,
}
for path in (OUT / "deployment.json", backup_dir / "deployment.json"):
    path.write_text(json.dumps(receipt, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(receipt, ensure_ascii=False, indent=2))
