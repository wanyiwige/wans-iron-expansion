from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
import json,re,hashlib
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'development-records/hazen-armor-expansion'
BASE=ROOT.parent/'mods/wans_iron_expansion-1.2.9-illusionist.3-pure-darkness-dev.16-hazen-armor.1.jar'
BUILD=ROOT/'build/libs/wans_iron_expansion-1.2.9-illusionist.3.jar'
TARGET=OUT/'wans_iron_expansion-1.2.9-illusionist.3-pure-darkness-dev.16-hazen-armor.2.jar'
prefix='cn/wanyiwige/alexcavesspellsbook/hazen/'
patched={prefix+n for n in ('HazenAddedArmor$Armor.class','HazenAddedArmor$Spec.class','HazenAddedArmorAudit.class')}
with ZipFile(BASE) as old,ZipFile(BUILD) as build:
    entries={n:old.read(n) for n in old.namelist() if not n.endswith('/')}
    for n in patched:entries[n]=build.read(n)
    data=json.loads((ROOT/'src/main/resources/hazen-added-armor.json').read_text(encoding='utf8'))
    before=json.loads(entries['hazen-added-armor.json']);assert len(data)==len(before)==167
    for a,b in zip(before,data):
        assert {k:v for k,v in a.items() if k!='animation_names'}=={k:v for k,v in b.items() if k!='animation_names'},a['id']
        uri=b['animation'];ns,path=uri.split(':',1);clips=json.loads(entries['assets/'+ns+'/'+path])['animations']
        assert b['animation_names']==list(clips),b['id']
    entries['hazen-added-armor.json']=json.dumps(data,ensure_ascii=False,indent=2).encode()
    key='META-INF/mods.toml';s=entries[key].decode().replace('dev.16-hazen-armor.1','dev.16-hazen-armor.2');entries[key]=s.encode()
    with ZipFile(TARGET,'w',ZIP_DEFLATED) as z:
        for n,b in entries.items():z.writestr(n,b)
    changed=[n for n in entries if entries[n]!=old.read(n)]
    assert set(changed)<=patched|{'hazen-added-armor.json',key}
    report={'base_sha256':hashlib.sha256(BASE.read_bytes()).hexdigest(),'fixed_sha256':hashlib.sha256(TARGET.read_bytes()).hexdigest(),'changed_entries':changed,'items':167,'missing_lists_before':sum(x.get('animation_names') is None for x in before),'missing_lists_after':sum(x.get('animation_names') is None for x in data)}
    (OUT/'animation-hotfix-verification.json').write_text(json.dumps(report,indent=2),encoding='utf8')
print(TARGET)
print(json.dumps(report))
