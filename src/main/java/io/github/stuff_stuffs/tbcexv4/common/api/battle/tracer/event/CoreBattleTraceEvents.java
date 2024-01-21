package io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeam;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeamRelation;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.attachment.BattleAttachmentType;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import java.util.Optional;

public final class CoreBattleTraceEvents {
    public record SetAttachment(BattleAttachmentType<?, ?> type) implements BattleTraceEvent {
    }

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


    public record PreSetBiome(int x, int y, int z, RegistryEntry<Biome> attemptedBiome) implements BattleTraceEvent {
    }

    public record SetBiome(int x, int y, int z, RegistryEntry<Biome> oldBiome,
                           RegistryEntry<Biome> newBiome) implements BattleTraceEvent {
    }

    public record ActionRoot(Optional<BattleParticipantHandle> source) implements BattleTraceEvent {
    }

    private CoreBattleTraceEvents() {
    }
}
