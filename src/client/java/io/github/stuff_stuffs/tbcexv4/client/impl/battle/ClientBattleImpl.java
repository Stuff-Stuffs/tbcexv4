package io.github.stuff_stuffs.tbcexv4.client.impl.battle;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
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
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracerView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.CoreBattleTraceEvents;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.turn.TurnManager;
import io.github.stuff_stuffs.tbcexv4.common.api.event.EventMap;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Tbcexv4Util;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.log.BattleLogContextImpl;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.BattleStateImpl;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
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
    private AnimationQueue queue;
    private double time = 0;
    private final Queue<BattleLogContextImpl> logs;

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
        logs = new ArrayDeque<>();
        initialize();
    }

    public void tick() {
        time = time + 1;
        while (!logs.isEmpty() && logs.peek().time < time) {
            MinecraftClient.getInstance().player.sendMessage(Tbcexv4Util.concat(logs.poll().collect().toArray(new Text[0])));
        }
    }

    private void initialize() {
        turnManager = turnManagerFactory.get();
        state = new BattleStateImpl(this, createEnv(), sourceWorld, builder, participantEventBuilder);
        tracer = BattleTracer.create(new CoreBattleTraceEvents.Root());
        queue = new AnimationQueueImpl();
        try (final var transaction = state.transactionManager().open(); final var span = tracer.push(new CoreBattleTraceEvents.TurnManagerSetup(), transaction)) {
            turnManager.setup(state, transaction, span);
            transaction.commit();
        }
        tracer.all().forEach(node -> AnimationFactoryRegistry.create(node.event()).ifPresent(this::pushAnimation));
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
        final BattleTracerView.Node<?> latest = tracer.latest();
        final Optional<ActionSource> actionSource = action.source();
        final BattleLogContextImpl logContext = new BattleLogContextImpl();
        try (final var transaction = state.transactionManager().open(); final var span = tracer.push(new CoreBattleTraceEvents.ActionRoot(actionSource.map(ActionSource::actor)), transaction)) {
            action.apply(state, transaction, tracer, logContext);
            actionSource.ifPresent(source -> turnManager.onAction(source.energy(), source.actor(), state, transaction, span));
            transaction.commit();
        }
        final OptionalDouble logTime = tracer.after(latest.timeStamp()).mapToDouble(node -> AnimationFactoryRegistry.create(node.event()).map(this::pushAnimation).orElse(0.0)).min();
        if (logTime.isEmpty()) {
            throw new RuntimeException();
        }
        logContext.time = logTime.getAsDouble();
        logs.add(logContext);
    }

    private double pushAnimation(final Animation<BattleRenderState> animation) {
        final double currentTime = time(0);
        final double t = queue.enqueue(wrap(animation), currentTime, Double.POSITIVE_INFINITY);
        if (Double.isNaN(t)) {
            Tbcexv4.LOGGER.error("Could not schedule animation!");
            return currentTime;
        }
        return t;
    }

    @Override
    public TurnManager turnManager() {
        return turnManager;
    }

    public double time(final double partial) {
        return (time + partial);
    }

    private Animation<BattleRenderState> wrap(final Animation<BattleRenderState> animation) {
        return (time, state, context) -> {
            final Result<List<Animation.TimedEvent>, Unit> result = animation.animate(time, state, context);
            if (result instanceof Result.Failure<List<Animation.TimedEvent>, Unit>) {
                return result;
            }
            final Result.Success<List<Animation.TimedEvent>, Unit> success = (Result.Success<List<Animation.TimedEvent>, Unit>) result;
            final List<Animation.TimedEvent> mods = new ArrayList<>(success.val().size() + 1);
            double last = Double.NEGATIVE_INFINITY;
            for (final Animation.TimedEvent modifier : success.val()) {
                mods.add(modifier);
                last = Math.max(last, modifier.end());
            }
            final Result<List<Animation.TimedEvent>, Unit> reserve = state.completeLock(time, last, context);
            if (reserve instanceof Result.Failure<List<Animation.TimedEvent>, Unit>) {
                return new Result.Failure<>(Unit.INSTANCE);
            }
            mods.addAll(((Result.Success<List<Animation.TimedEvent>, Unit>) reserve).val());
            return new Result.Success<>(mods);
        };
    }
}
