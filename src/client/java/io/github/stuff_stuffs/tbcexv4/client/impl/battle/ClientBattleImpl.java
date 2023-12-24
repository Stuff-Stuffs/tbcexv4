package io.github.stuff_stuffs.tbcexv4.client.impl.battle;

import io.github.stuff_stuffs.tbcexv4.client.impl.battle.state.env.ClientBattleEnvironmentImpl;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantEventInitEvent;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleStateEventInitEvent;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.env.BattleEnvironment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.CoreBattleTraceEvents;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.turn.TurnManager;
import io.github.stuff_stuffs.tbcexv4.common.api.event.EventMap;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.BattleStateImpl;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ClientBattleImpl implements Battle {
    private final List<BattleAction> actions;
    private final ClientWorld world;
    private final BattleHandle handle;
    private final RegistryKey<World> sourceWorld;
    private final BlockPos min;
    private final EventMap.Builder builder;
    private final EventMap.Builder participantEventBuilder;
    private final int xSize, ySize, zSize;
    private final Supplier<TurnManager> turnManagerFactory;
    private TurnManager turnManager;
    private BattleState state;
    private BattleTracer tracer;

    public ClientBattleImpl(final ClientWorld world, final BattleHandle handle, final RegistryKey<World> sourceWorld, final BlockPos min, final int xSize, final int ySize, final int zSize, final Supplier<TurnManager> factory) {
        turnManagerFactory = factory;
        actions = new ArrayList<>();
        this.world = world;
        this.handle = handle;
        this.sourceWorld = sourceWorld;
        this.min = min;
        builder = EventMap.builder();
        BattleStateEventInitEvent.EVENT.invoker().addEvents(builder);
        participantEventBuilder = EventMap.builder();
        BattleParticipantEventInitEvent.EVENT.invoker().addEvents(participantEventBuilder);
        this.xSize = xSize;
        this.ySize = ySize;
        this.zSize = zSize;
        initialize();
    }

    private void initialize() {
        turnManager = turnManagerFactory.get();
        state = new BattleStateImpl(this, createEnv(), sourceWorld, builder, participantEventBuilder);
        tracer = BattleTracer.create(new CoreBattleTraceEvents.Root());
        try (final var transaction = state.transactionManager().open(); final var span = tracer.push(new CoreBattleTraceEvents.TurnManagerSetup(), transaction)) {
            turnManager.setup(state, transaction, span);
        }
    }

    private BattleEnvironment createEnv() {
        return new ClientBattleEnvironmentImpl(this, world, (xSize + 15) / 16, (ySize + 15) / 16, (zSize + 15) / 16);
    }

    public void trim(final int size) {
        if (actions.size() > size) {
            actions.subList(size, actions.size()).clear();
            initialize();
            for (final BattleAction action : actions) {
                pushAction(action);
            }
        }
    }

    @Override
    public BattleHandle handle() {
        return handle;
    }

    @Override
    public BattleState state() {
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
    public BattlePhase phase() {
        return state != null ? state.phase() : BattlePhase.SETUP;
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

    @Override
    public void pushAction(final BattleAction action) {
        actions.add(action);
        final Optional<BattleParticipantHandle> source = action.source();
        try (final var transaction = state.transactionManager().open(); final var span = tracer.push(new CoreBattleTraceEvents.ActionRoot(source), transaction)) {
            action.apply(state, transaction, tracer);
            source.ifPresent(participantHandle -> turnManager.onAction(participantHandle, state, transaction, span));
            transaction.commit();
        }
    }

    @Override
    public TurnManager turnManager() {
        return turnManager;
    }
}
