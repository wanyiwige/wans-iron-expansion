"""Stage a minimal set of installed Forge mods for an isolated runServer smoke test."""

import shutil
import tomllib
from collections import deque
from pathlib import Path
from zipfile import BadZipFile, ZipFile


project = Path(__file__).resolve().parents[1]
instance = project.parent
source_mods = instance / "mods"
test_mods = project / "run/mods"
roacw = project / "vendor/roacw-1.2.2.jar"
seeds = {
    "roacw", "foolish", "entomology_spell", "brutality", "fantasy_ending",
    "gtbcs_geomancy_plus", "traveloptics", "alexscaves", "citadel",
    "irons_spellbooks", "geckolib", "cataclysm_spellbooks", "caelus",
    "twilightforest", "curios",
}
builtins = {"minecraft", "forge", "javafml", "mixinextras", "java"}
on_gradle_classpath = {
    "irons_spellbooks", "alexscaves", "citadel", "curios", "geckolib",
    "alex_caves_spellsbook", "asterismarcanum",
}


def metadata(path: Path):
    try:
        with ZipFile(path) as archive:
            return tomllib.loads(archive.read("META-INF/mods.toml").decode("utf-8", errors="replace"))
    except (BadZipFile, KeyError, ValueError, UnicodeDecodeError):
        return None


files = [roacw] + [p for p in source_mods.glob("*.jar") if p != roacw]
by_id = {}
data_by_path = {}
for path in files:
    data = metadata(path)
    if data is None:
        continue
    data_by_path[path] = data
    for mod in data.get("mods", []):
        by_id.setdefault(mod["modId"], path)

queue = deque(sorted(seeds))
chosen = set()
missing = set()
while queue:
    mod_id = queue.popleft()
    if mod_id in chosen or mod_id in builtins:
        continue
    path = by_id.get(mod_id)
    if path is None:
        missing.add(mod_id)
        continue
    chosen.add(mod_id)
    for dependency in data_by_path[path].get("dependencies", {}).get(mod_id, []):
        if dependency.get("mandatory"):
            queue.append(dependency["modId"])

if missing:
    raise SystemExit("Missing installed dependencies: " + ", ".join(sorted(missing)))
if test_mods.exists() and any(test_mods.iterdir()):
    raise SystemExit(f"Test mods directory is not empty: {test_mods}")
test_mods.mkdir(parents=True, exist_ok=True)
paths = {by_id[mod_id] for mod_id in chosen if mod_id not in on_gradle_classpath}
for path in sorted(paths):
    shutil.copy2(path, test_mods / path.name)
print(f"Staged {len(paths)} JARs in {test_mods}")
print("Mod IDs:", ", ".join(sorted(chosen)))
