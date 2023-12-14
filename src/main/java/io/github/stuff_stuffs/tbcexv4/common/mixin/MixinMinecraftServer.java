package io.github.stuff_stuffs.tbcexv4.common.mixin;

import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.ServerBattleWorld;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.VoidChunkGenerator;
import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.ServerDynamicRegistryType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.level.UnmodifiableLevelProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {
    @Shadow
    @Final
    private Map<RegistryKey<World>, ServerWorld> worlds;

    @Shadow
    @Final
    private Executor workerExecutor;

    @Shadow
    @Final
    protected LevelStorage.Session session;

    @Shadow
    @Final
    protected SaveProperties saveProperties;

    @Shadow
    public abstract ServerWorld getOverworld();

    @Shadow
    public abstract CombinedDynamicRegistries<ServerDynamicRegistryType> getCombinedDynamicRegistries();

    @Inject(method = "createWorlds", at = @At("RETURN"))
    private void createBattleWorlds(final WorldGenerationProgressListener worldGenerationProgressListener, final CallbackInfo ci) {
        final UnmodifiableLevelProperties unmodifiableLevelProperties = new UnmodifiableLevelProperties(saveProperties, saveProperties.getMainWorldProperties());
        final ServerWorld overworld = getOverworld();
        final ChunkGenerator generator = new VoidChunkGenerator(new FixedBiomeSource(getCombinedDynamicRegistries().getCombinedRegistryManager().get(RegistryKeys.BIOME).getEntry(BiomeKeys.PLAINS).get()));
        for (final Map.Entry<RegistryKey<World>, ServerWorld> entry : worlds.entrySet().stream().toList()) {
            final RegistryKey<World> key = Tbcexv4.battleWorldKey(entry.getKey());
            worlds.put(key, new ServerBattleWorld((MinecraftServer) (Object) this, workerExecutor, session, unmodifiableLevelProperties, key, new DimensionOptions(entry.getValue().getDimensionEntry(), generator), worldGenerationProgressListener, false, 0, List.of(), false, overworld.getRandomSequences()));
        }
    }
}
