package cn.wanyiwige.alexcavesspellsbook.candy;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class CandyConfig {
    public static final ForgeConfigSpec SERVER;
    public static final ForgeConfigSpec.IntValue OWNER_BUDGET, DIMENSION_BUDGET;
    public static final ForgeConfigSpec CLIENT;
    public static final ForgeConfigSpec.IntValue PARTICLES_PER_TICK, TRANSPARENT_LAYERS;
    static {
        ForgeConfigSpec.Builder server = new ForgeConfigSpec.Builder();
        OWNER_BUDGET = server.comment("Maximum live candy effects and summons per caster.").defineInRange("owner_entity_budget", 32, 1, 64);
        DIMENSION_BUDGET = server.defineInRange("dimension_entity_budget", 256, 1, 512);
        SERVER = server.build();
        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        PARTICLES_PER_TICK = client.defineInRange("particles_per_entity_tick", 4, 0, 4);
        TRANSPARENT_LAYERS = client.defineInRange("transparent_layers", 3, 1, 3);
        CLIENT = client.build();
    }
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER, "wans-candy-server.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT, "wans-candy-client.toml");
    }
    private CandyConfig() { }
}
