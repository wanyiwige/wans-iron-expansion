"""Verify icon-only release against its captured deployed baseline."""
from pathlib import Path
import hashlib,io,json,tomllib,zipfile
from PIL import Image
root=Path(__file__).resolve().parents[1]
art=root/'artwork/candy-spell-icons-redraw-20261018'
baseline=json.loads((art/'baseline.json').read_text(encoding='utf-8'))
release=root/'build/libs/wans_iron_expansion-1.2.2.jar'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
prefix='assets/alex_caves_spellsbook/textures/gui/spell_icons/'
expected={prefix+r['id']+'.png' for r in baseline['icons']}
assert sha(art/'before-1.2.2.jar')==baseline['active_sha256']
with zipfile.ZipFile(art/'before-1.2.2.jar') as old,zipfile.ZipFile(release) as new:
 assert new.testzip() is None
 assert set(old.namelist())==set(new.namelist()),'JAR entries added or removed'
 changed={n for n in new.namelist() if old.read(n)!=new.read(n)}
 assert changed-{'META-INF/MANIFEST.MF'}==expected,changed^expected
 assert b'version="1.2.2"' in new.read('META-INF/mods.toml') or b'version = "1.2.2"' in new.read('META-INF/mods.toml')
 hashes=[]
 for name in sorted(expected):
  raw=new.read(name)
  with Image.open(io.BytesIO(raw)) as im:
   im.load();assert im.size==(16,16) and im.mode=='RGBA'
  assert raw==(root/'src/main/resources'/name).read_bytes()
  hashes.append(hashlib.sha256(raw).hexdigest())
 assert len(set(hashes))==26
matches=[]
for path in (root.parent/'mods').glob('*.jar'):
 with zipfile.ZipFile(path) as z:
  if 'META-INF/mods.toml' not in z.namelist():continue
  raw=z.read('META-INF/mods.toml')
  try:source=raw.decode('utf-8-sig')
  except UnicodeDecodeError:source=raw.decode('latin1')
  meta=tomllib.loads(source)
  ids=[m.get('modId') for m in meta.get('mods',[])]
  if set(ids)&{'alex_caves_spellsbook','roacw'}:matches.append(str(path))
assert matches==[str(root.parent/'mods/wans_iron_expansion-1.2.2.jar')],matches
result={'version':'1.2.2','baseline_sha256':baseline['active_sha256'],'release_sha256':sha(release),'changed_entries':sorted(changed),'unique_valid_16x16_icons':26,'only_icon_resources_changed':True,'active_mod_jars':matches,'game_tested':False}
(art/'verification.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(result,ensure_ascii=False,indent=2))
