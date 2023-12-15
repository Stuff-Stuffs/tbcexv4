package io.github.stuff_stuffs.tbcexv4.common.impl.battle;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantEventInitEvent;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleStateEventInitEvent;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.env.BattleEnvironment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransaction;
import io.github.stuff_stuffs.tbcexv4.common.api.event.EventMap;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.BattleStateImpl;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.env.ServerBattleEnvironmentImpl;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.BattlePersistentState;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.ServerBattleWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServerBattleImpl implements Battle {
    private static final long VERSION = 0;
    private final List<BattleAction> actions;
    private final ServerBattleWorld world;
    private final BattleHandle handle;
    private final RegistryKey<World> sourceWorld;
    private final BlockPos min;
    private final int xSize, ySize, zSize;
    private final BattleState state;
    private final BattleTracer tracer;

    public ServerBattleImpl(final ServerBattleWorld world, final BattleHandle handle, final RegistryKey<World> sourceWorld, final BlockPos min, final int xSize, final int ySize, final int zSize) {
        this.handle = handle;
        this.sourceWorld = sourceWorld;
        actions = new ArrayList<>();
        this.world = world;
        this.min = min;
        final EventMap.Builder builder = EventMap.builder();
        BattleStateEventInitEvent.EVENT.invoker().addEvents(builder);
        final EventMap.Builder participantEventBuilder = EventMap.builder();
        BattleParticipantEventInitEvent.EVENT.invoker().addEvents(participantEventBuilder);
        this.xSize = xSize;
        this.ySize = ySize;
        this.zSize = zSize;
        state = new BattleStateImpl(this, createEnv(), sourceWorld, builder, participantEventBuilder);
        tracer = BattleTracer.create();
    }

    private BattleEnvironment createEnv() {
        return new ServerBattleEnvironmentImpl(this);
    }

    @Override
    public BattleHandle handle() {
        return handle;
    }

    @Override
    public BattleState state() {
        if (state == null) {
            throw new RuntimeException();
        }
        return state;
    }

    @Override
    public int actions() {
        return actions.size();
    }

    @Override
    public BattleAction action(final int index) {
        return actions.get(index);
    }

    @Override
    public void pushAction(final BattleAction action) {
        actions.add(action);
        try(var open = state.transactionManager().open()) {
            action.apply(state, open, tracer);
            open.commit();
        }
    }

    @Override
    public BattlePhase phase() {
        return state != null ? state.phase() : BattlePhase.SETUP;
    }

    public ServerBattleWorld world() {
        return world;
    }

    @Override
    public int worldX(final int localX) {
        return min.getX() + localX;
    }

    @Override
    public int localX(final int worldX) {
        return worldX - min.getX();
    }

    @Override
    public int worldY(final int localY) {
        return min.getY() + localY;
    }

    @Override
    public int localY(final int worldY) {
        return worldY - min.getY();
    }

    @Override
    public int worldZ(final int localZ) {
        return min.getZ() + localZ;
    }

    @Override
    public int localZ(final int worldZ) {
        return worldZ - min.getZ();
    }

    @Override
    public int xSize() {
        return xSize;
    }

    @Override
    public int ySize() {
        return ySize;
    }

    @Override
    public int zSize() {
        return zSize;
    }

    public NbtCompound serialize() {
        final NbtList actionList = new NbtList();
        final Codec<BattleAction> codec = BattleAction.codec(world::getRegistryManager);
        for (final BattleAction action : actions) {
            final NbtCompound wrapper = new NbtCompound();
            wrapper.put("data", codec.encodeStart(NbtOps.INSTANCE, action).getOrThrow(false, Tbcexv4.LOGGER::error));
            actionList.add(wrapper);
        }
        final NbtCompound nbt = new NbtCompound();
        nbt.putLong("version", VERSION);
        final Optional<NbtElement> result = RegistryKey.createCodec(RegistryKeys.WORLD).encodeStart(NbtOps.INSTANCE, sourceWorld).result();
        if (result.isEmpty()) {
            throw new RuntimeException();
        }
        nbt.put("sourceWorld", result.get());
        nbt.put("actions", actionList);
        nbt.putInt("x", xSize);
        nbt.putInt("y", ySize);
        nbt.putInt("z", zSize);
        return nbt;
    }

    public static Optional<Pair<BattlePersistentState.Token, ServerBattleImpl>> deserialize(final NbtCompound nbt, final BattleHandle handle, final ServerBattleWorld world) {
        if (nbt.getLong("version") != VERSION) {
            return Optional.empty();
        }
        final Optional<Pair<BattlePersistentState.Token, BlockPos>> min = Tbcexv4.getBattlePersistentState(world).allocate(nbt.getInt("x"), nbt.getInt("z"), world);
        if (min.isEmpty()) {
            return Optional.empty();
        }
        final Optional<RegistryKey<World>> sourceWorld = RegistryKey.createCodec(RegistryKeys.WORLD).parse(NbtOps.INSTANCE, nbt.get("sourceWorld")).result();
        if (sourceWorld.isEmpty()) {
            throw new RuntimeException();
        }
        final ServerBattleImpl battle = new ServerBattleImpl(world, handle, sourceWorld.get(), min.get().getSecond(), nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
        final NbtList actions = nbt.getList("actions", NbtElement.COMPOUND_TYPE);
        final Codec<BattleAction> codec = BattleAction.codec(world::getRegistryManager);
        for (final NbtElement action : actions) {
            final NbtCompound wrapper = (NbtCompound) action;
            final Optional<BattleAction> result = codec.parse(NbtOps.INSTANCE, wrapper.get("data")).result();
            if (result.isEmpty()) {
                return Optional.empty();
            }
            final BattleAction battleAction = result.get();
            battle.pushAction(battleAction);
        }
        return Optional.of(Pair.of(min.get().getFirst(), battle));
    }
}
