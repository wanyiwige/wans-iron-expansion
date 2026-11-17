package cn.wanyiwige.alexcavesspellsbook.hazen;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HazenSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "alex_caves_spellsbook");
    private static final Map<String, RegistryObject<SoundEvent>> ENTRIES = new LinkedHashMap<>();
    static {
        try (var stream = HazenSounds.class.getResourceAsStream("/hazen-sound-ids.json")) {
            String[] names = new com.google.gson.Gson().fromJson(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8), String[].class);
            for (String name : names) ENTRIES.put(name, SOUNDS.register("hazen_" + name,
                    () -> SoundEvent.createVariableRangeEvent(HazenEquipment.id("hazen_" + name))));
        } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }
    public static void register(IEventBus bus) { SOUNDS.register(bus); }
    public static SoundEvent get(String id) {
        var sound = ENTRIES.get(id);
        if (sound == null) throw new IllegalArgumentException("Unknown original sound " + id);
        return sound.get();
    }
    public static void play(Level level, Vec3 pos, String sound, float volume, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, get(sound), SoundSource.PLAYERS, volume, pitch);
    }
    private HazenSounds() {}
}
