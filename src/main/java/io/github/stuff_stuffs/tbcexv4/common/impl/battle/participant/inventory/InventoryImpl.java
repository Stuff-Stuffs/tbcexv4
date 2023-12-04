package io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.inventory;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.Inventory;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.InventoryHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.CoreBattleTraceEvents;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.participant.BasicParticipantEvents;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Collectors;

public class InventoryImpl implements Inventory {
    private final BattleParticipant participant;
    private final Int2ObjectMap<InventoryEntryImpl> entries;
    private int nextId = 0;

    public InventoryImpl(final BattleParticipant participant) {
        this.participant = participant;
        entries = new Int2ObjectLinkedOpenHashMap<>();
    }

    @Override
    public InventoryEntry get(final InventoryHandle handle) {
        if (!handle.parent().equals(participant.handle())) {
            throw new RuntimeException();
        }
        return entries.computeIfAbsent(((InventoryHandleImpl) handle).id, id -> new InventoryEntryImpl(this, handle));
    }

    @Override
    public Iterable<? extends InventoryEntry> entries() {
        return entries.values().stream().filter(input -> input.stack != null).collect(Collectors.toList());
    }

    @Override
    public Result<InventoryEntry, GiveError> give(final BattleItemStack stack, final BattleTracer.Span<?> tracer) {
        if (!participant.events().invoker(BasicParticipantEvents.PRE_GIVE_INVENTORY_STACK_KEY).onPreGiveInventoryStack(participant, stack, tracer)) {
            return new Result.Failure<>(GiveError.EVENT);
        }
        final InventoryHandleImpl handle = new InventoryHandleImpl(participant.handle(), nextId++);
        final InventoryEntryImpl entry = new InventoryEntryImpl(this, handle, stack);
        entries.put(handle.id, entry);
        try (final var span = tracer.push(new CoreBattleTraceEvents.ParticipantSetStack(handle, Optional.empty(), Optional.of(stack)))) {
            participant.events().invoker(BasicParticipantEvents.POST_GIVE_INVENTORY_STACK_KEY).onPostGiveInventoryStack(participant, handle, span);
        }
        return new Result.Success<>(entry);
    }

    private static final class InventoryEntryImpl implements InventoryEntry {
        private final InventoryImpl inventory;
        private final InventoryHandle handle;
        private @Nullable BattleItemStack stack;

        private InventoryEntryImpl(final InventoryImpl inventory, final InventoryHandle handle, final BattleItemStack stack) {
            this.inventory = inventory;
            this.handle = handle;
            this.stack = stack;
        }

        private InventoryEntryImpl(final InventoryImpl inventory, final InventoryHandle handle) {
            this.inventory = inventory;
            this.handle = handle;
            stack = null;
        }

        @Override
        public Result<Unit, GiveError> set(final BattleItemStack stack, final BattleTracer.Span<?> tracer) {
            if (!inventory.participant.events().invoker(BasicParticipantEvents.PRE_SET_INVENTORY_STACK_KEY).onPreSetInventoryStack(inventory.participant, handle, Optional.of(stack), tracer)) {
                return new Result.Failure<>(GiveError.EVENT);
            }
            final Optional<BattleItemStack> old = Optional.ofNullable(this.stack);
            this.stack = stack;
            try (final var span = tracer.push(new CoreBattleTraceEvents.ParticipantSetStack(handle, old, Optional.of(stack)))) {
                inventory.participant.events().invoker(BasicParticipantEvents.POST_SET_INVENTORY_STACK_KEY).onPostSetInventoryStack(inventory.participant, handle, old, span);
            }
            return new Result.Success<>(Unit.INSTANCE);
        }

        @Override
        public Result<Integer, TakeError> take(final int amount, final BattleTracer.Span<?> tracer) {
            if (stack == null) {
                return new Result.Failure<>(TakeError.EMPTY_STACK);
            }
            final Optional<BattleItemStack> next = stack.count() > amount ? Optional.of(new BattleItemStack(stack.item(), stack.count() - amount)) : Optional.empty();
            if (!inventory.participant.events().invoker(BasicParticipantEvents.PRE_SET_INVENTORY_STACK_KEY).onPreSetInventoryStack(inventory.participant, handle, next, tracer)) {
                return new Result.Failure<>(TakeError.EVENT);
            }
            final BattleItemStack old = stack;
            stack = next.orElse(null);
            try (final var span = tracer.push(new CoreBattleTraceEvents.ParticipantSetStack(handle, Optional.of(old), next))) {
                inventory.participant.events().invoker(BasicParticipantEvents.POST_SET_INVENTORY_STACK_KEY).onPostSetInventoryStack(inventory.participant, handle, Optional.of(old), span);
            }
            return new Result.Success<>(old.count() - (stack == null ? 0 : stack.count()));
        }

        @Override
        public InventoryHandle handle() {
            return handle;
        }

        @Override
        public Optional<BattleItemStack> stack() {
            return Optional.ofNullable(stack);
        }
    }

    public static final class InventoryHandleImpl implements InventoryHandle {
        public static final Codec<InventoryHandleImpl> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BattleParticipantHandle.CODEC.fieldOf("parent").forGetter(InventoryHandleImpl::parent),
                Codec.INT.fieldOf("id").forGetter(handle -> handle.id)
        ).apply(instance, InventoryHandleImpl::new));
        private final BattleParticipantHandle parent;
        private final int id;

        private InventoryHandleImpl(final BattleParticipantHandle parent, final int id) {
            this.parent = parent;
            this.id = id;
        }

        @Override
        public BattleParticipantHandle parent() {
            return parent;
        }
    }
}
