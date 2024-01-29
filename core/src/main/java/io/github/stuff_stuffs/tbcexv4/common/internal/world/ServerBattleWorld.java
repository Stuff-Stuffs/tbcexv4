package io.github.stuff_stuffs.tbcexv4.common.internal.world;

import io.github.stuff_stuffs.tbcexv4.common.internal.BattleManager;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.spawner.SpecialSpawner;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class ServerBattleWorld extends ServerWorld {
    private final RegistryKey<World> sourceKey;
    private final BattleManager battleManager;
    private boolean modifiable = false;

    public ServerBattleWorld(final MinecraftServer server, final Executor workerExecutor, final LevelStorage.Session session, final ServerWorldProperties properties, final RegistryKey<World> worldKey, final DimensionOptions dimensionOptions, final WorldGenerationProgressListener worldGenerationProgressListener, final boolean debugWorld, final long seed, final List<SpecialSpawner> spawners, final boolean shouldTickTime, @Nullable final RandomSequencesState randomSequencesState) {
        super(server, workerExecutor, session, properties, worldKey, dimensionOptions, worldGenerationProgressListener, debugWorld, seed, spawners, shouldTickTime, randomSequencesState);
        final Path directory = session.getDirectory().path().resolve("tbcexv4");
        sourceKey = RegistryKey.of(RegistryKeys.WORLD, Tbcexv4.parseGeneratedId(worldKey.getValue()));
        battleManager = new BattleManager(this, directory.resolve("battles"), directory.resolve("data"));
    }

    public void setModifiable(final boolean modifiable) {
        this.modifiable = modifiable;
    }

    @Override
    public boolean setBlockState(final BlockPos pos, final BlockState state, final int flags, final int maxUpdateDepth) {
        if (modifiable) {
            return super.setBlockState(pos, state, flags, maxUpdateDepth);
        } else {
            return false;
        }
    }

    @Override
    public void tick(final BooleanSupplier shouldKeepTicking) {
        super.tick(shouldKeepTicking);
        battleManager.tick();
    }

    @Override
    public void close() throws IOException {
        battleManager.close();
        for (Entity entity : this.iterateEntities()) {
            if(entity instanceof ServerPlayerEntity player) {
                player.moveToWorld(getServer().getWorld(sourceKey()));
            }
        }
        super.close();
    }

    @Override
    public void save(@Nullable ProgressListener progressListener, boolean flush, boolean savingDisabled) {
    }

    public BattleManager battleManager() {
        return battleManager;
    }

    public RegistryKey<World> sourceKey() {
        return sourceKey;
    }

    public void runAction(final Consumer<World> consumer) {
        setModifiable(true);
        consumer.accept(this);
        setModifiable(false);
    }
}
