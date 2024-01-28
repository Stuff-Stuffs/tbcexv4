package io.github.stuff_stuffs.tbcexv4.common.internal;

import io.github.stuff_stuffs.event_gen.api.event.gen.EventComparisonInfo;
import io.github.stuff_stuffs.event_gen.api.event.gen.EventInfo;
import io.github.stuff_stuffs.event_gen.api.event.gen.EventKeyLocation;
import io.github.stuff_stuffs.event_gen.api.event.gen.EventPackageLocation;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.DamagePhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantInitialState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantAttachmentType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.damage.DamageType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.InventoryHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment.Equipment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.equipment.EquipmentSlot;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItem;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.Pather;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.Stat;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.StatModificationPhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeam;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeamRelation;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.attachment.BattleAttachmentType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4util.gen.GenTraceEvent;
import io.github.stuff_stuffs.tbcexv4util.gen.TracePackage;
import io.github.stuff_stuffs.tbcexv4util.log.BattleLogLevel;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class Tbcexv4DataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(final FabricDataGenerator fabricDataGenerator) {

    }

    @EventKeyLocation(location = "io.github.stuff_stuffs.tbcexv4.common.generated_events.BasicEvents")
    @EventPackageLocation("io.github.stuff_stuffs.tbcexv4.common.generated_events")
    public static abstract class Events {
        @EventInfo()
        public abstract void EndBattle(BattleState state, BattleTransactionContext transactionContext, BattleTracer.Span<?> span);

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

        @EventInfo(defaultValue = "true", combiner = "Boolean.logicalAnd")
        public abstract boolean PreSetBiome(BattleState state, int x, int y, int z, RegistryEntry<Biome> oldBiome, RegistryEntry<Biome> newBiome, BattleTransactionContext transactionContext, BattleTracer.Span<?> span);

        @EventInfo()
        public abstract void PostSetBiome(BattleState state, int x, int y, int z, RegistryEntry<Biome> oldBiome, RegistryEntry<Biome> newBiome, BattleTransactionContext transactionContext, BattleTracer.Span<?> span);
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
        public abstract <T> void PostAddModifierEvent(BattleParticipant participant, Stat<T> stat, StatModificationPhase phase, T oldValue, T newValue, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo(defaultValue = "damage", combiner = "io.github.stuff_stuffs.tbcexv4.common.api.util.Tbcexv4Util.selectSecond")
        @EventComparisonInfo(comparedType = DamagePhase.class, comparator = "io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries.DamagePhases.COMPARATOR")
        public abstract double PreDamageEvent(BattleParticipant participant, double damage, DamageType damageType, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo()
        public abstract void PostDamageEvent(BattleParticipant participant, double damage, DamageType damageType, double overflow, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo(defaultValue = "heal", combiner = "io.github.stuff_stuffs.tbcexv4.common.api.util.Tbcexv4Util.selectSecond")
        public abstract double PreHealEvent(BattleParticipant participant, double heal, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo()
        public abstract void PostHealEvent(BattleParticipant participant, double healed, double overflow, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo(defaultValue = "amount", combiner = "io.github.stuff_stuffs.tbcexv4.common.api.util.Tbcexv4Util.selectSecond")
        public abstract double PreSetHealthEvent(BattleParticipant participant, double amount, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo()
        public abstract void PostSetHealthEvent(BattleParticipant participant, double oldHealth, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo(defaultValue = "node", combiner = "io.github.stuff_stuffs.tbcexv4.common.api.util.Tbcexv4Util.selectSecond")
        public abstract Optional<Pather.PathNode> PreMoveEvent(BattleParticipant participant, Optional<Pather.PathNode> node, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo()
        public abstract void MoveEvent(BattleParticipant participant, Pather.PathNode node, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);
    }

    @EventKeyLocation(location = "io.github.stuff_stuffs.tbcexv4.common.generated_events.participant.ParticipantInventoryEvents")
    @EventPackageLocation("io.github.stuff_stuffs.tbcexv4.common.generated_events.participant.inventory")
    public static abstract class InventoryEvents {
        @EventInfo(defaultValue = "true", combiner = "Boolean.logicalAnd")
        public abstract boolean PreSetStack(BattleParticipant participant, InventoryHandle handle, Optional<BattleItemStack> newStack, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo()
        public abstract void PostSetStack(BattleParticipant participant, InventoryHandle handle, Optional<BattleItemStack> oldStack, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo(defaultValue = "true", combiner = "Boolean.logicalAnd")
        public abstract boolean PreGiveStack(BattleParticipant participant, BattleItemStack newStack, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo()
        public abstract void PostGiveStack(BattleParticipant participant, InventoryHandle handle, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo(defaultValue = "true", combiner = "Boolean.logicalAnd")
        public abstract boolean PreUnequip(BattleParticipant participant, Equipment equipment, BattleItem item, EquipmentSlot slot, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo()
        public abstract void PostUnequip(BattleParticipant participant, Equipment equipment, BattleItem item, EquipmentSlot slot, InventoryHandle handle, BattleTransactionContext transactionContext, BattleTracer.Span<?> trace);

        @EventInfo(defaultValue = "true", combiner = "Boolean.logicalAnd")
        public abstract boolean PreEquip(BattleParticipant participant, Equipment equipment, BattleItem item, EquipmentSlot slot, BattleTransactionContext transactionContext, BattleTracer.Span<?> tracer);

        @EventInfo()
        public abstract void PostEquip(BattleParticipant participant, Equipment equipment, BattleItem item, EquipmentSlot slot, BattleTransactionContext transactionContext, BattleTracer.Span<?> span);
    }

    @TracePackage(value = "io.github.stuff_stuffs.tbcexv4.common.generated_traces")
    public static abstract class CoreTraces {
        @GenTraceEvent(format = "Setting attachment type: {type}")
        public abstract void SetAttachmentTrace(BattleAttachmentType<?, ?> type);

        @GenTraceEvent(format = "", level = BattleLogLevel.NONE)
        public abstract void RootTrace();

        @GenTraceEvent(format = "", level = BattleLogLevel.NONE)
        public abstract void TurnManagerSetupTrace();

        @GenTraceEvent(format = "Trying to set bounds from {currentBounds} to {attemptedBounds}", level = BattleLogLevel.DEBUG)
        public abstract void PreSetBoundsTrace(BattleBounds currentBounds, BattleBounds attemptedBounds);

        @GenTraceEvent(format = "Setting bounds from {currentBounds} to {attemptedBounds}", level = BattleLogLevel.NORMAL)
        public abstract void SetBoundsTrace(BattleBounds currentBounds, BattleBounds attemptedBounds);

        @GenTraceEvent(format = "Trying to set blockState at {pos} to {attemptedState}", level = BattleLogLevel.DEBUG)
        public abstract void PreSetBlockStateTrace(BattlePos pos, BlockState attemptedState);

        @GenTraceEvent(format = "Setting blockState at {pos} from {oldState} to {newState}", level = BattleLogLevel.NORMAL)
        public abstract void SetBlockStateTrace(BattlePos pos, BlockState oldState, BlockState newState);

        @GenTraceEvent(format = "Trying to set team relation between {first} and {second} to {relation}", level = BattleLogLevel.DEBUG)
        public abstract void PreSetTeamRelationTrace(BattleParticipantTeam first, BattleParticipantTeam second, BattleParticipantTeamRelation relation);

        @GenTraceEvent(format = "Setting team relation between {first} and {second} from {oldRelation} to {newRelation}", level = BattleLogLevel.NORMAL)
        public abstract void SetTeamRelationTrace(BattleParticipantTeam first, BattleParticipantTeam second, BattleParticipantTeamRelation oldRelation, BattleParticipantTeamRelation newRelation);

        @GenTraceEvent(format = "Trying to set biome at {pos} to {biome}", level = BattleLogLevel.DEBUG)
        public abstract void PreSetBiomeTrace(BattlePos pos, RegistryEntry<Biome> biome);

        @GenTraceEvent(format = "Setting biome at {pos} from {oldBiome} to {newBiome}", level = BattleLogLevel.INFO)
        public abstract void SetBiomeTrace(BattlePos pos, RegistryEntry<Biome> oldBiome, RegistryEntry<Biome> newBiome);

        @GenTraceEvent(format = "Action root of {source}", level = BattleLogLevel.NORMAL)
        public abstract void ActionRootTrace(Optional<BattleParticipantHandle> source);
    }

    @TracePackage(value = "io.github.stuff_stuffs.tbcexv4.common.generated_traces.participant")
    public static abstract class ParticipantTraces {
        @GenTraceEvent(format = "Setting participant {handle}'s attachment {type}", level = BattleLogLevel.DEBUG)
        public abstract void SetParticipantAttachmentTrace(BattleParticipantHandle handle, BattleParticipantAttachmentType<?, ?> type, @Nullable Object snapshot);

        @GenTraceEvent(format = "Removing participant {handle}, cause {reason}", level = BattleLogLevel.NORMAL)
        public abstract void RemoveParticipantTrace(BattleParticipantHandle handle, BattleState.RemoveParticipantReason reason);

        @GenTraceEvent(format = "Trying to remove participant {handle}, cause {attemptedReason}", level = BattleLogLevel.DEBUG)
        public abstract void PreRemoveParticipantTrace(BattleParticipantHandle handle, BattleState.RemoveParticipantReason attemptedReason);

        @GenTraceEvent(format = "Setting stack in slot {handle} from {oldStack} to {newStack}", level = BattleLogLevel.INFO)
        public abstract void ParticipantSetStackTrace(InventoryHandle handle, Optional<BattleItemStack> oldStack, Optional<BattleItemStack> newStack);

        @GenTraceEvent(format = "Setting participant {handle}'s team from {oldTeam} to {newTeam}", level = BattleLogLevel.NORMAL)
        public abstract void ParticipantSetTeamTrace(BattleParticipantHandle handle, Optional<BattleParticipantTeam> oldTeam, BattleParticipantTeam newTeam);

        @GenTraceEvent(format = "Trying to set stack in slot {handle} to {attemptedStack}", level = BattleLogLevel.INFO)
        public abstract void PreParticipantSetStackTrace(InventoryHandle handle, Optional<BattleItemStack> attemptedStack);

        @GenTraceEvent(format = "Trying to set participant {handle}'s health to {attemptedHealth}", level = BattleLogLevel.INFO)
        public abstract void PreParticipantSetHealthTrace(BattleParticipantHandle handle, double attemptedHealth);

        @GenTraceEvent(format = "Setting participant {handle}'s health from {oldHealth} from {newHealth}", level = BattleLogLevel.NORMAL)
        public abstract void ParticipantSetHealthTrace(BattleParticipantHandle handle, double oldHealth, double newHealth);

        @GenTraceEvent(format = "Trying to apply {amount} damage to {handle}", level = BattleLogLevel.DEBUG)
        public abstract void PreDamageParticipantTrace(BattleParticipantHandle handle, double amount);

        @GenTraceEvent(format = "{handle} is taking {amount} damage", level = BattleLogLevel.NORMAL)
        public abstract void DamageParticipantTrace(BattleParticipantHandle handle, double amount);

        @GenTraceEvent(format = "Trying to add modifier for stat {stat} to participant {handle}")
        public abstract void PreAddParticipantStateModifierTrace(BattleParticipantHandle handle, Stat<?> stat);

        @GenTraceEvent(format = "Adding modifier for stat {stat} to participant {handle}", level = BattleLogLevel.DEBUG)
        public abstract void AddParticipantStateModifierTrace(BattleParticipantHandle handle, Stat<?> stat);

        @GenTraceEvent(format = "Trying to add participant with handle {handle}", level = BattleLogLevel.DEBUG)
        public abstract void PreAddParticipantTrace(BattleParticipantHandle handle);

        @GenTraceEvent(format = "Adding participant with handle {handle}", level = BattleLogLevel.NORMAL)
        public abstract void AddParticipantTrace(BattleParticipantHandle handle);

        @GenTraceEvent(format = "Trying to move participant {handle} along {path}", level = BattleLogLevel.DEBUG)
        public abstract void PreMoveParticipantTrace(BattleParticipantHandle handle, Pather.PathNode path);

        @GenTraceEvent(format = "Moved participant {handle} along {path}", level = BattleLogLevel.NORMAL)
        public abstract void MoveParticipantTrace(BattleParticipantHandle handle, Pather.PathNode path);

        @GenTraceEvent(format = "Trying to set {handle}'s pos to {pos}", level = BattleLogLevel.DEBUG)
        public abstract void PreSetParticipantPosTrace(BattleParticipantHandle handle, BattlePos pos);

        @GenTraceEvent(format = "Setting {handle}'s pos to {pos}", level = BattleLogLevel.NORMAL)
        public abstract void SetParticipantPosTrace(BattleParticipantHandle handle, BattlePos pos);

        @GenTraceEvent(format = "Trying to set participant {handle}'s bounds to {attemptedBounds}", level = BattleLogLevel.DEBUG)
        public abstract void PreSetParticipantBoundsTrace(BattleParticipantHandle handle, BattleParticipantBounds attemptedBounds);

        @GenTraceEvent(format = "Setting participant {handle}'s bounds from {oldBounds} to {newBounds}", level = BattleLogLevel.NORMAL)
        public abstract void SetParticipantBoundsTrace(BattleParticipantHandle handle, BattleParticipantBounds oldBounds, BattleParticipantBounds newBounds);

        @GenTraceEvent(format = "Trying to heal {handle} {amount}", level = BattleLogLevel.INFO)
        public abstract void PreHealParticipantTrace(BattleParticipantHandle handle, double amount);

        @GenTraceEvent(format = "Heal {handle} {amount} with {overflow} overflow", level = BattleLogLevel.NORMAL)
        public abstract void HealParticipantTrace(BattleParticipantHandle handle, double amount, double overflow);

    }
}
