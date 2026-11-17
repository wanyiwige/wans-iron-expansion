"""Remove resources copied from the ROACW 1.1.1 JAR, preserving Wan additions."""

import json
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED


PROJECT = Path(__file__).resolve().parents[1]
INSTANCE = PROJECT.parent
RESOURCES = PROJECT / "src/main/resources"
BASELINE = PROJECT / "vendor/roacw-1.1.1.jar"
LEGACY_ICONS = (
    "assets/roacw/textures/gui/spell_icons/final_rend.png",
    "assets/roacw/textures/gui/spell_icons/quick_strike.png",
)
PACK = INSTANCE / "resourcepacks/wan-roacw-legacy-icons-1.20.1.zip"


def same_json(left: bytes, right: bytes) -> bool:
    try:
        return json.loads(left) == json.loads(right)
    except (UnicodeDecodeError, json.JSONDecodeError):
        return False


def main() -> None:
    if not BASELINE.is_file() or not RESOURCES.is_dir():
        raise SystemExit("ROACW baseline or resource root is missing")
    if PACK.exists():
        raise SystemExit(f"Resource pack already exists: {PACK}")

    with ZipFile(BASELINE) as baseline:
        source_names = {
            name for name in baseline.namelist()
            if name.startswith(("assets/", "data/")) and not name.endswith("/")
        }
        native_names = {
            name for name in source_names
            if name.startswith(("assets/roacw/", "data/roacw/"))
        }
        shared_names = sorted(source_names - native_names)
        for name in native_names:
            current = RESOURCES / name
            if not current.is_file():
                raise SystemExit(f"Copied ROACW resource is missing: {name}")
            if current.read_bytes() != baseline.read(name) and not same_json(
                current.read_bytes(), baseline.read(name)
            ):
                raise SystemExit(f"ROACW resource has a local edit: {name}")

        for name in LEGACY_ICONS:
            if not (RESOURCES / name).is_file() or name in source_names:
                raise SystemExit(f"Unexpected legacy icon state: {name}")

        shared_actions = {}
        for name in shared_names:
            current = RESOURCES / name
            if not current.is_file():
                raise SystemExit(f"Copied shared resource is missing: {name}")
            original_bytes = baseline.read(name)
            if current.read_bytes() == original_bytes or same_json(
                current.read_bytes(), original_bytes
            ):
                shared_actions[name] = None
                continue
            if not original_bytes.strip():
                shared_actions[name] = current.read_bytes()
                continue
            original = json.loads(original_bytes)
            edited = json.loads(current.read_bytes())
            if not isinstance(original.get("values"), list) or not isinstance(
                edited.get("values"), list
            ):
                raise SystemExit(f"Shared resource needs manual review: {name}")
            inherited = {json.dumps(value, sort_keys=True) for value in original["values"]}
            retained = [
                value for value in edited["values"]
                if json.dumps(value, sort_keys=True) not in inherited
            ]
            if not retained:
                shared_actions[name] = None
            else:
                edited["values"] = retained
                shared_actions[name] = (json.dumps(edited, ensure_ascii=False, indent=2) + "\n").encode()

        PACK.parent.mkdir(parents=True, exist_ok=True)
        with ZipFile(PACK, "x", ZIP_DEFLATED) as pack:
            pack.writestr(
                "pack.mcmeta",
                json.dumps({"pack": {"pack_format": 15, "description": "Wan ROACW legacy spell icons"}}),
            )
            for name in LEGACY_ICONS:
                pack.write(RESOURCES / name, name)

        for name in native_names:
            (RESOURCES / name).unlink()
        for name in LEGACY_ICONS:
            (RESOURCES / name).unlink()
        for name, replacement in shared_actions.items():
            path = RESOURCES / name
            if replacement is None:
                path.unlink()
            else:
                path.write_bytes(replacement)

    print(f"Removed {len(native_names)} copied ROACW resources and {len(LEGACY_ICONS)} icon overrides")
    print(f"Processed {len(shared_actions)} shared resource paths")
    print(f"Preserved icon overrides in {PACK}")


if __name__ == "__main__":
    main()
