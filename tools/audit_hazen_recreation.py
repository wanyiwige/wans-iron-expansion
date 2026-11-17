"""Read original Hazen files; never generate Java by replacing upstream APIs."""
from pathlib import Path
import collections
import hashlib
import json
import re
import zipfile

PROJECT = Path(__file__).resolve().parents[1]
INSTANCE = PROJECT.parent
ORIGINAL = PROJECT / 'development-records/hazen-recreation/reference-1.4.0.14'
JAVA = ORIGINAL / 'src/main/java'
PACKAGE = JAVA / 'net/hazen/hazennstuff'
OUT = PROJECT / 'development-records/hazen-recreation'


def clean(s):
    return re.sub(r'/\*.*?\*/|//[^\n]*', '', s, flags=re.S)


def balanced(s, start, opening='(', closing=')'):
    depth = 0
    quoted = False
    escaped = False
    for i in range(start, len(s)):
        c = s[i]
        if quoted:
            if escaped:
                escaped = False
            elif c == '\\':
                escaped = True
            elif c == '"':
                quoted = False
        elif c == '"':
            quoted = True
        elif c == opening:
            depth += 1
        elif c == closing:
            depth -= 1
            if depth == 0:
                return s[start + 1:i]
    raise ValueError('Unbalanced source')


def split_args(s):
    result, start, stack, quoted = [], 0, [], False
    for i, c in enumerate(s):
        if c == '"' and (i == 0 or s[i - 1] != '\\'):
            quoted = not quoted
        if quoted:
            continue
        if c in '([{':
            stack.append(c)
        elif c in ')]}':
            stack.pop()
        elif c == ',' and not stack:
            result.append(s[start:i].strip())
            start = i + 1
    result.append(s[start:].strip())
    return result


def read(path):
    return path.read_text(encoding='utf-8-sig')


def digest(data):
    return hashlib.sha256(data).hexdigest()


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    jar_path = Path(json.loads(read(INSTANCE / 'mod-backports-1.20.1/inputs.json'))['hazennstuff'])
    legacy_path = INSTANCE / 'mods/hazennstuff-watered-down-edition-1.1.2.jar'
    legacy_source = INSTANCE / 'mod-backports-1.20.1/reference-decompiled/hazennstuff_legacy/net/hazen/hazennstuff/Registries/HnSItemRegistry.java'
    legacy_ids = set(re.findall(r'ITEMS.register\(\s*"([^"]+)"', read(legacy_source)))
    registry = clean(read(PACKAGE / 'Registries/HnSItemRegistry.java'))
    imports = {fq.rsplit('.', 1)[-1]: fq for fq in re.findall(r'import ([\w.]+);', registry)}
    all_items, weapons = [], []
    with zipfile.ZipFile(jar_path) as jar:
        names = set(jar.namelist())
        asset_hashes = {}
        mismatches = []
        for name in sorted(n for n in names if n.startswith('assets/') and not n.endswith('/')):
            raw = jar.read(name)
            asset_hashes[name] = digest(raw)
            candidates = [ORIGINAL / root / name for root in ('src/main/resources', 'src/generated/resources')]
            if not any(p.exists() and digest(p.read_bytes()) == digest(raw) for p in candidates):
                mismatches.append(name)
        for m in re.finditer(r'ITEMS\.register\s*\(', registry):
            expression = balanced(registry, registry.index('(', m.start()))
            args = split_args(expression)
            if not re.fullmatch(r'"[\w]+"', args[0]):
                continue
            item_id = args[0][1:-1]
            row = {'id': item_id, 'legacy': item_id in legacy_ids, 'registration': expression}
            cm = re.search(r'(\w+)::new|new (\w+)\(', args[1])
            cls = next((x for x in cm.groups() if x), '') if cm else ''
            fq = imports.get(cls, '')
            row['class'] = fq or cls
            all_items.append(row)
            if '.Weapons.' not in fq and '.Staves.' not in fq:
                continue
            source = JAVA / (fq.replace('.', '/') + '.java')
            text = clean(read(source))
            row = dict(row, source=str(source.relative_to(INSTANCE)), source_sha256=digest(source.read_bytes()))
            row['base_class'] = re.search(r'extends (\w+)', text).group(1)
            row['source_definition'] = text
            row['new'] = not row['legacy']
            row['excluded'] = 'historical user exclusion: Minecraft mace' if item_id == 'skyscorcher' else None
            row['resource_refs'] = []
            # Read exact resource literals from the sibling model/renderer classes.
            for sibling in source.parent.glob('*.java'):
                st = clean(read(sibling))
                for path in re.findall(r'"((?:geo|textures|animations)/[^"\n]+)"', st):
                    if path not in row['resource_refs']:
                        row['resource_refs'].append(path)
            row['missing_resource_refs'] = [p for p in row['resource_refs'] if 'assets/hazennstuff/' + p not in names and not p.startswith('animations/wizard_armor')]
            row['status'] = 'excluded' if row['excluded'] else ('existing_legacy' if row['legacy'] else 'pending_recreation')
            weapons.append(row)
        inventory = {'original_jar': str(jar_path), 'original_sha256': digest(jar_path.read_bytes()),
                     'legacy_jar': str(legacy_path), 'legacy_sha256': digest(legacy_path.read_bytes()),
                     'items': all_items, 'weapons': weapons, 'asset_hashes': asset_hashes,
                     'upstream_asset_mismatches': mismatches}
    (OUT / 'original-inventory.json').write_text(json.dumps(inventory, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps({'registered_items': len(all_items), 'weapons_and_staves': len(weapons),
                      'new_weapons_and_staves': sum(w['new'] for w in weapons),
                      'original_assets': len(asset_hashes), 'upstream_asset_mismatches': mismatches,
                      'unresolved_weapon_resources': {w['id']: w['missing_resource_refs'] for w in weapons if w['missing_resource_refs']}}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
