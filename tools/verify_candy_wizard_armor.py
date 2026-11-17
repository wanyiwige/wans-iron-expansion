"""Check shipped armor geometry, UV coverage, and scope against preview.11."""
from pathlib import Path
from PIL import Image
import argparse,base64,hashlib,io,json,zipfile

root=Path(__file__).resolve().parents[1]
art=root/'artwork/candy-wizard-armor-20261017'
parser=argparse.ArgumentParser()
parser.add_argument('--version',default='1.2.1-candy-pixel-preview.12')
parser.add_argument('--report-dir',default=str(art))
parser.add_argument('--icon-size',type=int)
args=parser.parse_args()
report_dir=Path(args.report_dir)
assets=root/'src/main/resources/assets/alex_caves_spellsbook'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
bb=json.loads((art/'candy_wizard_armor.bbmodel').read_text(encoding='utf-8'))
geo=json.loads((assets/'geo/candy_wizard_armor.geo.json').read_text())['minecraft:geometry'][0]
texture=Image.open(assets/'textures/models/armor/candy_wizard.png').convert('RGBA')
assert texture.size==(512,256)
assert bb['resolution']=={'width':512,'height':256}
assert geo['description']['texture_width']==512 and geo['description']['texture_height']==256
embedded=base64.b64decode(bb['textures'][0]['source'].split(',',1)[1])
assert Image.open(io.BytesIO(embedded)).convert('RGBA').tobytes()==texture.tobytes()
assert bb['textures'][0]['uv_width']==512 and bb['textures'][0]['uv_height']==256
elements=[e for e in bb['elements'] if e.get('export',True)]
cubes=[c for b in geo['bones'] for c in b.get('cubes',[])]
assert len(elements)==len(cubes)==77 and len(geo['bones'])==9
assert len([e for e in bb['elements'] if not e.get('export',True)])==3
assert all('_preview' not in b['name'] for b in geo['bones'])
def rect(face):
 x,y=face['uv'];w,h=face['uv_size']
 return tuple(round(v,6) for v in (min(x,x+w),min(y,y+h),max(x,x+w),max(y,y+h)))
for e in elements:
 matches=[c for c in cubes if sorted(tuple(v['uv']) for v in e['faces'].values())==sorted(rect(v) for v in c['uv'].values())]
 assert len(matches)==1,e['name']
 c=matches[0]
 size=[round(b-a,6) for a,b in zip(e['from'],e['to'])]
 assert all(abs(a-b)<1e-5 for a,b in zip(size,c['size'])),e['name']
 origin=[-e['to'][0],e['from'][1],e['from'][2]]
 assert all(abs(a-b)<1e-5 for a,b in zip(origin,c['origin'])),e['name']
 assert sorted(tuple(v['uv']) for v in e['faces'].values())==sorted(rect(v) for v in c['uv'].values()),e['name']
 for face in c['uv'].values():
  x1,y1,x2,y2=rect(face)
  assert 0<=x1<x2<=512 and 0<=y1<y2<=256
  assert texture.crop((x1,y1,x2,y2)).getchannel('A').getextrema()==(255,255),e['name']
baseline=json.loads((art/'baseline.json').read_text())
changed=[p.replace('\\','/') for p,h in baseline.items() if not (root/p).exists() or sha(root/p)!=h]
allowed={f'src/main/resources/assets/alex_caves_spellsbook/lang/{lang}.json' for lang in ['zh_cn','en_us']}
allowed|={f'src/main/java/cn/wanyiwige/alexcavesspellsbook/registry/{name}.java' for name in ['ModItems','ModCreativeTabs']}
assert set(changed)==allowed,changed
old_path=root/'build/libs/wans_iron_expansion-1.2.1-candy-pixel-preview.11.jar'
jar=root/'build/libs'/('wans_iron_expansion-'+args.version+'.jar')
with zipfile.ZipFile(old_path) as old,zipfile.ZipFile(jar) as new:
 before=set(old.namelist());after=set(new.namelist());assert not before-after
 modified=[n for n in sorted(before&after) if old.read(n)!=new.read(n)]
 permitted={'META-INF/MANIFEST.MF','META-INF/mods.toml'}
 permitted|={f'assets/alex_caves_spellsbook/lang/{lang}.json' for lang in ['zh_cn','en_us']}
 permitted|={f'cn/wanyiwige/alexcavesspellsbook/registry/{name}.class' for name in ['ModItems','ModCreativeTabs']}
 assert set(modified)==permitted,modified
 for lang in ['zh_cn','en_us']:
  path=f'assets/alex_caves_spellsbook/lang/{lang}.json';a=json.loads(old.read(path));b=json.loads(new.read(path))
  assert all(b.get(k)==v for k,v in a.items())
 added=[n for n in sorted(after-before) if not n.endswith('/')]
 assert all('candy_wizard' in n or 'CandyWizard' in n for n in added),added
 for name in ['hat','robe','leggings','boots']:
  item='candy_wizard_'+name
  model=json.loads(new.read(f'assets/alex_caves_spellsbook/models/item/{item}.json'))
  assert model['textures']['layer0']==f'alex_caves_spellsbook:item/{item}'
  recipe=json.loads(new.read(f'data/alex_caves_spellsbook/recipes/{item}.json'))
  assert recipe['result']['item']==f'alex_caves_spellsbook:{item}'
  if args.icon_size:
   icon=Image.open(io.BytesIO(new.read(f'assets/alex_caves_spellsbook/textures/item/{item}.png'))).convert('RGBA')
   assert icon.size==(args.icon_size,args.icon_size)
   assert set(icon.getchannel('A').getdata())=={0,255}
 for relative in ['geo/candy_wizard_armor.geo.json','textures/models/armor/candy_wizard.png','animations/candy_wizard_armor.animation.json']:
  assert new.read('assets/alex_caves_spellsbook/'+relative)==(assets/relative).read_bytes()
log=(report_dir/'build.log').read_text(encoding='utf-8-sig')
assert 'BUILD SUCCESSFUL' in log and 'PASS: native GeckoLib JSON parse and bake' in log
report={'status':'PASS','atlas':[512,256],'pixels_per_model_unit':2,'armor_bones':9,'exported_cubes':77,'opaque_faces':462,'preview_cubes_excluded':3,'embedded_texture_matches':True,'native_geckolib_bake':'PASS','build':'PASS','changed_existing_source_files':changed,'changed_existing_jar_entries':modified,'added_jar_files':added,'jar':str(jar),'sha256':sha(jar),'game_render_tested':False,'deployed':False}
(report_dir/'verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({k:v for k,v in report.items() if not isinstance(v,list)},ensure_ascii=False,indent=2))
