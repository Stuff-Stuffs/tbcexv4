package io.github.stuff_stuffs.tbcexv4.common.impl.battle;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantEventInitEvent;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleStateEventInitEvent;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.event.EventMap;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.BattleStateImpl;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.BattleServerWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BattleImpl implements Battle {
    private static final long VERSION = 0;
    private final List<BattleAction> actions;
    private final BattleServerWorld world;
    private final BattleHandle handle;
    private final BlockPos min;
    private final EventMap.Builder builder;
    private final EventMap.Builder participantEventBuilder;
    private final int xSize, ySize, zSize;
    private BattleState state;
    private BattleTracer tracer;

    public BattleImpl(final BattleServerWorld world, final BattleHandle handle, final BlockPos min, final int xSize, final int ySize, final int zSize) {
        this.handle = handle;
        actions = new ArrayList<>();
        this.world = world;
        this.min = min;
        builder = EventMap.builder();
        participantEventBuilder = EventMap.builder();
        this.xSize = xSize;
        this.ySize = ySize;
        this.zSize = zSize;
        BattleStateEventInitEvent.EVENT.invoker().addEvents(builder);
        BattleParticipantEventInitEvent.EVENT.invoker().addEvents(participantEventBuilder);
        state = new BattleStateImpl(this, builder, participantEventBuilder);
        tracer = BattleTracer.create();
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

    public void trim(final int size) {
        if (actions.size() > size) {
            actions.subList(size, actions.size()).clear();
            state = null;
            state = new BattleStateImpl(this, builder, participantEventBuilder);
            tracer = BattleTracer.create();
            for (final BattleAction action : actions) {
                pushAction(action);
            }
        }
    }

    @Override
    public void pushAction(final BattleAction action) {
        actions.add(action);
        action.apply(state, tracer);
    }

    @Override
    public BattlePhase phase() {
        return state != null ? state.phase() : BattlePhase.SETUP;
    }

    @Override
    public BattleServerWorld world() {
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
        nbt.put("actions", actionList);
        nbt.putInt("x", xSize);
        nbt.putInt("y", ySize);
        nbt.putInt("z", zSize);
        return nbt;
    }

    public static Optional<BattleImpl> deserialize(final NbtCompound nbt, final BattleHandle handle, final BattleServerWorld world) {
        if (nbt.getLong("version") != VERSION) {
            return Optional.empty();
        }
        final Optional<BlockPos> min = Tbcexv4.getBattlePersistentState(world).allocate(nbt.getInt("x"), world);
        if (min.isEmpty()) {
            return Optional.empty();
        }
        final BattleImpl battle = new BattleImpl(world, handle, min.get(), nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
        final NbtList actions = nbt.getList("actions", NbtElement.COMPOUND_TYPE);
        final Codec<BattleAction> codec = BattleAction.codec(world::getRegistryManager);
        for (final NbtElement action : actions) {
            final Optional<BattleAction> result = codec.parse(NbtOps.INSTANCE, action).result();
            if (result.isEmpty()) {
                return Optional.empty();
            }
            final BattleAction battleAction = result.get();
            battle.pushAction(battleAction);
        }
        return Optional.of(battle);
    }
}
