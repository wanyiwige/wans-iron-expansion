from pathlib import Path
from zipfile import ZipFile
import json,hashlib
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'development-records/hazen-armor-expansion'
JAR=OUT/'wans_iron_expansion-1.2.9-illusionist.3-pure-darkness-dev.16-hazen-armor.1.jar'
with ZipFile(JAR) as z:
    assert z.testzip() is None
    data=json.loads(z.read('hazen-added-armor.json'));names=set(z.namelist())
    assert len(data)==167 and len({s['id'] for s in data})==167
    assert len({s['family'] for s in data})==42
    assert all(s['family']!='legacy_fireblossom' for s in data)
    atlas=json.loads(z.read('assets/minecraft/atlases/blocks.json'))
    sprites={s.get('resource') for s in atlas['sources']}
    lang={locale:json.loads(z.read('assets/alex_caves_spellsbook/lang/'+locale+'.json')) for locale in ('zh_cn','en_us')}
    for s in data:
        for key,prefix in [('model','geo/'),('animation','animations/'),('texture','textures/')]:
            uri=s.get(key)
            if uri and uri.startswith('alex_caves_spellsbook:'):
                ns,rel=uri.split(':',1)
                assert rel.startswith(prefix),(s['id'],'Gecko discovery',uri)
                assert 'assets/'+ns+'/'+rel in names,(s['id'],'resource',uri)
        model=json.loads(z.read('assets/alex_caves_spellsbook/models/item/'+s['id']+'.json'))
        assert model.get('parent') in ('minecraft:item/generated','item/generated'),s['id']
        for uri in model.get('textures',{}).values():
            assert uri in sprites,(s['id'],'atlas',uri)
            ns,rel=uri.split(':',1);assert 'assets/'+ns+'/textures/'+rel+'.png' in names,(s['id'],'sprite',uri)
        for locale in lang:assert 'item.alex_caves_spellsbook.'+s['id'] in lang[locale]
        for b in s['resolved_attributes']:
            key=b['attribute'].upper()
            boost=any(k in key for k in ('MAX_MANA','SPELL_POWER')) or b['attribute'] in ('Attributes.ATTACK_DAMAGE','Attributes.MAX_HEALTH')
            expected=b['original']*(1.2 if boost and b['original']>0 else 1)
            assert abs(b['value']-expected)<1e-5,(s['id'],b)
    report={'items':167,'families':42,'attribute_entries':sum(len(s['resolved_attributes']) for s in data),'icons_and_languages':'pass','gecko_discovery_paths':'pass','balance_multiplier':1.2,'archive_integrity':'pass','sha256':hashlib.sha256(JAR.read_bytes()).hexdigest()}
    (OUT/'resource-verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
    print(json.dumps(report))
