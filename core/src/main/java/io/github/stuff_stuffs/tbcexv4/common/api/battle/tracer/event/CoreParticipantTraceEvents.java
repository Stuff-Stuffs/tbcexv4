package io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantAttachmentType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.InventoryHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.Pather;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.Stat;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeam;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class CoreParticipantTraceEvents {

    private CoreParticipantTraceEvents() {
    }

    public record SetParticipantAttachment(BattleParticipantHandle handle, BattleParticipantAttachmentType<?, ?> type,
                                           @Nullable Object snapshot) implements BattleTraceEvent {
    }

    public record RemoveParticipant(
            BattleParticipantHandle handle,
            BattleState.RemoveParticipantReason reason
    ) implements BattleTraceEvent {
    }

    public record PreRemoveParticipant(
            BattleParticipantHandle handle,
            BattleState.RemoveParticipantReason attemptedReason
    ) implements BattleTraceEvent {
    }

    public record ParticipantSetStack(
            InventoryHandle handle,
            Optional<BattleItemStack> oldStack,
            Optional<BattleItemStack> newStack
    ) implements BattleTraceEvent {
    }

    public record ParticipantSetTeam(
            BattleParticipantHandle handle,
            Optional<BattleParticipantTeam> oldTeam,
            BattleParticipantTeam newTeam
    ) implements BattleTraceEvent {
    }

    public record PreParticipantSetStack(
            InventoryHandle handle,
            Optional<BattleItemStack> attemptedStack
    ) implements BattleTraceEvent {
    }

    public record ParticipantSetHealth(
            BattleParticipantHandle handle,
            double oldHealth,
            double newHealth
    ) implements BattleTraceEvent {
    }

    public record PreParticipantSetHealth(
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

    public record PreHealParticipant(
            BattleParticipantHandle handle,
            double attemptedAmount
    ) implements BattleTraceEvent {
    }

    public record DamageParticipant(BattleParticipantHandle handle, double amount) implements BattleTraceEvent {
    }

    public record PreDamageParticipant(
            BattleParticipantHandle handle,
            double attemptedAmount
    ) implements BattleTraceEvent {
    }

    public record AddParticipantStateModifier(
            BattleParticipantHandle handle,
            Stat<?> stat
    ) implements BattleTraceEvent {
    }

    public record PreAddParticipantStateModifier(
            BattleParticipantHandle handle,
            Stat<?> stat
    ) implements BattleTraceEvent {
    }

    public record AddParticipant(BattleParticipantHandle handle) implements BattleTraceEvent {
    }

    public record PreAddParticipant(BattleParticipantHandle handle) implements BattleTraceEvent {
    }

    public record PostMoveParticipant(
            BattleParticipantHandle handle,
            Pather.PathNode pathNode
    ) implements BattleTraceEvent {
    }

    public record PreMoveParticipant(BattleParticipantHandle handle, Pather.PathNode path) implements BattleTraceEvent {
    }

    public record SetParticipantPos(
            BattleParticipantHandle handle,
            BattlePos oldPos,
            BattlePos newPos
    ) implements BattleTraceEvent {
    }

    public record PreSetParticipantPos(
            BattleParticipantHandle handle,
            BattlePos attemptedBounds
    ) implements BattleTraceEvent {
    }

    public record SetParticipantBounds(
            BattleParticipantHandle handle,
            BattleParticipantBounds oldBounds,
            BattleParticipantBounds newBounds
    ) implements BattleTraceEvent {
    }

    public record PreSetParticipantBounds(
            BattleParticipantHandle handle,
            BattleParticipantBounds attemptedBounds
    ) implements BattleTraceEvent {
    }
}