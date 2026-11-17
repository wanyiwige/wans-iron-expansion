"""Acceptance inventory and release/resource checks for the 21 selected Boss spells."""
from pathlib import Path
import collections
import hashlib
import json
import re
import tomllib
import zipfile
import io
from PIL import Image

project=Path(__file__).resolve().parents[1]
instance=project.parent
jar=project/'build/libs/wans_iron_expansion-1.1.10.jar'
folder=project/'src/main/java/cn/wanyiwige/alexcavesspellsbook/foolish'
catalog=(folder/'BossSpellSpec.java').read_text(encoding='utf-8')
entries=re.findall(r'^\s+([A-Z_]+)\("([^"]+)", "([^"]+)", "([^"]+)", (\d+), (\d+), (\d+), (\d+), (\d+)\)',catalog,re.M)
expected={'pyre':('irons_spellbooks:fire',2),'thrasher':('traveloptics:aqua',2),
          'varmint_king':('irons_spellbooks:nature',2),'thunderbird':('irons_spellbooks:lightning',3),
          'scarlant_queen':(None,2),'terrorflora':('irons_spellbooks:nature',4),'astralis':('kubejs:astral',4)}
assert len(entries)==19 and len({e[1] for e in entries})==19
for boss,(school,count) in expected.items():
    selected=[e for e in entries if e[2]==boss]
    assert len(selected)==count,(boss,selected)
    if school:assert {e[3] for e in selected}=={school}
assert {e[1]:e[3] for e in entries if e[2]=='scarlant_queen'}=={
    'scarlant_capsule_spell':'entomology_spell:insect','scarlet_edict':'irons_spellbooks:blood'}
assert {e[1] for e in entries if e[2]=='astralis'}=={
    'astral_chain_slash','meteor_judgment','seeking_star_blade','falling_star_bomb'}
assets=json.loads((project/'development-checks/foolish-boss-batch/assets.json').read_text(encoding='utf-8'))
icon_manifest=json.loads((project/'development-checks/foolish-boss-icons/generation-manifest.json').read_text(encoding='utf-8-sig'))
assert len(icon_manifest)==21 and len({e['sha256'] for e in icon_manifest})==21
assert len(assets)==19 and {e['id'].split(':')[1] for e in assets}=={e[1] for e in entries}
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
with zipfile.ZipFile(jar) as z,zipfile.ZipFile(instance/'mods/foolish-1.3.0 patched.jar') as original:
    assert len(z.namelist())==len(set(z.namelist()))
    metadata=tomllib.loads(z.read('META-INF/mods.toml').decode())
    assert metadata['mods'][0]['version']=='1.1.10'
    deps=metadata['dependencies']['alex_caves_spellsbook']
    assert {'foolish','gtbcs_geomancy_plus','entomology_spell','traveloptics'}<= {e['modId'] for e in deps if e['mandatory']}
    for entry in assets:
        icon='assets/alex_caves_spellsbook/textures/gui/spell_icons/'+entry['id'].split(':')[1]+'.png'
        assert hashlib.sha256(z.read(icon)).hexdigest()==entry['icon_sha256']
        for lang in ['zh_cn','en_us']:
            data=json.loads(z.read('assets/alex_caves_spellsbook/lang/'+lang+'.json'))
            key='spell.'+entry['id'].replace(':','.')
            assert key in data and key+'.guide' in data
            assert 'itemGroup.alex_caves_spellsbook.foolish_boss_spells' in data
    for entry in icon_manifest:
        icon='assets/alex_caves_spellsbook/textures/gui/spell_icons/'+entry['id']+'.png'
        assert z.read(icon)==(project/'src/main/resources'/icon).read_bytes()
        assert hashlib.sha256(z.read(icon)).hexdigest()==entry['sha256']
        with Image.open(io.BytesIO(z.read(icon))) as image:
            assert image.size==(32,32)
            assert image.convert('RGBA').getchannel('A').getextrema()==(255,255),entry['id']
    for model,texture,animation in [
        ('stormmine','blanker','animation.stormmine.idle'),('seedbomb','seedbomb','animation.seedbomb.fly'),
        ('meteor','meteor','animation.meteor.spin'),('newstarblade','newstarblade','animation.starblade.spin'),
        ('starbomb','blanker','animation.starbomb.spin'),('scarlant','ants_ants_were_the_ants','animation.scarlant.walk'),
        ('scarlantassistant','scarlantassistant','animation.scarlantassistant.walk'),
        ('scarlantsentinel','antguard','animation.scarlantsentinel.walk'),('terrorfloraleaf','leaf','animation.vine.idle'),
        ('newterrorfloravine','vine','animation.vine.tangleidle')]:
        assert 'assets/foolish/geo/'+model+'.geo.json' in original.namelist()
        assert 'assets/foolish/textures/entities/'+texture+'.png' in original.namelist()
        assert animation in json.loads(original.read('assets/foolish/animations/'+model+'.animation.json'))['animations']
    for p in folder.rglob('*.java'):
        name='cn/wanyiwige/alexcavesspellsbook/foolish/'+str(p.relative_to(folder)).replace('\\','/').replace('.java','.class')
        assert name in z.namelist(),name
        assert jar.stat().st_mtime>=p.stat().st_mtime,'Stale class: '+name
    for name in z.namelist():
        if name.endswith('.json'):json.loads(z.read(name).decode('utf-8-sig'))
    json_count=sum(n.endswith('.json') for n in z.namelist())
spells=[{'id':'alex_caves_spellsbook:'+e[1],'boss':'foolish:'+e[2],'school':e[3],
         'base_power':[int(e[4])+int(e[5])*i for i in range(3)],'levels':[1,2,3],'rarity':'legendary','drop_level':1} for e in entries]
spells.extend({'id':'alex_caves_spellsbook:'+id,'boss':'foolish:sacilic_abomination','school':'gtbcs_geomancy_plus:geo',
               'levels':[1,2,3],'rarity':'legendary','drop_level':1} for id in ['sacilite_rain','seeking_sacilite_swarm'])
out=project/'development-checks/foolish-boss-batch/release-verification.json'
report={'artifact':str(jar),'sha256':sha(jar),'bytes':jar.stat().st_size,'spells':spells,'spell_count':len(spells),
        'creative_scroll_count':len(spells)*3,'json_files_checked':json_count,'external_models_checked':10,
        'opaque_pixel_icons_checked':len(icon_manifest),'icon_size':[32,32],
        'deployed':False,'runtime_verified':False,'source_hashes':{str(p.relative_to(project)):sha(p) for p in folder.rglob('*.java')}}
out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:report[k] for k in ['sha256','spell_count','creative_scroll_count','json_files_checked','external_models_checked']},ensure_ascii=False))
