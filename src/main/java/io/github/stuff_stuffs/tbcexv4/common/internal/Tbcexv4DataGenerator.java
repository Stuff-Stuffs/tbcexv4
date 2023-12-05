package io.github.stuff_stuffs.tbcexv4.common.internal;

import io.github.stuff_stuffs.event_gen.api.event.gen.EventInfo;
import io.github.stuff_stuffs.event_gen.api.event.gen.EventKeyLocation;
import io.github.stuff_stuffs.event_gen.api.event.gen.EventPackageLocation;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantInitialState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.InventoryHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.Stat;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeam;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeamRelation;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.block.BlockState;

import java.util.Optional;

public class Tbcexv4DataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(final FabricDataGenerator fabricDataGenerator) {

    }

    @EventKeyLocation(location = "io.github.stuff_stuffs.tbcexv4.common.generated_events.BasicEvents")
    @EventPackageLocation("io.github.stuff_stuffs.tbcexv4.common.generated_events")
    public static abstract class Events {
        @EventInfo(defaultValue = "true", combiner = "Boolean.logicalAnd")
        public abstract boolean PreSetBoundsEvent(BattleState state, BattleBounds newBounds, BattleTransactionContext transactionContext, BattleTracer.Span<?> span);

        @EventInfo()
        public abstract void PostSetBoundsEvent(BattleState state, BattleBounds oldBounds, BattleTransactionContext transactionContext, BattleTracer.Span<?> span);

        @EventInfo(defaultValue = "true", combiner = "Boolean.logicalAnd")
        public abstract boolean PreAddParticipantEvent(BattleState state, BattleParticipantInitialState participant, BattleParticipantHandle prospectiveHandle, BattleTransactionContext transactionContext, BattleTracer.Span<?> span);

        @EventInfo()
        public abstract void PostAddParticipantEvent(BattleState state, BattleParticipant participant, BattleTransactionContext transactionContext, BattleTracer.Span<?> span);

        @EventInfo(defaultValue = "true", combiner = "Boolean.logicalAnd")
        public abstract boolean PreRemoveParticipantEvent(BattleParticipant participant, BattleState.RemoveParticipantReason reason, BattleTransactionContext transactionContext, BattleTracer.Span<?> span);

        @EventInfo()
        public abstract void PostRemoveParticipantEvent(BattleState state, BattleParticipantHandle handle, BattleState.RemoveParticipantReason reason, BattleTransactionContext transactionContext, BattleTracer.Span<?> span);

        @EventInfo(defaultValue = "true", combiner = "Boolean.logicalAnd")
        public abstract boolean PreSetTeamRelationEvent(BattleState state, BattleParticipantTeam first, BattleParticipantTeam second, BattleParticipantTeamRelation newRelation, BattleTransactionContext transactionContext, BattleTracer.Span<?> span);

        @EventInfo()
        public abstract void PostSetTeamRelationEvent(BattleState state, BattleParticipantTeam first, BattleParticipantTeam second, BattleParticipantTeamRelation oldRelation, BattleTransactionContext transactionContext, BattleTracer.Span<?> span);
    }

    @EventKeyLocation(location = "io.github.stuff_stuffs.tbcexv4.common.generated_events.env.BasicEnvEvents")
    @EventPackageLocation("io.github.stuff_stuffs.tbcexv4.common.generated_events.env")
    public static abstract class EnvEvents {
        @EventInfo(defaultValue = "true", combiner = "Boolean.logicalAnd")
        public abstract boolean PreSetBlockStateEvent(BattleState state, int x, int y, int z, BlockState newState, BattleTransactionContext transactionContext, BattleTracer.Span<?> span);

        @EventInfo()
        public abstract void PostSetBlockStateEvent(BattleState state, int x, int y, int z, BlockState oldState, BattleTransactionContext transactionContext, BattleTracer.Span<?> span);
    }

    @EventKeyLocation(location = "io.github.stuff_stuffs.tbcexv4.common.generated_events.participant.BasicParticipantEvents")
    @EventPackageLocation("io.github.stuff_stuffs.tbcexv4.common.generated_events.participant")
    public static abstract class ParticipantEvents {
        @EventInfo(defaultValue = "true", combiner = "Boolean.logicalAnd")
        public abstract boolean PreSetBoundsEvent(BattleParticipant participant, BattleParticipantBounds newBounds, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo()
        public abstract void PostSetBoundsEvent(BattleParticipant participant, BattleParticipantBounds oldBounds, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo(defaultValue = "true", combiner = "Boolean.logicalAnd")
        public abstract boolean PreSetPosEvent(BattleParticipant participant, BattlePos newPos, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo()
        public abstract void PostSetPosEvent(BattleParticipant participant, BattlePos oldPos, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo()
        public abstract <T> void PostAddModifierEvent(BattleParticipant participant, Stat<T> stat, T oldValue, T newValue, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo(defaultValue = "damage", combiner = "io.github.stuff_stuffs.tbcexv4.common.api.util.Tbcexv4Util.selectSecond")
        public abstract double PreDamageEvent(BattleParticipant participant, double damage, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo()
        public abstract void PostDamageEvent(BattleParticipant participant, double damage, double overflow, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo(defaultValue = "heal", combiner = "io.github.stuff_stuffs.tbcexv4.common.api.util.Tbcexv4Util.selectSecond")
        public abstract double PreHealEvent(BattleParticipant participant, double heal, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo()
        public abstract void PostHealEvent(BattleParticipant participant, double healed, double overflow, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo(defaultValue = "amount", combiner = "io.github.stuff_stuffs.tbcexv4.common.api.util.Tbcexv4Util.selectSecond")
        public abstract double PreSetHealthEvent(BattleParticipant participant, double amount, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo()
        public abstract void PostSetHealthEvent(BattleParticipant participant, double oldHealth, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo(defaultValue = "true", combiner = "Boolean.logicalAnd")
        public abstract boolean PreSetInventoryStack(BattleParticipant participant, InventoryHandle handle, Optional<BattleItemStack> newStack, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo()
        public abstract void PostSetInventoryStack(BattleParticipant participant, InventoryHandle handle, Optional<BattleItemStack> oldStack, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo(defaultValue = "true", combiner = "Boolean.logicalAnd")
        public abstract boolean PreGiveInventoryStack(BattleParticipant participant, BattleItemStack newStack, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo()
        public abstract void PostGiveInventoryStack(BattleParticipant participant, InventoryHandle handle, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);
    }
}
