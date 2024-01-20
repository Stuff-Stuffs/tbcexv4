package io.github.stuff_stuffs.tbcexv4test.client;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationFactoryRegistry;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ParticipantRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.Property;
import io.github.stuff_stuffs.tbcexv4.client.api.render.renderer.ModelRendererRegistry;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.Pather;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.CoreBattleTraceEvents;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import net.fabricmc.api.ClientModInitializer;
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
            if (event instanceof final CoreBattleTraceEvents.AddParticipant add) {
                return Optional.of((time, state, context) -> {
                    final var folder = Result.<Animation.TimedEvent>mutableFold();
                    folder.accept(state.addParticipant(add.handle(), time, context));
                    folder.accept(
                            state.getParticipant(add.handle(), time).map(
                                    participant -> participant.modelRoot().getProperty(
                                            ModelRenderState.EXTENTS
                                    ).setDefaultValue(
                                            new Vec3d(1, 1, 1),
                                            time,
                                            context
                                    )
                            ).orElse(
                                    Result.failure(Unit.INSTANCE)
                            )
                    );
                    folder.accept(
                            state.getParticipant(add.handle(), time).map(
                                    participant -> participant.modelRoot().getProperty(
                                            ModelRenderState.RENDERER
                                    ).setDefaultValue(
                                            ModelRendererRegistry.DEFAULT_RENDERER,
                                            time,
                                            context
                                    )
                            ).orElse(
                                    Result.failure(Unit.INSTANCE)
                            )
                    );
                    folder.accept(
                            state.getParticipant(add.handle(), time).map(
                                    participant -> participant.modelRoot().getProperty(
                                            ModelRenderState.POSITION
                                    ).setDefaultValue(
                                            new Vec3d(0, 0.5, 0),
                                            time,
                                            context
                                    )
                            ).orElse(
                                    Result.failure(Unit.INSTANCE)
                            )
                    );
                    folder.accept(state.getParticipant(add.handle(), time).map(
                            participant -> participant.modelRoot().getProperty(
                                    ModelRenderState.TEXTURE_DATA
                            ).setDefaultValue(
                                    Optional.of(
                                            new ModelRenderState.TextureData(
                                                    new Identifier("textures/entity/player/wide/steve.png"),
                                                    16,
                                                    16,
                                                    16,
                                                    0,
                                                    0,
                                                    48,
                                                    48
                                            )
                                    ),
                                    time,
                                    context
                            )
                    ).orElse(Result.failure(Unit.INSTANCE)));
                    return folder.get();
                });
            } else if (event instanceof final CoreBattleTraceEvents.RemoveParticipant remove) {
                return Optional.of((time, state, context) -> state.removeParticipant(remove.handle(), time, context).mapSuccess(List::of));
            } else if (event instanceof final CoreBattleTraceEvents.SetParticipantPos setPos) {
                return Optional.of((time, state, context) -> {
                    final BattlePos pos = setPos.newPos();
                    final Vec3d vec = new Vec3d(pos.x() + 0.5, pos.y(), pos.z() + 0.5);
                    return state.getParticipant(setPos.handle(), time).map(participant -> participant.getProperty(ParticipantRenderState.POSITION).setDefaultValue(vec, time, context).mapSuccess(List::of)).orElseGet(() -> Result.failure(Unit.INSTANCE));
                });
            }
            return Optional.empty();
        });
        AnimationFactoryRegistry.register(event -> {
            if (event instanceof final CoreBattleTraceEvents.PostMoveParticipant move) {
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
                        folder.accept(state.getProperty(ParticipantRenderState.POSITION).setDefaultValue(vec, time + index, context));
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
