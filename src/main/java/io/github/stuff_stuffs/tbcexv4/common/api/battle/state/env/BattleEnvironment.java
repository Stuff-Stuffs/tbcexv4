package io.github.stuff_stuffs.tbcexv4.common.api.battle.state.env;

import io.github.stuff_stuffs.event_gen.api.event.gen.EventViewable;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import net.minecraft.block.BlockState;

@EventViewable(viewClass = BattleEnvironmentView.class)
public interface BattleEnvironment extends BattleEnvironmentView {
    boolean setBlockState(int x, int y, int z, BlockState state, BattleTracer.Span<?> tracer);
}
