package io.github.stuff_stuffs.tbcexv4.common.api.battle.state.env;

import io.github.stuff_stuffs.event_gen.api.event.gen.EventViewable;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.world.biome.Biome;

@EventViewable(viewClass = BattleEnvironmentView.class)
public interface BattleEnvironment extends BattleEnvironmentView {
    boolean setBlockState(int x, int y, int z, BlockState state, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

    boolean setBiome(int x, int y, int z, Biome biome, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);
}
