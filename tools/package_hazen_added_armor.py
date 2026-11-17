from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
import json,hashlib,re
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'development-records/hazen-armor-expansion'
BASE=ROOT.parent/'mods/wans_iron_expansion-1.2.9-illusionist.3-pure-darkness-dev.16.jar'
BUILD=ROOT/'build/libs/wans_iron_expansion-1.2.9-illusionist.3.jar'
TARGET=OUT/'wans_iron_expansion-1.2.9-illusionist.3-pure-darkness-dev.16-hazen-armor.1.jar'
RES=ROOT/'src/main/resources'
items=json.loads((RES/'hazen-added-armor.json').read_text(encoding='utf8'))
with ZipFile(BASE) as old,ZipFile(BUILD) as build:
    entries={n:old.read(n) for n in old.namelist() if not n.endswith('/')}
    for name in build.namelist():
        if name.endswith('.class') and any(part in name for part in ('/HazenAddedArmor','/AddedArmorAttachmentRenderer')):
            assert name not in entries,name
            entries[name]=build.read(name)
    entry='cn/wanyiwige/alexcavesspellsbook/hazen/HazenEquipment.class'
    entries[entry]=build.read(entry)
    entries['hazen-added-armor.json']=(RES/'hazen-added-armor.json').read_bytes()
    for kind in ('geo','animations','textures'):
        for p in (RES/('assets/alex_caves_spellsbook/'+kind+'/hazen_added')).rglob('*'):
            if p.is_file():entries[p.relative_to(RES).as_posix()]=p.read_bytes()
    for p in (RES/'assets/alex_caves_spellsbook/textures/hazen_added/effect_source').glob('*.png'):
        entries['assets/alex_caves_spellsbook/textures/mob_effect/'+p.name]=p.read_bytes()
    atlas='assets/minecraft/atlases/blocks.json'
    a=json.loads(entries.get(atlas,b'{"sources":[]}'))
    seen={row.get('resource') for row in a['sources'] if row.get('type')=='single'}
    for s in items:
        path='assets/alex_caves_spellsbook/models/item/'+s['id']+'.json'
        assert path not in entries,path
        entries[path]=(RES/path).read_bytes()
        for uri in json.loads(entries[path]).get('textures',{}).values():
            if uri not in seen:a['sources'].append({'type':'single','resource':uri});seen.add(uri)
    entries[atlas]=json.dumps(a,ensure_ascii=False,indent=2).encode()
    for locale in ('zh_cn','en_us'):
        path='assets/alex_caves_spellsbook/lang/'+locale+'.json'
        original=json.loads(entries[path]);new=json.loads((RES/path).read_text(encoding='utf8'))
        keys={'item.alex_caves_spellsbook.'+s['id'] for s in items}
        keys|={k for k in new if 'hazen_added' in k or k=='key.categories.hazen_added'}
        for k in keys:original[k]=new[k]
        entries[path]=json.dumps(original,ensure_ascii=False,indent=2).encode()
    metadata='META-INF/mods.toml'
    s=entries[metadata].decode();s=re.sub(r'(?m)^version="[^"]+"', 'version="1.2.9-illusionist.3-pure-darkness-dev.16-hazen-armor.1"',s,count=1);entries[metadata]=s.encode()
    with ZipFile(TARGET,'w',ZIP_DEFLATED) as z:
        for n,b in entries.items():z.writestr(n,b)
    changed=[n for n in old.namelist() if not n.endswith('/') and entries[n]!=old.read(n)]
    assert set(changed)<= {entry,atlas,metadata,'assets/alex_caves_spellsbook/lang/zh_cn.json','assets/alex_caves_spellsbook/lang/en_us.json'},changed
    report={'base_sha256':hashlib.sha256(BASE.read_bytes()).hexdigest(),'candidate_sha256':hashlib.sha256(TARGET.read_bytes()).hexdigest(),'changed_existing_entries':changed,'new_entries':sorted(set(entries)-set(old.namelist()))}
    (OUT/'candidate-verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
print(TARGET)
print('Changed existing entries:',changed)
