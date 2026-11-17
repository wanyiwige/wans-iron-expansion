"""Validate complete item/data wiring and strict JAR scope against candy preview.10."""
from pathlib import Path
from zipfile import ZipFile
from PIL import Image
import json,hashlib,re
root=Path(__file__).resolve().parents[1];art=root/'artwork/candy-rune-orb-20261017'
version='1.2.1-candy-pixel-preview.11';jar=root/f'build/libs/wans_iron_expansion-{version}.jar'
old=root/'build/libs/wans_iron_expansion-1.2.1-candy-pixel-preview.10.jar'
namespace='alex_caves_spellsbook';assets='assets/'+namespace+'/';data='data/'+namespace+'/'
def read(path):return json.loads((root/'src/main/resources'/path).read_text(encoding='utf-8'))
new_resources={data+'irons_spellbooks/upgrade_orb_type/candy_power.json'}
for item in ['candy_rune','candy_upgrade_orb']:
    new_resources|={assets+'models/item/'+item+'.json',assets+'textures/item/'+item+'.png',assets+'textures/item/'+item+'.png.mcmeta',data+'recipes/'+item+'.json',data+'advancements/recipes/'+item+'.json'}
    model=read(assets+'models/item/'+item+'.json');assert model=={'parent':'minecraft:item/generated','textures':{'layer0':namespace+':item/'+item}}
    im=Image.open(root/'src/main/resources'/assets/f'textures/item/{item}.png').convert('RGBA')
    assert im.size==(16,16) and set(im.getchannel('A').getdata())=={0,255}
    assert read(assets+f'textures/item/{item}.png.mcmeta')['texture']['blur'] is False
    recipe=read(data+'recipes/'+item+'.json');assert recipe['type']=='minecraft:crafting_shaped'
    assert recipe['result']=={'item':namespace+':'+item,'count':1}
    assert read(data+'advancements/recipes/'+item+'.json')['rewards']['recipes']==[namespace+':'+item]
    for language in ['zh_cn','en_us']:assert read(assets+'lang/'+language+'.json')['item.'+namespace+'.'+item]
