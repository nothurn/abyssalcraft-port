package dev.abyssalport.entity.ai;

import dev.abyssalport.entity.ShoggothEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

/** Reserve a helper's movement for the builder instead of letting its stroll goal overwrite it. */
public final class AssistMonolithGoal extends Goal {
    private final ShoggothEntity helper;
    public AssistMonolithGoal(ShoggothEntity helper) {
        this.helper = helper;
        setFlags(EnumSet.of(Flag.MOVE));
    }
    @Override public boolean canUse() { return helper.isAssisting() && helper.getTarget() == null; }
    @Override public void start() { helper.getNavigation().stop(); }
    @Override public void stop() { helper.getNavigation().stop(); }
}
