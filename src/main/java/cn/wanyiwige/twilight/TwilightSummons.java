package cn.wanyiwige.twilight;

import io.redspace.ironsspellbooks.api.events.SpellSummonEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.capabilities.magic.*;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.mobs.goals.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

final class TwilightSummons {
    static void goals(PathfinderMob mob, IMagicSummon summon, GoalSelector movement, GoalSelector targets) {
        targets.removeAllGoals(g -> g instanceof NearestAttackableTargetGoal<?> || g instanceof HurtByTargetGoal);
        movement.addGoal(3,new GenericFollowOwnerGoal(mob,summon::getSummoner,1,10,2,false,50));
        targets.addGoal(1,new GenericOwnerHurtByTargetGoal(mob,summon::getSummoner));
        targets.addGoal(2,new GenericOwnerHurtTargetGoal(mob,summon::getSummoner));
        targets.addGoal(3,new GenericCopyOwnerTargetGoal(mob,summon::getSummoner));
    }
    static Entity owner(Entity mob) {
        Entity owner=SummonManager.getOwner(mob);
        if(owner==null && mob.getPersistentData().hasUUID("TwilightOwner") && mob.level() instanceof ServerLevel server)
            owner=server.getEntity(mob.getPersistentData().getUUID("TwilightOwner"));
        return owner;
    }
    static void dismiss(Entity entity){
        if(entity.level() instanceof ServerLevel server){
            String id=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).getPath();
            net.minecraft.core.particles.ParticleOptions particle=id.equals("summoned_carminite_golem")?net.minecraft.core.particles.DustParticleOptions.REDSTONE:id.equals("summoned_death_tome")?net.minecraft.core.particles.ParticleTypes.ENCHANT:id.equals("advanced_loyal_zombie")?TwilightParticles.EXANIMATED_FIRE.get():net.minecraft.core.particles.ParticleTypes.SNOWFLAKE;
            TwilightVisuals.burst(server,entity.position(),particle,25,.3);entity.discard();
        }
    }
    static void cast(AbstractSpell spell,String id,ServerLevel level,int rank,LivingEntity caster,CastSource source,MagicData magic) {
        boolean loyal=id.equals("summon_loyal_zombie");
        if(!loyal && magic.getPlayerRecasts().hasRecastForSpell(spell))return;
        SummonedEntitiesCastData data=magic.getAdditionalCastData() instanceof SummonedEntitiesCastData old ? old : new SummonedEntitiesCastData();
        EntityType<? extends PathfinderMob> type=switch(id){
            case "summon_winter_wolves"->TwilightEntities.WINTER_WOLF.get();
            case "summon_minotaurs"->TwilightEntities.MINOTAUR.get();
            case "summon_carminite_golem"->TwilightEntities.CARMINITE_GOLEM.get();
            case "summon_death_library"->TwilightEntities.DEATH_TOME.get();
            default->TwilightEntities.LOYAL_ZOMBIE.get();
        };
        for(int i=0;i<rank;i++) {
            PathfinderMob mob=type.create(level);if(mob==null)continue;
            Vec3 position=io.redspace.ironsspellbooks.api.util.Utils.moveToRelativeGroundLevel(level,caster.position().add(caster.getRandom().nextGaussian()*3,2,caster.getRandom().nextGaussian()*3),5);
            mob.moveTo(position.x,position.y,position.z,caster.getYRot(),0);
            mob.getPersistentData().putUUID("TwilightOwner",caster.getUUID());SummonManager.setOwner(mob,caster);
            mob.finalizeSpawn(level,level.getCurrentDifficultyAt(mob.blockPosition()),MobSpawnType.MOB_SUMMONED,null,null);
            SpellSummonEvent<PathfinderMob> event=new SpellSummonEvent<>(caster,mob,spell.getSpellResource(),rank);
            MinecraftForge.EVENT_BUS.post(event);
            var creature=event.getCreature();
            if(level.addFreshEntity(creature))SummonManager.initSummon(caster,creature,loyal?100:12000,data);
        }
        magic.getPlayerRecasts().addRecast(new RecastInstance(spell.getSpellId(),rank,loyal?5:2,12000,source,data),magic);
        magic.setAdditionalCastData(data);
    }
    private TwilightSummons(){}
}
