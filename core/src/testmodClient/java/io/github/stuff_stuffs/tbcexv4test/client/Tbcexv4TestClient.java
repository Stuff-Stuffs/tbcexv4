package io.github.stuff_stuffs.tbcexv4test.client;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationFactoryRegistry;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.Property;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ParticipantRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.model.AnimationConverter;
import io.github.stuff_stuffs.tbcexv4.client.api.render.model.ModelConverter;
import io.github.stuff_stuffs.tbcexv4.client.mixin.AccessorEntityModelLoader;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.Pather;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import io.github.stuff_stuffs.tbcexv4.common.generated_traces.participant.*;
import io.github.stuff_stuffs.tbcexv4test.RenderDataParticipantAttachmentView;
import io.github.stuff_stuffs.tbcexv4test.Tbcexv4Test;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.animation.WardenAnimations;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Tbcexv4TestClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AnimationFactoryRegistry.register(event -> {
            if (event instanceof final AddParticipantTrace add) {
                return Optional.of((time, state, context) -> state.addParticipant(add.handle, time, context).mapSuccess(List::of));
            } else if (event instanceof final RemoveParticipantTrace remove) {
                return Optional.of((time, state, context) -> state.removeParticipant(remove.handle, time, context).mapSuccess(List::of));
            } else if (event instanceof final SetParticipantPosTrace setPos) {
                return Optional.of((time, state, context) -> {
                    final Vec3d vec = setPos.pos.bottomCenter();
                    return state.getParticipant(setPos.handle, time).map(participant -> participant.getProperty(ParticipantRenderState.POSITION).setDefaultValue(vec, time, context).mapSuccess(List::of)).orElseGet(() -> Result.failure(Unit.INSTANCE));
                });
            } else if (event instanceof final DamageParticipantTrace damageParticipant) {
                return Optional.of(
                        ParticipantRenderState.lift(
                                ModelRenderState.lift(
                                        new AnimationConverter(WardenAnimations.SNIFFING, true), List.of()
                                ),
                                damageParticipant.handle
                        )
                );
            } else if (event instanceof final SetParticipantAttachmentTrace attachment) {
                if (attachment.type == Tbcexv4Test.RENDER_DATA_ATTACHMENT && attachment.snapshot instanceof final RenderDataParticipantAttachmentView attachmentView) {
                    final EntityModelLayer layer = switch (attachmentView.type()) {
                        case PLAYER -> EntityModelLayers.PLAYER;
                        case PIG -> EntityModelLayers.PIG;
                        case SHEEP -> EntityModelLayers.WARDEN;
                    };
                    final Identifier texture = switch (attachmentView.type()) {
                        case PLAYER -> new Identifier("textures/entity/player/wide/steve.png");
                        case PIG -> new Identifier("textures/entity/pig/pig.png");
                        case SHEEP -> new Identifier("textures/entity/warden/warden.png");
                    };
                    final TexturedModelData data = ((AccessorEntityModelLoader) MinecraftClient.getInstance().getEntityModelLoader()).getModelParts().get(layer);
                    final ModelConverter model = new ModelConverter(data, texture, false);
                    return Optional.of(ParticipantRenderState.lift(model, attachment.handle));
                }
            } else if (event instanceof final MoveParticipantTrace move) {
                final List<Pather.PathNode> flattened = new ArrayList<>(64);
                Pather.PathNode node = move.path;
                while (node != null) {
                    flattened.add(0, node);
                    node = node.prev();
                }
                return Optional.of(ParticipantRenderState.lift((time, state, context) -> {
                    final Property<Vec3d> property = state.getProperty(ParticipantRenderState.POSITION);
                    final int size = flattened.size();
                    final var folder = Result.<Animation.TimedEvent>mutableFold();
                    for (int index = 0; index < size; index++) {
                        final Pather.PathNode cursor = flattened.get(index);
                        final Vec3d vec = cursor.pos().bottomCenter();
                        folder.accept(state.getProperty(ParticipantRenderState.POSITION).setDefaultValue(vec, time + index + 0.5, context));
                    }
                    final Result<Animation.TimedEvent, Unit> reserved = property.reserve(t -> {
                        final Pather.PathNode base = flattened.get(MathHelper.clamp((int) (t - time), 0, size - 1));
                        final Pather.PathNode next = flattened.get(MathHelper.clamp(1 + (int) (t - time), 0, size - 1));
                        final Vec3d first = base.pos().bottomCenter();
                        final Vec3d second = next.pos().bottomCenter();
                        final double v = MathHelper.fractionalPart(t);
                        return first.multiply(1 - v).add(second.multiply(v));
                    }, time, time + flattened.size(), t -> 1, context, Property.ReservationLevel.ACTION);
                    return folder.accept(reserved).get();
                }, move.handle));
            }
            return Optional.empty();
        });
    }
}
