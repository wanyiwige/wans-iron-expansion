from __future__ import annotations

import json
import uuid
from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
TEXTURE_DIR = ROOT / "src/main/resources/assets/alex_caves_spellsbook/textures/entity"
MODEL_DIR = ROOT / "src/main/resources/assets/alex_caves_spellsbook/models/entity"
PREVIEW_DIR = ROOT / "docs/previews"


def make_texture(path: Path, base: tuple[int, int, int], bright: tuple[int, int, int], accent: tuple[int, int, int]) -> None:
    image = Image.new("RGBA", (64, 32), (*base, 255))
    draw = ImageDraw.Draw(image)
    for y in range(32):
        shade = int(24 * (1 - y / 31))
        draw.line((0, y, 63, y), fill=tuple(min(255, c + shade) for c in base) + (255,))
    for x in range(0, 64, 8):
        draw.rectangle((x, 0, x + 2, 31), fill=(*bright, 255))
        draw.line((x + 3, 0, x + 3, 31), fill=(18, 22, 31, 255))
    draw.rectangle((0, 4, 63, 7), fill=(*accent, 255))
    draw.rectangle((0, 20, 63, 23), fill=(34, 39, 48, 255))
    draw.line((0, 31, 63, 31), fill=(6, 8, 13, 255))
    image.save(path)


CUBES = [
    ("shaft", [-9, -1, -1], [8, 1, 1], [0, 0, 20, 4]),
    ("core", [-3, -2, -2], [4, 2, 2], [0, 5, 18, 13]),
    ("head", [7, -2.5, -2.5], [11, 2.5, 2.5], [24, 5, 42, 15]),
    ("tip", [11, -1.5, -1.5], [14, 1.5, 1.5], [44, 5, 56, 11]),
    ("tail_collar", [-11, -2, -2], [-8, 2, 2], [0, 15, 14, 23]),
    ("fin_up", [-11, -5, -0.5], [-5, -1, 0.5], [16, 15, 30, 20]),
    ("fin_down", [-11, 1, -0.5], [-5, 5, 0.5], [30, 15, 44, 20]),
    ("fin_left", [-11, -0.5, -5], [-5, 0.5, -1], [16, 22, 30, 27]),
    ("fin_right", [-11, -0.5, 1], [-5, 0.5, 5], [36, 22, 50, 27]),
]


def make_element(name: str, start: list[float], end: list[float], uv: list[int]) -> dict:
    element_id = str(uuid.uuid5(uuid.NAMESPACE_URL, f"alex-caves-spellsbook:{name}"))
    faces = {
        side: {"uv": uv, "texture": 0}
        for side in ("north", "east", "south", "west", "up", "down")
    }
    return {
        "name": name,
        "box_uv": False,
        "rescale": False,
        "locked": False,
        "from": start,
        "to": end,
        "autouv": 0,
        "color": 0,
        "origin": [0, 0, 0],
        "faces": faces,
        "type": "cube",
        "uuid": element_id,
    }


def make_bbmodel(path: Path) -> None:
    elements = [make_element(*cube) for cube in CUBES]
    model = {
        "meta": {
            "format_version": "4.10",
            "model_format": "modded_entity",
            "box_uv": False,
        },
        "name": "magnetic_seeking_arrow",
        "model_identifier": "alex_caves_spellsbook:magnetic_seeking_arrow",
        "visible_box": [3, 2, 0],
        "variable_placeholders": "",
        "resolution": {"width": 64, "height": 32},
        "elements": elements,
        "outliner": [element["uuid"] for element in elements],
        "textures": [
            {
                "path": "../../textures/entity/magnetic_seeking_arrow_scarlet.png",
                "name": "magnetic_seeking_arrow_scarlet.png",
                "folder": "entity",
                "namespace": "alex_caves_spellsbook",
                "id": "0",
                "particle": False,
                "render_mode": "default",
                "visible": True,
                "mode": "bitmap",
                "saved": True,
                "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, "alex-caves-spellsbook:scarlet-arrow-texture")),
            },
            {
                "path": "../../textures/entity/magnetic_seeking_arrow_azure.png",
                "name": "magnetic_seeking_arrow_azure.png",
                "folder": "entity",
                "namespace": "alex_caves_spellsbook",
                "id": "1",
                "particle": False,
                "render_mode": "default",
                "visible": False,
                "mode": "bitmap",
                "saved": True,
                "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, "alex-caves-spellsbook:azure-arrow-texture")),
            },
        ],
    }
    path.write_text(json.dumps(model, ensure_ascii=False, indent=2), encoding="utf-8")


def draw_arrow_preview(draw: ImageDraw.ImageDraw, ox: int, oy: int, color: tuple[int, int, int], glow: tuple[int, int, int]) -> None:
    outline = (13, 18, 27, 255)
    draw.polygon([(ox + 18, oy + 50), (ox + 54, oy + 42), (ox + 125, oy + 42),
                  (ox + 142, oy + 31), (ox + 166, oy + 50), (ox + 142, oy + 69),
                  (ox + 125, oy + 58), (ox + 54, oy + 58)], fill=outline)
    draw.polygon([(ox + 28, oy + 50), (ox + 58, oy + 47), (ox + 131, oy + 47),
                  (ox + 145, oy + 38), (ox + 157, oy + 50), (ox + 145, oy + 62),
                  (ox + 131, oy + 53), (ox + 58, oy + 53)], fill=(*color, 255))
    draw.rectangle((ox + 82, oy + 43, ox + 107, oy + 57), fill=(*glow, 255))
    draw.line((ox + 31, oy + 50, ox + 154, oy + 50), fill=(235, 244, 255, 230), width=2)
    draw.polygon([(ox + 47, oy + 45), (ox + 20, oy + 27), (ox + 61, oy + 44)], fill=(*color, 255), outline=outline)
    draw.polygon([(ox + 47, oy + 55), (ox + 20, oy + 73), (ox + 61, oy + 56)], fill=(*color, 255), outline=outline)


def make_preview(path: Path) -> None:
    image = Image.new("RGBA", (420, 120), (24, 27, 34, 255))
    draw = ImageDraw.Draw(image)
    draw_arrow_preview(draw, 15, 10, (199, 24, 44), (255, 92, 111))
    draw_arrow_preview(draw, 220, 10, (24, 82, 210), (71, 190, 255))
    image.save(path)


def main() -> None:
    TEXTURE_DIR.mkdir(parents=True, exist_ok=True)
    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    PREVIEW_DIR.mkdir(parents=True, exist_ok=True)
    make_texture(TEXTURE_DIR / "magnetic_seeking_arrow_scarlet.png", (116, 12, 29), (210, 23, 46), (255, 96, 112))
    make_texture(TEXTURE_DIR / "magnetic_seeking_arrow_azure.png", (11, 47, 130), (22, 95, 229), (71, 190, 255))
    make_bbmodel(MODEL_DIR / "magnetic_seeking_arrow.bbmodel")
    make_preview(PREVIEW_DIR / "magnetic_seeking_arrow_preview.png")


if __name__ == "__main__":
    main()
