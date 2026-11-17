"""Release/resource and installed-JAR integration audit; does not claim game validation."""
from pathlib import Path
from io import BytesIO
import json,hashlib,re,subprocess,tomllib,zipfile
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
INSTANCE=ROOT.parent
OUT=ROOT/'development-checks/toxic-audit'
RES=ROOT/'src/main/resources'
PREFIX='assets/alex_caves_spellsbook/'
PACKAGE='cn/wanyiwige/alexcavesspellsbook/'
JAR=ROOT/'build/libs/wans_iron_expansion-1.2.jar'
JAVAP=Path('C:/Program Files/Microsoft/jdk-17.0.15.6-hotspot/bin/javap.exe')
SPELLS=['gamma_jet','waste_drum_cast','raycat_purification','proton_lance','critical_fission','reactor_ward']
ENTITIES=['gamma_jet_effect','waste_drum_spell','raycat_purification_effect','proton_lance_effect','critical_fission_effect','reactor_ward_effect']
MIGRATIONS=['traveloptics:annihilation','spirit_mediumship_spell:mediumship_alexcaves_brainiac',
 'spirit_mediumship_spell:mediumship_alexcaves_tremorzilla','alex_caves_spellsbook:radium_bolt',
 'alex_caves_spellsbook:corrosive_cloud','irons_spellbooks:acid_orb','irons_spellbooks:poison_breath',
 'irons_spellbooks:poison_splash','irons_spellbooks:poison_arrow','dacxirons:acid_ball_spell']
def sha(data):return hashlib.sha256(data).hexdigest()
def readjson(path):return json.loads(path.read_text(encoding='utf-8-sig'))
inputs=readjson(OUT/'migration-inputs.json')['inputs']
for row in inputs:
    path=INSTANCE/'mods'/row['jar']
    assert sha(path.read_bytes())==row['sha256'].lower(),path
def installed(part):return next(INSTANCE/'mods'/r['jar'] for r in inputs if part in r['jar'])
def javap(jar,cls):
    result=subprocess.run([str(JAVAP),'-c','-p','-s','-classpath',str(jar),cls],capture_output=True,encoding='utf-8',errors='replace',check=True)
    (OUT/'migration-bytecode'/('release-'+cls.rsplit('.',1)[-1]+'.txt')).write_text(result.stdout,encoding='utf-8')
    return result.stdout
def method(text,name):
    found=[m for m in re.finditer(r'^  (?:public|private|protected).+?\b'+re.escape(name)+r'\([^\n]*\);\n',text,re.M)]
    assert len(found)==1,(name,len(found))
    start=found[0].start();tail=text[found[0].end():]
    end=re.search(r'^  (?:public|private|protected|static)\b',tail,re.M)
    return text[start:found[0].end()+(end.start() if end else len(tail))]
targets=[]
def target(jar,cls,methods,invoke=None):
    text=javap(jar,cls)
    for name,descriptor in methods.items():assert 'descriptor: '+descriptor in method(text,name),(cls,name,descriptor)
    result={'class':cls,'methods':methods}
    if invoke:
        name,needle=invoke;count=method(text,name).count(needle);assert count==1,(cls,name,count);result['redirect_matches']=count
    targets.append(result)
iss=installed('irons_spellbooks-');sms=installed('spirit_mediumship_spell-')
target(iss,'io.redspace.ironsspellbooks.api.config.SpellConfigManager',{'getSpellConfigValue':'(Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;Lio/redspace/ironsspellbooks/api/config/SpellConfigParameter;)Ljava/lang/Object;'})
for cls in ['poison_arrow.PoisonArrow','poison_cloud.PoisonSplash']:
    desc='(Lnet/minecraft/world/phys/Vec3;)V' if cls.endswith('PoisonArrow') else '()V'
    target(iss,'io.redspace.ironsspellbooks.entity.spells.'+cls,{'createPoisonCloud':desc},('createPoisonCloud','net/minecraft/world/level/Level.m_7967_:(Lnet/minecraft/world/entity/Entity;)Z'))
target(iss,'io.redspace.ironsspellbooks.entity.spells.poison_cloud.PoisonCloud',{'applyEffect':'(Lnet/minecraft/world/entity/LivingEntity;)V'})
for cls in ['SummonedBrainiac','SummonedTremorzilla']:
    target(sms,'com.inolia_zaicek.spirit_mediumship_spell.entity.alexcaves.'+cls,{'getSummonSchoolId':'()Ljava/lang/String;','getDamageSchool':'()Ljava/lang/String;'})
