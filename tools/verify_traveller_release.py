"""Check the packaged Traveller migration and preservation of the active release."""
from pathlib import Path
import hashlib
import json
import tomllib
import zipfile

project = Path(__file__).resolve().parents[1]
instance = project.parent
out = project / "outputs/traveller-migration-20261008"
release_path = project / "build/libs/wans_iron_expansion-1.1.7.jar"
previous_path = instance / "mods/wans_iron_expansion-1.1.6.jar"
if not previous_path.exists():
    deployment = json.loads((out / "deployment-1.1.7.json").read_text(encoding="utf-8-sig"))
    previous_path = Path(deployment["backup"])
audit = json.loads((out / "migration-validation.json").read_text(encoding="utf-8-sig"))
expected = {}
for row in audit["files"]:
    namespace, spell = row["spell"].split(":", 1)
    name = f"data/{namespace}/irons_spellbooks_spell_config/{spell}.json"
    expected[name] = (project / "src/main/resources" / name).read_bytes()
assert len(expected) == 16
for spell in ("teleport", "recall", "portal"):
    name = f"data/irons_spellbooks/irons_spellbooks_spell_config/{spell}.json"
    expected[name] = (project / "src/main/resources" / name).read_bytes()
for name in ("assets/kubejs/models/item/scroll_traveller.json", "assets/kubejs/textures/item/scroll_traveller.png"):
    expected[name] = (project / "src/main/resources" / name).read_bytes()

with zipfile.ZipFile(release_path) as release, zipfile.ZipFile(previous_path) as previous:
    names = release.namelist()
    assert len(names) == len(set(names)), "Duplicate JAR entries"
    assert release.testzip() is None, "JAR CRC validation failed"
    metadata = tomllib.loads(release.read("META-INF/mods.toml").decode("utf-8"))
    mods = {mod["modId"]: mod for mod in metadata["mods"]}
    assert mods["alex_caves_spellsbook"]["version"] == "1.1.7"
    assert "roacw" in mods
    for name, data in expected.items():
        assert release.read(name) == data, f"Packaged resource differs: {name}"
    for name in expected:
        if "/irons_spellbooks_spell_config/" in name:
            assert json.loads(release.read(name))["irons_spellbooks:school"] == "kubejs:traveller"
    assert "data/iss_csw/irons_spellbooks_spell_config/name_less_teleport.json" not in names
    model = json.loads(release.read("assets/kubejs/models/item/scroll_traveller.json"))
    assert model["textures"]["layer0"] == "kubejs:item/scroll_traveller"
    live_texture = instance / "kubejs/assets/kubejs/textures/item/scroll_traveller.png"
    assert release.read("assets/kubejs/textures/item/scroll_traveller.png") == live_texture.read_bytes()
    vendor = project / "vendor/roacw-1.1.1-unpacked"
    vendor_classes = list((vendor / "net/kamaarion/roacw").rglob("*.class"))
    assert len(vendor_classes) == 98
    for path in vendor_classes:
        assert release.read(path.relative_to(vendor).as_posix()) == path.read_bytes(), path
    checked_json = 0
    for name in names:
        if name.endswith(".json"):
            json.loads(release.read(name).decode("utf-8-sig"))
            checked_json += 1
    old_names = {n for n in previous.namelist() if not n.endswith("/")}
    new_names = {n for n in names if not n.endswith("/")}
    removed = sorted(old_names - new_names)
    added = sorted(new_names - old_names)
    changed = sorted(n for n in old_names & new_names if previous.read(n) != release.read(n))
    allowed = set(expected) | {"META-INF/mods.toml", "META-INF/MANIFEST.MF"}
    assert not removed, f"Existing files removed: {removed}"
    assert set(added + changed) <= allowed, f"Unexpected release changes: {set(added + changed) - allowed}"
    classes = [n for n in old_names if n.endswith(".class")]
    assert all(release.read(n) == previous.read(n) for n in classes)

report = {
    "artifact": str(release_path),
    "sha256": hashlib.sha256(release_path.read_bytes()).hexdigest().upper(),
    "bytes": release_path.stat().st_size,
    "new_school_overrides": 16,
    "total_school_overrides_checked": 19,
    "json_files_checked": checked_json,
    "preserved_classes": len(classes),
    "roacw_vendor_classes_preserved": 98,
    "added_entries": added,
    "changed_entries": changed,
    "removed_entries": removed,
    "deployed": (instance / "mods/wans_iron_expansion-1.1.7.jar").exists()
    and (instance / "mods/wans_iron_expansion-1.1.7.jar").read_bytes() == release_path.read_bytes(),
    "gameplay_verified": False,
}
(out / "release-validation-1.1.7.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(report, ensure_ascii=False, indent=2))
