from __future__ import annotations

import hashlib
import io
import json
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
VERSION = "1.2.3-roacw-separated-preview.1"
JAR = ROOT / "outputs" / "radiation-ten-spells-20261021" / f"wans_iron_expansion-{VERSION}.jar"
ACTIVE = ROOT.parent / "mods" / JAR.name
BASE = "assets/alex_caves_spellsbook/"
SPELLS = (
    "isotope_brand", "neutron_rebound", "moderation_field", "lead_curtain",
    "chain_reaction", "radiation_film", "cloud_chamber_trail",
    "cherenkov_wake", "decay_transposition", "critical_matrix",
)
EFFECTS = (
    "isotope", "neutron", "field", "curtain", "chain", "film", "cloud",
    "trail", "wake", "shell", "matrix", "lead_curtain_metal",
)


def read_png(z: zipfile.ZipFile, name: str) -> Image.Image:
    return Image.open(io.BytesIO(z.read(name))).convert("RGBA")


def main() -> None:
    assert JAR.is_file(), JAR
    with zipfile.ZipFile(JAR) as z:
        assert z.testzip() is None, "JAR CRC failure"
        entries = set(z.namelist())
        assert "META-INF/mods.toml" in entries
        assert "cn/wanyiwige/alexcavesspellsbook/toxic/RadiationNewEffectEntity.class" in entries
        assert "cn/wanyiwige/alexcavesspellsbook/toxic/RadiationHitEvents.class" in entries
        for lang in ("zh_cn", "en_us"):
            translations = json.loads(z.read(BASE + f"lang/{lang}.json"))
            for spell in SPELLS:
                assert f"spell.alex_caves_spellsbook.{spell}" in translations, (lang, spell)
                assert f"spell.alex_caves_spellsbook.{spell}.guide" in translations, (lang, spell)
        for spell in SPELLS:
            path = BASE + f"textures/gui/spell_icons/{spell}.png"
            assert path in entries and path + ".mcmeta" in entries, spell
            icon = read_png(z, path)
            assert icon.size == (64, 64), spell
            assert icon.getchannel("A").getextrema() == (255, 255), (spell, "opaque")
            for y in range(0, 64, 2):
                for x in range(0, 64, 2):
                    a = icon.getpixel((x, y))
                    assert all(icon.getpixel((x + dx, y + dy)) == a for dx in (0, 1) for dy in (0, 1)), (
                        spell, "mixed pixel lattice", x, y
                    )
        for effect in EFFECTS:
            path = BASE + f"textures/entity/toxic/{effect}.png"
            assert path in entries and path + ".mcmeta" in entries, effect
            sprite = read_png(z, path)
            assert sprite.size == ((64, 64) if effect == "lead_curtain_metal" else (32, 32)), effect
            if effect != "lead_curtain_metal":
                assert sprite.getchannel("A").getextrema()[0] == 0, (effect, "transparency")
        model = json.loads(z.read(BASE + "models/item/toxic_lead_prop.json"))
        assert len(model["elements"]) >= 9
        assert model["textures"]["0"] == "alex_caves_spellsbook:entity/toxic/lead_curtain_metal"
        assert all(len(element["faces"]) == 6 for element in model["elements"])
        assert all(b"blur" in z.read(BASE + f"textures/entity/toxic/{effect}.png.mcmeta") for effect in EFFECTS)
    digest = hashlib.sha256(JAR.read_bytes()).hexdigest()
    active_digest = hashlib.sha256(ACTIVE.read_bytes()).hexdigest() if ACTIVE.is_file() else None
    receipt = {
        "jar": str(JAR),
        "sha256": digest,
        "bytes": JAR.stat().st_size,
        "spell_count": len(SPELLS),
        "spell_ids": list(SPELLS),
        "opaque_icons": len(SPELLS),
        "transparent_effect_sprites": len(EFFECTS) - 1,
        "full_depth_model_elements": len(model["elements"]),
        "jar_crc": "pass",
        "game_tested": False,
        "active_jar": str(ACTIVE) if ACTIVE.is_file() else None,
        "active_sha256": active_digest,
        "deployed": active_digest == digest,
    }
    out = JAR.with_name("verification.json")
    out.write_text(json.dumps(receipt, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps(receipt, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
