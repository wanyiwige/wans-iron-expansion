"""Rebuild evidenced survival data for native Forge 1.20.1 registries.

Run once per batch baseline. All omissions and generated files are recorded.
"""
from pathlib import Path
import datetime, hashlib, json, re, zipfile

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
OUT=ROOT/'development-records/hazen-recreation'
NS='alex_caves_spellsbook'
REPORT=OUT/'survival-map.json'
def read(p): return json.loads(p.read_text(encoding='utf-8'))
def digest(b): return hashlib.sha256(b).hexdigest()

def main():
    inventory=read(OUT/'original-inventory.json')
    archive=Path(inventory['original_jar'])
    assert digest(archive.read_bytes())==inventory['original_sha256']
    if REPORT.exists():
        baseline=read(REPORT)['backup']
    else:
        folder=ROOT/'development-backups'/('before-hazen-survival-'+datetime.datetime.now().strftime('%Y%m%d-%H%M%S'))
        folder.mkdir(parents=True)
        baseline=str((folder/'before-survival.zip').relative_to(ROOT))
        paths=[p for d in ['src','tools','docs','development-checks/hazen-native/src'] for p in (ROOT/d).rglob('*') if p.is_file()]
        paths += [ROOT/'gradle.properties',ROOT/'development-checks/hazen-native/build.gradle']
        with zipfile.ZipFile(ROOT/baseline,'w',zipfile.ZIP_DEFLATED) as z:
            for p in paths:z.write(p,p.relative_to(ROOT).as_posix())
        with zipfile.ZipFile(ROOT/baseline) as z:
            assert z.testzip() is None
            for p in paths:assert digest(z.read(p.relative_to(ROOT).as_posix()))==digest(p.read_bytes())
    prior=read(REPORT) if REPORT.exists() else {}
    generated=prior.get('generated_files',[]);receipts=[];deferred=[];changed_recipes=prior.get('changed_recipes',[])
    def write(path,data):
        path.parent.mkdir(parents=True,exist_ok=True)
        path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        generated.append(path.relative_to(ROOT).as_posix())
    idmap=read(OUT/'recipe-map.json')['id_map']
    for orig,dest in read(OUT/'armor-recreation-map.json')['original_aliases'].items():idmap['hazennstuff:'+orig]=NS+':hazen_'+dest
    for spec in read(RES/'hazen-secondary-spells.json'):idmap['hazennstuff:'+spec['id']]=NS+':hazen_'+spec['id']
    ores=['chlorophyte_ore','dreadstone_ore','solar_core_blackstone_ore','solar_core_netherrack_ore']
    wood=['wisewood_log','wisewood_wood','stripped_wisewood_log','stripped_wisewood_wood','wisewood_planks']
    for name in ores+wood:idmap['hazennstuff:'+name]=NS+':hazen_'+name
    idmap['irons_spellbooks:wisewood_planks']=NS+':hazen_wisewood_planks'
    installed={}
    # Resource entries prove data availability; native registry validation follows in QA.
    for path in (ROOT.parent/'mods').glob('*.jar'):
        if path.name.startswith('wans_iron_expansion-'):continue
        with zipfile.ZipFile(path) as jar:
            for n in jar.namelist():
                if n.endswith('.json'):installed[n]=path
    old={x['id'] for x in inventory['items'] if x['legacy']}
    def remap(x):
        if isinstance(x,str):return idmap.get(x,x)
        if isinstance(x,list):return [remap(v) for v in x]
        if isinstance(x,dict):return {k:remap(v) for k,v in x.items()}
        return x
    def source(z,n):
        raw=z.read(n)
        receipts.append({'source':n,'sha256':digest(raw)})
        return json.loads(raw)
    with zipfile.ZipFile(archive) as z:
        def asset(original,dest):
            raw=z.read(original);p=RES/dest;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(raw)
            generated.append(p.relative_to(ROOT).as_posix());receipts.append({'source':original,'target':p.relative_to(ROOT).as_posix(),'sha256':digest(raw)})
        for ore in ores:
            model=source(z,f'assets/hazennstuff/models/block/{ore}.json')
            def texture_ref(x):
                if isinstance(x,str) and x.startswith('hazennstuff:'):
                    path=x.split(':',1)[1];dest=f'assets/{NS}/textures/hazen/survival/{path}.png'
                    asset('assets/hazennstuff/textures/'+path+'.png',dest)
                    return NS+':hazen/survival/'+path
                return x
            model['textures']={k:texture_ref(v) for k,v in model['textures'].items()}
            write(RES/f'assets/{NS}/models/block/hazen_{ore}.json',model)
            write(RES/f'assets/{NS}/models/item/hazen_{ore}.json',{'parent':NS+':block/hazen_'+ore})
            write(RES/f'assets/{NS}/blockstates/hazen_{ore}.json',{'variants':{'':{'model':NS+':block/hazen_'+ore}}})
            loot=remap(source(z,f'data/hazennstuff/loot_table/blocks/{ore}.json'))
            def legacy_loot(x):
                if isinstance(x,list):return [legacy_loot(v) for v in x]
                if not isinstance(x,dict):return x
                x={k:legacy_loot(v) for k,v in x.items() if k!='random_sequence'}
                if 'predicates' in x:
                    x['enchantments']=x.pop('predicates')['minecraft:enchantments']
                return x
            write(RES/f'data/{NS}/loot_tables/blocks/hazen_{ore}.json',legacy_loot(loot))
        for name in wood:
            model=source(z,f'assets/hazennstuff/models/block/{name}.json')
            for k,v in model.get('textures',{}).items():
                if v.startswith('hazennstuff:'):
                    path=v.split(':')[1]
                    asset('assets/hazennstuff/textures/'+path+'.png',f'assets/{NS}/textures/hazen/survival/{path}.png')
                    model['textures'][k]=NS+':hazen/survival/'+path
            write(RES/f'assets/{NS}/models/block/hazen_{name}.json',model)
            state=source(z,f'assets/hazennstuff/blockstates/{name}.json')
            for variant in state['variants'].values():variant['model']=NS+':block/hazen_'+name
            write(RES/f'assets/{NS}/blockstates/hazen_{name}.json',state)
            write(RES/f'assets/{NS}/models/item/hazen_{name}.json',{'parent':NS+':block/hazen_'+name})
            write(RES/f'data/{NS}/loot_tables/blocks/hazen_{name}.json',legacy_loot(remap(source(z,f'data/hazennstuff/loot_table/blocks/{name}.json'))))
        for name in ['wisewood_log','wisewood_planks','wisewood_wood']:
            data=remap(source(z,f'data/hazennstuff/recipe/crafting/blocks/{name}.json'))
            result=data['result'];result['item']=result.pop('id')
            if name=='wisewood_planks':data['ingredients']=[{'tag':NS+':hazen/survival/wisewood_logs'}]
            data['conditions']=[{'type':NS+':hazen_enabled','item':'@survival'}]
            write(RES/f'data/{NS}/recipes/hazen/survival/{name}.json',data)
        write(RES/f'data/{NS}/tags/items/hazen/survival/wisewood_logs.json',{'values':[NS+':hazen_'+v for v in wood if v!='wisewood_planks']})
        for tag,values in [('logs',[v for v in wood if v!='wisewood_planks']),('logs_that_burn',[v for v in wood if v!='wisewood_planks']),('planks',['wisewood_planks'])]:
            # Only append to existing tags; never overwrite another module's entries.
            for kind in ['items','blocks']:
                path=RES/f'data/minecraft/tags/{kind}/{tag}.json';data=read(path) if path.exists() else {'replace':False,'values':[]}
                data['values']=list(dict.fromkeys(data['values']+[NS+':hazen_'+v for v in values]));write(path,data)
        path=RES/'data/minecraft/tags/blocks/mineable/axe.json';data=read(path) if path.exists() else {'replace':False,'values':[]}
        data['values']=list(dict.fromkeys(data['values']+[NS+':hazen_'+v for v in wood]));write(path,data)
        # QA does not load the merged ROACW module. Its fixture tag omits only
        # those absent registry IDs; production tags keep all existing entries.
        tag=read(RES/'data/minecraft/tags/blocks/mineable/pickaxe.json') if (RES/'data/minecraft/tags/blocks/mineable/pickaxe.json').exists() else {'values':[]}
        tag['values']=[v for v in tag['values'] if not isinstance(v,str) or not v.startswith('roacw:')]
        # Ores are appended below; include them here for first-run consistency.
        tag['values']=list(dict.fromkeys(tag['values']+[NS+':hazen_'+v for v in ores]))
        write(ROOT/'development-checks/hazen-native/src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json',tag)
        for lang in ['zh_cn','en_us']:
            source_lang=source(z,f'assets/hazennstuff/lang/{lang}.json');target=RES/f'assets/{NS}/lang/{lang}.json';data=read(target)
            for name in ores+wood:data[f'block.{NS}.hazen_{name}']=source_lang.get('block.hazennstuff.'+name,name)
            write(target,data)
        for tag,ore_values in [('minecraft:mineable/pickaxe',ores),('minecraft:needs_diamond_tool',[v for v in ores if v.startswith('solar_')]),('minecraft:needs_iron_tool',['dreadstone_ore']),('forge:needs_netherite_tool',['chlorophyte_ore'])]:
            namespace,path=tag.split(':');target=RES/f'data/{namespace}/tags/blocks/{path}.json'
            data=read(target) if target.exists() else {'replace':False,'values':[]}
            data['values']=[v for v in data['values'] if v not in [NS+':hazen_'+x for x in ores]]
            data['values']=list(dict.fromkeys(data['values']+[NS+':hazen_'+x for x in ore_values]))
            write(target,data)
        for feature in ['chlorophyte_ore','dreadstone_ore','solar_core_ore']:
            for category,name in [('configured_feature',feature),('placed_feature',feature+'_placed')]:
                data=remap(source(z,f'data/hazennstuff/worldgen/{category}/{name}.json'))
                if category=='placed_feature':data['feature']=NS+':hazen/'+feature
                write(RES/f'data/{NS}/worldgen/{category}/hazen/{name}.json',data)
            original=source(z,f'data/hazennstuff/neoforge/biome_modifier/add_{feature}.json')
            write(RES/f'data/{NS}/forge/biome_modifier/hazen/{feature}.json',{
                'type':NS+':hazen_features','biomes':original['biomes'],'features':NS+':hazen/'+feature+'_placed','step':original['step']})
        # Zenalite is already generated by the legacy mod: never double its density.
        # Rebuild only source loot pools whose referenced content exists in this batch.
        def valid_entry(e):
            text=json.dumps(e)
            if '"options"' in text:
                deferred.append({'reason':'unimplemented enchantment','entry':e});return None
            name=e.get('name','')
            if e.get('type')=='minecraft:loot_table':
                deferred.append({'reason':'unimplemented nested source loot table','entry':e});return None
            if e.get('type')=='minecraft:item' and name.startswith('hazennstuff:') and name.split(':')[1] not in old:
                deferred.append({'reason':'unimplemented source item','entry':e});return None
            if e.get('type')=='minecraft:tag' and name=='aces_spell_utils:hydro_focus':e['name']=NS+':hazen/hydro_focus'
            return e
        def convert(x):
            if isinstance(x,list):return [convert(v) for v in x]
            if not isinstance(x,dict):return x
            x={k:convert(v) for k,v in x.items() if k!='random_sequence'}
            if x.get('condition')=='neoforge:loot_table_id':x['condition']='forge:loot_table_id'
            if x.get('condition')=='minecraft:random_chance_with_enchanted_bonus':
                assert x['enchantment']=='minecraft:looting'
                chance=x['unenchanted_chance'];ench=x['enchanted_chance'];increment=ench['per_level_above_first']
                assert abs(ench['base']-chance-increment)<1e-6
                x={'condition':'minecraft:random_chance_with_looting','chance':chance,'looting_multiplier':increment}
            if x.get('function')=='minecraft:enchanted_count_increase':
                assert x.pop('enchantment')=='minecraft:looting';x['function']='minecraft:looting_enchant'
            if isinstance(x.get('count'),dict) and 'min' in x['count']:x['count'].setdefault('type','minecraft:uniform')
            if isinstance(x.get('rolls'),dict) and 'min' in x['rolls']:x['rolls'].setdefault('type','minecraft:uniform')
            return x
        modifiers=[];tables=[]
        for n in z.namelist():
            if not n.endswith('.json') or not any(t in n for t in ['/loot_modifiers/entity_drops/','/loot_modifiers/chests/']):continue
            data=source(z,n)
            # Bogged is a 1.21 mob. Preserve the gap rather than assign its drops to another entity.
            if 'minecraft:entities/bogged' in json.dumps(data):deferred.append({'source':n,'reason':'Bogged absent in Minecraft 1.20.1'});continue
            key=data['key'];poolpath='data/hazennstuff/loot_table/'+key.split(':')[1]+'.json'
            loot=remap(source(z,poolpath));pools=[]
            for pool in loot.get('pools',[]):
                entries=[v for e in pool.get('entries',[]) if (v:=valid_entry(e)) is not None]
                # The legacy pack already appends this axe to the same vault. Keep
                # its existing route rather than add a second roll for the same item.
                entries=[e for e in entries if not (e.get('name')=='hazennstuff:legionnaire_warlock_axe' and 'citadel_vault' in n)]
                if entries:pool['entries']=entries;pools.append(pool)
            if not pools:continue
            table=NS+':hazen/survival/'+key.split(':')[1]
            write(RES/f'data/{NS}/loot_tables/hazen/survival/{key.split(":")[1]}.json',convert({'pools':pools}))
            tables.append(table)
            data=convert(data);data['type']=NS+':hazen_append';data['table']=table;data.pop('key')
            modifier=n.split('/loot_modifiers/')[1][:-5];ident=NS+':hazen/survival/'+modifier
            write(RES/f'data/{NS}/loot_modifiers/hazen/survival/{modifier}.json',data);modifiers.append(ident)
        target=RES/'data/forge/loot_modifiers/global_loot_modifiers.json'
        globaldata=read(target) if target.exists() else {'replace':False,'entries':[]}
        globaldata['entries']=list(dict.fromkeys(globaldata['entries']+modifiers));write(target,globaldata)
        # Recipe compatibility: furnace results are strings in 1.20.1, never stacks.
        for path in (RES/f'data/{NS}/recipes/hazen').rglob('*.json'):
            data=read(path);before=json.dumps(data,sort_keys=True);data=remap(data)
            if data['type'] in ['minecraft:smelting','minecraft:blasting','minecraft:smoking','minecraft:campfire_cooking'] and isinstance(data['result'],dict):
                assert data['result'].get('count',1)==1
                data['result']=data['result']['item']
            def tags(x):
                if isinstance(x,list):return [tags(v) for v in x]
                if not isinstance(x,dict):return x
                x={k:tags(v) for k,v in x.items()}
                if x.get('tag') in ['c:mushrooms','c:glass_blocks']:
                    x['tag']=NS+':hazen/survival/'+x['tag'].split(':')[1]
                return x
            data=tags(data)
            if json.dumps(data,sort_keys=True)!=before:write(path,data);changed_recipes.append(path.relative_to(ROOT).as_posix())
        write(RES/f'data/{NS}/tags/items/hazen/survival/mushrooms.json',{'values':['minecraft:brown_mushroom','minecraft:red_mushroom']})
        # Original c:glass_blocks includes stained glass, not panes.
        colors=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
        write(RES/f'data/{NS}/tags/items/hazen/survival/glass_blocks.json',{'values':['minecraft:glass','minecraft:tinted_glass']+['minecraft:'+c+'_stained_glass' for c in colors]})
        tag=read(RES/'data/forge/tags/blocks/needs_netherite_tool.json')
        tag['values']=[v for v in tag['values'] if not isinstance(v,str) or not v.startswith('roacw:')]
        write(ROOT/'development-checks/hazen-native/src/main/resources/data/forge/tags/blocks/needs_netherite_tool.json',tag)
        unlocks=[]
        for path in (RES/f'data/{NS}/recipes/hazen').rglob('*.json'):
            data=read(path);rid=NS+':'+path.relative_to(RES/f'data/{NS}/recipes').with_suffix('').as_posix()
            ingredients=[]
            def scan(x):
                if isinstance(x,dict):
                    if 'item' in x and isinstance(x['item'],str):ingredients.append({'items':[x['item']]})
                    elif 'tag' in x:ingredients.append({'tag':x['tag']})
                    else:
                        for v in x.values():scan(v)
                elif isinstance(x,list):
                    for v in x:scan(v)
            for k in ['key','ingredients','ingredient','base','template','addition']:scan(data.get(k))
            if not ingredients:continue
            gate={'player':[{'condition':NS+':hazen_survival_enabled'}]}
            criteria={f'has_{i}':{'trigger':'minecraft:inventory_changed','conditions':dict(gate,items=[v])} for i,v in enumerate(ingredients)}
            criteria['has_recipe']={'trigger':'minecraft:recipe_unlocked','conditions':dict(gate,recipe=rid)}
            advancement={'criteria':criteria,'requirements':[list(criteria)],'rewards':{'recipes':[rid]}}
            dest=RES/f'data/{NS}/advancements/hazen/survival/recipes'/path.relative_to(RES/f'data/{NS}/recipes/hazen')
            write(dest,advancement);unlocks.append(rid)
    atlaspath=RES/'assets/minecraft/atlases/blocks.json';atlas=read(atlaspath)
    for row in receipts:
        target=row.get('target','')
        if '/textures/hazen/survival/' in target:
            resource=NS+':'+target.split('/textures/')[1].removesuffix('.png')
            entry={'type':'minecraft:single','resource':resource}
            if entry not in atlas['sources']:atlas['sources'].append(entry)
    write(atlaspath,atlas)
    report=dict(backup=baseline,backup_sha256=digest((ROOT/baseline).read_bytes()),original_sha256=inventory['original_sha256'],
                generated_files=sorted(set(generated)),receipts=receipts,ore_blocks=ores,wood_blocks=wood,ore_feature_count=3,
                modifiers=modifiers,loot_tables=tables,recipe_unlocks=unlocks,changed_recipes=sorted(set(changed_recipes)),deferred=deferred,
                preserved_legacy_zenalite_worldgen=True,
                known_gaps=['Void Wanderer stardust/Starfury/cosmic rune requires unrecreated original mob',
                    'Bogged overgrown bone source absent in Minecraft 1.20.1',
                    'Source weapons without recipe or loot remain source-unobtainable; no invented recipes',
                    'Unrecreated accessories, armor and enchantment pools are explicitly deferred'])
    REPORT.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:len(report[k]) for k in ['generated_files','modifiers','loot_tables','recipe_unlocks','changed_recipes','deferred']},indent=2))
    print('Backup:',baseline)

if __name__=='__main__':main()
