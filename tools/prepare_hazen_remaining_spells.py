"""Extract evidenced spell definitions/assets; Java behavior is implemented separately."""
from pathlib import Path
import hashlib
import json
import re
import zipfile
from audit_hazen_recreation import PROJECT, PACKAGE, OUT, read, clean, balanced

NS = 'alex_caves_spellsbook'
RES = PROJECT / 'src/main/resources'
KINDS = {
 'bone_bolt':['BoneBolt'], 'soul_flame_bolt':['SoulflameBolt'], 'ice_arrow':['IceArrow','IceCloud'],
 'energy_burst':['EnergyBurst'], 'counterspell_spider_lily':['SpiderLily'], 'shadow_reaver':['ShadowReaver'],
 'cosmic_bolt':['CosmicBolt'], 'hydrobullet':['HydroBullet'], 'water_bolt':['WaterBolt'],
 'razorblade_typhoon':['RazorbladeTyphoon'], 'horn_shell':['HornShell'], 'trident_jetstream':['TridentJetstream'],
 'syringe_barrage':['Syringe'], 'evercomet_barrage':['Evercomet'], 'soul_seekers':['SoulSeeker'],
 'arcane_cards':['Club','Diamond','Heart','Spade'], 'endraconic_meteor':['EndraconicMeteor','EndraconicBreath'],
 'violent_regurgitation':['FleshChunk','FleshPiece','FleshMound'], 'scorching_slash':['ScorchingBlade'],
 'fiery_dagger':['FieryDaggerMagicProjectile'], 'hailstorm':['IceChunkA','IceChunkB','IceChunkC'],
 'death_sentence':['DeathSentence'], 'umbrashift_barrage':['ShadowDagger'],
 'dazzling_obliteration':['NinguDemonbanePetalblade','NinguDemonbanePetalbladeShuriken'],
 'golden_shower':['IchorStream'],
}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')


