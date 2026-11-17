package cn.wanyiwige.alexcavesspellsbook.hazen;

public final class HazenAddedArmorAbility {
    public static final net.minecraftforge.network.simple.SimpleChannel CHANNEL=net.minecraftforge.network.NetworkRegistry.newSimpleChannel(new net.minecraft.resources.ResourceLocation("alex_caves_spellsbook","hazen_added_armor"),()->"1","1"::equals,"1"::equals);
    public record Activate(){}
    public static void register(){CHANNEL.registerMessage(0,Activate.class,(m,b)->{},b->new Activate(),(m,c)->{var context=c.get();context.enqueueWork(()->activate(context.getSender()));context.setPacketHandled(true);},java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));}
    public static void activate(net.minecraft.server.level.ServerPlayer p){
        if(p==null||!p.isAlive()||!HazenAddedArmorEffects.family(p,"garments_of_the_first_flamebearer"))return;
        var boots=p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).getItem();if(p.getCooldowns().isOnCooldown(boots))return;
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(HazenAddedArmorEffects.BUFFS.get("TYROS_SOUL_STATE").get(),1200,0,true,true,true));p.getCooldowns().addCooldown(boots,2400);
        p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,p.getX(),p.getY()+1,p.getZ(),40,.4,.05,.4,0);
        p.level().playSound(null,p.blockPosition(),io.redspace.ironsspellbooks.registries.SoundRegistry.FIRE_CAST.get(),net.minecraft.sounds.SoundSource.PLAYERS,1.5f,1);
    }
    private HazenAddedArmorAbility(){}
}
