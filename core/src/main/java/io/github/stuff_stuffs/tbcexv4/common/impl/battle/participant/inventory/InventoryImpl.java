package io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.inventory;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.Inventory;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.InventoryHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment.Equipment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment.EquipmentSlot;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItem;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.DeltaSnapshotParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import io.github.stuff_stuffs.tbcexv4.common.generated_events.participant.ParticipantInventoryEvents;
import io.github.stuff_stuffs.tbcexv4.common.generated_traces.participant.ParticipantSetStackTrace;
import io.github.stuff_stuffs.tbcexv4.common.generated_traces.participant.PreParticipantSetStackTrace;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class InventoryImpl extends DeltaSnapshotParticipant<InventoryImpl.Delta> implements Inventory {
    private final BattleParticipant participant;
    private final Map<EquipmentSlot, EquipEntry> equipped;
    private final Int2ObjectMap<InventoryEntryImpl> entries;
    private int nextId = 0;

    public InventoryImpl(final BattleParticipant participant) {
        this.participant = participant;
        equipped = new Object2ReferenceOpenHashMap<>();
        entries = new Int2ObjectLinkedOpenHashMap<>();
    }

    @Override
    public InventoryEntry get(final InventoryHandle handle) {
        if (!handle.parent().equals(participant.handle())) {
            throw new RuntimeException();
        }
        return entries.computeIfAbsent(((InventoryHandleImpl) handle).id, id -> new InventoryEntryImpl(this, handle, null));
    }

    @Override
    public Iterable<? extends InventoryEntry> entries() {
        return entries.values().stream().filter(input -> input.stack != null).collect(Collectors.toList());
    }

    @Override
    public Optional<? extends BattleItem> equippedItem(final EquipmentSlot slot) {
        final EquipEntry entry = equipped.get(slot);
        if (entry == null) {
            return Optional.empty();
        }
        return Optional.of(entry.item);
    }

    @Override
    public Result<InventoryEntry, GiveError> give(final BattleItemStack stack, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        final InventoryHandleImpl handle = new InventoryHandleImpl(participant.handle(), nextId++);
        try (final var preSpan = tracer.push(new PreParticipantSetStackTrace(handle, Optional.of(stack)), transactionContext)) {
            if (!participant.events().invoker(ParticipantInventoryEvents.PRE_GIVE_STACK_KEY, transactionContext).onPreGiveStack(participant, stack, transactionContext, preSpan)) {
                return new Result.Failure<>(GiveError.EVENT);
            }
            final InventoryEntryImpl entry = new InventoryEntryImpl(this, handle, stack);
            entries.put(handle.id, entry);
            delta(transactionContext, new GiveDelta(handle.id));
            try (final var span = preSpan.push(new ParticipantSetStackTrace(handle, Optional.empty(), Optional.of(stack)), transactionContext)) {
                participant.events().invoker(ParticipantInventoryEvents.POST_GIVE_STACK_KEY, transactionContext).onPostGiveStack(participant, handle, transactionContext, span);
            }
            return new Result.Success<>(entry);
        }
    }

    @Override
    public Result<Unit, EquipError> equip(final BattleItem item, final EquipmentSlot slot, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        final EquipEntry entry = equipped.get(slot);
        if (entry != null) {
            return new Result.Failure<>(new EquipError.SlotFilled());
        }
        final Set<EquipmentSlot> blocking = new ObjectOpenHashSet<>();
        for (final RegistryEntry<EquipmentSlot> e : Tbcexv4Registries.EquipmentSlots.REGISTRY.iterateEntries(slot.blockedBy())) {
            if (equipped.containsKey(e.value())) {
                blocking.add(e.value());
            }
        }
        for (final RegistryEntry<EquipmentSlot> e : Tbcexv4Registries.EquipmentSlots.REGISTRY.iterateEntries(slot.blocks())) {
            if (equipped.containsKey(e.value())) {
                blocking.add(e.value());
            }
        }
        if (!blocking.isEmpty()) {
            return new Result.Failure<>(new EquipError.Blocked(blocking));
        }
        final Optional<Equipment> opt = item.equipmentForSlot(slot, participant);
        if (opt.isEmpty()) {
            return new Result.Failure<>(new EquipError.InvalidItem());
        }
        final Equipment equipment = opt.get();
        if (!participant.events().invoker(ParticipantInventoryEvents.PRE_EQUIP_KEY, transactionContext).onPreEquip(participant, equipment, item, slot, transactionContext, tracer)) {
            return new Result.Failure<>(new EquipError.Event());
        }
        equipped.put(slot, new EquipEntry(item, equipment));
        equipment.init(participant, transactionContext, tracer);
        delta(transactionContext, new EquipDelta(slot));
        return new Result.Success<>(Unit.INSTANCE);
    }

    @Override
    public Result<InventoryHandle, UnequipError> unequip(final EquipmentSlot slot, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
        final EquipEntry entry = equipped.get(slot);
        if (entry == null) {
            return new Result.Failure<>(new UnequipError.SlotEmpty());
        }
        final boolean allowed = participant.events().invoker(ParticipantInventoryEvents.PRE_UNEQUIP_KEY, transactionContext).onPreUnequip(participant, entry.equipment, entry.item, slot, transactionContext, tracer);
        if (!allowed) {
            return new Result.Failure<>(new UnequipError.Event());
        }
        final InventoryHandle handle;
        try (final var inner = transactionContext.openNested()) {
            final Result<InventoryEntry, GiveError> give = give(new BattleItemStack(entry.item, 1), transactionContext, tracer);
            if (give instanceof Result.Failure<InventoryEntry, GiveError> failure) {
                inner.abort();
                return new Result.Failure<>(new UnequipError.InventoryGive(failure.error()));
            }
            inner.commit();
            handle = ((Result.Success<InventoryEntry, GiveError>) give).val().handle();
        }
        participant.events().invoker(ParticipantInventoryEvents.POST_UNEQUIP_KEY, transactionContext).onPostUnequip(participant, entry.equipment, entry.item, slot, handle, transactionContext, tracer);
        equipped.remove(slot);
        entry.equipment.deinit(participant, transactionContext, tracer);
        delta(transactionContext, new UnequipDelta(slot, entry));
        return new Result.Success<>(handle);
    }

    @Override
    public Optional<? extends Equipment> equipment(final EquipmentSlot slot) {
        final EquipEntry entry = equipped.get(slot);
        if (entry == null) {
            return Optional.empty();
        }
        return Optional.of(entry.equipment);
    }

    @Override
    protected void revertDelta(final Delta delta) {
        delta.apply(this);
    }

    private static final class InventoryEntryImpl implements InventoryEntry {
        private final InventoryImpl inventory;
        private final InventoryHandle handle;
        private @Nullable BattleItemStack stack;

        private InventoryEntryImpl(final InventoryImpl inventory, final InventoryHandle handle, final @Nullable BattleItemStack stack) {
            this.inventory = inventory;
            this.handle = handle;
            this.stack = stack;
        }

        @Override
        public Result<Unit, GiveError> set(final BattleItemStack stack, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
            try (final var preSpan = tracer.push(new PreParticipantSetStackTrace(handle, Optional.of(stack)), transactionContext)) {
                if (!inventory.participant.events().invoker(ParticipantInventoryEvents.PRE_SET_STACK_KEY, transactionContext).onPreSetStack(inventory.participant, handle, Optional.of(stack), transactionContext, preSpan)) {
                    return new Result.Failure<>(GiveError.EVENT);
                }
                final Optional<BattleItemStack> old = Optional.ofNullable(this.stack);
                inventory.delta(transactionContext, new SetStackDelta(((InventoryHandleImpl) handle).id, this.stack));
                this.stack = stack;
                try (final var span = preSpan.push(new ParticipantSetStackTrace(handle, old, Optional.of(stack)), transactionContext)) {
                    inventory.participant.events().invoker(ParticipantInventoryEvents.POST_SET_STACK_KEY, transactionContext).onPostSetStack(inventory.participant, handle, old, transactionContext, span);
                }
                return new Result.Success<>(Unit.INSTANCE);
            }
        }

        @Override
        public Result<Integer, TakeError> take(final int amount, final BattleTransactionContext transactionContext, final BattleTracer.Span<?> tracer) {
            if (stack == null) {
                return new Result.Failure<>(TakeError.EMPTY_STACK);
            }
            final Optional<BattleItemStack> next = stack.count() > amount ? Optional.of(new BattleItemStack(stack.item(), stack.count() - amount)) : Optional.empty();
            try (final var preSpan = tracer.push(new PreParticipantSetStackTrace(handle, next), transactionContext)) {
                if (!inventory.participant.events().invoker(ParticipantInventoryEvents.PRE_SET_STACK_KEY, transactionContext).onPreSetStack(inventory.participant, handle, next, transactionContext, preSpan)) {
                    return new Result.Failure<>(TakeError.EVENT);
                }
                final BattleItemStack old = stack;
                inventory.delta(transactionContext, new SetStackDelta(((InventoryHandleImpl) handle).id, stack));
                stack = next.orElse(null);
                try (final var span = preSpan.push(new ParticipantSetStackTrace(handle, Optional.of(old), next), transactionContext)) {
                    inventory.participant.events().invoker(ParticipantInventoryEvents.POST_SET_STACK_KEY, transactionContext).onPostSetStack(inventory.participant, handle, Optional.of(old), transactionContext, span);
                }
                return new Result.Success<>(old.count() - (stack == null ? 0 : stack.count()));
            }
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

    private record EquipEntry(BattleItem item, Equipment equipment) {
    }

    public sealed interface Delta {
        void apply(InventoryImpl inventory);
    }

    private record GiveDelta(int id) implements Delta {
        @Override
        public void apply(final InventoryImpl inventory) {
            inventory.entries.remove(id);
        }
    }

    private record SetStackDelta(int id, @Nullable BattleItemStack stack) implements Delta {
        @Override
        public void apply(final InventoryImpl inventory) {
            inventory.entries.get(id).stack = stack;
        }
    }

    private record UnequipDelta(EquipmentSlot slot, EquipEntry entry) implements Delta {
        @Override
        public void apply(final InventoryImpl inventory) {
            inventory.equipped.put(slot, entry);
        }
    }

    private record EquipDelta(EquipmentSlot slot) implements Delta {
        @Override
        public void apply(final InventoryImpl inventory) {
            inventory.equipped.remove(slot);
        }
    }
}





