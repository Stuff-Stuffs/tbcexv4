package io.github.stuff_stuffs.tbcexv4.common.api.battle.state;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleBounds;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePhase;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeam;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeamRelation;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.attachment.BattleAttachment;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.attachment.BattleAttachmentType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.env.BattleEnvironmentView;
import io.github.stuff_stuffs.tbcexv4.common.api.event.EventMapView;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.Set;

public interface BattleStateView {
    RegistryKey<World> sourceWorld();

    EventMapView events();

    BattleEnvironmentView environment();

    BattlePhase phase();

    BattleBounds bounds();

    BattleParticipantTeamRelation relation(BattleParticipantTeam first, BattleParticipantTeam second);

    Set<BattleParticipantHandle> participants();

    Set<BattleParticipantHandle> participants(BattleParticipantTeam team);

    BattleParticipantView participant(BattleParticipantHandle handle);

    <V, T extends BattleAttachment> Optional<V> attachmentView(BattleAttachmentType<V, T> type);
}
