package io.github.stuff_stuffs.tbcexv4test.client;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationFactoryRegistry;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ParticipantRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.Property;
import io.github.stuff_stuffs.tbcexv4.client.api.render.model.ModelConverter;
import io.github.stuff_stuffs.tbcexv4.client.mixin.AccessorEntityModelLoader;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.Pather;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.CoreParticipantTraceEvents;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Easing;
import io.github.stuff_stuffs.tbcexv4.common.api.util.EasingFunction;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import io.github.stuff_stuffs.tbcexv4test.RenderDataParticipantAttachmentView;
import io.github.stuff_stuffs.tbcexv4test.Tbcexv4Test;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.TexturedModelData;
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
            if (event instanceof final CoreParticipantTraceEvents.AddParticipant add) {
                return Optional.of((time, state, context) -> state.addParticipant(add.handle(), time, context).mapSuccess(List::of));
            } else if (event instanceof final CoreParticipantTraceEvents.RemoveParticipant remove) {
                return Optional.of((time, state, context) -> state.removeParticipant(remove.handle(), time, context).mapSuccess(List::of));
            } else if (event instanceof final CoreParticipantTraceEvents.SetParticipantPos setPos) {
                return Optional.of((time, state, context) -> {
                    final BattlePos pos = setPos.newPos();
                    final Vec3d vec = new Vec3d(pos.x() + 0.5, pos.y(), pos.z() + 0.5);
                    return state.getParticipant(setPos.handle(), time).map(participant -> participant.getProperty(ParticipantRenderState.POSITION).setDefaultValue(vec, time, context).mapSuccess(List::of)).orElseGet(() -> Result.failure(Unit.INSTANCE));
                });
            } else if (event instanceof final CoreParticipantTraceEvents.DamageParticipant damage) {
                return Optional.of(
                        ParticipantRenderState.lift(
                                ModelRenderState.lift(
                                        (time, state, context) -> state
                                                .getProperty(
                                                        ModelRenderState.COLOR
                                                )
                                                .reserve(
                                                        t -> 0xFFFF0000,
                                                        time,
                                                        time + 20,
                                                        Easing.in(EasingFunction.LINEAR, time, time + 20),
                                                        context,
                                                        Property.ReservationLevel.ACTION
                                                ).mapSuccess(
                                                        List::of
                                                ),
                                        (state, path, context) -> ModelRenderState.VisitPathResult.DESCEND_ACCEPT,
                                        false
                                ),
                                damage.handle()
                        )
                );
            } else if (event instanceof final CoreParticipantTraceEvents.SetParticipantAttachment attachment) {
                if (attachment.type() == Tbcexv4Test.RENDER_DATA_ATTACHMENT && attachment.snapshot() instanceof final RenderDataParticipantAttachmentView attachmentView) {
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
                    return Optional.of(ParticipantRenderState.lift(model, attachment.handle()));
                }
            } else if (event instanceof final CoreParticipantTraceEvents.PostMoveParticipant move) {
                final int depth = move.pathNode().depth();
                final List<Pather.PathNode> flattened = new ArrayList<>(depth);
                Pather.PathNode node = move.pathNode();
                for (int i = 0; i < depth; i++) {
                    flattened.add(0, node);
                    node = node.prev();
                }
                return Optional.of(ParticipantRenderState.lift((time, state, context) -> {
                    final Property<Vec3d> property = state.getProperty(ParticipantRenderState.POSITION);
                    final int size = flattened.size();
                    final var folder = Result.<Animation.TimedEvent>mutableFold();
                    for (int index = 0; index < size; index++) {
                        final Pather.PathNode cursor = flattened.get(index);
                        final Vec3d vec = new Vec3d(cursor.x() + 0.5, cursor.y(), cursor.z() + 0.5);
                        folder.accept(state.getProperty(ParticipantRenderState.POSITION).setDefaultValue(vec, time + index + 0.5, context));
                    }
                    final Result<Animation.TimedEvent, Unit> reserved = property.reserve(t -> {
                        final Pather.PathNode base = flattened.get(MathHelper.clamp((int) (t - time), 0, size - 1));
                        final Pather.PathNode next = flattened.get(MathHelper.clamp(1 + (int) (t - time), 0, size - 1));
                        final Vec3d first = new Vec3d(base.x() + 0.5, base.y(), base.z() + 0.5);
                        final Vec3d second = new Vec3d(next.x() + 0.5, next.y(), next.z() + 0.5);
                        final double v = MathHelper.fractionalPart(t);
                        return first.multiply(1 - v).add(second.multiply(v));
                    }, time, time + depth, t -> 1, context, Property.ReservationLevel.ACTION);
                    return folder.accept(reserved).get();
                }, move.handle()));
            }
            return Optional.empty();
        });
    }
}
