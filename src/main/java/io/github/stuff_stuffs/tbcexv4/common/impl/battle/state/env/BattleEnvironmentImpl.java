package io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.env;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.env.BattleEnvironment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.CoreBattleTraceEvents;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.env.BasicEnvEvents;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.BattleStateImpl;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class BattleEnvironmentImpl implements BattleEnvironment {
    private final BlockPos.Mutable scratch;
    private final Battle battle;

    public BattleEnvironmentImpl(final Battle view) {
        battle = view;
        scratch = new BlockPos.Mutable();
    }

    public BattleView battle() {
        return battle;
    }

    @Override
    public boolean setBlockState(final int x, final int y, final int z, final BlockState state, final BattleTracer.Span<?> tracer) {
        ((BattleStateImpl)battle.state()).ensureBattleOngoing();
        scratch.set(battle.worldX(x), battle.worldY(y), battle.worldZ(z));
        final BlockState oldState = battle.world().getBlockState(scratch);
        if (oldState == state) {
            return true;
        }
        if (!battle.state().events().invoker(BasicEnvEvents.PRE_SET_BLOCK_STATE_EVENT_KEY).onPreSetBlockStateEvent(battle.state(), x, y, z, state, tracer)) {
            return false;
        }
        battle.world().runAction(world -> world.setBlockState(scratch, state));
        try (final var span = tracer.push(new CoreBattleTraceEvents.SetBlockState(x, y, z, oldState, state))) {
            battle.state().events().invoker(BasicEnvEvents.POST_SET_BLOCK_STATE_EVENT_KEY).onPostSetBlockStateEvent(battle.state(), x, y, z, oldState, span);
        }
        return true;
    }

    @Override
    public BlockState blockState(final int x, final int y, final int z) {
        scratch.set(battle.worldX(x), battle.worldY(y), battle.worldZ(z));
        return battle.world().getBlockState(scratch);
    }
}
