package io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantAttachmentType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.InventoryHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.Stat;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeam;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeamRelation;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.attachment.BattleAttachmentType;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import java.util.Optional;

public final class CoreBattleTraceEvents {
    public record Root() implements BattleTraceEvent {
    }

    public record Invalid() implements BattleTraceEvent {
    }

    public record TurnManagerSetup() implements BattleTraceEvent {
    }

    public record PreSetBounds(BattleBounds attemptedBounds) implements BattleTraceEvent {
    }

    public record SetBounds(BattleBounds oldBounds, BattleBounds newBounds) implements BattleTraceEvent {
    }

    public record PreSetBlockState(
            int x,
            int y,
            int z,
            BlockState attemptedState
    ) implements BattleTraceEvent {
    }

    public record SetBlockState(
            int x,
            int y,
            int z,
            BlockState oldState,
            BlockState newState
    ) implements BattleTraceEvent {
    }

    public record PreSetParticipantBounds(
            BattleParticipantBounds attemptedBounds
    ) implements BattleTraceEvent {
    }

    public record SetParticipantBounds(
            BattleParticipantBounds oldBounds,
            BattleParticipantBounds newBounds
    ) implements BattleTraceEvent {
    }

    public record PreSetParticipantPos(BattlePos attemptedBounds) implements BattleTraceEvent {
    }

    public record SetParticipantPos(BattlePos oldPos, BattlePos newPos) implements BattleTraceEvent {
    }

    public record PreSetTeamRelation(
            BattleParticipantTeam first,
            BattleParticipantTeam second,
            BattleParticipantTeamRelation attemptedRelation
    ) implements BattleTraceEvent {
    }

    public record SetTeamRelation(
            BattleParticipantTeam first,
            BattleParticipantTeam second,
            BattleParticipantTeamRelation oldRelation,
            BattleParticipantTeamRelation newRelation
    ) implements BattleTraceEvent {
    }

    public record PreAddParticipant(BattleParticipantHandle handle) implements BattleTraceEvent {
    }

    public record AddParticipant(BattleParticipantHandle handle) implements BattleTraceEvent {
    }

    public record PreAddParticipantStateModifier(
            BattleParticipantHandle handle,
            Stat<?> stat
    ) implements BattleTraceEvent {
    }


    public record AddParticipantStateModifier(
            BattleParticipantHandle handle,
            Stat<?> stat
    ) implements BattleTraceEvent {
    }

    public record PreDamageParticipant(
            BattleParticipantHandle handle,
            double attemptedAmount
    ) implements BattleTraceEvent {
    }

    public record DamageParticipant(BattleParticipantHandle handle, double amount) implements BattleTraceEvent {
    }

    public record PreHealParticipant(
            BattleParticipantHandle handle,
            double attemptedAmount
    ) implements BattleTraceEvent {
    }

    public record HealParticipant(
            BattleParticipantHandle handle,
            double amount,
            double overflow
    ) implements BattleTraceEvent {
    }

    public record PreParticipantSetHealth(
            BattleParticipantHandle handle,
            double attemptedAmount
    ) implements BattleTraceEvent {
    }

    public record ParticipantSetHealth(
            BattleParticipantHandle handle,
            double oldHealth,
            double newHealth
    ) implements BattleTraceEvent {
    }

    public record PreParticipantSetStack(
            InventoryHandle handle,
            Optional<BattleItemStack> attemptedStack
    ) implements BattleTraceEvent {
    }

    public record ParticipantSetTeam(
            BattleParticipantHandle handle,
            Optional<BattleParticipantTeam> oldTeam,
            BattleParticipantTeam newTeam
    ) implements BattleTraceEvent {
    }

    public record ParticipantSetStack(
            InventoryHandle handle,
            Optional<BattleItemStack> oldStack,
            Optional<BattleItemStack> newStack
    ) implements BattleTraceEvent {
    }

    public record PreRemoveParticipant(
            BattleParticipantHandle handle,
            BattleState.RemoveParticipantReason attemptedReason
    ) implements BattleTraceEvent {
    }

    public record RemoveParticipant(
            BattleParticipantHandle handle,
            BattleState.RemoveParticipantReason reason
    ) implements BattleTraceEvent {
    }

    public record PreSetBiome(int x, int y, int z, RegistryEntry<Biome> attemptedBiome) implements BattleTraceEvent {
    }

    public record SetBiome(int x, int y, int z, RegistryEntry<Biome> oldBiome,
                           RegistryEntry<Biome> newBiome) implements BattleTraceEvent {
    }

    public record ActionRoot(Optional<BattleParticipantHandle> source) implements BattleTraceEvent {
    }

    public record SetAttachment(BattleAttachmentType<?, ?> type) implements BattleTraceEvent {
    }

    public record SetParticipantAttachment(BattleParticipantAttachmentType<?, ?> type) implements BattleTraceEvent {
    }

    private CoreBattleTraceEvents() {
    }
}