def main():
    inv = json.loads(read(OUT/'original-inventory.json'))
    original = Path(inv['original_jar'])
    assert hashlib.sha256(original.read_bytes()).hexdigest() == inv['original_sha256']
    existing = {'thorn_chakram','shooting_star','spectral_axe','nights_edge_strike','terraprismic_barrage','call_forth_terraprisma','prismatic_shift'}
    registry = clean(read(PACKAGE/'Spells/HnSSpellRegistries.java'))
    imports = {fq.rsplit('.',1)[-1]:fq for fq in re.findall(r'import ([\w.]+);', registry)}
    sound_fields=dict(re.findall(r'\b([A-Z_0-9]+)\s*=\s*registerSoundEvent\("([^"]+)"',clean(read(PACKAGE/'Registries/HnSSounds.java'))))
    specs, projectiles, receipts, particles = [], {}, [], []
    with zipfile.ZipFile(original) as jar:
        names = set(jar.namelist())
        def copy(path, dest=None):
            target = dest or ('geo/hazen/secondary/'+path[4:] if path.startswith('geo/') else
                              'animations/hazen/secondary/'+path[11:] if path.startswith('animations/') else
                              'textures/hazen/secondary/'+path[9:] if path.startswith('textures/') else
                              'sounds/hazen/secondary/'+path[7:] if path.startswith('sounds/') else path)
            raw = jar.read('assets/hazennstuff/'+path)
            file = RES/f'assets/{NS}/{target}'; file.parent.mkdir(parents=True,exist_ok=True);file.write_bytes(raw)
            receipts.append(dict(archive=str(original),original='assets/hazennstuff/'+path,target=str(file.relative_to(PROJECT)),sha256=hashlib.sha256(raw).hexdigest()))
            return NS+':'+target
        for cls in re.findall(r'registerSpell\(new (\w+)\(', registry):
            path = PACKAGE.parents[2]/(imports[cls].replace('.','/')+'.java') if cls in imports else next((PACKAGE/'Spells/Schools').rglob(cls+'.java'))
            source = clean(read(path))
            ident = re.findall(r'"([a-z_]+)"',re.search(r'spellId\s*=([^;]+);',source)[1])[-1]
            if ident in existing: continue
            def arg(method):
                match = re.search(method+r'\(',source)
                return balanced(source,source.index('(',match.start())) if match else None
            cooldown = re.sub(r'\((?:double|float)\)|[fFdD]|\s','',arg('setCooldownSeconds'))
            spec = dict(id=ident,source=str(path.relative_to(PROJECT)),source_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                        school=re.search(r'setSchoolResource\(([^)]+)\)',source)[1].split('.')[-1].removesuffix('_RESOURCE').lower(),
                        rarity=arg('setMinRarity').split('.')[-1],max_level=int(arg('setMaxLevel')),cooldown=float(cooldown),
                        cast_type=re.search(r'return CastType\.(\w+)',source)[1],
                        lootable='return false;' not in (re.search(r'boolean allowLooting\([^)]*\)\s*\{([^}]+)',source)[1] if 'boolean allowLooting' in source else ''),
                        craftable=arg('setAllowCrafting')!='false')
            for method,field in [('getCastStartSound','start_sound'),('getCastFinishSound','finish_sound')]:
                sound_method=re.search(method+r'\([^)]*\)\s*\{',source)
                body=balanced(source,sound_method.end()-1,'{','}') if sound_method else ''
                sound_ref=re.search(r'(HnSSounds|SoundRegistry)\.([A-Z_0-9]+)',body)
                spec[field]='.'.join(sound_ref.groups()) if sound_ref else ''
                if sound_ref and sound_ref[1]=='HnSSounds':spec[field]='HnSSounds.'+sound_fields[sound_ref[2]]
            for field in ['manaCostPerLevel','baseSpellPower','spellPowerPerLevel','castTime','baseManaCost']:
                spec[field]=float(re.search(r'this\.'+field+r'\s*=\s*([\d.]+)',source)[1])
            specs.append(spec)
            icon = f'textures/gui/spell_icons/{ident}.png'
            copy(icon,f'textures/gui/spell_icons/hazen_{ident}.png')
            if 'assets/hazennstuff/'+icon+'.mcmeta' in names:copy(icon+'.mcmeta',f'textures/gui/spell_icons/hazen_{ident}.png.mcmeta')
        assert len(specs)==31
        for spell, classes in KINDS.items():
            for cls in classes:
                candidates=list((PACKAGE/'Entity/Spells').rglob(cls+'.java'));assert len(candidates)==1,(cls,candidates)
                path=candidates[0]; source=clean(read(path));kind=re.sub(r'(?<!^)(?=[A-Z])','_',cls).lower()
                # Stable source entity IDs rather than inferred spell IDs.
                kind={'soulflame_bolt':'soul_flame_bolt','hydro_bullet':'hydrobullet','fiery_dagger_magic_projectile':'fiery_dagger_magic_projectile'}.get(kind,kind)
                speed_match=re.search(r'float getSpeed\(\)\s*\{\s*return ([\d.]+)',source)
                speed=float(speed_match[1]) if speed_match else 1.25 if cls=='IchorStream' else 0
                record=dict(kind=kind,spell=spell,speed=speed,source=str(path.relative_to(PROJECT)),source_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                            model=None,textures=[],animation=None,animation_name=None,render_type='cutout',frame_ticks=1,scale=[1,1,1])
                models=list(path.parent.glob('*Model.java'))
                if models:
                    model_source=read(models[0]);geo=re.search(r'"(geo/[^\"]+)"',model_source)
                    anim=re.search(r'"(animations/[^\"]+)"',model_source)
                    tex=re.search(r'"(textures/[^\"]+)"',model_source)
                    if geo and anim and tex:
                        record['model']=copy(geo[1]);record['animation']=copy(anim[1])
                        texture_prefix=tex[1]
                        tex_paths=[texture_prefix] if texture_prefix.endswith('.png') else sorted(n.split('assets/hazennstuff/')[1] for n in names if n.startswith('assets/hazennstuff/'+texture_prefix) and n.endswith('.png'))
                        assert tex_paths,(cls,texture_prefix)
                        record['textures']=[copy(t) for t in tex_paths]
                        available=json.loads(jar.read('assets/hazennstuff/'+anim[1]))['animations']
                        requested=re.findall(r'thenLoop\("([^\"]+)"',source)
                        record['animation_name']=next((a for a in requested if a in available),next(iter(available)) if len(available)==1 else None)
                        if record['animation_name'] is None and list(available)==['idle_1','idle_2'] and requested==['idle']:
                            record['animation_name']='idle_1'
                            record['source_animation_correction']='Original references absent idle; use supplied idle_1 instead.'
                        assert record['animation_name'],(cls,list(available),requested)
                        renderers='\n'.join(read(p)for p in path.parent.glob('*Renderer.java'))
                        record['render_type']='eyes' if 'RenderType.eyes' in renderers else 'energy' if 'RenderType.energySwirl' in renderers else 'translucent' if 'RenderType.entityTranslucent' in renderers else 'magic' if 'CustomerRenderType.magic' in renderers else 'cutout'
                        frame=re.search(r'tickCount\s*/\s*(\d+)',model_source)
                        if frame:record['frame_ticks']=int(frame[1])
                        scale=re.search(r'poseStack.scale\(\s*([\d.]+)[fF]?,\s*([\d.]+)[fF]?,\s*([\d.]+)[fF]?\)',renderers)
                        if scale:record['scale']=[float(scale[i])for i in (1,2,3)]
                projectiles[kind]=record
        # Two native sprite/arrow render paths do not have Gecko models.
        for path in sorted(n.split('assets/hazennstuff/')[1] for n in names if n.startswith('assets/hazennstuff/textures/spells/') and n.endswith(('.png','.mcmeta'))):
            copy(path)
        sounds=json.loads(read(RES/f'assets/{NS}/sounds.json'));sound_ids=json.loads(read(RES/'hazen-sound-ids.json'))
        for ident, data in json.loads(jar.read('assets/hazennstuff/sounds.json')).items():
            new=dict(data);new_sounds=[]
            for sound in data.get('sounds',[]):
                entry=dict(sound) if isinstance(sound,dict) else None;value=entry['name'] if entry else sound
                namespace,path=value.split(':',1) if ':'in value else ('hazennstuff',value)
                if namespace=='hazennstuff' and 'assets/hazennstuff/sounds/'+path+'.ogg' in names:
                    copy('sounds/'+path+'.ogg');value=f'{NS}:hazen/secondary/'+path
                if entry:entry['name']=value;new_sounds.append(entry)
                else:new_sounds.append(value)
            new['sounds']=new_sounds;sounds['hazen_secondary_'+ident]=new
            if 'secondary_'+ident not in sound_ids:sound_ids.append('secondary_'+ident)
        write(RES/f'assets/{NS}/sounds.json',sounds);write(RES/'hazen-sound-ids.json',sound_ids)
        for path in sorted(n for n in names if n.startswith('assets/hazennstuff/particles/') and n.endswith('.json')):
            data=json.loads(jar.read(path));ident=path.split('/')[-1][:-5]
            for i,texture in enumerate(data.get('textures',[])):
                ns,tex=texture.split(':')
                assert ns=='hazennstuff'
                for suffix in ['.png','.png.mcmeta']:
                    key='textures/particle/'+tex+suffix
                    if 'assets/hazennstuff/'+key in names:copy(key,'textures/particle/hazen/secondary/'+tex+suffix)
                data['textures'][i]=f'{NS}:hazen/secondary/'+tex
            write(RES/f'assets/{NS}/particles/hazen_secondary_{ident}.json',data);particles.append(ident)
        for ident in ['flame_kissed','moonkissed','ichor','infested','spores','electrocuted','gluttonous_hunger']:
            copy('textures/mob_effect/'+ident+'.png',f'textures/mob_effect/hazen_{ident}.png')
        copy('player_animations/cast_animation.json')
        for damage in ['spores','soul_fire']:
            key='data/hazennstuff/damage_type/'+damage+'.json'
            if key in names:write(RES/f'data/{NS}/damage_type/hazen/{damage}.json',json.loads(jar.read(key)))
        for lang in ['zh_cn','en_us']:
            source=json.loads(jar.read(f'assets/hazennstuff/lang/{lang}.json'));dest=RES/f'assets/{NS}/lang/{lang}.json';data=json.loads(read(dest))
            for key,value in source.items():
                if key.startswith(('spell.hazennstuff.','effect.hazennstuff.')):
                    kind,_,tail=key.split('.',2);data[f'{kind}.{NS}.hazen_{tail}']=value
            write(dest,data)
        # Equipment tags enable the source's cinderous/ionic branches without hard-coded lookalikes.
        id_map=json.loads(read(OUT/'recipe-map.json'))['id_map']
        for spec in json.loads(read(RES/'hazen-recreated-weapons.json')):id_map['hazennstuff:'+spec['id']]=f'{NS}:hazen_'+spec['id']
        for tag in ['cinderous_equipment','ionic_splitter']:
            candidates=[n for n in names if '/tags/item/'in n and n.endswith('/'+tag+'.json')]
            for path in candidates:
                raw=json.loads(jar.read(path));values=[]
                for value in raw['values']:
                    if isinstance(value,str):values.append(dict(id=id_map.get(value,value),required=False))
                    else:values.append(value)
                write(RES/f'data/{NS}/tags/items/hazen/secondary/{tag}.json',dict(replace=False,values=values))
    write(RES/'hazen-secondary-spells.json',specs)
    write(RES/'hazen-secondary-projectiles.json',projectiles)
    write(RES/'hazen-secondary-particles.json',particles)
    inputs=json.loads(read(PROJECT.parent/'mod-backports-1.20.1/inputs.json'))
    with zipfile.ZipFile(inputs['hazentouvelib']) as library:
        original_icon='assets/hazentouvelib/textures/mob_effect/hexed.png'
        raw=library.read(original_icon);target=RES/f'assets/{NS}/textures/mob_effect/hazen_hexed.png';target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(raw)
        receipts.append(dict(archive=inputs['hazentouvelib'],original=original_icon,target=str(target.relative_to(PROJECT)),sha256=hashlib.sha256(raw).hexdigest()))
    for lang in ['zh_cn','en_us']:
        path=RES/f'assets/{NS}/lang/{lang}.json';data=json.loads(read(path))
        data[f'effect.{NS}.hazen_hexed']='邪咒' if lang=='zh_cn' else 'Hexed'
        data[f'attribute.{NS}.hazen_hunger_steal']='饥饿窃取' if lang=='zh_cn' else 'Hunger Steal'
        write(path,data)
    write(OUT/'secondary-asset-receipts.json',receipts)
    print(json.dumps(dict(spells=len(specs),projectiles=len(projectiles),particles=len(particles),exact_asset_receipts=len(receipts)),indent=2))


if __name__=='__main__':main()
