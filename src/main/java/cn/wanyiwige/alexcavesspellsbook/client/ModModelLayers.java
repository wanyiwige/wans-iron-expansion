package cn.wanyiwige.alexcavesspellsbook.client;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public final class ModModelLayers {
    public static final ModelLayerLocation MAGNETIC_MAGE_ARMOR = new ModelLayerLocation(
            new ResourceLocation(AlexCavesSpellsbook.MOD_ID, "magnetic_mage_armor"), "main");

    private ModModelLayers() {
    }
}
