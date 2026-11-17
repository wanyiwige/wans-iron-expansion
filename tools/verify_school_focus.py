"""Regression audit for Scroll Forge focus-slot admission and school matching."""
from pathlib import Path
from collections import defaultdict
import hashlib,json,zipfile,tomllib
root=Path(__file__).resolve().parents[1]
out=root/'development-checks/school-focus-20261018'
tag='data/irons_spellbooks/tags/items/school_focus.json'
prefix='alex_caves_spellsbook:'
schools=['candy','toxic','hazen/cosmic','hazen/radiance','hazen/shadow','hazen/hydro','cloud']
targets=[prefix+s+'_focus' for s in schools]
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
release=root/'build/libs/wans_iron_expansion-1.2.2.jar'
with zipfile.ZipFile(out/'before.jar') as old,zipfile.ZipFile(release) as new:
 assert new.testzip() is None
 assert set(old.namelist())==set(new.namelist())
 changed={n for n in new.namelist() if old.read(n)!=new.read(n)}
 assert changed-{'META-INF/MANIFEST.MF'}=={tag},changed
 before=json.loads(old.read(tag));after=json.loads(new.read(tag))
 assert not after['replace']
 assert set(before['values']).issubset(after['values'])
 assert all('#'+t not in before['values'] for t in targets),'Baseline no longer reproduces regression'
 assert all('#'+t in after['values'] for t in targets)
 assert len(after['values'])==len(set(after['values']))
 assert all(new.read(n)==(root/'src/main/resources'/n).read_bytes() for n in changed if n!= 'META-INF/MANIFEST.MF')
 version=tomllib.loads(new.read('META-INF/mods.toml').decode('utf-8'))['mods'][0]['version'];assert version=='1.2.2'
 # Resolve nested item tags as supplied by the installed mods, substituting candidate.
 graph=defaultdict(list);owners=[]
 for p in (root.parent/'mods').glob('*.jar'):
  with zipfile.ZipFile(p) as deployed:
   if 'META-INF/mods.toml' in deployed.namelist():
    raw=deployed.read('META-INF/mods.toml')
    try:txt=raw.decode('utf-8-sig')
    except UnicodeDecodeError:txt=raw.decode('latin1')
    if any(m.get('modId')=='alex_caves_spellsbook' for m in tomllib.loads(txt).get('mods',[])):owners.append(str(p))
  with zipfile.ZipFile(release if p.name=='wans_iron_expansion-1.2.2.jar' else p) as z:
   for n in z.namelist():
    parts=n.split('/')
    if len(parts)<5 or parts[0]!='data' or parts[2:4]!=['tags','items'] or not n.endswith('.json'):continue
    key=parts[1]+':'+('/'.join(parts[4:])[:-5]);data=json.loads(z.read(n))
    if data.get('replace',False):graph[key]=[]
    graph[key].extend(v if isinstance(v,str) else v['id'] for v in data.get('values',[]))
 def resolve(key,seen=()):
  assert key not in seen,'Tag cycle'
  values=set()
  for v in graph[key]:values.update(resolve(v[1:],seen+(key,)) if v.startswith('#') else [v])
  return values
 accepted=resolve('irons_spellbooks:school_focus');rows=[]
 for target in targets:
  own=resolve(target);assert own and own<=accepted,(target,own-accepted)
  rows.append({'school_focus_tag':target,'items':sorted(own),'slot_admission_and_school_matching':True})
 assert owners==[str(root.parent/'mods/wans_iron_expansion-1.2.2.jar')],owners
 result={'version':version,'old_sha256':sha(out/'before.jar'),'new_sha256':sha(release),'changed_entries':sorted(changed),'schools':rows,'active_mod_jars':owners,'runtime_tested':False}
 (out/'verification.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
 print(json.dumps(result,ensure_ascii=False,indent=2))
