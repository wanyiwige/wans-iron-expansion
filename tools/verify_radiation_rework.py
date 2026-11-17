"""Inspect final bytes, native pixel grids, six-face UV coverage and regression evidence."""
from pathlib import Path
from io import BytesIO
from PIL import Image
import hashlib,json,shutil,tomllib,zipfile

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'outputs/radiation-rework-20261022'
VERSION='1.2.4-radiation-rework.1'
BUILT=ROOT/f'build/libs/wans_iron_expansion-{VERSION}.jar'
JAR=OUT/BUILT.name
BASE='assets/alex_caves_spellsbook/'
SPELLS=('isotope_brand','neutron_rebound','moderation_field','lead_curtain','chain_reaction','radiation_film','cloud_chamber_trail','cherenkov_wake','decay_transposition','critical_matrix')
SPRITES=('isotope','neutron','field','curtain','chain','film','cloud','wake','shell','matrix','impact')

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()

OUT.mkdir(parents=True,exist_ok=True)
assert BUILT.is_file()
shutil.copy2(BUILT,JAR)
assert sha(BUILT)==sha(JAR)
log=(OUT/'build-and-checks.log').read_text(encoding='utf-8-sig',errors='replace')
for evidence in ('BUILD SUCCESSFUL', '> Task :reobfJar', 'PASS RADIATION_RULES 836 checks', 'PASS: 501 progression checks', 'ATLAS_DISCOVERY expected=11 discovered=10 missing=1', 'ATLAS_DISCOVERY expected=11 discovered=11 missing=0'):
    assert evidence in log,evidence