target(sms,'com.inolia_zaicek.spirit_mediumship_spell.util.SummonScaling',{'getSchoolPower':'(Lnet/minecraft/world/entity/LivingEntity;Ljava/lang/String;)D','getSchoolPowerValue':'(Lnet/minecraft/world/entity/LivingEntity;Ljava/lang/String;)Ljava/lang/Double;'})
target(sms,'com.inolia_zaicek.spirit_mediumship_spell.event.SummonDamageEventHandler',{'resolveDamageType':'(Ljava/lang/String;)Lnet/minecraft/resources/ResourceKey;','onLivingHurt':'(Lnet/minecraftforge/event/entity/living/LivingHurtEvent;)V'},('onLivingHurt','net/minecraft/world/entity/LivingEntity.m_6469_:(Lnet/minecraft/world/damagesource/DamageSource;F)Z'))
javap(iss,'io.redspace.ironsspellbooks.capabilities.magic.MagicManager')

with zipfile.ZipFile(JAR) as z:
    names=set(z.namelist());assert len(names)==len(z.namelist());assert z.testzip() is None
    metadata=tomllib.loads(z.read('META-INF/mods.toml').decode())
    mods={m['modId']:m for m in metadata['mods']};assert mods['alex_caves_spellsbook']['version']=='1.2';assert 'roacw' in mods
    for name in names:
        if name.endswith('.json'):json.loads(z.read(name).decode('utf-8-sig'))
    for path in RES.rglob('*'):
        if path.is_file():
            name=path.relative_to(RES).as_posix()
            if name in {'META-INF/mods.toml','pack.mcmeta'}:continue
            if name=='hazen-recreated-weapons.json' or '/hazen/' in name or name.startswith(PREFIX+'models/item/hazen_'):continue
            assert z.read(name)==path.read_bytes(),name
    for path in (ROOT/'vendor/roacw-1.1.1-unpacked/net/kamaarion/roacw').rglob('*.class'):
        name='net/kamaarion/roacw/'+path.relative_to(ROOT/'vendor/roacw-1.1.1-unpacked/net/kamaarion/roacw').as_posix()
        assert z.read(name)==path.read_bytes(),name
    assert sum(n.startswith('net/kamaarion/roacw/') and n.endswith('.class') for n in names)==98
    source=(ROOT/'src/main/java'/PACKAGE/'toxic/ToxicMigrations.java').read_text()
    assert set(re.findall(r'"([a-z_]+:[a-z_]+)"',source))==set(MIGRATIONS)
    for cls in ['ToxicRegistry','ToxicSpell','GammaJetEntity','WasteDrumSpellEntity','RaycatPurificationEntity','ProtonLanceEntity','CriticalFissionEntity','ReactorWardEntity','ToxicProtection','ToxicMarks','client/ToxicEffectRenderer','client/VanillaDIPixelLight']:
        data=z.read(PACKAGE+'toxic/'+cls+'.class');assert data[:4]==b'\xca\xfe\xba\xbe';assert data[6:8]==b'\x00\x3d'
    mixins=json.loads(z.read('mixins.alex_caves_spellsbook.json'))['mixins']
    for cls in ['ToxicSpellSchoolMixin','ToxicSummonSchoolMixin','ToxicSummonPowerMixin','ToxicSummonDamageMixin','ToxicPoisonCloudOriginMixin','ToxicPoisonCloudDamageMixin']:
        assert cls in mixins;z.read(PACKAGE+'mixin/'+cls+'.class')
    focus=json.loads(z.read('data/alex_caves_spellsbook/tags/items/toxic_focus.json'))
    assert focus['values']==['alexscaves:fissile_core']
    images={}
    def checkimage(name,size,opaque=False):
        raw=z.read(PREFIX+name);im=Image.open(BytesIO(raw)).convert('RGBA');assert im.size==(size,size),name
        alpha=set(im.getchannel('A').getdata());assert alpha=={255} if opaque else alpha<={0,255},(name,alpha)
        meta=json.loads(z.read(PREFIX+name+'.mcmeta'));assert meta['texture']['blur'] is False
        images[name]={'size':im.size,'alpha_values':sorted(alpha),'colors':len(im.getcolors(size*size) or []),'sha256':sha(raw)}
    for id in SPELLS:checkimage('textures/gui/spell_icons/'+id+'.png',64,True)
    checkimage('textures/item/scroll_toxic.png',64)
    assert images['textures/item/scroll_toxic.png']['colors']<=17
    for name in ['radiation_mark','spark','puddle','splat','shield_panel','burst','beam_segment','pulse','smoke','cleanse']:checkimage('textures/entity/toxic/'+name+'.png',32)
    models={}
    for prop,export,cubes in [('toxic_waste_prop','toxic_waste_cuboid_final',14),('toxic_reactor_prop','toxic_reactor_emitter_detailed',9)]:
        model=json.loads(z.read(PREFIX+'models/item/'+prop+'.json'))
        original=readjson(ROOT/'artwork/toxic-models'/f'{export}.json')
        assert model['elements']==original['elements'];assert len(model['elements'])==cubes
        assert 'parent' not in model;assert 'format_version' not in model
        for e in model['elements']:
            assert all(e['to'][a]>e['from'][a] for a in range(3)),e['name']
            assert set(e['faces'])=={'north','south','east','west','up','down'},e['name']
            for f in e['faces'].values():
                assert all(0<=v<=16 for v in f['uv']);assert f['uv'][0]!=f['uv'][2] and f['uv'][1]!=f['uv'][3],e['name']
                texture=model['textures'][f['texture'].removeprefix('#')]
                ns,path=texture.split(':');im=Image.open(BytesIO(z.read(f'assets/{ns}/textures/{path}.png')))
                assert im.size==(128,128) and len(im.getcolors(16384) or [])>10,(prop,texture)
        models[prop]={'closed_volumes':cubes,'mapped_faces':cubes*6,'source_editor_export':export+'.bbmodel'}
        if prop=='toxic_waste_prop':
            body=next(e for e in model['elements'] if e['name']=='silver_cuboid_body')
            assert body['to'][0]-body['from'][0]==body['to'][2]-body['from'][2]
            assert not any('handle' in e['name'] for e in model['elements'])
            band=next(e for e in model['elements'] if e['name']=='warning_band')
            assert band['to'][1]-band['from'][1]>=6
            assert len({band['faces'][f]['texture'] for f in ['north','south','east','west']})==4
    for lang in ['zh_cn','en_us']:
        data=json.loads(z.read(PREFIX+'lang/'+lang+'.json'))
        for s in SPELLS:assert data['spell.alex_caves_spellsbook.'+s] and data['spell.alex_caves_spellsbook.'+s+'.guide']
        for e in ENTITIES:assert data['entity.alex_caves_spellsbook.'+e]
    old=INSTANCE/'mods/wans_iron_expansion-1.1.17.jar'
    with zipfile.ZipFile(old) as previous:
        assert not {n for n in previous.namelist() if not n.endswith('/')}-names,'Previous entries removed'
    built_names=sorted(n for n in names if n.startswith(PACKAGE+'toxic/'))

