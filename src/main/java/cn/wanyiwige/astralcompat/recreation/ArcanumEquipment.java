package cn.wanyiwige.astralcompat.recreation;

import cn.wanyiwige.astralcompat.registry.AstralAttributes;
import cn.wanyiwige.astralcompat.registry.AstralSpells;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.item.weapons.*;
import io.redspace.ironsspellbooks.item.armor.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.*;
import java.util.function.Consumer;

public final class ArcanumEquipment {
    public static final class Astrolabe extends SpellBook implements GeoItem {
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        public Astrolabe() {
            super(12,new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC));
            withSpellbookAttributes(new AttributeContainer(AttributeRegistry.MAX_MANA,200,AttributeModifier.Operation.ADDITION),
                    new AttributeContainer(AstralAttributes.SPELL_POWER,.2,AttributeModifier.Operation.ADDITION),
                    new AttributeContainer(AttributeRegistry.MANA_REGEN,.05,AttributeModifier.Operation.MULTIPLY_BASE));
        }
        @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {}
        @Override public void initializeClient(Consumer<IClientItemExtensions> c) { c.accept(cn.wanyiwige.astralcompat.client.ArcanumContentClient.itemExtensions("astrolabe")); }
    }
    public static final class CelestialStaff extends StaffItem implements GeoItem,IPresetSpellContainer {
        private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
        public CelestialStaff() { super(new Item.Properties().stacksTo(1).durability(2031).fireResistant().rarity(Rarity.EPIC),1.5,-2.5,staffAttributes()); }
        private static Map<Attribute,AttributeModifier> staffAttributes() {
            Map<Attribute,AttributeModifier> out=new LinkedHashMap<>();
            out.put(AstralAttributes.SPELL_POWER.get(),modifier("staff_astral",.15,AttributeModifier.Operation.ADDITION));
            out.put(AttributeRegistry.MANA_REGEN.get(),modifier("staff_regen",.1,AttributeModifier.Operation.MULTIPLY_BASE));
            out.put(AttributeRegistry.COOLDOWN_REDUCTION.get(),modifier("staff_cooldown",.1,AttributeModifier.Operation.MULTIPLY_BASE));return out;
        }
        @Override public void initializeSpellContainer(ItemStack stack) {
            if(stack!=null && !ISpellContainer.isSpellContainer(stack)) ISpellContainer.set(stack,ISpellContainer.create(1,false,true));
        }
        @Override public ItemStack getDefaultInstance() { ItemStack s=super.getDefaultInstance();initializeSpellContainer(s);return s; }
        @Override public void inventoryTick(ItemStack stack,Level level,Entity owner,int slot,boolean selected) { initializeSpellContainer(stack);super.inventoryTick(stack,level,owner,slot,selected); }
        @Override public boolean hasCustomRendering() { return true; }
        @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {
            c.add(new AnimationController<>(this,"idle",0,s->{s.getController().setAnimation(RawAnimation.begin().thenLoop("idle"));return PlayState.CONTINUE;}));
        }
        @Override public void initializeClient(Consumer<IClientItemExtensions> c) { c.accept(cn.wanyiwige.astralcompat.client.ArcanumContentClient.itemExtensions("celestial_staff")); }
    }
    public static final class AstralArmor extends ImbuableChestplateArmorItem {
        public AstralArmor(Type type) {
            super(ExtendedArmorMaterials.PYROMANCER,type,new Item.Properties().stacksTo(1).durability(base(type)*37),
                    new AttributeContainer(AttributeRegistry.MAX_MANA,125,AttributeModifier.Operation.ADDITION),
                    new AttributeContainer(AstralAttributes.SPELL_POWER,.1,AttributeModifier.Operation.ADDITION),
                    new AttributeContainer(AttributeRegistry.SPELL_POWER,.05,AttributeModifier.Operation.MULTIPLY_BASE));
        }
        private static int base(Type type) { return switch(type) {case HELMET->11;case CHESTPLATE->16;case LEGGINGS->15;case BOOTS->13;}; }
        @Override @OnlyIn(Dist.CLIENT) public GeoArmorRenderer<?> supplyRenderer() { return cn.wanyiwige.astralcompat.client.ArcanumContentClient.armorRenderer(); }
    }
    public static final class Talisman extends io.redspace.ironsspellbooks.item.CastingItem implements IPresetSpellContainer {
        public Talisman() { super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)); }
        @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand) {
            ItemStack stack=player.getItemInHand(hand);initializeSpellContainer(stack);
            if(!level.isClientSide) {
                var gateway=AstralSpells.ASTRAL_GATEWAY.get();
                String slot=hand==net.minecraft.world.InteractionHand.MAIN_HAND
                        ?io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.MAINHAND:io.redspace.ironsspellbooks.api.magic.SpellSelectionManager.OFFHAND;
                if(!gateway.attemptInitiateCast(stack,gateway.getLevelFor(1,player),level,player,CastSource.SWORD,true,slot))
                    return net.minecraft.world.InteractionResultHolder.fail(stack);
            }
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
        @Override public void initializeSpellContainer(ItemStack stack) {
            if(stack!=null && !ISpellContainer.isSpellContainer(stack)) {
                ISpellContainerMutable spells=ISpellContainer.create(1,false,true).mutableCopy();
                spells.addSpell(AstralSpells.ASTRAL_GATEWAY.get(),1,true);ISpellContainer.set(stack,spells.toImmutable());
            }
        }
        @Override public ItemStack getDefaultInstance() { ItemStack s=super.getDefaultInstance();initializeSpellContainer(s);return s; }
        @Override public void inventoryTick(ItemStack stack,Level level,Entity owner,int slot,boolean selected) { initializeSpellContainer(stack);super.inventoryTick(stack,level,owner,slot,selected); }
    }
    private static AttributeModifier modifier(String name,double amount,AttributeModifier.Operation op) {
        return new AttributeModifier(UUID.nameUUIDFromBytes(("wan_astral_"+name).getBytes(java.nio.charset.StandardCharsets.UTF_8)),name,amount,op);
    }
    private ArcanumEquipment() {}
}
