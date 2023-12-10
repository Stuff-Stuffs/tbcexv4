package io.github.stuff_stuffs.tbcexv4.common.internal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.ServerBattleImpl;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.ServerBattleWorld;
import it.unimi.dsi.fastutil.objects.Object2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BattleManager implements AutoCloseable {
    //Five minutes
    private static final long TIME_OUT = 20 * 60 * 5;
    private final Map<BattleHandle, LoadedBattle> loadedBattles;
    private final Map<UUID, LoadedPlayerData> loadedPlayerData;
    private final ServerBattleWorld world;
    private final Path battleDirectory;
    private final Path dataDirectory;
    private long ticks;

    public BattleManager(final ServerBattleWorld world, final Path battleDirectory, final Path dataDirectory) {
        this.world = world;
        this.battleDirectory = battleDirectory;
        this.dataDirectory = dataDirectory;
        loadedBattles = new Object2ReferenceLinkedOpenHashMap<>();
        loadedPlayerData = new Object2ReferenceLinkedOpenHashMap<>();
    }

    public Optional<Battle> createBattle(final BattleHandle handle, final RegistryKey<World> sourceWorld, final int width, final int height, final int depth) {
        final Optional<BlockPos> allocation = Tbcexv4.getBattlePersistentState(world).allocate(width, world);
        if (allocation.isEmpty()) {
            return Optional.empty();
        }
        final ServerBattleImpl battle = new ServerBattleImpl(world, handle, sourceWorld, allocation.get(), width, height, depth);
        final LoadedBattle loadedBattle = new LoadedBattle(battle, ticks);
        loadedBattles.put(handle, loadedBattle);
        return Optional.of(battle);
    }

    public Optional<? extends Battle> getOrLoadBattle(final BattleHandle handle) {
        final LoadedBattle battle = loadedBattles.get(handle);
        if (battle != null) {
            battle.lastAccessed = ticks;
            return Optional.of(battle.battle);
        }
        try {
            final NbtCompound compound = NbtIo.readCompressed(battleDirectory.resolve(handle.toFileName()), NbtTagSizeTracker.ofUnlimitedBytes());
            final Optional<ServerBattleImpl> deserialized = ServerBattleImpl.deserialize(compound, handle, world);
            if (deserialized.isPresent()) {
                loadedBattles.put(handle, new LoadedBattle(deserialized.get(), ticks));
                return deserialized;
            }
        } catch (final IOException e) {
            Tbcexv4.LOGGER.error("Error while loading battle!", e);
        }
        return Optional.empty();
    }

    public Set<BattleHandle> participantBattles(final UUID id) {
        final LoadedPlayerData data = getOrCreateData(id);
        return Collections.unmodifiableSet(data.battles);
    }

    private LoadedPlayerData getOrCreateData(final UUID id) {
        LoadedPlayerData data = loadedPlayerData.get(id);
        if (data != null) {
            return data;
        }
        try {
            final NbtCompound compound = NbtIo.readCompressed(dataDirectory.resolve(idToFilename(id)), NbtTagSizeTracker.ofUnlimitedBytes());
            final Optional<LoadedPlayerData> result = LoadedPlayerData.CODEC.parse(NbtOps.INSTANCE, compound).result();
            if (result.isPresent()) {
                data = result.get();
                data.lastAccessed = ticks;
                loadedPlayerData.put(id, data);
                return data;
            }
        } catch (final Throwable e) {
            Tbcexv4.LOGGER.error("Error while loading participant data!", e);
        }
        data = new LoadedPlayerData();
        data.lastAccessed = ticks;
        return data;
    }

    private static String idToFilename(final UUID id) {
        return id.toString() + ".battle_data";
    }

    public void tick() {
        ticks++;
        final Set<BattleHandle> battlesToUnload = new ObjectOpenHashSet<>();
        for (final Map.Entry<BattleHandle, LoadedBattle> entry : loadedBattles.entrySet()) {
            if (entry.getValue().lastAccessed + TIME_OUT < ticks) {
                battlesToUnload.add(entry.getKey());
            }
        }
        saveBattles(battlesToUnload);
        final Set<UUID> dataToUnload = new ObjectOpenHashSet<>();
        for (final Map.Entry<UUID, LoadedPlayerData> entry : loadedPlayerData.entrySet()) {
            if (entry.getValue().lastAccessed + TIME_OUT < ticks) {
                dataToUnload.add(entry.getKey());
            }
        }
        saveData(dataToUnload);
    }

    private CompletableFuture<BattleSaveResult> writeBattle(final BattleHandle handle, final ServerBattleImpl battle) {
        final NbtCompound serialized = battle.serialize();
        final Path path = battleDirectory.resolve(handle.toFileName());
        return CompletableFuture.supplyAsync(() -> {
            try {
                NbtIo.writeCompressed(serialized, path);
            } catch (final Throwable e) {
                return new BattleSaveResult(handle, e);
            }
            return new BattleSaveResult(handle, null);
        }, Util.getIoWorkerExecutor());
    }

    private CompletableFuture<BattleSaveResult> saveBattle(final BattleHandle handle) {
        final LoadedBattle battle = loadedBattles.remove(handle);
        if (battle == null) {
            return CompletableFuture.completedFuture(new BattleSaveResult(handle, null));
        }
        return writeBattle(handle, battle.battle);
    }

    private void saveBattles(final Set<BattleHandle> handles) {
        if (handles.isEmpty()) {
            return;
        }
        final List<CompletableFuture<BattleSaveResult>> futures = new ArrayList<>();
        for (final BattleHandle handle : handles) {
            futures.add(saveBattle(handle));
        }
        final CompletableFuture<List<BattleSaveResult>> future = Util.combine(futures);
        final List<BattleSaveResult> results = future.join();
        for (final BattleSaveResult result : results) {
            if (result.error != null) {
                Tbcexv4.LOGGER.error("Error while saving battle with id {}!", result.handle, result.error);
            }
        }
    }

    private CompletableFuture<DataSaveResult> writeData(final UUID id, final LoadedPlayerData data) {
        final Optional<NbtElement> result = LoadedPlayerData.CODEC.encodeStart(NbtOps.INSTANCE, data).result();
        if (result.isEmpty()) {
            return CompletableFuture.completedFuture(new DataSaveResult(id, new EncodeException()));
        }
        final NbtCompound wrapper = new NbtCompound();
        wrapper.put("data", result.get());
        final Path path = battleDirectory.resolve(idToFilename(id));
        return CompletableFuture.supplyAsync(() -> {
            try {
                NbtIo.writeCompressed(wrapper, path);
            } catch (final Throwable e) {
                return new DataSaveResult(id, e);
            }
            return new DataSaveResult(id, null);
        }, Util.getIoWorkerExecutor());
    }

    private CompletableFuture<DataSaveResult> saveData(final UUID id) {
        final LoadedPlayerData data = loadedPlayerData.remove(id);
        if (data == null) {
            return CompletableFuture.completedFuture(new DataSaveResult(id, null));
        }
        return writeData(id, data);
    }

    private void saveData(final Set<UUID> ids) {
        if (ids.isEmpty()) {
            return;
        }
        final List<CompletableFuture<DataSaveResult>> futures = new ArrayList<>();
        for (final UUID handle : ids) {
            futures.add(saveData(handle));
        }
        final CompletableFuture<List<DataSaveResult>> future = Util.combine(futures);
        final List<DataSaveResult> results = future.join();
        for (final DataSaveResult result : results) {
            if (result.error != null) {
                Tbcexv4.LOGGER.error("Error while saving data with id {}!", result.id, result.error);
            }
        }
    }

    private static final class EncodeException extends RuntimeException {
    }

    @Override
    public void close() {
        saveBattles(Set.copyOf(loadedBattles.keySet()));
        saveData(Set.copyOf(loadedPlayerData.keySet()));
    }

    private static final class LoadedBattle {
        private final ServerBattleImpl battle;
        private long lastAccessed;

        private LoadedBattle(final ServerBattleImpl battle, final long ticks) {
            this.battle = battle;
            lastAccessed = ticks;
        }
    }

    private static final class LoadedPlayerData {
        private static final Codec<LoadedPlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BattleHandle.CODEC.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("battles").forGetter(data -> data.battles)
        ).apply(instance, LoadedPlayerData::new));
        private final Set<BattleHandle> battles;
        private long lastAccessed;

        private LoadedPlayerData(final Set<BattleHandle> battles) {
            this.battles = new ObjectOpenHashSet<>(battles);
            lastAccessed = -1;
        }

        private LoadedPlayerData() {
            battles = new ObjectOpenHashSet<>();
            lastAccessed = -1;
        }
    }

    private static final class BattleSaveResult {
        private final BattleHandle handle;
        private final @Nullable Throwable error;

        private BattleSaveResult(final BattleHandle handle, final @Nullable Throwable error) {
            this.handle = handle;
            this.error = error;
        }
    }

    private static final class DataSaveResult {
        private final UUID id;
        private final @Nullable Throwable error;

        private DataSaveResult(final UUID id, final @Nullable Throwable error) {
            this.id = id;
            this.error = error;
        }
    }
}
