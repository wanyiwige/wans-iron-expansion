"""Preview removal of the native Hazen production module; --apply performs it.

Never changes mods/, saves/, original input archives, or audit/rollback backups.
"""
from pathlib import Path
import argparse
import datetime
import hashlib
import json
import re
import zipfile

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / 'development-records/hazen-recreation/removal-manifest.json'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    manifest = json.loads(MANIFEST.read_text(encoding='utf-8'))
    managed = []
    for row in manifest['added_files']:
        path = (ROOT / row['path']).resolve()
        if not path.is_relative_to(ROOT.resolve()): raise SystemExit('Path outside project: ' + str(path))
        if not path.exists(): continue
        if hashlib.sha256(path.read_bytes()).hexdigest() != row['sha256']:
            raise SystemExit('File changed since manifest; preserve edits and review manually: ' + str(path))
        managed.append(path)
    shared = {}
    for relative, additions in manifest.get('shared_list_additions', {}).items():
        path=ROOT/relative
        data=json.loads(path.read_text(encoding='utf-8-sig'))
        for entry in additions['entries']:
            if entry not in data[additions['key']]: raise SystemExit('Shared entry changed: '+str(entry))
            data[additions['key']].remove(entry)
        shared[path]=json.dumps(data,ensure_ascii=False,indent=2)+'\n'
    for relative, additions in manifest.get('atlas_additions', {}).items():
        path = ROOT / relative
        data = json.loads(path.read_text(encoding='utf-8-sig'))
        for entry in additions:
            if entry not in data['sources']: raise SystemExit('Atlas entry changed: ' + str(entry))
            data['sources'].remove(entry)
        shared[path] = json.dumps(data, ensure_ascii=False, indent=2) + '\n'
    for relative, hook in manifest['integration_hooks'].items():
        path = ROOT / relative
        text = path.read_text(encoding='utf-8-sig')
        if text.count(hook) != 1: raise SystemExit('Integration hook changed: ' + str(path))
        shared[path] = text.replace(hook, '')
    for relative, keys in manifest['language_additions'].items():
        path = ROOT / relative
        data = json.loads(path.read_text(encoding='utf-8-sig'))
        for key, expected in keys.items():
            if data.get(key) != expected: raise SystemExit('Translation changed: ' + key)
            del data[key]
        shared[path] = json.dumps(data, ensure_ascii=False, indent=2) + '\n'
    notice = ROOT / 'THIRD_PARTY_NOTICES.md'
    text = notice.read_text(encoding='utf-8-sig')
    text, count = re.subn(r'\n<!-- HAZEN_RECREATION_NOTICE_BEGIN -->.*?<!-- HAZEN_RECREATION_NOTICE_END -->\n', '', text, flags=re.S)
    if count != 1: raise SystemExit('Hazen provenance notice changed')
    shared[notice] = text
    print(json.dumps({'mode': 'apply' if args.apply else 'preview', 'production_files_to_remove': len(managed),
                      'integration_files_to_edit': [str(p.relative_to(ROOT)) for p in shared],
                      'kept': ['mods', 'saves', 'configuration', 'audit records', 'backups', 'development checks'],
                      'next_step': 'Rebuild the project after removal. Existing JARs are not changed.'}, indent=2))
    if not args.apply: return
    backup = ROOT / 'development-backups' / ('before-hazen-removal-' + datetime.datetime.now().strftime('%Y%m%d-%H%M%S') + '.zip')
    with zipfile.ZipFile(backup, 'w', zipfile.ZIP_DEFLATED) as jar:
        for path in managed + list(shared): jar.write(path, path.relative_to(ROOT))
    with zipfile.ZipFile(backup) as jar:
        if jar.testzip() is not None: raise SystemExit('Removal backup failed integrity check')
        for path in managed + list(shared):
            if hashlib.sha256(jar.read(path.relative_to(ROOT).as_posix())).digest() != hashlib.sha256(path.read_bytes()).digest():
                raise SystemExit('Removal backup hash mismatch')
    for path, text in shared.items(): path.write_text(text, encoding='utf-8')
    for path in managed: path.unlink()
    print('Production module removed. Recovery archive: ' + str(backup))


if __name__ == '__main__': main()
