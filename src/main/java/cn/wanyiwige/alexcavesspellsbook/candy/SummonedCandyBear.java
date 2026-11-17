package cn.wanyiwige.alexcavesspellsbook.candy;

import com.github.alexmodguy.alexscaves.server.entity.living.GummyBearEntity;
import com.github.alexmodguy.alexscaves.server.entity.util.GummyColors;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;

public final class SummonedCandyBear extends GummyBearEntity implements IMagicSummon {
    private final CandySummonBrain brain=new CandySummonBrain(this,false);
    public SummonedCandyBear(EntityType<? extends Animal> type,Level level){super(type,level);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));}
    public void attach(CandyEffectEntity parent,int color){brain.attach(parent);setGummyColor(GummyColors.fromOrdinal(color));}
    @Override public void tick(){super.tick();brain.tick();}
    @Override public void onUnSummon(){brain.unsummon();}
}
