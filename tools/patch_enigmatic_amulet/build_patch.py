"""Build a bytecode-only Enigmatic Delicacy 1.0.1 startup fix."""
from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[2]
HERE = Path(__file__).resolve().parent
INSTANCE = ROOT.parent
SOURCE = INSTANCE / "mods" / "enigmaticdelicacy-1.0.1.jar"
OUT = ROOT / "outputs" / "crash-fix-20261021"
OUT.mkdir(parents=True, exist_ok=True)
TARGET = OUT / SOURCE.name
CLASS = "auviotre/enigmatic/delicacy/contents/crafting/EnigmaticAmuletRecipe.class"
JDK = Path(r"C:\Program Files\Microsoft\jdk-17.0.15.6-hotspot") / "bin"
ASM = sorted((Path.home() / ".gradle/caches/modules-2/files-2.1/org.ow2.asm/asm").glob("**/asm-9.9.1.jar"))
assert len(ASM) == 1 and SOURCE.is_file()


def sha(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


with zipfile.ZipFile(SOURCE) as source:
    assert source.testzip() is None
    assert CLASS in source.namelist()
    assert not any(name.upper().startswith("META-INF/") and name.upper().endswith((".SF", ".RSA", ".DSA")) for name in source.namelist())
    original_class = source.read(CLASS)

input_class = OUT / "original-recipe.class"
patched_class = OUT / "patched-recipe.class"
input_class.write_bytes(original_class)
subprocess.run([str(JDK / "javac.exe"), "-cp", str(ASM[0]), "-d", str(OUT), str(HERE / "RecipeInitPatch.java")], check=True)
subprocess.run([str(JDK / "java.exe"), "-cp", os.pathsep.join((str(OUT), str(ASM[0]))), "RecipeInitPatch", str(input_class), str(patched_class)], check=True)
assert patched_class.read_bytes() != original_class

with zipfile.ZipFile(SOURCE) as source, zipfile.ZipFile(TARGET, "w") as target:
    for entry in source.infolist():
        target.writestr(entry, patched_class.read_bytes() if entry.filename == CLASS else source.read(entry.filename))

with zipfile.ZipFile(TARGET) as jar:
    assert jar.testzip() is None
    assert len(jar.namelist()) == len(set(jar.namelist()))
    assert jar.read(CLASS) == patched_class.read_bytes()

receipt = {"source": str(SOURCE), "source_sha256": sha(SOURCE), "release": str(TARGET),
           "release_sha256": sha(TARGET), "class": CLASS,
           "original_class_sha256": hashlib.sha256(original_class).hexdigest(),
           "patched_class_sha256": hashlib.sha256(patched_class.read_bytes()).hexdigest(),
           "jar_crc": "pass", "deployed": False}
(OUT / "enigmatic-patch-verification.json").write_text(json.dumps(receipt, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(receipt, ensure_ascii=False, indent=2))
