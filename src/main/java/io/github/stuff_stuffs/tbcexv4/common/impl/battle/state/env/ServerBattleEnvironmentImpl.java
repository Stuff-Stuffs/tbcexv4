package io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.env;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleView;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.ServerBattleImpl;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.BattleStateImpl;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class ServerBattleEnvironmentImpl extends AbstractBattleEnvironmentImpl {
    private final BlockPos.Mutable scratch;

    public ServerBattleEnvironmentImpl(final Battle battle) {
        super(battle);
        scratch = new BlockPos.Mutable();
    }

    public BattleView battle() {
        return battle;
    }

    @Override
    protected void setBlockState0(final int x, final int y, final int z, final BlockState state) {
        ((BattleStateImpl) battle.state()).ensureBattleOngoing();
        scratch.set(battle.worldX(x), battle.worldY(y), battle.worldZ(z));
        ((ServerBattleImpl) battle).world().runAction(world -> world.setBlockState(scratch, state));
    }

    @Override
    protected BlockState getBlockState0(final int x, final int y, final int z) {
        scratch.set(battle.worldX(x), battle.worldY(y), battle.worldZ(z));
        return ((ServerBattleImpl) battle).world().getBlockState(scratch);
    }
}
