package io.github.stuff_stuffs.tbcexv4.common.api.battle;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleStateView;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.BattleServerWorld;

public interface BattleView {
    BattleHandle handle();

    BattleStateView state();

    int actions();

    BattleAction action(int index);

    BattlePhase phase();

    BattleServerWorld world();

    int worldX(int localX);

    int localX(int worldX);

    int worldY(int localY);

    int localY(int worldY);

    int worldZ(int localZ);

    int localZ(int worldZ);

    int xSize();

    int ySize();

    int zSize();
}
