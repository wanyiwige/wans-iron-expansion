package cn.wanyiwige.alexcavesspellsbook.candy;

import com.github.alexmodguy.alexscaves.server.entity.living.GumbeeperEntity;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** The native cannon model/charge dial, with its self-explosion AI removed. */
public final class SummonedCandySentry extends GumbeeperEntity implements IMagicSummon {
    private final CandySummonBrain brain=new CandySummonBrain(this,true);
    public SummonedCandySentry(EntityType<? extends Monster> type,Level level){super(type,level);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));}
    public void attach(CandyEffectEntity parent){brain.attach(parent);setCharged(false);setExploding(false);}
    @Override public void tick(){setExploding(false);super.tick();setExploding(false);brain.tick();}
    @Override public void onUnSummon(){brain.unsummon();}
}
