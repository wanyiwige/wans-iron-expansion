"""Freeze, preview or apply a hash-guarded rollback of survival batch only.

--freeze records current files against the immutable preview.7 source backup.
Default mode is read-only. --apply backs up current files before restoration.
"""
from pathlib import Path
import argparse,datetime,hashlib,json,zipfile

ROOT=Path(__file__).resolve().parents[1]
RECORDS=ROOT/'development-records/hazen-recreation'
MANIFEST=RECORDS/'survival-removal-manifest.json'
def sha(b):return hashlib.sha256(b).hexdigest()
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--freeze',action='store_true');parser.add_argument('--apply',action='store_true');a=parser.parse_args()
    if a.freeze and a.apply:raise SystemExit('Choose freeze or apply')
    report=json.loads((RECORDS/'survival-map.json').read_text(encoding='utf-8'))
    original=ROOT/report['backup']
    assert sha(original.read_bytes())==report['backup_sha256'],'Backup hash mismatch'
    if a.freeze:
        paths={p for folder in ['src','tools','docs','development-checks/hazen-native/src'] for p in (ROOT/folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts}
        paths.update([ROOT/'gradle.properties',ROOT/'development-checks/hazen-native/build.gradle',ROOT/'development-checks/ItemAtlasDiscovery.java'])
        rows=[]
        with zipfile.ZipFile(original) as z:
            old=set(z.namelist())
            for p in sorted(paths):
                name=p.relative_to(ROOT).as_posix();raw=p.read_bytes();before=z.read(name) if name in old else None
                # The atlas harness predates this batch but was outside its source
                # backup. Preserve it; the standalone survival rollback has no
                # dependency on reverting a development-only broader texture scan.
                if name=='development-checks/ItemAtlasDiscovery.java':continue
                if raw!=before:rows.append(dict(path=name,current_sha256=sha(raw),previous_sha256=sha(before) if before is not None else None))
        MANIFEST.write_text(json.dumps(dict(original_backup=report['backup'],original_backup_sha256=report['backup_sha256'],changed_files=rows),indent=2)+'\n',encoding='utf-8')
    manifest=json.loads(MANIFEST.read_text(encoding='utf-8'))
    targets=[]
    for row in manifest['changed_files']:
        p=(ROOT/row['path']).resolve()
        assert p.is_relative_to(ROOT.resolve()),'Outside project'
        assert p.is_file() and sha(p.read_bytes())==row['current_sha256'],'Changed after manifest; preserve edits: '+str(p)
        targets.append((p,row))
    print(json.dumps(dict(mode='freeze' if a.freeze else 'apply' if a.apply else 'preview',files=len(targets),restore=sum(r['previous_sha256']is not None for _,r in targets),remove=sum(r['previous_sha256']is None for _,r in targets),preserves=['preview.7 weapons, armor and spells','installed JARs','world saves','user config','audit records','backups']),indent=2))
    if not a.apply:return
    backup=ROOT/'development-backups'/('before-survival-removal-'+datetime.datetime.now().strftime('%Y%m%d-%H%M%S')+'.zip')
    with zipfile.ZipFile(backup,'w',zipfile.ZIP_DEFLATED) as z:
        for p,_ in targets:z.write(p,p.relative_to(ROOT).as_posix())
    with zipfile.ZipFile(backup) as z:
        assert z.testzip()is None
        for p,_ in targets:assert sha(z.read(p.relative_to(ROOT).as_posix()))==sha(p.read_bytes())
    with zipfile.ZipFile(original) as z:
        for p,row in targets:
            if row['previous_sha256']is None:p.unlink()
            else:
                raw=z.read(row['path']);assert sha(raw)==row['previous_sha256'];p.write_bytes(raw)
    print('Source restored to preview.7. Rebuild to obtain a rollback JAR. Recovery archive:',backup)
if __name__=='__main__':main()
