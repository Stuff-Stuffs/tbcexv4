package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.bridge;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.Property;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public interface BattleRenderStateView extends RenderStateView {
    Optional<ParticipantRenderStateView> getParticipant(BattleParticipantHandle handle);

    Set<BattleParticipantHandle> participants();

    void addParticipant(BattleParticipantHandle handle);

    void removeParticipant(BattleParticipantHandle handle, Property.@Nullable ReservationLevel level);

    Optional<BattleEffectRenderStateView> getEffect(Identifier id);

    Set<Identifier> effects();

    void addEffect(Identifier id);

    void removeEffect(Identifier id, Property.@Nullable ReservationLevel level);
}