coverage={}
with zipfile.ZipFile(JAR) as z,zipfile.ZipFile(OUT/'before-preview.2.jar') as before,zipfile.ZipFile(OUT/'source-before-rework.zip') as source:
    assert z.testzip() is None
    mods=tomllib.loads(z.read('META-INF/mods.toml').decode('utf-8-sig'))['mods']
    assert len(mods)==1 and mods[0]['modId']=='alex_caves_spellsbook' and mods[0]['version']==VERSION
    for cls in ('RadiationNewEffectEntity','RadiationRules','ToxicMarks','ToxicSpell','client/RadiationEffectRenderer','client/RadiationHeadMarks'):
        assert f'cn/wanyiwige/alexcavesspellsbook/toxic/{cls}.class' in z.namelist(),cls
    for cls in ('TwilightEquipment','TwilightEquipment$Book','TwilightEquipment$FieryBook','TwilightEquipment$Weapon','TwilightEquipment$Scepter'):
        name=f'cn/wanyiwige/twilight/{cls}.class'
        assert z.read(name)==before.read(name),f'Twilight startup fix changed: {cls}'
    for spell in SPELLS:
        name=BASE+f'textures/gui/spell_icons/{spell}.png'
        image=Image.open(BytesIO(z.read(name))).convert('RGBA')
        assert image.size==(64,64) and image.getchannel('A').getextrema()==(255,255),spell
        assert len(image.getcolors(4096))<=21,spell
        for y in range(0,64,2):
            for x in range(0,64,2):
                assert all(image.getpixel((x+dx,y+dy))==image.getpixel((x,y)) for dx in (0,1) for dy in (0,1)),(spell,x,y)
        assert z.read(name)!=before.read(name),f'Icon not redrawn: {spell}'
    for sprite in SPRITES:
        name=BASE+f'textures/entity/toxic/rework/{sprite}.png'
        image=Image.open(BytesIO(z.read(name))).convert('RGBA');size=16 if sprite=='matrix' else 32
        assert image.size==(size,size*4),sprite
        meta=json.loads(z.read(name+'.mcmeta'))
        assert meta['texture']=={'blur':False,'clamp':True} and 'animation' not in meta,sprite
        assert set(image.getchannel('A').getdata())<={0,255},sprite
        for frame in range(4):
            tile=image.crop((0,frame*size,size,(frame+1)*size));alpha=list(tile.getchannel('A').getdata())
            assert 0<sum(a>0 for a in alpha)<len(alpha),f'Invalid transparent frame {sprite} {frame}'
        coverage[sprite]=round(sum(a>0 for a in image.getchannel('A').getdata())/(size*size*4),3)
    assert coverage['field']<.4 and coverage['curtain']<.3 and coverage['cloud']<.25,coverage
    assert Image.open(BytesIO(z.read(BASE+'textures/entity/toxic/rework/pixel.png'))).size==(1,1)
    metal=Image.open(BytesIO(z.read(BASE+'textures/entity/toxic/lead_curtain_metal.png'))).convert('RGBA')
    assert metal.size==(64,64) and metal.getchannel('A').getextrema()==(255,255)
    model=json.loads(z.read(BASE+'models/item/toxic_lead_prop.json'))
    old_model=json.loads(source.read('src/main/resources/'+BASE+'models/item/toxic_lead_prop.json'))
    geometry=lambda m:[(e['name'],e['from'],e['to']) for e in m['elements']]
    assert geometry(model)==geometry(old_model) and len(model['elements'])==9
    faces=0
    for element in model['elements']:
        dx,dy,dz=[b-a for a,b in zip(element['from'],element['to'])]
        assert min(dx,dy,dz)>0
        assert set(element['faces'])=={'north','south','east','west','up','down'}
        for side,data in element['faces'].items():
            u,v,r,b=data['uv'];assert 0<=u<r<=16 and 0<=v<b<=16
            w,h=(dx,dy) if side in ('north','south') else (dz,dy) if side in ('east','west') else (dx,dz)
            assert abs(r-u)==w and abs(b-v)==h,(element['name'],side,'unequal texel density')
            assert data['texture']=='#0';faces+=1
    atlas=json.loads(z.read('assets/minecraft/atlases/blocks.json'))
    old_atlas=json.loads(source.read('src/main/resources/assets/minecraft/atlases/blocks.json'))
    assert atlas['sources'][:len(old_atlas['sources'])]==old_atlas['sources']
    assert sum(s.get('resource')=='alex_caves_spellsbook:entity/toxic/lead_curtain_metal' for s in atlas['sources'])==1
    for language in ('zh_cn','en_us'):
        data=json.loads(z.read(BASE+f'lang/{language}.json'))
        old=json.loads((OUT/f'{language}-before.json').read_text(encoding='utf-8'))
        for spell in SPELLS:
            assert data[f'spell.alex_caves_spellsbook.{spell}.guide']!=old.get(f'spell.alex_caves_spellsbook.{spell}.guide')
        allowed=lambda k: k.startswith('ui.alex_caves_spellsbook.toxic.new.') or k.startswith('ui.alex_caves_spellsbook.toxic.detail.') or k.startswith('ui.alex_caves_spellsbook.radiation.') or k=='effect.alex_caves_spellsbook.radiation_mark' or any(k==f'spell.alex_caves_spellsbook.{s}.guide' for s in SPELLS)
        assert all(data.get(k)==v for k,v in old.items() if not allowed(k)),'Unrelated translation changed'

report={'jar':str(JAR),'sha256':sha(JAR),'bytes':JAR.stat().st_size,'version':VERSION,
        'spell_count':10,'redrawn_opaque_icons':10,'animated_world_sprites':11,'frames_per_sprite':4,
        'sprite_opaque_coverage':coverage,'preserved_blockbench_elements':9,'equal_density_textured_faces':faces,
        'rules_checks':836,'progression_checks':501,'native_atlas_before':{'expected':11,'discovered':10,'missing':1},
        'native_atlas_after':{'expected':11,'discovered':11,'missing':0},'jar_crc':'pass',
        'game_started':False,'gameplay_verified':False,'gpu_render_verified':False,'deployed':False}
(OUT/'verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
