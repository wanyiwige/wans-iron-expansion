package cn.wanyiwige.alexcavesspellsbook.candy;

import com.github.alexmodguy.alexscaves.server.entity.living.GingerbreadManEntity;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class SummonedCandyGinger extends GingerbreadManEntity implements IMagicSummon {
    private final CandySummonBrain brain=new CandySummonBrain(this,false);
    public SummonedCandyGinger(EntityType<? extends Monster> type,Level level){super(type,level);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));}
    public void attach(CandyEffectEntity parent,int variant,int teamColor){brain.attach(parent);setVariant(variant%9);setGingerbreadTeamColor(teamColor);}
    @Override public void tick(){super.tick();brain.tick();}
    @Override public void onUnSummon(){brain.unsummon();}
}
