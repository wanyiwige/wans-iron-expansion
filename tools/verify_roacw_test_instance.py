"""Check the separated ROACW test instance before launching it."""

import hashlib
import re
import sys
import tomllib
from collections import defaultdict
from pathlib import Path
from zipfile import BadZipFile, ZipFile


test = Path(sys.argv[1])
mods = test / "mods"
providers = defaultdict(list)
for path in mods.glob("*.jar"):
    try:
        with ZipFile(path) as archive:
            data = tomllib.loads(
                archive.read("META-INF/mods.toml").decode("utf-8", errors="replace")
            )
        for mod in data.get("mods", []):
            providers[mod["modId"]].append(path.name)
    except (BadZipFile, KeyError, ValueError, UnicodeDecodeError):
        pass

for mod_id in ("alex_caves_spellsbook", "roacw"):
    assert len(providers[mod_id]) == 1, (mod_id, providers[mod_id])
wan_jar = mods / providers["alex_caves_spellsbook"][0]
with ZipFile(wan_jar) as archive:
    twilight_classes = [
        name for name in archive.namelist()
        if name.startswith("cn/wanyiwige/twilight/") and name.endswith(".class")
    ]
    twilight_assets = [
        name for name in archive.namelist()
        if name.startswith("assets/wans_twilight_spellbooks/")
    ]
assert twilight_classes and twilight_assets, "Twilight preview content missing"
assert any(mods.glob("*twilightforest*.jar")), "Twilight Forest dependency missing"
assert (test / "saves/新的世界/level.dat").is_file()
assert (test / "kubejs/client_scripts/zhcn_roacw.js").is_file()
assert (test / "resourcepacks/wan-roacw-legacy-icons-1.20.1.zip").is_file()
for mod_id in ("alex_caves_spellsbook", "roacw"):
    path = mods / providers[mod_id][0]
    print(mod_id, path.name, hashlib.sha256(path.read_bytes()).hexdigest())
print("JAR files:", len(list(mods.glob("*.jar"))))
print("Twilight preview:", len(twilight_classes), "classes,", len(twilight_assets), "asset entries")
print("Test world and localization: present")
