package cn.wanyiwige.alexcavesspellsbook.mixin;

import cn.wanyiwige.alexcavesspellsbook.registry.ModSchools;
import io.redspace.ironsspellbooks.api.config.SpellConfigManager;
import io.redspace.ironsspellbooks.api.config.SpellConfigParameter;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/** Runtime-only cloud school and scroll-forge availability; original defaults/config stay intact. */
@Mixin(value = SpellConfigManager.class, remap = false)
public abstract class CloudSpellSchoolMixin {
    @Unique
    private static final Set<String> wansIronExpansion$cloudSpells = Set.of(
            "legendary_spellbooks:cloud_rail", "legendary_spellbooks:cloud_ring",
            "legendary_spellbooks:cumulo_charge", "legendary_spellbooks:nimbus_array",
            "legendary_spellbooks:triple_nimbus_array", "legendary_spellbooks:tornado",
            "legendary_spellbooks:quad_tornado",
            "somakespells:thunder_cloud", "aether_spellbooks:thunderhead",
            "aether_spellbooks:aercloud_step", "aether_spellbooks:cloud_sentinels",
            "universal_spell:weather_cycle", "apprenticecodex:graced_rain",
            "spirit_mediumship_spell:mediumship_legendarymonsters_cloud_golem");

    @Inject(method = "getSpellConfigValue", at = @At("HEAD"), cancellable = true, remap = false)
    private static void wansIronExpansion$overrideCloudSchool(AbstractSpell spell,
            SpellConfigParameter<?> parameter, CallbackInfoReturnable<Object> callback) {
        if ((parameter != SpellConfigParameter.SCHOOL && parameter != SpellConfigParameter.ALLOW_CRAFTING)
                || !ModSchools.CLOUD.isPresent()) {
            return;
        }
        ResourceLocation id = spell.getSpellResource();
        if (id != null && wansIronExpansion$cloudSpells.contains(id.toString())
                && (parameter == SpellConfigParameter.SCHOOL
                    || "legendary_spellbooks".equals(id.getNamespace()))) {
            callback.setReturnValue(parameter == SpellConfigParameter.SCHOOL
                    ? ModSchools.CLOUD.get() : Boolean.TRUE);
        }
    }
}
