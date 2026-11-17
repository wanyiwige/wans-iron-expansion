from pathlib import Path
import hashlib
import json
import zipfile

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'development-records/hazen-recreation'
BACKUP = ROOT / 'development-backups/before-hazen-armor-20261016-115217/before-armor-with-atlas.zip'
paths = set()
with zipfile.ZipFile(BACKUP) as previous:
    before_names = set(previous.namelist())
    for name in before_names:
        # Only the known shared files changed during this armor batch are restored.
        if name.startswith(('src/', 'tools/', 'docs/')) or name == 'gradle.properties' or name.endswith('HazenQa.java'):
            path = ROOT / name
            if path.is_file() and path.read_bytes() != previous.read(name): paths.add(path)
    for pattern in ['src/main/java/cn/wanyiwige/alexcavesspellsbook/hazen/**/*Armor*.java',
                    'src/main/resources/assets/alex_caves_spellsbook/hazen/armor/**/*',
                    'src/main/resources/assets/alex_caves_spellsbook/geo/hazen/armor/**/*',
                    'src/main/resources/assets/alex_caves_spellsbook/textures/hazen/armor/**/*',
                    'src/main/resources/assets/alex_caves_spellsbook/sounds/hazen/armor/**/*',
                    'src/main/resources/data/alex_caves_spellsbook/recipes/hazen/armor_batch1/*.json',
                    'src/main/resources/assets/alex_caves_spellsbook/models/item/hazen_melee_rune.json']:
        paths.update(p for p in ROOT.glob(pattern) if p.is_file())
    for spec in json.loads((ROOT/'src/main/resources/hazen-armor-sets.json').read_text(encoding='utf-8')):
        for part in ['helmet','chestplate','leggings','boots']:
            paths.add(ROOT/f'src/main/resources/assets/alex_caves_spellsbook/models/item/hazen_{spec["id"]}_{part}.json')
    for name in ['src/main/resources/hazen-armor-sets.json',
                 'src/main/resources/assets/alex_caves_spellsbook/textures/mob_effect/hazen_swordmaster_set_bonus.png',
                 'src/main/resources/data/alex_caves_spellsbook/tags/items/hazen/focus/melee_focus.json',
                 'development-checks/hazen-native/src/main/java/cn/wanyiwige/hazenqa/HazenArmorTests.java',
                 'docs/hazen-armor-batch1.md']:
        paths.add(ROOT/name)
    rows=[]
    for path in sorted(paths):
        name=path.relative_to(ROOT).as_posix()
        rows.append(dict(path=name, current_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                         previous_sha256=hashlib.sha256(previous.read(name)).hexdigest() if name in before_names else None))
manifest=dict(original_backup=BACKUP.relative_to(ROOT).as_posix(), original_backup_sha256=hashlib.sha256(BACKUP.read_bytes()).hexdigest(), changed_files=rows)
(OUT/'armor-removal-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
print('Managed armor rollback files:',len(rows))
