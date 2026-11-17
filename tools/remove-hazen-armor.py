"""Guarded armor-only rollback. Preview by default; never alters installed JARs."""
from pathlib import Path
import argparse
import datetime
import hashlib
import json
import zipfile
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
RECORDS = ROOT / 'development-records/hazen-recreation'


def sha(data): return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    manifest = json.loads((RECORDS / 'armor-removal-manifest.json').read_text(encoding='utf-8'))
    archive = ROOT / manifest['original_backup']
    if sha(archive.read_bytes()) != manifest['original_backup_sha256']: raise SystemExit('Original backup changed')
    targets = []
    for row in manifest['changed_files']:
        path = (ROOT / row['path']).resolve()
        if not path.is_relative_to(ROOT.resolve()): raise SystemExit('Path outside project: ' + str(path))
        if not path.is_file() or sha(path.read_bytes()) != row['current_sha256']:
            raise SystemExit('File changed after armor preparation; preserve manual work: ' + str(path))
        targets.append((path, row))
    print(json.dumps(dict(mode='apply' if args.apply else 'preview', changed_files=len(targets),
                          restore=sum(r['previous_sha256'] is not None for _,r in targets),
                          remove=sum(r['previous_sha256'] is None for _,r in targets),
                          preserves=['installed mods', 'saves', 'config', 'previous weapon/spell module', 'audit records', 'backups'],
                          next_step='Rebuild after source rollback; installed JARs remain untouched.'), indent=2))
    if not args.apply: return
    backup = ROOT / 'development-backups' / ('before-armor-removal-' + datetime.datetime.now().strftime('%Y%m%d-%H%M%S') + '.zip')
    with zipfile.ZipFile(backup, 'w', zipfile.ZIP_DEFLATED) as z:
        for path,_ in targets: z.write(path, path.relative_to(ROOT))
    with zipfile.ZipFile(backup) as z:
        assert z.testzip() is None
        for path,_ in targets:
            assert sha(z.read(path.relative_to(ROOT).as_posix())) == sha(path.read_bytes())
    with zipfile.ZipFile(archive) as original:
        for path,row in targets:
            if row['previous_sha256'] is None: path.unlink()
            else:
                raw = original.read(row['path'])
                assert sha(raw) == row['previous_sha256']
                path.write_bytes(raw)
    subprocess.run([sys.executable, str(ROOT / 'tools/build_hazen_removal_manifest.py')], check=True)
    print('Armor source rollback complete. Recovery archive: ' + str(backup))


if __name__ == '__main__': main()
