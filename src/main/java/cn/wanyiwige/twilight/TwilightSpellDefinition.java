package cn.wanyiwige.twilight;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
/** Audited configuration values; behaviors are implemented separately for Forge 1.20.1. */
public record TwilightSpellDefinition(String id, ResourceLocation school, SpellRarity rarity, int max, double cooldown, CastType cast, boolean crafting, int mana, int manaStep, float power, float powerStep, int ticks) {
 public static final List<TwilightSpellDefinition> ALL = List.of(
  new TwilightSpellDefinition("aurora_missile", SchoolRegistry.ICE_RESOURCE, SpellRarity.EPIC, 6, 1.0, CastType.INSTANT, true, 10, 2, 12.0F, 1.0F, 0),
  new TwilightSpellDefinition("avalanche", SchoolRegistry.ICE_RESOURCE, SpellRarity.COMMON, 6, 45.0, CastType.LONG, true, 50, 10, 10.0F, 2.0F, 20),
  new TwilightSpellDefinition("carminite_pull", SchoolRegistry.LIGHTNING_RESOURCE, SpellRarity.COMMON, 10, 12.0, CastType.CONTINUOUS, true, 3, 1, 1.0F, 1.0F, 100),
  new TwilightSpellDefinition("druid_bolt", SchoolRegistry.NATURE_RESOURCE, SpellRarity.COMMON, 10, 1.0, CastType.INSTANT, true, 10, 2, 12.0F, 1.0F, 0),
  new TwilightSpellDefinition("exanimated_fireball", TwilightFoundation.id("exanimated"), SpellRarity.LEGENDARY, 5, 25.0, CastType.LONG, true, 60, 15, 2.0F, 2.0F, 40),
  new TwilightSpellDefinition("exanimated_ray", TwilightFoundation.id("exanimated"), SpellRarity.RARE, 5, 25.0, CastType.INSTANT, true, 25, 15, 8.0F, 2.0F, 0),
  new TwilightSpellDefinition("exanimated_step", TwilightFoundation.id("exanimated"), SpellRarity.RARE, 3, 12.0, CastType.INSTANT, true, 30, 10, 10.0F, 2.0F, 0),
  new TwilightSpellDefinition("exanimated_trident", TwilightFoundation.id("exanimated"), SpellRarity.EPIC, 4, 8.0, CastType.LONG, true, 50, 10, 14.0F, 2.0F, 35),
  new TwilightSpellDefinition("fire_bettle_spit", SchoolRegistry.FIRE_RESOURCE, SpellRarity.COMMON, 10, 1.0, CastType.INSTANT, true, 10, 2, 1.0F, 1.0F, 0),
  new TwilightSpellDefinition("fortifying_shields", SchoolRegistry.HOLY_RESOURCE, SpellRarity.LEGENDARY, 1, 25.0, CastType.LONG, false, 120, 0, 4.0F, 1.0F, 30),
  new TwilightSpellDefinition("hydra_mortar", SchoolRegistry.NATURE_RESOURCE, SpellRarity.RARE, 6, 16.0, CastType.LONG, true, 50, 5, 15.0F, 1.0F, 45),
  new TwilightSpellDefinition("ice_bomb", SchoolRegistry.ICE_RESOURCE, SpellRarity.UNCOMMON, 8, 12.0, CastType.LONG, true, 30, 5, 8.0F, 3.0F, 20),
  new TwilightSpellDefinition("jet_eruption", SchoolRegistry.FIRE_RESOURCE, SpellRarity.COMMON, 6, 45.0, CastType.LONG, true, 50, 10, 10.0F, 2.0F, 20),
  new TwilightSpellDefinition("life_sacrifice", SchoolRegistry.BLOOD_RESOURCE, SpellRarity.LEGENDARY, 1, 30.0, CastType.INSTANT, false, 35, 15, 2.0F, 1.0F, 0),
  new TwilightSpellDefinition("minotaur_lunge", SchoolRegistry.NATURE_RESOURCE, SpellRarity.COMMON, 5, 15.0, CastType.LONG, true, 30, 15, 5.0F, 1.0F, 10),
  new TwilightSpellDefinition("mosquito_swarm", SchoolRegistry.BLOOD_RESOURCE, SpellRarity.UNCOMMON, 10, 20.0, CastType.LONG, true, 40, 10, 6.0F, 1.0F, 30),
  new TwilightSpellDefinition("summon_carminite_golem", SchoolRegistry.LIGHTNING_RESOURCE, SpellRarity.RARE, 4, 80.0, CastType.LONG, true, 75, 10, 5.0F, 2.0F, 30),
  new TwilightSpellDefinition("summon_death_library", SchoolRegistry.ENDER_RESOURCE, SpellRarity.UNCOMMON, 4, 150.0, CastType.LONG, true, 50, 10, 10.0F, 3.0F, 30),
  new TwilightSpellDefinition("summon_loyal_zombie", SchoolRegistry.BLOOD_RESOURCE, SpellRarity.LEGENDARY, 1, 10.0, CastType.LONG, false, 19, 1, 4.0F, 1.0F, 10),
  new TwilightSpellDefinition("summon_minotaurs", SchoolRegistry.NATURE_RESOURCE, SpellRarity.RARE, 5, 80.0, CastType.LONG, true, 75, 10, 5.0F, 2.0F, 30),
  new TwilightSpellDefinition("summon_winter_wolves", SchoolRegistry.ICE_RESOURCE, SpellRarity.RARE, 5, 80.0, CastType.LONG, true, 75, 10, 5.0F, 2.0F, 30),
  new TwilightSpellDefinition("twilight_bolt", SchoolRegistry.ENDER_RESOURCE, SpellRarity.LEGENDARY, 1, 0.0, CastType.INSTANT, false, 30, 5, 10.0F, 1.0F, 0));
}
