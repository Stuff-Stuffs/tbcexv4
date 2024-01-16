package io.github.stuff_stuffs.tbcexv4.common.api.battle.state.env;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.Pather;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.BlockView;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

public interface BattleEnvironmentView {
    BlockState blockState(int x, int y, int z);

    RegistryEntry<Biome> biome(int x, int y, int z);

    BlockView asBlockView();

    void cachePaths(BattleParticipantHandle handle, Pather.Paths paths);

    @Nullable Pather.Paths lookupCachedPaths(BattleParticipantHandle handle);
}
