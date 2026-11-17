"""Packaging/schema/reference audit, not runtime acceptance. Optional argv[1]: nbtlib path."""
from pathlib import Path
import json,zipfile,hashlib,re,struct,sys,io,gzip
if len(sys.argv)>1:sys.path.insert(0,sys.argv[1])
import nbtlib
P=Path(__file__).resolve().parents[1]
R=P/'src/main/resources'
artifact=P/'build/libs/wans_iron_expansion-1.2.1-astral-preview.2.jar'
baseline=P/'build/libs/wans_iron_expansion-1.2.1-astral-preview.1.jar'
source=P.parent/'1.21.1 移植/asterismarcanum-1.21.1-0.1.0.jar'
checks=[]
seen=set()
def check(name,result):
 assert result,name
 if name not in seen:checks.append(name);seen.add(name)
with zipfile.ZipFile(artifact) as z,zipfile.ZipFile(baseline) as old,zipfile.ZipFile(source) as original,zipfile.ZipFile(P/'libs/irons_spellbooks-3.16.3.jar') as iss:
 entries=set(z.namelist())
 def data(n):return json.loads(z.read(n))
 blocks={Path(n).stem for n in original.namelist() if n.startswith('assets/asterismarcanum/blockstates/') and n.endswith('.json')}
 items=blocks|set('ethereal_talisman lumine_dust mothspawn dragonfly_spawn_egg lunar_moth_spawn_egg astromancer_spawn_egg astral_helmet astral_chestplate astral_leggings astral_boots celestial_staff astrolabe astral_rune dragonfly_wings astral_upgrade_orb'.split())
 check('27 source blocks',len(blocks)==27)
 check('42 items and block items',len(items)==42)
 for id in items:check('Item model '+id,f'assets/asterismarcanum/models/item/{id}.json' in entries)
 for id in blocks:check('Blockstate '+id,f'assets/asterismarcanum/blockstates/{id}.json'in entries)
 cn=data('assets/asterismarcanum/lang/zh_cn.json')
 for id in items:
  key=('block.' if id in blocks else 'item.')+'asterismarcanum.'+id
  check('Chinese name '+id,key in cn and bool(re.search('[\u4e00-\u9fff]',cn[key])))
 check('Compass exact translation key',cn['structure.asterismarcanum.astral_observatory']=='星辉观测台')
 check('Dimension Chinese name',cn['dimension.asterismarcanum.astral_sea']=='星界之海')
 check('Structure registration','data/asterismarcanum/worldgen/structure/astral_observatory.json'in entries)
 check('Structure set registration','data/asterismarcanum/worldgen/structure_set/astral_observatory.json'in entries)
 check('No NeoForge or singular 1.21 data paths',not any(n.startswith('data/asterismarcanum/') and n.split('/')[2] in ['recipe','structure','loot_table','neoforge'] for n in entries))
 recipes=[n for n in entries if n.startswith(('data/asterismarcanum/recipes/','data/minecraft/recipes/')) and n.endswith('.json') and (n.startswith('data/asterismarcanum/') or 'lumine'in n)]
 for n in recipes:
  d=data(n)
  if isinstance(d.get('result'),dict):check('Legacy recipe result '+n,'item'in d['result'] and 'components'not in d['result'])
  if d.get('type')=='minecraft:stonecutting':check('Stonecutting format '+n,isinstance(d['result'],str) and isinstance(d['count'],int))
  if d.get('type')=='minecraft:crafting_shaped':check('Recipe pattern '+n,set(''.join(d['pattern']))-{' '}==set(d['key']))
 check('88 recipes including local talisman',len(recipes)==88)
 check('Talisman survival access','data/asterismarcanum/recipes/ethereal_talisman.json'in entries)
 check('Leggings namespace fixed',data('data/asterismarcanum/recipes/astral_leggings.json')['result']['item']=='asterismarcanum:astral_leggings')
 check('Installed tarnished headpiece ID',data('data/asterismarcanum/recipes/astrolabe.json')['key']['C']['item']=='irons_spellbooks:tarnished_crown')
 orb=data('data/asterismarcanum/irons_spellbooks/upgrade_orb_type/astral_spell_power.json')
 check('Upgrade orb native codec',orb['operation']=='ADDITION' and orb['containerItem']=={'Count':1,'id':'asterismarcanum:astral_upgrade_orb'} and orb['attribute']=='kubejs:astral_spell_power')
 # Existing tag values must survive merging into a single mod JAR.
 for n in set(old.namelist())&entries:
  if '/tags/'in n and n.endswith('.json'):
   try:a=json.loads(old.read(n));b=data(n)
   except (ValueError,UnicodeError):continue
   if isinstance(a,dict) and 'values'in a:check('Retained tag '+n,all(v in b.get('values',[]) for v in a['values']))
 # Every custom template pool, processor list and structure reference resolves.
 for n in entries:
  if n.startswith('data/asterismarcanum/worldgen/template_pool/') and n.endswith('.json'):
   d=data(n)
   for e in d['elements']:
    e=e['element'];ns,path=e['location'].split(':')
    check('Pool template '+e['location'],f'data/{ns}/structures/{path}.nbt'in entries)
    processor=e.get('processors','minecraft:empty')
    if processor.startswith('asterismarcanum:'):check('Pool processor '+processor,'data/asterismarcanum/worldgen/processor_list/'+processor.split(':')[1]+'.json'in entries)
   fallback=d['fallback']
   if fallback.startswith('asterismarcanum:'):check('Fallback pool '+fallback,'data/asterismarcanum/worldgen/template_pool/'+fallback.split(':')[1]+'.json'in entries)
 structures=[n for n in entries if n.startswith('data/asterismarcanum/structures/') and n.endswith('.nbt')]
 check('33 structure pieces',len(structures)==33)
 counters={'stacks':0,'jigsaws':0,'entities':0}
 def walk(x):
  if isinstance(x,dict):
   check('No ItemStack components in NBT','components'not in x)
   if 'id'in x and 'Count'in x:counters['stacks']+=1
   if str(x.get('id',''))=='minecraft:jigsaw':
    counters['jigsaws']+=1
    pool=str(x.get('pool','minecraft:empty'))
    if pool.startswith('asterismarcanum:'):check('Jigsaw pool '+pool,'data/asterismarcanum/worldgen/template_pool/'+pool.split(':')[1]+'.json'in entries)
   for value in x.values():walk(value)
  elif isinstance(x,(list,nbtlib.List)):
   for value in x:walk(value)
 for n in structures:
  check('Built template matches saved source '+n,z.read(n)==(R/n).read_bytes())
  d=nbtlib.File.parse(io.BytesIO(gzip.decompress(z.read(n))))
  check('Template version '+n,int(d['DataVersion'])==3465)
  counters['entities']+=len(d.get('entities',[]))
  for block in d['palette']:
   name=str(block['Name'])
   if name.startswith('asterismarcanum:'):check('Custom palette block '+name,name.split(':')[1]in blocks)
   check('No unavailable decoration '+name,not name.startswith('irons_patreon_lib:') and not any(t in name for t in ['copper_trapdoor','copper_grate','chiseled_copper','wisewood_chiseled']))
  walk(d)
 check('Converted nested inventory items',counters['stacks']>0)
 check('Embedded astromancer preserved',counters['entities']==1)
 dim=data('data/asterismarcanum/dimension_type/asterism_dimension_type.json')
 layers=data('data/asterismarcanum/dimension/astral_sea.json')['generator']['settings']['layers']
 check('Dimension height and layers consistent',sum(x['height']for x in layers)==dim['height']==128)
 check('Dimension light within range',0<=dim['ambient_light']<=1)
 for resource in ['pocket_dimension_sky','pocket_clouds']:check('Native sky texture '+resource,f'assets/irons_spellbooks/textures/environment/{resource}.png'in iss.namelist())
 for n in entries:
  if n.endswith('.json') and n.startswith('assets/asterismarcanum/'):
   d=data(n)
   if '/models/'in n:
    for texture in d.get('textures',{}).values():
     if not isinstance(texture,str)or texture.startswith('#'):continue
     ns,path=texture.split(':')if ':'in texture else ('minecraft',texture)
     if ns=='asterismarcanum':check('Model texture '+texture,f'assets/{ns}/textures/{path}.png'in entries)
 # Both travel spells, all mob renderers and source spell content remain present.
 for cls in ['ArcanumContent','ArcanumEquipment','ArcanumInsect','SummonedMoth','Astromancer','MothspawnProjectile','AstralSea','ArcanumTravelSpells$Gateway','ArcanumTravelSpells$LunarMoths']:
  n='cn/wanyiwige/astralcompat/recreation/'+cls+'.class';check('Java17 '+cls,struct.unpack('>H',z.read(n)[6:8])[0]==61)
 client=z.read('cn/wanyiwige/astralcompat/client/ArcanumContentClient.class')
 check('All five new entity renderers wired',all(n in client for n in [b'DRAGONFLY',b'LUNAR_MOTH',b'SUMMONED_MOTH',b'ASTROMANCER',b'THROWN_EGG']))
 check('Original scroll unchanged',z.read('assets/asterismarcanum/textures/item/scroll_astral.png')==original.read('assets/asterismarcanum/textures/item/scroll_astral.png'))
 for n in old.namelist():
  if n.startswith('net/kamaarion/roacw/')and n.endswith('.class'):check('ROACW unchanged '+n,z.read(n)==old.read(n))
 check('No imported 1.21 classes',not any(n.startswith('com/birdie/')and n.endswith('.class') for n in entries))
 check('ZIP integrity',z.testzip()is None)
report={'artifact':str(artifact),'size':artifact.stat().st_size,'sha256':hashlib.sha256(artifact.read_bytes()).hexdigest(),'checks_passed':len(checks),'recipe_count':len(recipes),'structure_piece_count':len(structures),'structure_nbt_counts':counters,'checks':checks,'runtime_status':'not tested; not deployed'}
out=P/'development-records/asterism-recreation/phase4-verification.json';out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:v for k,v in report.items()if k!='checks'},ensure_ascii=False,indent=2))
