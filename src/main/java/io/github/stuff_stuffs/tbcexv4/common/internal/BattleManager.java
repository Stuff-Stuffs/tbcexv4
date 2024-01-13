package io.github.stuff_stuffs.tbcexv4.common.internal;

import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.ai.ActionSearchStrategy;
import io.github.stuff_stuffs.tbcexv4.common.api.ai.Scorer;
import io.github.stuff_stuffs.tbcexv4.common.api.ai.Scorers;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.request.BattleActionRequest;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.request.BattleActionRequestType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransaction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.turn.TurnManagerType;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Tbcexv4Util;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.BasicEvents;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.ServerBattleImpl;
import io.github.stuff_stuffs.tbcexv4.common.internal.network.BattleUpdatePacket;
import io.github.stuff_stuffs.tbcexv4.common.internal.network.WatchRequestResponsePacket;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.BattlePersistentState;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.ServerBattleWorld;
import it.unimi.dsi.fastutil.objects.Object2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BattleManager implements AutoCloseable {
    //Five minutes
    private static final long TIME_OUT = 20 * 60 * 5;
    private final Map<BattleHandle, LoadedBattle> loadedBattles;
    private final Map<UUID, LoadedPlayerData> loadedPlayerData;
    private final ServerBattleWorld world;
    private final ServerWorld sourceWorld;
    private final Path battleDirectory;
    private final Path dataDirectory;
    private final Map<BattleHandle, AiTask> ongoingAi;
    private long ticks;

    public BattleManager(final ServerBattleWorld world, final Path battleDirectory, final Path dataDirectory) {
        this.world = world;
        this.battleDirectory = battleDirectory;
        this.dataDirectory = dataDirectory;
        loadedBattles = new Object2ReferenceLinkedOpenHashMap<>();
        loadedPlayerData = new Object2ReferenceLinkedOpenHashMap<>();
        sourceWorld = world.getServer().getWorld(world.sourceKey());
        ongoingAi = new Object2ReferenceOpenHashMap<>();
    }

    public <P> Optional<Battle> createBattle(final int width, final int height, final int depth, final TurnManagerType<P> type, final P parameter) {
        final Optional<Pair<BattlePersistentState.Token, BlockPos>> allocation = Tbcexv4.getBattlePersistentState(sourceWorld).allocate(width, depth, world);
        if (allocation.isEmpty()) {
            return Optional.empty();
        }
        final Random random = sourceWorld.random;
        BattleHandle handle;
        do {
            handle = new BattleHandle(world.sourceKey(), new UUID(random.nextLong(), random.nextLong()));
        } while (getOrLoadBattle(handle).isPresent());
        final ServerBattleImpl battle = new ServerBattleImpl(world, handle, world.sourceKey(), allocation.get().getSecond(), width, height, depth, new ServerBattleImpl.TurnManagerContainer<>(type, parameter), createAiController(handle));
        setupListeners(handle, battle.state());
        final LoadedBattle loadedBattle = new LoadedBattle(battle, allocation.get().getFirst(), ticks);
        loadedBattles.put(handle, loadedBattle);
        return Optional.of(battle);
    }

    private void setupListeners(final BattleHandle handle, final BattleState state) {
        state.events().registerTerminalView(BasicEvents.POST_ADD_PARTICIPANT_EVENT_KEY, (battleState, participant, transactionContext, span) -> addBattle(participant.handle().id(), handle));
        state.events().registerTerminalView(BasicEvents.POST_REMOVE_PARTICIPANT_EVENT_KEY, (battleState, participantHandle, reason, transactionContext, span) -> resolveBattle(participantHandle.id(), handle));
        state.events().registerTerminalView(BasicEvents.END_BATTLE_KEY, (battleState, transactionContext, span) -> {
            for (final BattleParticipantHandle participant : battleState.participants()) {
                resolveBattle(participant.id(), handle);
            }
        });
    }

    public Optional<? extends Battle> getOrLoadBattle(final BattleHandle handle) {
        if (!handle.sourceWorld().equals(world.sourceKey())) {
            return Optional.empty();
        }
        final LoadedBattle battle = loadedBattles.get(handle);
        if (battle != null) {
            battle.lastAccessed = ticks;
            return Optional.of(battle.battle);
        }
        try {
            final NbtCompound compound = NbtIo.readCompressed(handle.toPath(battleDirectory), NbtTagSizeTracker.ofUnlimitedBytes());
            final Optional<Pair<BattlePersistentState.Token, ServerBattleImpl>> deserialized = ServerBattleImpl.deserialize(compound, handle, world, createAiController(handle), this::setupListeners);
            if (deserialized.isPresent()) {
                final Pair<BattlePersistentState.Token, ServerBattleImpl> pair = deserialized.get();
                loadedBattles.put(handle, new LoadedBattle(pair.getSecond(), pair.getFirst(), ticks));
                pair.getSecond().setLoading(false);
                return Optional.of(pair.getSecond());
            }
        } catch (final IOException e) {
            if (!(e instanceof NoSuchFileException)) {
                Tbcexv4.LOGGER.error("Error while loading battle!", e);
            }
        }
        return Optional.empty();
    }

    private ServerBattleImpl.AiController createAiController(final BattleHandle handle) {
        final ActionSearchStrategy strategy = ActionSearchStrategy.basic(1);
        return new ServerBattleImpl.AiController() {

            @Override
            public void compute(final BattleParticipantHandle pHandle, final BattleTransaction context, final BattleTracer tracer) {
                final AiTask removed = ongoingAi.remove(handle);
                if (removed != null) {
                    BattleManager.cancel(removed);
                }
                final Runnable cleanup = () -> {
                    context.abort();
                    context.close();
                };
                final ServerBattleImpl battle = loadedBattles.get(handle).battle;
                final BattleParticipant participant = battle.state().participant(pHandle);
                final CompletableFuture<CompletableFuture<Unit>> cancellation = new CompletableFuture<>();
                final CompletableFuture<Optional<BattleAction>> future = CompletableFuture.supplyAsync(() -> {
                    final Scorer scorer = Scorer.sum(Scorers.health(pHandle), Scorers.teamHealth(pHandle), Scorers.enemyTeamHealth(pHandle));
                    final Optional<BattleActionRequest> search = strategy.search(participant, scorer, tracer, context, tracer.latest().hashCode(), cancellation);
                    return search.map(req -> extract(req.type(), req));
                }, ((ServerExtensions) world.getServer()).tbcexv4$getBackgroundExecutor());
                ongoingAi.put(handle, new AiTask(future, cancellation, cleanup));
            }

            @Override
            public void cancel() {
                final AiTask removed = ongoingAi.remove(handle);
                if (removed != null) {
                    BattleManager.cancel(removed);
                }
            }
        };
    }

    private static void cancel(final AiTask task) {
        if (task.actionFuture.isDone()) {
            task.cleanup.run();
        } else {
            final CompletableFuture<Unit> wait = new CompletableFuture<>();
            task.cancellationFuture.complete(wait);
            try {
                wait.join();
            } catch (final Exception e) {
                Tbcexv4.LOGGER.error("Error while awaiting ai task!", e);
            }
            task.cleanup.run();
        }
    }

    private static <T extends BattleActionRequest> BattleAction extract(final BattleActionRequestType<T> type, final BattleActionRequest request) {
        return type.extract((T) request);
    }

    public Set<BattleHandle> resolvedBattles(final UUID id) {
        final LoadedPlayerData data = getOrCreateData(id);
        return Collections.unmodifiableSet(data.battles);
    }

    public Set<BattleHandle> unresolvedBattles(final UUID id) {
        final LoadedPlayerData data = getOrCreateData(id);
        return Collections.unmodifiableSet(data.unresolvedBattles);
    }

    public void addBattle(final UUID id, final BattleHandle handle) {
        getOrCreateData(id).unresolvedBattles.add(handle);
    }

    public void resolveBattle(final UUID id, final BattleHandle handle) {
        getOrCreateData(id).unresolvedBattles.remove(handle);
        getOrCreateData(id).battles.add(handle);
    }

    private LoadedPlayerData getOrCreateData(final UUID id) {
        LoadedPlayerData data = loadedPlayerData.get(id);
        if (data != null) {
            return data;
        }
        try {
            final NbtCompound compound = NbtIo.readCompressed(idToFilename(id), NbtTagSizeTracker.ofUnlimitedBytes());
            final Optional<LoadedPlayerData> result = LoadedPlayerData.CODEC.parse(NbtOps.INSTANCE, compound.get("data")).result();
            if (result.isPresent()) {
                data = result.get();
                data.lastAccessed = ticks;
                loadedPlayerData.put(id, data);
                return data;
            }
        } catch (final Throwable e) {
            if (!(e instanceof NoSuchFileException)) {
                Tbcexv4.LOGGER.error("Error while loading participant data!", e);
            }
        }
        data = new LoadedPlayerData();
        loadedPlayerData.put(id, data);
        data.lastAccessed = ticks;
        return data;
    }

    private Path idToFilename(final UUID id) {
        return Tbcexv4Util.resolveRegistryKey(dataDirectory, sourceWorld.getRegistryKey()).resolve(id.toString() + ".battle_data");
    }

    public void tick() {
        for (final Iterator<Map.Entry<BattleHandle, AiTask>> iterator = ongoingAi.entrySet().iterator(); iterator.hasNext(); ) {
            final Map.Entry<BattleHandle, AiTask> entry = iterator.next();
            final AiTask task = entry.getValue();
            if (task.actionFuture.isDone()) {
                try {
                    final BattleHandle key = entry.getKey();
                    final Optional<BattleAction> action = task.actionFuture.get();
                    task.cleanup.run();
                    iterator.remove();
                    if (action.isPresent()) {
                        final LoadedBattle battle = loadedBattles.get(key);
                        if (battle != null) {
                            battle.battle.pushAction(action.get());
                        } else {
                            Tbcexv4.LOGGER.error("Ai action for not loaded battle!");
                        }
                    } else {
                        Tbcexv4.LOGGER.error("Not ai actions available!");
                    }
                } catch (final Exception e) {
                    Tbcexv4.LOGGER.error("Error while getting ai action!", e);
                }
            }
        }
        ticks++;
        final BattleCodecContext codecContext = BattleCodecContext.create(world.getRegistryManager());
        final Codec<BattleAction> codec = BattleAction.codec(codecContext);
        for (final ServerPlayerEntity player : List.copyOf(world.getPlayers())) {
            final BattleHandle handle = ((ServerPlayerExtensions) player).tbcexv4$watching();
            if (handle == null) {
                final GameMode mode = player.interactionManager.getPreviousGameMode();
                player.changeGameMode(mode == null ? player.server.getDefaultGameMode() : mode);
                player.networkHandler.player = player.getServer().getPlayerManager().respawnPlayer(player, true);
            } else {
                final Optional<? extends Battle> battle = getOrLoadBattle(handle);
                if (battle.isEmpty()) {
                    ((ServerPlayerExtensions) player).tbcev4$setWatching(null);
                    ServerPlayNetworking.send(player, WatchRequestResponsePacket.createEmpty());
                    player.networkHandler.player = player.getServer().getPlayerManager().respawnPlayer(player, true);
                } else {
                    player.changeGameMode(GameMode.SPECTATOR);
                    final int index = ((ServerPlayerExtensions) player).tbcexv4$watchIndex();
                    final int actionCount = battle.get().actions();
                    if (index < actionCount) {
                        for (int i = index; i < actionCount; i++) {
                            final Optional<NbtElement> result = codec.encodeStart(NbtOps.INSTANCE, battle.get().action(i)).result();
                            if (result.isEmpty()) {
                                throw new RuntimeException();
                            }
                            ServerPlayNetworking.send(player, new BattleUpdatePacket(handle, i, result.get()));
                        }
                        ((ServerPlayerExtensions) player).tbcexv4$setWatchIndex(actionCount);
                    }
                }
            }
        }
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
        final Path path = handle.toPath(battleDirectory);
        return CompletableFuture.supplyAsync(() -> {
            try {
                final Path parent = path.getParent();
                if (!Files.isDirectory(parent)) {
                    Files.createDirectories(parent);
                }
                NbtIo.writeCompressed(serialized, path);
            } catch (final Throwable e) {
                return new BattleSaveResult(handle, null, e);
            }
            return new BattleSaveResult(handle, null, null);
        }, Util.getIoWorkerExecutor());
    }

    private CompletableFuture<BattleSaveResult> saveBattle(final BattleHandle handle) {
        final LoadedBattle battle = loadedBattles.remove(handle);
        if (battle == null) {
            return CompletableFuture.completedFuture(new BattleSaveResult(handle, null, null));
        }
        final AiTask task = ongoingAi.get(handle);
        if (task != null) {
            cancel(task);
        }
        return writeBattle(handle, battle.battle).thenApply(result -> {
            if (result.error == null) {
                return new BattleSaveResult(result.handle, battle.token, null);
            }
            return result;
        });
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
            if (result.token != null) {
                Tbcexv4.getBattlePersistentState(sourceWorld).deallocate(result.token);
            }
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
        final Path path = idToFilename(id);
        return CompletableFuture.supplyAsync(() -> {
            try {
                final Path parent = path.getParent();
                if (!Files.isDirectory(parent)) {
                    Files.createDirectories(parent);
                }
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

    public void reload(final List<ServerPlayerEntity> players) {
        final Set<BattleHandle> active = new ObjectOpenHashSet<>(loadedBattles.keySet());
        saveBattles(active);
        for (final ServerPlayerEntity player : players) {
            final BattleHandle handle = ((ServerPlayerExtensions) player).tbcexv4$watching();
            if (handle == null || !handle.sourceWorld().equals(sourceWorld.getRegistryKey())) {
                continue;
            }
            final Optional<? extends Battle> opt = getOrLoadBattle(handle);
            if (opt.isPresent()) {
                ((ServerPlayerExtensions) player).tbcev4$setWatching(null);
                ((ServerPlayerExtensions) player).tbcev4$setWatching(handle);
            } else {
                ((ServerPlayerExtensions) player).tbcev4$setWatching(null);
            }
        }
    }

    private static final class EncodeException extends RuntimeException {
    }

    @Override
    public void close() {
        for (final AiTask value : ongoingAi.values()) {
            cancel(value);
        }
        ongoingAi.clear();
        saveBattles(Set.copyOf(loadedBattles.keySet()));
        saveData(Set.copyOf(loadedPlayerData.keySet()));
        sourceWorld.getPersistentStateManager().save();
    }

    private static final class LoadedBattle {
        private final ServerBattleImpl battle;
        private final BattlePersistentState.Token token;
        private long lastAccessed;

        private LoadedBattle(final ServerBattleImpl battle, final BattlePersistentState.Token token, final long ticks) {
            this.battle = battle;
            this.token = token;
            lastAccessed = ticks;
        }
    }

    private static final class LoadedPlayerData {
        private static final Codec<LoadedPlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BattleHandle.CODEC.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("battles").forGetter(data -> data.battles),
                BattleHandle.CODEC.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("unresolved").forGetter(data -> data.unresolvedBattles)
        ).apply(instance, LoadedPlayerData::new));
        private final Set<BattleHandle> battles;
        private final Set<BattleHandle> unresolvedBattles;
        private long lastAccessed;

        private LoadedPlayerData(final Set<BattleHandle> battles, final Set<BattleHandle> unresolvedBattles) {
            this.battles = new ObjectOpenHashSet<>(battles);
            this.unresolvedBattles = new ObjectOpenHashSet<>(unresolvedBattles);
            lastAccessed = -1;
        }

        private LoadedPlayerData() {
            battles = new ObjectOpenHashSet<>();
            unresolvedBattles = new ObjectOpenHashSet<>();
            lastAccessed = -1;
        }
    }

    private record BattleSaveResult(
            BattleHandle handle,
            BattlePersistentState.Token token,
            @Nullable Throwable error
    ) {
    }


    private record DataSaveResult(UUID id, @Nullable Throwable error) {
    }

    private record AiTask(
            CompletableFuture<Optional<BattleAction>> actionFuture,
            CompletableFuture<CompletableFuture<Unit>> cancellationFuture,
            Runnable cleanup
    ) {
    }
}