baseline=readjson(ROOT/'development-backups/toxic-1.2-before-20261015-200853/source-sha256.json')
changed=[]
for name,h in baseline.items():
    path=ROOT/name;assert path.exists(),name
    if sha(path.read_bytes())!=h.lower():changed.append(name.replace('\\','/'))
allowed={'src/main/java/'+PACKAGE+'AlexCavesSpellsbook.java','src/main/java/'+PACKAGE+'registry/ModCreativeTabs.java',
 'src/main/resources/mixins.alex_caves_spellsbook.json','src/main/resources/data/neoforge/tags/damage_type/is_magic.json',
 'src/main/resources/'+PREFIX+'lang/zh_cn.json','src/main/resources/'+PREFIX+'lang/en_us.json','gradle.properties'}
assert not set(changed)-allowed,changed
report={'version':'1.2','artifact':str(JAR),'bytes':JAR.stat().st_size,'sha256':sha(JAR.read_bytes()),
 'migrated_spell_ids':MIGRATIONS,'new_spells':SPELLS,'checked_installed_inputs':len(inputs),
 'injection_targets':targets,'models':models,'images':images,'changed_baseline_files':changed,
 'roacw_classes_preserved':98,'new_toxic_class_entries':built_names,
 'build_reobfuscated':'> Task :reobfJar' in (OUT/'build.log').read_text(encoding='utf-8-sig'),
 'deployed':False,'game_launch_verified':False,'gameplay_visual_multiplayer_verified':False,
 'validation_scope':'JAR CRC, metadata, source/resource identity, texture alpha/UV coverage, model export identity, installed target signatures and redirect match counts. No live Mixin transformation or game execution.'}
(OUT/'release-validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({k:report[k] for k in ['artifact','sha256','new_spells','changed_baseline_files','models','deployed','game_launch_verified']},ensure_ascii=False,indent=2))
