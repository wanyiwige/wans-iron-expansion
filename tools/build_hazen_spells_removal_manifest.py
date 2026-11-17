"""Freeze the secondary-spell batch's exact reversible source/resource changes."""
from pathlib import Path
import hashlib
import json
import zipfile

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'development-records/hazen-recreation'
BACKUP=ROOT/'development-backups/before-hazen-spells-20261016-121200/before-spells.zip'
managed=json.loads((OUT/'removal-manifest.json').read_text(encoding='utf-8'))
paths={ROOT/row['path']for row in managed['added_files']}
paths.update(ROOT/p for p in ['gradle.properties','src/main/resources/assets/alex_caves_spellsbook/lang/zh_cn.json',
    'src/main/resources/assets/alex_caves_spellsbook/lang/en_us.json',
    'development-checks/hazen-native/src/main/java/cn/wanyiwige/hazenqa/HazenQa.java',
    'development-checks/hazen-native/src/main/java/cn/wanyiwige/hazenqa/HazenNativeTests.java',
    'development-checks/hazen-native/src/main/java/cn/wanyiwige/hazenqa/HazenSecondaryTests.java',
    'tools/audit_hazen_remaining.py','tools/verify_hazen_release.py','tools/build_hazen_removal_manifest.py',
    'docs/hazen-recreation.md','docs/hazen-spells-batch2.md'])
rows=[]
with zipfile.ZipFile(BACKUP)as before:
    old=set(before.namelist())
    for path in sorted(paths):
        if not path.is_file():continue
        name=path.relative_to(ROOT).as_posix();raw=path.read_bytes()
        if name in old and raw==before.read(name):continue
        rows.append(dict(path=name,current_sha256=hashlib.sha256(raw).hexdigest(),previous_sha256=hashlib.sha256(before.read(name)).hexdigest()if name in old else None))
manifest=dict(original_backup=BACKUP.relative_to(ROOT).as_posix(),original_backup_sha256=hashlib.sha256(BACKUP.read_bytes()).hexdigest(),changed_files=rows)
(OUT/'secondary-removal-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
print('Managed secondary-spell rollback files:',len(rows))
