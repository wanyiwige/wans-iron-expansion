"""Recover original armor attributes, preserving source and balanced values separately."""
from pathlib import Path
import sys, json, re, collections, shutil
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/'tools'))
from audit_hazen_recreation import clean, balanced, split_args
OUT = ROOT/'development-records/hazen-armor-expansion'
REF = ROOT/'development-records/hazen-recreation/reference-1.4.0.14/src/main/java'
RES = ROOT/'src/main/resources'
SOURCE_RES = ROOT.parent/'hazen-equipment-forge/src/main/resources'
HELPER = clean((ROOT.parent/'mod-backports-1.20.1/upstream/hazentouvelib/src/main/java/net/hazen/hazentouvelib/Items/Armor/HLArmorItem.java').read_text(encoding='utf8'))

def containers(text, variables=None):
    rows=[]
    for m in re.finditer(r'new AttributeContainer\s*\(',text):
        args=split_args(balanced(text,text.index('(',m.start())))
        if len(args)!=3: raise ValueError(args)
        attr=args[0].replace('.get()','').strip()
        attr=(variables or {}).get(attr,attr)
        num=re.sub(r'\((?:double|float|int)\)','',args[1]).strip()
        num=re.sub(r'[fFdD]$','',num)
        rows.append({'attribute':attr,'original':float(num),'operation':args[2].split('.')[-1]})
    return rows

def tiers(text):
    rows=[]
    for m in re.finditer(r'\b((?:dormant|school|pure|paragon|ascended)Tier(?:Dual|Tri)?|schoolAttributes)\s*\(',text):
        name=m[1];args=split_args(balanced(text,text.index('(',m.start())))
        if name=='schoolAttributes':
            rows.extend([{'attribute':args[0].replace('.get()',''),'original':.1,'operation':'ADD_MULTIPLIED_BASE'},
                         {'attribute':'AttributeRegistry.MAX_MANA','original':125,'operation':'ADD_VALUE'},
                         {'attribute':'AttributeRegistry.SPELL_POWER','original':.05,'operation':'ADD_MULTIPLIED_BASE'}]);continue
        method=re.search(r'AttributeContainer\[\]\s+'+name+r'\s*\(',HELPER)
        if not method:raise ValueError(name)
        start=HELPER.index('(',method.start());params=split_args(balanced(HELPER,start))
        variables={p.split()[-1]:a.replace('.get()','') for p,a in zip(params,args)}
        body=balanced(HELPER,HELPER.index('{',start),'{','}')
        rows.extend(containers(body,variables))
    return rows

