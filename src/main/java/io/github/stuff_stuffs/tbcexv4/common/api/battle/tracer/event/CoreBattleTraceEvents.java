package io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.InventoryHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItemStack;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.stat.Stat;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeam;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeamRelation;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import net.minecraft.block.BlockState;

import java.util.Optional;

public final class CoreBattleTraceEvents {
    public record SetBounds(BattleBounds oldBounds, BattleBounds newBounds) implements BattleTraceEvent {
    }

    public record SetBlockState(
            int x,
            int y,
            int z,
            BlockState oldState,
            BlockState newState
    ) implements BattleTraceEvent {
    }

    public record SetParticipantBounds(
            BattleParticipantBounds oldBounds,
            BattleParticipantBounds newBounds
    ) implements BattleTraceEvent {
    }

    public record SetParticipantPos(BattlePos oldPos, BattlePos newPos) implements BattleTraceEvent {
    }

    public record SetTeamRelation(
            BattleParticipantTeam first, BattleParticipantTeam second,
            BattleParticipantTeamRelation oldRelation,
            BattleParticipantTeamRelation newRelation
    ) implements BattleTraceEvent {
    }

    public record AddParticipant(BattleParticipantHandle handle) implements BattleTraceEvent {
    }

    public record AddParticipantStateModifier(
            BattleParticipantHandle handle,
            Stat<?> stat
    ) implements BattleTraceEvent {
    }

    public record DamageParticipant(BattleParticipantHandle handle, double amount) implements BattleTraceEvent {
    }

    public record HealParticipant(
            BattleParticipantHandle handle,
            double amount,
            double overflow
    ) implements BattleTraceEvent {
    }

    public record ParticipantSetHealth(
            BattleParticipantHandle handle,
            double oldHealth,
            double newHealth
    ) implements BattleTraceEvent {
    }

    public record ParticipantSetStack(
            InventoryHandle handle,
            Optional<BattleItemStack> oldStack,
            Optional<BattleItemStack> newStack
    ) implements BattleTraceEvent {
    }

    public record RemoveParticipant(
            BattleParticipantHandle handle,
            BattleState.RemoveParticipantReason reason
    ) implements BattleTraceEvent {
    }

    private CoreBattleTraceEvents() {
    }
}
