"""Static source/resource audit only: does not compile or change an active JAR."""
import hashlib,json,re,struct
from pathlib import Path

project=Path(__file__).resolve().parents[1]
assets=project/'src/main/resources/assets/alex_caves_spellsbook'
package=project/'src/main/java/cn/wanyiwige/alexcavesspellsbook'
item=(package/'item/LandOfTheLustrousItem.java').read_text(encoding='utf8')
assert 'super(12, properties)' in item
assert 'initializeSpellContainer(stack)' in item
assert 'LandOfTheLustrousItem' in (package/'registry/ModItems.java').read_text(encoding='utf8')
assert 'LAND_OF_THE_LUSTROUS.get().getDefaultInstance()' in (package/'registry/ModCreativeTabs.java').read_text(encoding='utf8')
assert 'LandOfTheLustrousCurioRenderer::new' in (package/'client/ClientModEvents.java').read_text(encoding='utf8')
tag=json.loads((project/'src/main/resources/data/curios/tags/items/spellbook.json').read_text(encoding='utf8'))
assert 'alex_caves_spellsbook:land_of_the_lustrous' in tag['values']
model=json.loads((assets/'models/item/land_of_the_lustrous.json').read_text(encoding='utf8'))
assert model['parent']=='minecraft:item/generated' and 'loader' not in model
icon=(assets/'textures/item/land_of_the_lustrous.png').read_bytes()
assert struct.unpack('>II',icon[16:24])==(66,62)
assert hashlib.sha256(icon).hexdigest()=='4285740d293b7f979b38518053efdf3e58d672387d6822853599db4dc5d6a84b'
for locale in ['zh_cn','en_us']:
    lang=json.loads((assets/'lang'/f'{locale}.json').read_text(encoding='utf8'))
    assert 'item.alex_caves_spellsbook.land_of_the_lustrous' in lang
    assert 'tooltip.alex_caves_spellsbook.land_of_the_lustrous' in lang
renderer=(package/'client/render/LandOfTheLustrousCurioRenderer.java').read_text(encoding='utf8')
references=re.findall(r'asset\("([^"]+)"\)',renderer)
assert all((assets/name).is_file() for name in references)
geometry=json.loads((assets/'geo/land_of_the_lustrous_caster.geo.json').read_text(encoding='utf8'))['minecraft:geometry'][0]
bones=geometry['bones'];names={b['name'] for b in bones}
assert len(names)==len(bones)
assert all(b.get('parent') is None or b['parent'] in names for b in bones)
assert next(b for b in bones if b['name']=='caster_root')['pivot']==[0,0,0]
low=[float('inf')]*3;high=[-float('inf')]*3;cube_count=0
for bone in bones:
    for cube in bone.get('cubes',[]):
        cube_count+=1
        assert all(s>0 for s in cube['size'])
        assert len(cube['uv'])==6
        for face in cube['uv'].values():
            assert all(0<=v<=138 for v in face['uv'])
            assert all(0<=face['uv'][i]+face['uv_size'][i]<=138 for i in range(2))
        for a in range(3):
            low[a]=min(low[a],cube['origin'][a]);high[a]=max(high[a],cube['origin'][a]+cube['size'][a])
assert cube_count==716
frame=(assets/'textures/curios/land_of_the_lustrous_caster.png').read_bytes()
original=(project.parent/'.codex_staging/land-of-the-lustrous-model/reference/caster_frame_00.png').read_bytes()
assert frame==original
assert struct.unpack('>II',frame[16:24])==(138,138)
assert json.loads((assets/'animations/land_of_the_lustrous_caster.animation.json').read_text(encoding='utf8'))['animations']=={}
active=project.parent/'mods/wans_iron_expansion-1.0.30.jar'
if not active.exists():
    deployment=json.loads((project/'outputs/land-of-the-lustrous-source/deployment-1.0.31.json').read_text(encoding='utf-8-sig'))
    active=Path(deployment['backup'])
gem_source=project.parent/'.codex_staging/gem-projectiles-v01'
gem_file=assets/'models/projectile/lustrous_gems.json'
assert gem_file.read_bytes()==(gem_source/'geometry_spec.json').read_bytes()
gems=json.loads(gem_file.read_text(encoding='utf8'))
assert len(gems)==9 and len({g['id'] for g in gems})==9
triangles=0
for gem in gems:
    texture=assets/'textures/entity/lustrous_gems'/f"{gem['file']}.png"
    assert texture.read_bytes()==(gem_source/'textures'/texture.name).read_bytes()
    assert struct.unpack('>II',texture.read_bytes()[16:24])==(64,16)
    for part in gem['parts']:
        vertices=part['vertices'];edges={};volume=0
        assert len(part['swatches'])==len(part['faces'])
        for face,swatch in zip(part['faces'],part['swatches']):
            assert len(face)==3 and 0<=swatch<len(gem['palette'])
            a,b,c=[vertices[index] for index in face]
            cross=[b[1]*c[2]-b[2]*c[1],b[2]*c[0]-b[0]*c[2],b[0]*c[1]-b[1]*c[0]]
            volume+=sum(a[i]*cross[i] for i in range(3))/6
            for i in range(3):
                edge=tuple(sorted((face[i],face[(i+1)%3])))
                edges[edge]=edges.get(edge,0)+1
        assert all(count==2 for count in edges.values()) and volume>0
        triangles+=len(part['faces'])
spell_icon=(assets/'textures/spell/land_of_the_lustrous.png').read_bytes()
assert spell_icon==(project/'outputs/land-of-the-lustrous-source/icon/land_of_the_lustrous_v01.png').read_bytes()
assert spell_icon[:8]==b'\x89PNG\r\n\x1a\n'
assert struct.unpack('>II',spell_icon[16:24])[0]==struct.unpack('>II',spell_icon[16:24])[1]
active_hash=hashlib.sha256(active.read_bytes()).hexdigest()
assert active_hash=='c679df6e6e32e7e6eba2bc1c61d002b1a914ee8d3d1f9f5d9659dadb67e56297'
result={'checks':'passed','validation_kind':'static source and resources only; no compilation or game run',
        'id':'alex_caves_spellsbook:land_of_the_lustrous','spell_slots':12,
        'gem_models':len(gems),'gem_triangles':triangles,'gem_meshes_closed':True,'gem_assets_match_original_models':True,
        'spell_icon_sha256':hashlib.sha256(spell_icon).hexdigest(),
        'spell_icon_dimensions':list(struct.unpack('>II',spell_icon[16:24])),
        'icon_dimensions':[66,62],'icon_unmodified':True,'caster_frame_unmodified':True,
        'bones':len(bones),'cubes':cube_count,'model_bounds_pixels':[low,high],
        'model_size_blocks_at_scale_0_48':[(high[i]-low[i])*.48/16 for i in range(3)],
        'curio_asset_references':references,'active_jar_sha256':active_hash,'active_jar_unchanged':True}
out=project/'outputs/land-of-the-lustrous-source/static-validation.json'
out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps(result,ensure_ascii=False,indent=2))
