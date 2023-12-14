package io.github.stuff_stuffs.tbcexv4.common.api.battle.state.env;

import net.minecraft.block.BlockState;
import net.minecraft.world.biome.Biome;

public interface BattleEnvironmentView {
    BlockState blockState(int x, int y, int z);

    Biome biome(int x, int y, int z);
}
