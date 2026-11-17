package cn.wanyiwige.alexcavesspellsbook.hazen;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Source definitions are data; casting/collision behavior is independently implemented. */
public final class HazenSecondaryRegistry {
    public record Spec(String id,String school,String rarity,int max_level,double cooldown,String cast_type,
                       boolean lootable,boolean craftable,int manaCostPerLevel,int baseSpellPower,int spellPowerPerLevel,int castTime,int baseManaCost,String start_sound,String finish_sound) {}
    public record Visual(String kind,String spell,float speed,String model,String[] textures,String animation,String animation_name,String render_type,int frame_ticks,float[] scale) {}
    public static final List<Spec> SPELLS;
    public static final Map<String,Visual> VISUALS;
    public static final Map<String,RegistryObject<EntityType<HazenSecondaryProjectile>>> TYPES=new LinkedHashMap<>();
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,"alex_caves_spellsbook");
    static {
        try(var specs=new InputStreamReader(HazenSecondaryRegistry.class.getResourceAsStream("/hazen-secondary-spells.json"),StandardCharsets.UTF_8);
            var visuals=new InputStreamReader(HazenSecondaryRegistry.class.getResourceAsStream("/hazen-secondary-projectiles.json"),StandardCharsets.UTF_8)) {
            SPELLS=List.of(new Gson().fromJson(specs,Spec[].class));
            VISUALS=new Gson().fromJson(visuals,new TypeToken<Map<String,Visual>>(){}.getType());
        }catch(java.io.IOException ex){throw new IllegalStateException(ex);}
        VISUALS.forEach((kind,spec)->TYPES.put(kind,ENTITIES.register("hazen_secondary_"+kind,()->
                EntityType.Builder.<HazenSecondaryProjectile>of((type,level)->new HazenSecondaryProjectile(type,level,kind),MobCategory.MISC)
                        .sized(kind.equals("ice_arrow")?.5f:1,kind.equals("flesh_mound")?.5f:1)
                        .clientTrackingRange(64).updateInterval(1).build(HazenEquipment.id("hazen_secondary_"+kind).toString()))));
    }
    public static void register(IEventBus bus){ENTITIES.register(bus);HazenSecondaryEffects.register(bus);}
    public static boolean enabled(String spell){return HazenConfig.EXTRA_SPELLS.get()&&HazenConfig.spellEnabled(spell);}
    public static HazenSecondaryProjectile projectile(String kind,LivingEntity caster,float damage,int level){
        var entity=TYPES.get(kind).get().create(caster.level());
        entity.setOwner(caster);entity.setDamage(damage);entity.spellLevel=level;
        entity.launchDirection=caster.getLookAngle();
        entity.setPos(caster.getEyePosition().subtract(0,entity.getBbHeight()*.5,0));
        entity.shoot(caster.getLookAngle());return entity;
    }
    public static HazenSecondaryProjectile spawn(String kind,LivingEntity caster,float damage,int level,Vec3 position,Vec3 direction){
        var entity=projectile(kind,caster,damage,level);entity.setPos(position);entity.shoot(direction);caster.level().addFreshEntity(entity);return entity;
    }
    private HazenSecondaryRegistry(){}
}
