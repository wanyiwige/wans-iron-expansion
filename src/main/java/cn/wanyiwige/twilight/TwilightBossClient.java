package cn.wanyiwige.twilight;

import io.redspace.ironsspellbooks.api.util.BossbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

public final class TwilightBossClient {
    private static final Map<UUID,SimpleSoundInstance> MUSIC=new HashMap<>();
    public static void track(TwilightCaster boss,boolean start){
        if(start){BossbarManager.startTracking(boss.getUUID(),new BossbarManager.BossbarSprite(TwilightFoundation.id("boss_bars/lich_boss_bar"),192,18,3,-1));
            SoundEvent music=io.redspace.ironsspellbooks.registries.SoundRegistry.DEAD_KING_SECOND_PHASE_MELODY_ALT.get();
            if(music!=null&&!MUSIC.containsKey(boss.getUUID())){var sound=SimpleSoundInstance.forMusic(music);MUSIC.put(boss.getUUID(),sound);Minecraft.getInstance().getSoundManager().play(sound);}
        }else{BossbarManager.stopTracking(boss.getUUID());var sound=MUSIC.remove(boss.getUUID());if(sound!=null)Minecraft.getInstance().getSoundManager().stop(sound);}
    }
    public static void tick(){Minecraft client=Minecraft.getInstance();Set<UUID> alive=new HashSet<>();if(client.level!=null)for(var entity:client.level.entitiesForRendering())if(entity instanceof TwilightCaster boss&&boss.isAlive())alive.add(entity.getUUID());MUSIC.entrySet().removeIf(entry->{if(alive.contains(entry.getKey()))return false;client.getSoundManager().stop(entry.getValue());BossbarManager.stopTracking(entry.getKey());return true;});}
    private TwilightBossClient(){}
}
