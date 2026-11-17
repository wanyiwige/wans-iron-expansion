package cn.wanyiwige.twilight;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.DeferredRegister;

public final class TwilightContentTab {
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,TwilightFoundation.NAMESPACE);
    static {TABS.register("content",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.wans_twilight_spellbooks.content")).icon(()->TwilightEquipment.get("lich_greatsword").getDefaultInstance()).displayItems((parameters,output)->{addItems(output);addScrolls(output);}).build());}
    public static void addItems(CreativeModeTab.Output output){TwilightItems.ITEMS.getEntries().forEach(item->output.accept(item.get().getDefaultInstance()));}
    public static void addScrolls(CreativeModeTab.Output output){TwilightSpells.SPELLS.getEntries().forEach(entry->{var spell=entry.get();if(!spell.isEnabled())return;for(int rank=1;rank<=spell.getMaxLevel();rank++){ItemStack scroll=new ItemStack(ItemRegistry.SCROLL.get());ISpellContainer.createScrollContainer(spell,rank,scroll);output.accept(scroll);}});}
    private TwilightContentTab(){}
}
