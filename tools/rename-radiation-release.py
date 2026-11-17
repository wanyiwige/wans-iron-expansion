"""Repackage only renamed school text into the verified 1.2 binary, retaining all classes/assets."""
from pathlib import Path
import hashlib,json,zipfile,shutil
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'outputs/toxic-1.2-20261015'
JAR=OUT/'wans_iron_expansion-1.2.jar'
BACKUP=OUT/'before-radiation-name'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
BACKUP.mkdir(exist_ok=True)
if not (BACKUP/JAR.name).exists():
    shutil.copy2(JAR,BACKUP/JAR.name)
    shutil.copy2(OUT/'release-validation.json',BACKUP/'release-validation.json')
    assert sha(JAR)==sha(BACKUP/JAR.name)
old=BACKUP/JAR.name
expected=json.loads((BACKUP/'release-validation.json').read_text(encoding='utf-8'))
assert sha(old)==expected['sha256']
temp=OUT/'radiation-name.tmp.jar'
prefix='assets/alex_caves_spellsbook/lang/'
changes={
 'zh_cn':{'school.alex_caves_spellsbook.toxic':'辐射','attribute.alex_caves_spellsbook.toxic_spell_power':'辐射法术强度',
 'attribute.alex_caves_spellsbook.toxic_magic_resist':'辐射魔法抗性','itemGroup.alex_caves_spellsbook.toxic_spells':'辐射学派法术',
 'death.attack.toxic_magic':'%1$s 被辐射能量吞噬了','death.attack.toxic_magic.player':'%1$s 被 %2$s 的辐射能量吞噬了'},
 'en_us':{'school.alex_caves_spellsbook.toxic':'Radiation','attribute.alex_caves_spellsbook.toxic_spell_power':'Radiation Spell Power',
 'attribute.alex_caves_spellsbook.toxic_magic_resist':'Radiation Magic Resistance','itemGroup.alex_caves_spellsbook.toxic_spells':'Radiation School Spells',
 'death.attack.toxic_magic':'%1$s was consumed by radiation energy','death.attack.toxic_magic.player':"%1$s was consumed by %2$s's radiation energy"}}
changed=[];classes=0
with zipfile.ZipFile(old) as base,zipfile.ZipFile(temp,'w') as out:
    for info in base.infolist():
        raw=base.read(info.filename);result=raw
        if info.filename in {prefix+'zh_cn.json',prefix+'en_us.json'}:
            lang=info.filename.rsplit('/',1)[-1].removesuffix('.json')
            text=raw.decode('utf-8');d=json.loads(text)
            for key,value in changes[lang].items():
                oldvalue=d[key];needle=json.dumps(key)+': '+json.dumps(oldvalue,ensure_ascii=False)
                replacement=json.dumps(key)+': '+json.dumps(value,ensure_ascii=False)
                assert needle in text; text=text.replace(needle,replacement,1)
            if lang=='zh_cn':text=text.replace('持续造成毒化伤害并施加中毒和腐蚀。','持续造成辐射伤害并施加中毒和腐蚀。')
            result=text.encode('utf-8')
            verified=json.loads(text)
            for k,v in changes[lang].items():assert verified[k]==v
            for k,v in d.items():
                if k not in changes[lang] and k!='spell.alex_caves_spellsbook.corrosive_cloud.guide':assert verified[k]==v
        elif info.filename=='META-INF/mods.toml':
            result=raw.replace(b'candy, and toxic spell content.',b'candy, and radiation spell content.')
        if raw!=result:changed.append(info.filename)
        out.writestr(info,result)
with zipfile.ZipFile(old) as a,zipfile.ZipFile(temp) as b:
    assert b.testzip() is None and a.namelist()==b.namelist()
    assert set(changed)=={prefix+'zh_cn.json',prefix+'en_us.json','META-INF/mods.toml'}
    for name in a.namelist():
        if name.endswith('.class'):classes+=1
        if name not in changed:assert a.read(name)==b.read(name),name
temp.replace(JAR)
report=expected.copy()
report.update({'artifact':str(JAR),'frozen_artifact':str(JAR),'bytes':JAR.stat().st_size,'sha256':sha(JAR),
 'display_school_name':{'zh_cn':'辐射','en_us':'Radiation'},'school_id':'alex_caves_spellsbook:toxic',
 'name_only_repackage':{'base_artifact':str(old),'base_sha256':sha(old),'changed_entries':changed,
 'all_other_entries_identical':True,'class_entries_identical':classes,
 'reason':'Retain verified 1.2 bytecode while unrelated Hazen development continues in the shared checkout.'}})
(OUT/'release-validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
(OUT/'SHA256SUMS.txt').write_text(sha(JAR)+'  '+JAR.name+'\n',encoding='utf-8')
print(json.dumps({'name':report['display_school_name'],'changed_entries':changed,'unchanged_classes':classes,'sha256':sha(JAR)},ensure_ascii=False,indent=2))
