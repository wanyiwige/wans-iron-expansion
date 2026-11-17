"""Remove only the 31-spell source batch; preview by default and retain previous content."""
from pathlib import Path
import argparse
import datetime
import hashlib
import json
import subprocess
import sys
import zipfile

ROOT=Path(__file__).resolve().parents[1]
def sha(data):return hashlib.sha256(data).hexdigest()
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--apply',action='store_true');args=parser.parse_args()
    manifest=json.loads((ROOT/'development-records/hazen-recreation/secondary-removal-manifest.json').read_text(encoding='utf-8'))
    original=ROOT/manifest['original_backup']
    if sha(original.read_bytes())!=manifest['original_backup_sha256']:raise SystemExit('Original backup changed')
    targets=[]
    for row in manifest['changed_files']:
        path=(ROOT/row['path']).resolve()
        if not path.is_relative_to(ROOT.resolve()):raise SystemExit('Outside workspace: '+str(path))
        if not path.is_file()or sha(path.read_bytes())!=row['current_sha256']:raise SystemExit('File changed; preserve manual edits: '+str(path))
        targets.append((path,row))
    print(json.dumps(dict(mode='apply'if args.apply else'preview',changed_files=len(targets),
        restore=sum(row['previous_sha256']is not None for _,row in targets),remove=sum(row['previous_sha256']is None for _,row in targets),
        preserves=['previous 47 weapons and seven spells','five armor sets','installed JARs','saves','config','audit records','backups']),indent=2))
    if not args.apply:return
    backup=ROOT/'development-backups'/('before-secondary-removal-'+datetime.datetime.now().strftime('%Y%m%d-%H%M%S')+'.zip')
    with zipfile.ZipFile(backup,'w',zipfile.ZIP_DEFLATED)as z:
        for path,_ in targets:z.write(path,path.relative_to(ROOT))
    with zipfile.ZipFile(backup)as z:
        assert z.testzip()is None
        for path,_ in targets:assert sha(z.read(path.relative_to(ROOT).as_posix()))==sha(path.read_bytes())
    with zipfile.ZipFile(original)as z:
        for path,row in targets:
            if row['previous_sha256']is None:path.unlink()
            else:
                raw=z.read(row['path']);assert sha(raw)==row['previous_sha256'];path.write_bytes(raw)
    subprocess.run([sys.executable,str(ROOT/'tools/build_hazen_removal_manifest.py')],check=True)
    print('Source rollback complete. Rebuild before replacing any installed JAR. Backup: '+str(backup))
if __name__=='__main__':main()
