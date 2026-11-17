"""Release/resource checks. This does not claim in-game behavior or render acceptance."""
from pathlib import Path
import hashlib,json,re,struct,zipfile
P=Path(__file__).resolve().parents[1]
source=P.parent/'1.21.1 移植/asterismarcanum-1.21.1-0.1.0.jar'
release=P/'build/libs/wans_iron_expansion-1.2.1-astral-preview.1.jar'
baseline=P.parent/'mods/wans_iron_expansion-1.2.1-hazen-preview.9.jar'
legacy=['starfire','brightburst','star_swarm','luminous_beam','piercing_light']
behaviors=['starfire','brightburst','star_swarm','luminous_beam','piercing_light','starcutter','silvery_barbs','celestial_tether','astral_echo']
new=['arcanum_'+x if i<6 else x for i,x in enumerate(behaviors)]
checks=[]
def check(name,result):
    assert result,name
    checks.append(name)
sha=lambda b:hashlib.sha256(b).hexdigest()
with zipfile.ZipFile(source) as original,zipfile.ZipFile(release) as z,zipfile.ZipFile(baseline) as base:
    check('ZIP integrity',z.testzip() is None)
    png=original.read('assets/asterismarcanum/textures/item/scroll_astral.png')
    check('Upstream scroll retained byte-for-byte',z.read('assets/asterismarcanum/textures/item/scroll_astral.png')==png)
    check('Old kubejs scroll texture also replaced byte-for-byte',z.read('assets/kubejs/textures/item/scroll_astral.png')==png)
    for model in ['assets/kubejs/models/item/scroll_astral.json','assets/asterismarcanum/models/item/arcanum_scroll_astral.json']:
        check(model,json.loads(z.read(model))['textures']['layer0']=='asterismarcanum:item/scroll_astral')
    mixins=json.loads(z.read('astral_spell_compat.mixins.json'))
    check('Unique-model mixin enabled','AstralScrollModelMixin' in mixins['client'])
    check('Model pre-bake registration',b'item/arcanum_scroll_astral' in z.read('cn/wanyiwige/astralcompat/client/ClientRegistration.class'))
    render_registration=z.read('cn/wanyiwige/astralcompat/client/ClientRegistration.class')
    check('Both new entity renderers registered',all(name in render_registration for name in [b'ArcanumEffectRenderer',b'ArcanumProjectileRenderer',b'ARCANUM_EFFECT',b'ARCANUM_PROJECTILE']))
    check('Legacy close slash cast removed',b'castStarcutter' not in z.read('cn/wanyiwige/astralcompat/events/AstralSpellEvents.class'))
    check('Same school as legacy spells',b'ASTRAL_RESOURCE' in z.read('cn/wanyiwige/astralcompat/recreation/RecreatedAstralSpell.class'))
    for locale in ['en_us','zh_cn']:
        d=json.loads(z.read(f'assets/asterismarcanum/lang/{locale}.json'))
        names=[d['spell.asterismarcanum.'+id] for id in legacy+new]
        check(locale+' all 14 display names distinct',len(set(names))==14)
        check(locale+' no legacy Starcutter name','spell.asterismarcanum.starcutter' not in d)
    registrations=(P/'src/main/java/cn/wanyiwige/astralcompat/registry/AstralSpells.java').read_text(encoding='utf-8')
    registered=re.findall(r'= (?:register|recreated)\("([a-z_]+)"',registrations)
    check('Exactly five legacy plus nine new registrations',set(registered)==set(legacy+new) and len(registered)==14)
    tabs=(P/'src/main/java/cn/wanyiwige/astralcompat/registry/ModCreativeTabs.java').read_text(encoding='utf-8')
    check('No retired spell in creative tab','"asterismarcanum:starcutter"' not in tabs)
    for id,behavior in zip(new,behaviors):
        icon=f'assets/asterismarcanum/textures/gui/spell_icons/{id}.png'
        check(id+' original icon',z.read(icon)==original.read(f'assets/asterismarcanum/textures/gui/spell_icons/{behavior}.png'))
    # Source diff and bytecode diff establish that the retained five implementations survived.
    for cls in ['entity/AstralAnchorEntity','entity/AstralBoltEntity','entity/PiercingStarEntity','client/AstralAnchorRenderer','client/AstralBoltRenderer','client/PiercingStarRenderer','spells/AstralSpell','registry/AstralSchools','registry/AstralAttributes']:
        n=f'cn/wanyiwige/astralcompat/{cls}.class'
        check('Unchanged legacy '+cls,z.read(n)==base.read(n))
    for cls in ['ArcanumProjectile','ArcanumEffect','ArcanumEvents','RecreatedAstralSpell']:
        n=f'cn/wanyiwige/astralcompat/recreation/{cls}.class'
        data=z.read(n)
        check(cls+' Java17',struct.unpack('>H',data[6:8])[0]==61)
        check(cls+' server-safe dependencies',b'net/minecraft/client/' not in data)
    for n in z.namelist():
        if n.endswith('.json') and (n.startswith('assets/asterismarcanum/') or n=='astral_spell_compat.mixins.json'):
            json.loads(z.read(n))
    check('No upstream 1.21 classes imported',not any(n.startswith('com/birdie/') and n.endswith('.class') for n in z.namelist()))
    vendor=[n for n in base.namelist() if n.startswith('net/kamaarion/roacw/') and n.endswith('.class')]
    check('All 98 ROACW classes unchanged',len(vendor)==98 and all(z.read(n)==base.read(n) for n in vendor))
    changed=[n for n in set(base.namelist())&set(z.namelist()) if base.read(n)!=z.read(n)]
    allowed=['cn/wanyiwige/astralcompat/','assets/asterismarcanum/','assets/kubejs/models/item/scroll_astral.json','assets/kubejs/textures/item/scroll_astral.png','META-INF/','THIRD_PARTY_NOTICES.md','astral_spell_compat.mixins.json']
    check('Existing changed entries confined to intended scope',all(any(n.startswith(a) for a in allowed) for n in changed))
report={'release':str(release),'size':release.stat().st_size,'sha256':sha(release.read_bytes()),'source_sha256':sha(source.read_bytes()),'baseline_sha256':sha(baseline.read_bytes()),'scroll_sha256':sha(png),'checks':checks,'changed_existing_entries':sorted(changed),'added_entries':sorted(set(z.namelist())-set(base.namelist())),'deployment':'not deployed','gameplay':'not tested','visuals':'not tested'}
out=P/'development-records/asterism-recreation';out.mkdir(parents=True,exist_ok=True)
(out/'verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'checks_passed':len(checks),'size':report['size'],'sha256':report['sha256'],'report':str(out/'verification.json')},ensure_ascii=False,indent=2))
