"""Verify the 1.1.8 release carries the approved 64px school scroll textures."""
from pathlib import Path
import hashlib
import json
import struct
import tomllib
import zipfile

project = Path(__file__).resolve().parents[1]
instance = project.parent
out = project / "outputs/scroll-clarity-release-20261009"
release_path = project / "build/libs/wans_iron_expansion-1.1.8.jar"
baseline_path = out / "baseline-wans_iron_expansion-1.1.8.jar"
previous_path = instance / "mods/wans_iron_expansion-1.1.7.jar"
if not previous_path.exists():
    deployment = json.loads((out / "deployment.json").read_text(encoding="utf-8-sig"))
    previous_path = Path(deployment["backup"])
allowed_pending_classes = {
    "cn/wanyiwige/alexcavesspellsbook/spells/SaciliteSpell",
    "cn/wanyiwige/alexcavesspellsbook/entities/SaciliteSpellProjectile",
    "cn/wanyiwige/alexcavesspellsbook/client/render/SaciliteCrystalRenderer",
}
hash_file = lambda path: hashlib.sha256(path.read_bytes()).hexdigest().upper()
with zipfile.ZipFile(release_path) as release, zipfile.ZipFile(previous_path) as previous, zipfile.ZipFile(baseline_path) as baseline:
    names = release.namelist()
    assert len(names) == len(set(names)), "Duplicate JAR entries"
    assert release.testzip() is None, "JAR CRC failure"
    metadata = tomllib.loads(release.read("META-INF/mods.toml").decode("utf-8"))
    mods = {row["modId"]: row for row in metadata["mods"]}
    assert mods["alex_caves_spellsbook"]["version"] == "1.1.8"
    assert "roacw" in mods
    textures = {}
    for school in ("astral", "traveller"):
        name = f"assets/kubejs/textures/item/scroll_{school}.png"
        image = release.read(name)
        assert struct.unpack(">II", image[16:24]) == (64, 64), school
        assert image == (project / "src/main/resources" / name).read_bytes()
        assert image == (instance / "kubejs" / name).read_bytes()
        model = json.loads(release.read(f"assets/kubejs/models/item/scroll_{school}.json"))
        assert model["textures"]["layer0"] == f"kubejs:item/scroll_{school}"
        textures[school] = {"width": 64, "height": 64, "sha256": hashlib.sha256(image).hexdigest().upper()}
    migration = json.loads((project / "outputs/traveller-migration-20261008/migration-validation.json").read_text(encoding="utf-8-sig"))
    configs = [f"data/{row['spell'].split(':')[0]}/irons_spellbooks_spell_config/{row['spell'].split(':')[1]}.json" for row in migration["files"]]
    configs += [f"data/irons_spellbooks/irons_spellbooks_spell_config/{spell}.json" for spell in ("teleport", "recall", "portal")]
    assert len(configs) == 19
    for name in configs:
        assert json.loads(release.read(name))["irons_spellbooks:school"] == "kubejs:traveller"
        assert release.read(name) == (project / "src/main/resources" / name).read_bytes()
    vendor = project / "vendor/roacw-1.1.1-unpacked"
    vendor_classes = list((vendor / "net/kamaarion/roacw").rglob("*.class"))
    assert len(vendor_classes) == 98
    for path in vendor_classes:
        assert release.read(path.relative_to(vendor).as_posix()) == path.read_bytes()
    classes = [n for n in names if n.endswith(".class")]
    assert {n for n in baseline.namelist() if n.endswith(".class")} == set(classes)
    assert all(release.read(n) == baseline.read(n) for n in classes), "Existing 1.1.8 implementation changed during rebuild"
    json_count = 0
    for name in names:
        if name.endswith(".json"):
            json.loads(release.read(name).decode("utf-8-sig"))
            json_count += 1
    old_names = {n for n in previous.namelist() if not n.endswith("/")}
    new_names = {n for n in names if not n.endswith("/")}
    removed = sorted(old_names - new_names)
    added = sorted(new_names - old_names)
    changed = sorted(n for n in old_names & new_names if release.read(n) != previous.read(n))
    assert not removed, f"Old release entries removed: {removed}"
    assert not added, f"Unexpected new entries: {added}"
    allowed_resources = {f"assets/kubejs/textures/item/scroll_{school}.png" for school in ("astral", "traveller")}
    allowed_resources |= {"META-INF/mods.toml", "META-INF/MANIFEST.MF"}
    for name in changed:
        if name.endswith(".class"):
            assert name[:-6].split("$", 1)[0] in allowed_pending_classes, name
        else:
            assert name in allowed_resources, name

active = instance / "mods/wans_iron_expansion-1.1.8.jar"
report = {
    "version": "1.1.8", "artifact": str(release_path), "sha256": hash_file(release_path),
    "bytes": release_path.stat().st_size, "textures": textures,
    "traveller_configs_checked": 19, "json_files_checked": json_count,
    "baseline_1_1_8_classes_preserved": len(classes), "roacw_classes_preserved": 98,
    "changed_from_active_1_1_7": changed,
    "deployed": active.exists() and hash_file(active) == hash_file(release_path),
    "game_visual_verified": False,
}
(out / "release-validation.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(report, ensure_ascii=False, indent=2))
