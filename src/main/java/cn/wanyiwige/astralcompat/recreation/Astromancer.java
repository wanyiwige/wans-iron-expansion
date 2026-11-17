package cn.wanyiwige.astralcompat.recreation;

import cn.wanyiwige.astralcompat.registry.*;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.NeutralWizard;
import io.redspace.ironsspellbooks.entity.mobs.wizards.IMerchantWizard;
import io.redspace.ironsspellbooks.entity.mobs.goals.*;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.registry.*;
import io.redspace.ironsspellbooks.player.AdditionalWanderingTrades;
import io.redspace.ironsspellbooks.loot.SpellFilter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.*;
import java.util.*;

public final class Astromancer extends NeutralWizard implements IMerchantWizard {
    private Player customer;
    private MerchantOffers offers;
    private long lastRestock,checkedDay;
    private int restocks;
    public Astromancer(EntityType<? extends PathfinderMob> type,Level level) { super(type,level);xpReward=25;setPersistenceRequired(); }
    @Override public boolean isPersistenceRequired() { return true; }
    @Override protected void registerGoals() {
        goalSelector.addGoal(1,new FloatGoal(this));
        goalSelector.addGoal(2,new Combo());
        goalSelector.addGoal(3,new WizardAttackGoal(this,1.5,25,40).setSpells(
                List.of(AstralSpells.ARCANUM_LUMINOUS_BEAM.get(),AstralSpells.ARCANUM_STARFIRE.get(),AstralSpells.ARCANUM_STAR_SWARM.get()),
                List.of(AstralSpells.CELESTIAL_TETHER.get(),AstralSpells.ARCANUM_BRIGHTBURST.get(),AstralSpells.ARCANUM_PIERCING_LIGHT.get()),
                List.of(AstralSpells.ASTRAL_ECHO.get()),List.of(SpellRegistry.EVASION_SPELL.get())).setSpellQuality(.25F,.25F).setDrinksPotions().setAllowFleeing(true));
        goalSelector.addGoal(4,new PatrolNearLocationGoal(this,30,.75));
        goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(10,new WizardRecoverGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,ArcanumInsect.class,10,true,false,t->t instanceof ArcanumInsect insect && insect.dragonfly()));
        targetSelector.addGoal(5,new ResetUniversalAngerTargetGoal<>(this,false));
    }
    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,40).add(Attributes.ATTACK_DAMAGE,2.5)
                .add(Attributes.ATTACK_KNOCKBACK,.2).add(Attributes.ARMOR,10).add(Attributes.FOLLOW_RANGE,24)
                .add(Attributes.MOVEMENT_SPEED,.35).add(AttributeRegistry.MAX_MANA.get(),100);
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data,CompoundTag tag) {
        SpawnGroupData result=super.finalizeSpawn(level,difficulty,reason,data,tag);
        setItemSlot(EquipmentSlot.HEAD,new ItemStack(ArcanumContent.get("astral_helmet")));
        setItemSlot(EquipmentSlot.CHEST,new ItemStack(ArcanumContent.get("astral_chestplate")));
        setItemSlot(EquipmentSlot.MAINHAND,ArcanumContent.STAFF.get().getDefaultInstance());
        for(EquipmentSlot slot:EquipmentSlot.values())setDropChance(slot,0);
        return result;
    }
    @Override public boolean isAlliedTo(Entity other) { return super.isAlliedTo(other)||other instanceof Astromancer; }
    @Override public boolean causeFallDamage(float d,float m,net.minecraft.world.damagesource.DamageSource s) { return false; }
    @Override protected boolean isImmobile() { return super.isImmobile()||isTrading(); }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand) {
        if(getTarget()!=null || isAngryAt(player))return super.mobInteract(player,hand);
        if(!level().isClientSide) { if(shouldRestock())restock();setTradingPlayer(player);openTradingScreen(player,getDisplayName(),0); }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public MerchantOffers getOffers() {
        if(offers==null) {
            offers=new MerchantOffers();
            var fillers=new ArrayList<net.minecraft.world.entity.npc.VillagerTrades.ItemListing>(List.of(
                    new AdditionalWanderingTrades.SimpleBuy(1,new ItemStack(ArcanumContent.WINGS.get()),6,12),
                    new AdditionalWanderingTrades.SimpleSell(1,new ItemStack(ArcanumContent.RUNE.get(),2),25,55),
                    new AdditionalWanderingTrades.SimpleSell(1,new ItemStack(ArcanumContent.WINGS.get()),6,12)));
            Collections.shuffle(fillers,new Random(random.nextLong()));
            for(var listing:fillers.subList(0,random.nextBoolean()?2:3))addOffer(listing.getOffer(this,random));
            for(var ink:List.of(io.redspace.ironsspellbooks.registries.ItemRegistry.INK_UNCOMMON,io.redspace.ironsspellbooks.registries.ItemRegistry.INK_RARE,io.redspace.ironsspellbooks.registries.ItemRegistry.INK_EPIC))
                if(random.nextFloat()<.25)addOffer(new AdditionalWanderingTrades.InkBuyTrade((io.redspace.ironsspellbooks.item.InkItem)ink.get()).getOffer(this,random));
            SpellFilter filter=new SpellFilter(AstralSchools.ASTRAL.get());
            addOffer(new AdditionalWanderingTrades.RandomScrollTrade(filter,0,.25F).getOffer(this,random));
            if(random.nextFloat()<.8)addOffer(new AdditionalWanderingTrades.RandomScrollTrade(filter,.3F,.7F).getOffer(this,random));
            if(random.nextFloat()<.8)addOffer(new AdditionalWanderingTrades.RandomScrollTrade(filter,.8F,1).getOffer(this,random));
            addOffer(new MerchantOffer(new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.MITHRIL_SCRAP.get(),6),new ItemStack(ArcanumContent.RUNE.get()),2,0,1.2F));
        }
        return offers;
    }
    private void addOffer(MerchantOffer offer) { if(offer!=null)offers.add(offer); }
    @Override public void setTradingPlayer(Player player) { customer=player; }
    @Override public Player getTradingPlayer() { return customer; }
    @Override public void overrideOffers(MerchantOffers offers) { this.offers=offers; }
    @Override public void notifyTrade(MerchantOffer offer) { offer.increaseUses(); }
    @Override public void notifyTradeUpdated(ItemStack stack) { playSound(SoundEvents.VILLAGER_YES,.7F,1); }
    @Override public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }
    @Override public int getRestocksToday() { return restocks; }
    @Override public void setRestocksToday(int n) { restocks=n; }
    @Override public long getLastRestockGameTime() { return lastRestock; }
    @Override public void setLastRestockGameTime(long n) { lastRestock=n; }
    @Override public long getLastRestockCheckDayTime() { return checkedDay; }
    @Override public void setLastRestockCheckDayTime(long n) { checkedDay=n; }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag);serializeMerchant(tag,offers,lastRestock,restocks); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag);deserializeMerchant(tag,v->offers=v); }
    private final class Combo extends Goal {
        private long next;
        private int phase,delay;
        private boolean started;
        private Combo() { setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
        @Override public boolean canUse() { return !isTrading()&&getTarget()!=null&&!isCasting()&&tickCount>next; }
        @Override public boolean canContinueToUse() { return !isTrading()&&getTarget()!=null&&getTarget().isAlive()&&(phase<3||isCasting()); }
        @Override public void start() { phase=0;delay=0;started=false;getNavigation().stop(); }
        @Override public void stop() { next=tickCount+100+random.nextInt(151); }
        @Override public void tick() {
            LivingEntity target=getTarget();if(target==null)return;getLookControl().setLookAt(target,30,30);
            if(isCasting()||delay-->0)return;
            AbstractSpell[] sequence={AstralSpells.ARCANUM_BRIGHTBURST.get(),AstralSpells.ARCANUM_STARCUTTER.get(),AstralSpells.ARCANUM_STAR_SWARM.get()};
            if(phase<3) { initiateCastSpell(sequence[phase++],2);delay=12; }
        }
    }
}
