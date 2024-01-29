package io.github.stuff_stuffs.tbcexv4.client.impl.battle;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationFactoryRegistry;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationQueue;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleRenderState;
import io.github.stuff_stuffs.tbcexv4.client.impl.battle.state.env.ClientBattleEnvironmentImpl;
import io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.AnimationQueueImpl;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.ActionSource;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantEventInitEvent;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleStateEventInitEvent;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.env.BattleEnvironment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.turn.TurnManager;
import io.github.stuff_stuffs.tbcexv4.common.api.event.EventMap;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import io.github.stuff_stuffs.tbcexv4.common.generated_traces.ActionRootTrace;
import io.github.stuff_stuffs.tbcexv4.common.generated_traces.RootTrace;
import io.github.stuff_stuffs.tbcexv4.common.generated_traces.TurnManagerSetupTrace;
import io.github.stuff_stuffs.tbcexv4.common.impl.BattleLogContextImpl;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.BattleStateImpl;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import io.github.stuff_stuffs.tbcexv4util.log.BattleLogLevel;
import io.github.stuff_stuffs.tbcexv4util.trace.BattleTracerView;
import it.unimi.dsi.fastutil.doubles.Double2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectSortedMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectSortedMaps;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
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
    private AnimationQueueImpl queue;
    private double time = 0;
    private final Double2ObjectSortedMap<List<BattleTracerView.Timestamp>> timestamps;
    private BattleLogContextImpl context;

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
        timestamps = new Double2ObjectAVLTreeMap<>();
        initialize();
    }

    public void tick() {
        for (final Double2ObjectMap.Entry<List<BattleTracerView.Timestamp>> entry : Double2ObjectSortedMaps.fastIterable(timestamps.tailMap(time))) {
            if (entry.getDoubleKey() < time + 1) {
                for (final BattleTracerView.Timestamp timestamp : entry.getValue()) {
                    final Text message = context.at(timestamp);
                    if (message != null) {
                        MinecraftClient.getInstance().player.sendMessage(message, false);
                    }
                }
            }
        }
        time = time + 1;
    }

    private void initialize() {
        timestamps.clear();
        context = new BattleLogContextImpl(BattleLogLevel.DEBUG);
        turnManager = turnManagerFactory.get();
        state = new BattleStateImpl(this, createEnv(), sourceWorld, builder, participantEventBuilder);
        tracer = BattleTracer.create(new RootTrace());
        queue = new AnimationQueueImpl();
        final BattleTracerView.Handle<?> h;
        try (final var transaction = state.transactionManager().open(); final var span = tracer.push(new TurnManagerSetupTrace(), transaction)) {
            turnManager.setup(state, transaction, span);
            transaction.commit();
            h = span.node().handle();
        }
        tracer.byHandle(h).event().log(tracer, h, context);
        tracer.all().forEach(node -> AnimationFactoryRegistry.create(node.event()).map(this::pushAnimation).ifPresent(t -> timestamps.computeIfAbsent(t, l -> new ArrayList<>()).add(node.timestamp())));
    }

    public AnimationQueue animationQueue() {
        return queue;
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
        final Optional<ActionSource> actionSource = action.source();
        final BattleTracerView.Handle<?> h;
        final BattleTracerView.Node<?> latest = tracer.latest();
        try (final var transaction = state.transactionManager().open(); final var span = tracer.push(new ActionRootTrace(actionSource.map(ActionSource::actor)), transaction)) {
            action.apply(state, transaction, span);
            actionSource.ifPresent(source -> turnManager.onAction(source.energy(), source.actor(), state, transaction, span));
            transaction.commit();
            h = span.node().handle();
        }
        tracer.byHandle(h).event().log(tracer, h, context);
        tracer.after(latest.timestamp()).forEach(node -> AnimationFactoryRegistry.create(node.event()).map(this::pushAnimation).ifPresent(t -> timestamps.computeIfAbsent(t, l -> new ArrayList<>()).add(node.timestamp())));
    }

    private double pushAnimation(final Animation<BattleRenderState> animation) {
        final double currentTime = time(0);
        final double t = queue.enqueue(new Animation<BattleRenderState>() {
            @Override
            public Result<List<TimedEvent>, Unit> animate(final double time, final BattleRenderState state, final AnimationContext context) {
                final Result<List<TimedEvent>, Unit> result = animation.animate(time, state, context);
                if (result instanceof final Result.Success<List<TimedEvent>, Unit> success) {
                    final List<TimedEvent> list = success.val();
                    double max = time;
                    for (final TimedEvent event : list) {
                        max = Math.max(max, event.end());
                    }
                    final Result<List<TimedEvent>, Unit> lockResult = state.completeLock(time, max, context);
                    if (lockResult instanceof Result.Failure<List<TimedEvent>, Unit>) {
                        return lockResult;
                    }
                    final List<TimedEvent> events = new ArrayList<>(list.size() + 5);
                    events.addAll(list);
                    events.addAll(((Result.Success<List<TimedEvent>, Unit>) lockResult).val());
                    return Result.success(events);
                }
                return result;
            }
        }, currentTime, Double.POSITIVE_INFINITY);
        if (Double.isNaN(t)) {
            Tbcexv4.LOGGER.error("Could not schedule animation!");
            return currentTime;
        }
        queue.checkpoint(t);
        return t;
    }

    @Override
    public TurnManager turnManager() {
        return turnManager;
    }

    public double time(final double partial) {
        return (time + partial);
    }
}
