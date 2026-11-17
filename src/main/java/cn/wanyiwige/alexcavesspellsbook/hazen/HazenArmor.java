package cn.wanyiwige.alexcavesspellsbook.hazen;

import com.google.gson.Gson;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Native armor implementation; original renderer variants share one item per piece. */
public final class HazenArmor {
    public record SetSpec(String id, String school, String rarity, int durability_multiplier,
                          String model, String texture, String animation) {
        public boolean melee() { return school == null; }
    }
    public static final List<SetSpec> SETS;
    public static final Map<String, RegistryObject<HazenArmorItem>> ITEMS = new LinkedHashMap<>();
    private static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, "alex_caves_spellsbook");
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, "alex_caves_spellsbook");
    public static final RegistryObject<MobEffect> SWORDMASTER = EFFECTS.register("hazen_swordmaster_set_bonus", () ->
            new MobEffect(MobEffectCategory.BENEFICIAL, 0xfbb741) {}
                    .addAttributeModifier(Attributes.ATTACK_SPEED, "a10c675c-a514-42ab-8c7e-1421f520232e", .15, AttributeModifier.Operation.MULTIPLY_BASE));
    static {
        try (var reader = new InputStreamReader(HazenArmor.class.getResourceAsStream("/hazen-armor-sets.json"), StandardCharsets.UTF_8)) {
            SETS = List.of(new Gson().fromJson(reader, SetSpec[].class));
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
        for (var spec : SETS) for (var type : ArmorItem.Type.values()) {
            String id = spec.id() + "_" + type.getName();
            ITEMS.put(id, REGISTRY.register("hazen_" + id, () -> new HazenArmorItem(spec, type)));
        }
    }
    public static void register(IEventBus bus) {
        REGISTRY.register(bus); EFFECTS.register(bus);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(HazenArmor.class);
    }
    public static void addToTab(CreativeModeTab.Output output) {
        ITEMS.forEach((id, item) -> { if (HazenConfig.armorEnabled(id, item.get().spec.id())) output.accept(item.get().getDefaultInstance()); });
    }
    @SubscribeEvent public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getCrafting().getItem() instanceof HazenArmorItem item) item.initializeSpellContainer(event.getCrafting());
    }
    @SubscribeEvent public static void affinity(ModifySpellLevelEvent event) {
        if (event.getEntity() == null || event.getSpell() != SpellRegistry.FROSTBITE_SPELL.get()) return;
        for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (event.getEntity().getItemBySlot(slot).getItem() instanceof HazenArmorItem item && item.getEquipmentSlot() == slot
                    && item.enabled() && item.spec.melee()) event.addLevels(1);
        }
    }
    public static boolean matchingMeleeSet(LivingEntity entity) {
        String set = null;
        for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(entity.getItemBySlot(slot).getItem() instanceof HazenArmorItem item)
                    || item.getEquipmentSlot() != slot || !item.enabled() || !item.spec.melee()) return false;
            if (set == null) set = item.spec.id(); else if (!set.equals(item.spec.id())) return false;
        }
        return true;
    }
    public static void updateSetBonus(LivingEntity entity) {
        if (!matchingMeleeSet(entity)) { entity.removeEffect(SWORDMASTER.get()); return; }
        var active = entity.getEffect(SWORDMASTER.get());
        if (active == null || active.getDuration() < 300)
            entity.addEffect(new MobEffectInstance(SWORDMASTER.get(), 320, 0, false, false, false));
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) updateSetBonus(event.player);
    }
    private HazenArmor() {}
}
