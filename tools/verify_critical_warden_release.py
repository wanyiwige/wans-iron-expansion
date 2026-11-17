"""Verify the release JAR, shipped model, native item icons and upgrade registration."""
import base64,hashlib,io,json,math,re,tomllib,zipfile
from pathlib import Path
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
CHECK=ROOT/'development-checks/radiation-armor-20261023'
ART=ROOT/'artwork/radiation-armor-release-20261023'
VERSION='1.2.7-radiation-armor.1'
JAR=ROOT/'build/libs'/f'wans_iron_expansion-{VERSION}.jar'
ns='alex_caves_spellsbook'
before=json.loads((CHECK/'active-before.json').read_text(encoding='utf-8'))[0]
old_path=Path(before['path'])
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
assert sha(old_path)==before['sha256'],'Active baseline changed during implementation'
with zipfile.ZipFile(JAR) as z,zipfile.ZipFile(old_path) as old:
    assert z.testzip() is None
    mods=tomllib.loads(z.read('META-INF/mods.toml').decode('utf-8'))['mods']
    assert len(mods)==1 and mods[0]['modId']==ns and mods[0]['version']==VERSION
    names=set(z.namelist());previous=set(old.namelist())
    assert not previous-names
    changed={n for n in names&previous if z.read(n)!=old.read(n)}
    allowed={'META-INF/MANIFEST.MF','META-INF/mods.toml',f'assets/{ns}/lang/zh_cn.json',f'assets/{ns}/lang/en_us.json',f'cn/wanyiwige/alexcavesspellsbook/toxic/ToxicRegistry.class',f'cn/wanyiwige/alexcavesspellsbook/toxic/RadiationEquipment.class'}
    assert changed==allowed,changed
    added={n for n in names-previous if not n.endswith('/')}
    assert len(added)==25 and all('critical_warden' in n or 'CriticalWarden' in n for n in added),added
    icons={}
    for part in ['helmet','chestplate','leggings','boots']:
        item='critical_warden_'+part
        model=json.loads(z.read(f'assets/{ns}/models/item/{item}.json'))
        assert model=={'parent':'minecraft:item/generated','textures':{'layer0':f'{ns}:item/{item}'}}
        data=z.read(f'assets/{ns}/textures/item/{item}.png')
        image=Image.open(io.BytesIO(data)).convert('RGBA')
        assert image.size==(16,16)
        assert {v[3] for v in image.get_flattened_data()}=={0,255}
        assert 6<=len({v for v in image.get_flattened_data() if v[3]})<=18
        assert image.getbbox()[0]>=1 and image.getbbox()[2]<=15
        meta=json.loads(z.read(f'assets/{ns}/textures/item/{item}.png.mcmeta'))
        assert meta['texture']['blur'] is False
        recipe=json.loads(z.read(f'data/{ns}/recipes/{item}.json'))
        assert recipe['result']['item']==f'{ns}:{item}'
        assert recipe['key']['C']['item']=='alexscaves:fissile_core'
        icons[item]={'size':image.size,'opaque_pixels':sum(v[3]>0 for v in image.get_flattened_data()),'colors':len({v for v in image.get_flattened_data() if v[3]})}
    for lang in ['zh_cn','en_us']:
        a=json.loads(old.read(f'assets/{ns}/lang/{lang}.json'));b=json.loads(z.read(f'assets/{ns}/lang/{lang}.json'))
        assert all(b.get(k)==v for k,v in a.items())
        assert len(b)-len(a)==4
    geo=json.loads(z.read(f'assets/{ns}/geo/critical_warden_armor.geo.json'))['minecraft:geometry'][0]
    texdata=z.read(f'assets/{ns}/textures/models/armor/critical_warden.png')
    tex=Image.open(io.BytesIO(texdata)).convert('RGBA')
    bb=json.loads((ROOT/'artwork/radiation-armor-v2-20261022/texture-materials-v5-large-canisters/critical_warden_large_canisters.bbmodel').read_text(encoding='utf-8'))
    bbtexture=Image.open(io.BytesIO(base64.b64decode(bb['textures'][0]['source'].split(',',1)[1]))).convert('RGBA')
    assert tex.size==(128,128) and tex.tobytes()==bbtexture.tobytes()
    assert len(geo['bones'])==42 and len({b['name'] for b in geo['bones']})==42
    faces=0
    for b in geo['bones']:
        for c in b.get('cubes',[]):
            assert len(c['uv'])==6
            for face,f in c['uv'].items():
                x,y=f['uv'];w,h=f['uv_size'];x1,x2=sorted([x,x+w]);y1,y2=sorted([y,y+h])
                assert 0<=x1<x2<=128 and 0<=y1<y2<=128
                assert tex.crop((math.floor(x1),math.floor(y1),math.ceil(x2),math.ceil(y2))).getextrema()[3]==(255,255),(b['name'],face)
                faces+=1
    assert faces==378
    for rel in ['geo/critical_warden_armor.geo.json','animations/critical_warden_armor.animation.json','textures/models/armor/critical_warden.png']:
        assert z.read(f'assets/{ns}/{rel}')==(ROOT/f'src/main/resources/assets/{ns}/{rel}').read_bytes()
source=(ROOT/'src/main/java/cn/wanyiwige/alexcavesspellsbook/toxic/CriticalWardenArmorItem.java').read_text(encoding='utf-8')
assert 'slot != getEquipmentSlot()' in source
assert 'UUID.nameUUIDFromBytes' in source and 'getType().getName()' in source
stats={}
for attr,operation,total in [('POWER','MULTIPLY_BASE',.5),('MAX_MANA','ADDITION',400),('MANA_REGEN','MULTIPLY_BASE',.10),('RESIST','MULTIPLY_BASE',.16)]:
    match=re.search(r'\.'+attr+r'\.get\(\), new double\[\]\{([^}]+)\}\[index\], AttributeModifier.Operation\.'+operation,source)
    assert match,attr
    values=[float(v.strip()) for v in match[1].split(',')];assert len(values)==4 and abs(sum(values)-total)<1e-8
    stats[attr]={'per_piece':values,'total':total,'operation':operation}
for log in ['build-fallback.log','native-bake.log']:
    text=(CHECK/log).read_text(encoding='utf-8-sig')
    assert 'BUILD SUCCESSFUL' in text
assert '> Task :reobfJar' in (CHECK/'build-fallback.log').read_text(encoding='utf-8-sig')
assert 'PASS: native GeckoLib parse and bake' in (CHECK/'native-bake.log').read_text(encoding='utf-8-sig')
report={'status':'PASS','version':VERSION,'jar':str(JAR),'bytes':JAR.stat().st_size,'sha256':sha(JAR),'icons':icons,'attributes':stats,'geometry_bones':42,'cubes':63,'opaque_uv_faces':378,'armor_texture_matches_approved_model':True,'native_bake_and_class_link':'PASS','changed_existing_jar_entries':sorted(changed),'added_jar_entries':len(added),'removed_jar_entries':0,'game_runtime_tested':False,'deployed':False}
(CHECK/'verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
(ART/'verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
