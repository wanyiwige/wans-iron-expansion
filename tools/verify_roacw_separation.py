"""Verify Wan's release no longer contains the standalone ROACW mod."""

import hashlib
import json
import re
import sys
from pathlib import Path
from zipfile import ZipFile


jar = Path(sys.argv[1])
with ZipFile(jar) as archive:
    names = set(archive.namelist())
    assert archive.testzip() is None, "Corrupt JAR entry"
    metadata = archive.read("META-INF/mods.toml").decode("utf-8")
    declarations = re.findall(r'^modId="([^"]+)"', metadata, re.MULTILINE)
    assert declarations.count("alex_caves_spellsbook") >= 1
    assert '[[mods]]\nmodId="roacw"' not in metadata
    assert 'versionRange="[1.2.2,)"' in metadata
    assert not any(name.startswith("net/kamaarion/roacw/") for name in names)
    assert not any(name.startswith(("assets/roacw/", "data/roacw/")) for name in names)
    for name in names:
        if name.endswith(".json"):
            json.loads(archive.read(name))
    print("JAR:", jar)
    print("SHA-256:", hashlib.sha256(jar.read_bytes()).hexdigest())
    print("Size:", jar.stat().st_size)
    print("Mod declarations:", len(re.findall(r'^\[\[mods\]\]', metadata, re.MULTILINE)))
    print("JSON files:", sum(name.endswith(".json") for name in names))
    print("ROACW classes/assets/data: 0/0/0")