power=read(data+'irons_spellbooks/upgrade_orb_type/candy_power.json')
assert power=={'amount':.05,'attribute':namespace+':candy_spell_power','containerItem':{'Count':1,'id':namespace+':candy_upgrade_orb'},'operation':'MULTIPLY_BASE'}
assert namespace+':candy_rune' in read('data/irons_spellbooks/tags/items/inscribed_rune.json')['values']
rune=read(data+'recipes/candy_rune.json');orb=read(data+'recipes/candy_upgrade_orb.json')
assert rune['key']=={'F':{'tag':namespace+':candy_focus'},'R':{'item':'irons_spellbooks:blank_rune'}}
assert rune['pattern']==['FFF','FRF','FFF'] and orb['pattern']==['RRR','ROR','RRR']
assert orb['key']=={'R':{'item':namespace+':candy_rune'},'O':{'item':'irons_spellbooks:upgrade_orb'}}
assert read(data+'tags/items/candy_focus.json')['values']==['alexscaves:radiant_essence']
source=root/'src/main/java/cn/wanyiwige/alexcavesspellsbook/registry'
items=(source/'ModItems.java').read_text(encoding='utf-8');keys=(source/'ModUpgradeOrbTypes.java').read_text();tabs=(source/'ModCreativeTabs.java').read_text(encoding='utf-8')
assert 'CANDY_RUNE = register("candy_rune")' in items
assert re.search(r'CANDY_UPGRADE_ORB.*?UpgradeOrbItem\(new Item.Properties\(\), ModUpgradeOrbTypes.CANDY_POWER\)',items,re.S)
assert '"candy_power"' in keys and 'CANDY_POWER' in keys
assert 'output.accept(ModItems.CANDY_RUNE.get())' in tabs and 'output.accept(ModItems.CANDY_UPGRADE_ORB.get())' in tabs
base=json.loads((art/'source-baseline.json').read_text())
changed_sources=[p for p,h in base.items() if hashlib.sha256((root/p).read_bytes()).hexdigest()!=h]
allowed_source={str(Path('src/main/java/cn/wanyiwige/alexcavesspellsbook/registry')/(name+'.java')) for name in ['ModItems','ModCreativeTabs','ModUpgradeOrbTypes']}
allowed_source|={str(Path('src/main/resources')/p) for p in [assets+'lang/zh_cn.json',assets+'lang/en_us.json','data/irons_spellbooks/tags/items/inscribed_rune.json']}
assert set(changed_sources)==allowed_source,changed_sources
with ZipFile(old) as a,ZipFile(jar) as b:
    before=set(a.namelist());after=set(b.namelist());assert not before-after and b.testzip() is None
    changed=sorted(n for n in before&after if a.read(n)!=b.read(n))
    allowed={'META-INF/MANIFEST.MF','META-INF/mods.toml',assets+'lang/zh_cn.json',assets+'lang/en_us.json','data/irons_spellbooks/tags/items/inscribed_rune.json'}
    allowed|={'cn/wanyiwige/alexcavesspellsbook/registry/'+name+'.class' for name in ['ModItems','ModCreativeTabs','ModUpgradeOrbTypes']}
    assert set(changed)<=allowed,changed
    added=sorted(after-before);assert {n for n in added if not n.endswith('/')}==new_resources,added
    for p in new_resources|{assets+'lang/zh_cn.json',assets+'lang/en_us.json','data/irons_spellbooks/tags/items/inscribed_rune.json'}:assert b.read(p)==(root/'src/main/resources'/p).read_bytes()
    for lang in ['zh_cn','en_us']:
        prior=json.loads(a.read(assets+'lang/'+lang+'.json'));updated=json.loads(b.read(assets+'lang/'+lang+'.json'))
        assert all(updated[k]==v for k,v in prior.items())
        assert set(updated)-set(prior)=={'item.'+namespace+'.candy_rune','item.'+namespace+'.candy_upgrade_orb'}
    prior=json.loads(a.read('data/irons_spellbooks/tags/items/inscribed_rune.json'))
    updated=json.loads(b.read('data/irons_spellbooks/tags/items/inscribed_rune.json'));assert set(prior['values'])<=set(updated['values'])
    native=json.loads((art/'iron-reference.json').read_text())
    with ZipFile(native['jar']) as n:
        original=json.loads(n.read('data/irons_spellbooks/irons_spellbooks/upgrade_orb_type/fire_power.json'))
        assert original['amount']==power['amount'] and original['operation']==power['operation']
        assert 'assets/irons_spellbooks/models/item/blank_rune.json' in n.namelist() and 'assets/irons_spellbooks/models/item/upgrade_orb.json' in n.namelist()
    for p in before:
        if p.startswith('cn/wanyiwige/alexcavesspellsbook/candy/') or p.startswith(assets+'textures/entity/candy/') or p.startswith(assets+'models/candy/'):
            assert a.read(p)==b.read(p),p
baseline=json.loads((root/'artwork/candy-first-five-20261017/baseline.json').read_text())
assert hashlib.sha256((root.parent/baseline['installed']).read_bytes()).hexdigest()==baseline['sha256']
log=(art/'build.log').read_text(encoding='utf-8',errors='replace');assert 'BUILD SUCCESSFUL' in log and '> Task :reobfJar' in log
report={'result':'PASS','version':version,'items':['alex_caves_spellsbook:candy_rune','alex_caves_spellsbook:candy_upgrade_orb'],
    'textures':'two 16x16 pixel sprites','upgradeAttribute':power['attribute'],'upgradeAmount':power['amount'],'operation':power['operation'],
    'recipesAndUnlocks':True,'creativeTab':True,'runeTag':True,'all26SpellClassesAndAssetsUnchanged':True,'previousLangValuesPreserved':True,
    'changedEntries':changed,'addedEntries':added,'jarBytes':jar.stat().st_size,'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),
    'installedUnchanged':True,'deployed':False,'minecraftRuntimeTested':False}
(art/'verification.json').write_text(json.dumps(report,indent=2),encoding='utf-8');print(json.dumps(report,indent=2))