manifest=json.loads((OUT/'import-manifest.json').read_text(encoding='utf8'))
pieces=[];unresolved=[]
for item in manifest['items']:
    s=dict(item);path=REF.joinpath(*s['source_class'].split('.')).with_suffix('.java')
    s['material']=dict(s['material'])
    if s['material']['repair'] in ('mithril_weave','magic_cloth','pyrium_ingot'):
        s['material']['repair_namespace']='irons_spellbooks'
    elif s['material']['repair']=='zenalite_ingot':
        s['material']['repair_namespace']='hazennstuff'
    text=clean(path.read_text(encoding='utf8'))
    try:attrs=containers(text)+tiers(text)
    except ValueError as e:unresolved.append({'id':s['id'],'error':str(e)});continue
    # Keep non-magic modifiers recovered from the original extraction too.
    for a in s['attributes']:
        row={'attribute':a['source_attribute'],'original':a['amount'],'operation':a['operation']}
        if row not in attrs:attrs.append(row)
    # Normalize extraction aliases before deduplication to avoid applying the
    # same arrow/critical modifier twice.
    aliases={'local.arrow_damage':'IronsLibRegistries.AttributeRegistry.ARROW_DAMAGE','local.crit_damage':'IronsLibRegistries.AttributeRegistry.CRIT_DAMAGE'}
    unique=[]
    for row in attrs:
        row['attribute']=aliases.get(row['attribute'],row['attribute'])
        if row not in unique:unique.append(row)
    attrs=unique
    compat={'addMaxMana':('optional:ars_nouveau:ars_nouveau.perk.max_mana',120,'ADD_VALUE'),
            'addManaRegen':('optional:ars_nouveau:ars_nouveau.perk.mana_regen',4,'ADD_VALUE'),
            'addArcaneResonance':('optional:malum:arcane_resonance',.15,'ADD_MULTIPLIED_BASE'),
            'addScytheProfeciency':('optional:malum:scythe_proficiency',.1,'ADD_MULTIPLIED_BASE'),
            'addSoulWardCapacity':('optional:malum:soul_ward_capacity',4,'ADD_VALUE'),
            'addSoulWardRecoveryRate':('optional:malum:soul_ward_recovery_rate',.15,'ADD_MULTIPLIED_BASE'),
            'addMagicProfeciency':('optional:lodestone:magic_proficiency',.15,'ADD_MULTIPLIED_BASE'),
            'addMagicResistance':('optional:lodestone:magic_resistance',.1,'ADD_MULTIPLIED_BASE')}
    for method,(attribute,amount,operation) in compat.items():
        if re.search(r'\b'+method+r'\(',text):
            row={'attribute':attribute,'original':amount,'operation':operation}
            if row not in attrs:attrs.append(row)
    for row in attrs:
        key=row['attribute'].upper()
        boost=('MAX_MANA' in key or 'SPELL_POWER' in key or key=='ATTRIBUTES.ATTACK_DAMAGE' or key=='ATTRIBUTES.MAX_HEALTH')
        row['value']=round(row['original']*(1.2 if boost and row['original']>0 else 1),6)
    s['resolved_attributes']=attrs
    s['imbuable']='Imbuable' in text.split('{',1)[0]
    s['original_set_effects']=sorted(set(re.findall(r'HnSEffects\.(\w+)',text)))
    s['applied_effects']=[{'id':a,'duration':int(b),'amplifier':int(c)} for a,b,c in re.findall(r'new MobEffectInstance\(\s*((?:HnSEffects|MobEffects)\.\w+)(?:\.get\(\))?\s*,\s*(\d+)\s*,\s*(\d+)',text)]
    # Effects in a key handler must not become passive equipment effects.
    s['applied_effects']=[e for e in s['applied_effects'] if e['id']!='HnSEffects.TYROS_SOUL_STATE']
    s['required_pieces']={slot:cls for slot,cls in re.findall(r'getItemBySlot\(Type\.(\w+)\.getSlot\(\)\)\.getItem\(\) instanceof (\w+)',text)}
    # Correct upstream copied checks that require an unrelated set.
    if s['family'] in ('maverick','slc_cat'):
        s['required_pieces']={slot:s['source_class'].rsplit('.',1)[-1] for slot in ('HELMET','CHESTPLATE','LEGGINGS','BOOTS')}
    # Chlorophyte body has no matching base-class helmet. Its three actual
    # helmet subclasses provide the intended archer/swordmaster/mage choices.
    if s['family']=='chlorophyte' and s['slot']!='HELMET':
        s['applied_effects']=[]
    s['frostbite_affinity']='AFFINITY_COMPONENT' in text and 'FROSTBITE_SPELL' in text
    for key in ('model','texture','animation'):
        uri=s.get(key)
        if not uri or not uri.startswith('hazen_equipment:'):continue
        rel=uri.split(':',1)[1];kind,tail=rel.split('/',1);target=kind+'/hazen_added/'+tail
        s[key]='alex_caves_spellsbook:'+target
        src=SOURCE_RES/'assets/hazen_equipment'/rel;dest=RES/'assets/alex_caves_spellsbook'/target
        dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dest)
        for suffix in ('.mcmeta',):
            if Path(str(src)+suffix).exists():shutil.copy2(str(src)+suffix,str(dest)+suffix)
    if s.get('animation'):
        namespace,path=s['animation'].split(':',1)
        animation_file=RES/'assets'/namespace/path
        if animation_file.exists():
            s['animation_names']=list(json.loads(animation_file.read_text(encoding='utf8')).get('animations',{}))
    s.setdefault('animation_names',[])
    pieces.append(s)
(OUT/'attribute-recovery-report.json').write_text(json.dumps({'recovered':len(pieces),'unresolved':unresolved,'balance_multiplier':1.2},ensure_ascii=False,indent=2),encoding='utf8')
if unresolved:raise RuntimeError(unresolved)
(RES/'hazen-added-armor.json').write_text(json.dumps(pieces,ensure_ascii=False,indent=2),encoding='utf8')
print('Recovered',len(pieces),'pieces;',sum(len(p['resolved_attributes']) for p in pieces),'attribute entries')
