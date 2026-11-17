package cn.wanyiwige.twilight;

import net.minecraft.world.entity.ai.attributes.*;
import net.minecraftforge.registries.*;

public final class TwilightAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES=DeferredRegister.create(ForgeRegistries.ATTRIBUTES,TwilightFoundation.NAMESPACE);
    public static final RegistryObject<Attribute> RESIST_PENETRATION=attribute("spell_res_penetration",0);
    public static final RegistryObject<Attribute> MANA_STEAL=attribute("mana_steal",0);
    public static final RegistryObject<Attribute> MANA_REND=attribute("mana_rend",0);
    public static final RegistryObject<Attribute> MAGIC_CRIT_DAMAGE=attribute("magic_crit_damage",1.5);
    public static final RegistryObject<Attribute> MAGIC_PROJECTILE_DAMAGE=attribute("magic_projectile_damage",1);
    private static RegistryObject<Attribute> attribute(String name,double base){return ATTRIBUTES.register(name,()->new RangedAttribute("attribute.wans_twilight_spellbooks."+name,base,0,100).setSyncable(true));}
    private TwilightAttributes(){}
}
