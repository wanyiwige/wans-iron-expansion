"""Verify the built spell integration and its external Foolish resources (not gameplay)."""
from pathlib import Path
import hashlib
import json
import tomllib
import zipfile

project = Path(__file__).resolve().parents[1]
instance = project.parent
jar = project / "build/libs/wans_iron_expansion-1.1.4.jar"
foolish = instance / "mods/foolish-1.3.0 patched.jar"
backup = project / "development-backups/foolish-sacilite-20261007-095803"
sha = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()
assert sha(foolish).upper() == "D85A9A4ACA626FD35BCC90F04A3DF14BEC8A1C0E79AABDD30753313AC642B7D7"
external = [
    "assets/foolish/geo/seekingsacilite.geo.json",
    "assets/foolish/animations/seekingsacilite.animation.json",
    "assets/foolish/textures/entities/seekingsacilite.png",
    "assets/foolish/models/block/sacilite_bomb.json",
    "assets/foolish/textures/block/sacilite-21.png_1.png",
    "assets/foolish/particles/sacilite_particle.json",
]
icons = {
    "sacilite_rain": "assets/foolish/textures/item/sacilite-71.png_1.png",
    "seeking_sacilite_swarm": "assets/foolish/textures/item/sacilite-31.png.png",
}
classes = ["spells/SaciliteSpell", "util/SaciliteSpellSupport", "entities/SaciliteSpellProjectile",
           "client/render/SaciliteCrystalRenderer", "client/render/SaciliteRainRenderer"]
sources = [project / f"src/main/java/cn/wanyiwige/alexcavesspellsbook/{name}.java" for name in classes]
assert all(jar.stat().st_mtime >= p.stat().st_mtime for p in sources), "JAR predates the final source edits"
report = {"artifact": str(jar), "sha256": sha(jar), "bytes": jar.stat().st_size,
          "foolish_sha256": sha(foolish), "runtime_tested": False, "deployed": False}
report["source_hashes"] = {str(p.relative_to(project)): sha(p) for p in sources}
with zipfile.ZipFile(jar) as release, zipfile.ZipFile(foolish) as original:
    entries = release.namelist()
    assert len(entries) == len(set(entries)), "Duplicate JAR entries"
    for name in classes:
        assert f"cn/wanyiwige/alexcavesspellsbook/{name}.class" in entries, name
    metadata = tomllib.loads(release.read("META-INF/mods.toml").decode())
    assert metadata["mods"][0]["version"] == "1.1.4"
    deps = metadata["dependencies"]["alex_caves_spellsbook"]
    assert {"foolish", "gtbcs_geomancy_plus"} <= {d["modId"] for d in deps if d["mandatory"]}
    report["external_assets"] = {name: hashlib.sha256(original.read(name)).hexdigest() for name in external}
    animation = json.loads(original.read(external[1]))
    assert "animation.seekingsacilite.fly" in animation["animations"]
    sounds = json.loads(original.read("assets/foolish/sounds.json"))
    assert {"bassdrop", "abominationstomp"} <= sounds.keys()
    report["icons"] = {}
    for spell, source in icons.items():
        name = f"assets/alex_caves_spellsbook/textures/gui/spell_icons/{spell}.png"
        assert release.read(name) == original.read(source), f"Icon does not match Foolish: {spell}"
        report["icons"][spell] = {"source": source, "sha256": hashlib.sha256(original.read(source)).hexdigest()}
    for lang in ("zh_cn", "en_us"):
        name = f"assets/alex_caves_spellsbook/lang/{lang}.json"
        current = json.loads(release.read(name))
        previous = json.loads((backup / "src/main/resources" / name).read_text(encoding="utf-8-sig"))
        assert all(current.get(key) == value for key, value in previous.items()), "Existing translations changed"
        for spell in icons:
            assert current[f"spell.alex_caves_spellsbook.{spell}"]
            assert current[f"spell.alex_caves_spellsbook.{spell}.guide"]
    assert "gtbcs_geomancy_plus".encode() in release.read("cn/wanyiwige/alexcavesspellsbook/spells/SaciliteSpell.class")
    for name in entries:
        if name.endswith(".json"):
            json.loads(release.read(name).decode("utf-8-sig"))
    report["json_files_checked"] = sum(n.endswith(".json") for n in entries)
report["backup_hashes"] = {str(p.relative_to(backup)): sha(p) for p in backup.rglob("*") if p.is_file()}
report["active_instance_jar"] = {"path": str(instance / "mods/wans_iron_expansion-1.1.3.jar"),
                                  "sha256": sha(instance / "mods/wans_iron_expansion-1.1.3.jar")}
out = project / "development-checks/foolish-sacilite/release-verification.json"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps({"verified": True, "artifact": str(jar), "sha256": report["sha256"],
                  "json_files_checked": report["json_files_checked"], "report": str(out)}, ensure_ascii=False))
