package cn.wanyiwige.alexcavesspellsbook.events;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import cn.wanyiwige.alexcavesspellsbook.spells.DarkSparkSpell;
import cn.wanyiwige.astralcompat.entity.AstralAnchorEntity;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AlexCavesSpellsbook.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DarkSparkEvents {
    private DarkSparkEvents() {
    }

    @SubscribeEvent
    public static void onCast(SpellOnCastEvent event) {
        if (event.getEntity().level().isClientSide || !DarkSparkSpell.SPELL_ID.equals(event.getSpellId())) return;
        AstralAnchorEntity anchor = DarkSparkSpell.findAnchor(event.getEntity());
        int age = anchor == null ? 0 : anchor.tickCount;
        int cost = DarkSparkSpell.manaCost(event.getSpellLevel(), age);
        event.setManaCost(cost);

        // Iron's Spells does not charge some cast sources itself.
        if (!event.getCastSource().consumesMana()
                && !(event.getEntity().isCreative() && !ServerConfigs.CREATIVE_MANA_COST.get())) {
            MagicData data = MagicData.getPlayerMagicData(event.getEntity());
            if (data.getMana() < cost) {
                Utils.serverSideCancelCast((ServerPlayer) event.getEntity());
                return;
            }
            data.setMana(data.getMana() - cost);
            PacketDistributor.sendToPlayer((ServerPlayer) event.getEntity(), new SyncManaPacket(data));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        MagicData data = MagicData.getPlayerMagicData(player);
        if (!data.isCasting() || !DarkSparkSpell.SPELL_ID.equals(data.getCastingSpellId())) return;
        if (player.isCreative() && !ServerConfigs.CREATIVE_MANA_COST.get()) return;
        AstralAnchorEntity anchor = DarkSparkSpell.findAnchor(player);
        int cost = DarkSparkSpell.manaCost(data.getCastingSpellLevel(), anchor == null ? 0 : anchor.tickCount);
        if (data.getMana() < cost) {
            Utils.serverSideCancelCast(player);
            if (anchor != null) anchor.discard();
        }
    }
}
