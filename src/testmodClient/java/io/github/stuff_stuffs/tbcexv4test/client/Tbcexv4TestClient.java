package io.github.stuff_stuffs.tbcexv4test.client;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.Tbcexv4ClientApi;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationFactoryRegistry;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.*;
import io.github.stuff_stuffs.tbcexv4.client.impl.battle.ClientBattleImpl;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.Pather;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.event.CoreBattleTraceEvents;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
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
                return Optional.of(new Animation<>() {
                    @Override
                    public Result<List<AppliedStateModifier<?>>, Unit> setup(final double time, final BattleRenderState state, final AnimationContext context) {
                        state.addEvent(new RenderState.Event() {
                            @Override
                            public void apply() {
                                state.addParticipant(add.handle());
                            }

                            @Override
                            public void undo() {
                                state.removeParticipant(add.handle());
                            }
                        }, time, context);
                        return new Result.Success<>(List.of());
                    }

                    @Override
                    public void cleanupFailure(final double time, final BattleRenderState state, final AnimationContext context) {

                    }
                });
            } else if (event instanceof final CoreBattleTraceEvents.RemoveParticipant remove) {
                return Optional.of(new Animation<>() {
                    @Override
                    public Result<List<AppliedStateModifier<?>>, Unit> setup(final double time, final BattleRenderState state, final AnimationContext context) {
                        state.addEvent(new RenderState.Event() {
                            @Override
                            public void apply() {
                                state.removeParticipant(remove.handle());
                            }

                            @Override
                            public void undo() {
                                state.addParticipant(remove.handle());
                            }
                        }, time, context);
                        return new Result.Success<>(List.of());
                    }

                    @Override
                    public void cleanupFailure(final double time, final BattleRenderState state, final AnimationContext context) {

                    }
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
                return Optional.of(ParticipantRenderState.lift(new Animation<>() {
                    @Override
                    public Result<List<AppliedStateModifier<?>>, Unit> setup(final double time, final ParticipantRenderState state, final AnimationContext context) {
                        final Property<Vec3d> property = state.getOrCreateProperty(ParticipantRenderState.POS_ID, PropertyTypes.VEC3D, new Vec3d(0, 0, 0));
                        final Result<AppliedStateModifier<Vec3d>, Unit> reserved = property.reserve(t -> {
                            final Pather.PathNode base = flattened.get(MathHelper.clamp((int) (t - time), 0, flattened.size() - 1));
                            final Pather.PathNode next = flattened.get(MathHelper.clamp(1 + (int) (t - time), 0, flattened.size() - 1));
                            property.setDefaultValue(new Vec3d(next.x() + 0.5, next.y(), next.z() + 0.5));
                            final Vec3d first = new Vec3d(base.x() + 0.5, base.y(), base.z() + 0.5);
                            final Vec3d second = new Vec3d(next.x() + 0.5, next.y(), next.z() + 0.5);
                            final double v = MathHelper.fractionalPart(t);
                            return first.multiply(1 - v).add(second.multiply(v));
                        }, time, time + depth, t -> 1, context, Property.ReservationLevel.ACTION);
                        if (reserved instanceof final Result.Success<AppliedStateModifier<Vec3d>, Unit> success) {
                            return new Result.Success<>(List.of(success.val()));
                        }
                        return new Result.Failure<>(Unit.INSTANCE);
                    }

                    @Override
                    public void cleanupFailure(final double time, final ParticipantRenderState state, final AnimationContext context) {
                        state.getProperty(ParticipantRenderState.POS_ID, PropertyTypes.VEC3D).ifPresent(prop -> prop.clearAll(context));
                    }
                }, move.handle()));
            }
            return Optional.empty();
        });
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            final Optional<BattleView> watched = Tbcexv4ClientApi.watched();
            if (watched.isPresent()) {
                final ClientBattleImpl battle = (ClientBattleImpl) watched.get();
                final MatrixStack matrices = context.matrixStack();
                matrices.push();
                matrices.translate(
                        -context.camera().getPos().x,
                        -context.camera().getPos().y,
                        -context.camera().getPos().z
                );
                matrices.translate(battle.worldX(0), battle.worldY(0) + 0.5, battle.worldZ(0));
                final BattleRenderState state = battle.animationQueue().state();
                final VertexConsumer buffer = context.consumers().getBuffer(RenderLayer.LINES);
                for (final BattleParticipantHandle handle : state.participants()) {
                    final ParticipantRenderState participant = state.getParticipant(handle);
                    final Optional<Property<Vec3d>> property = participant.getProperty(ParticipantRenderState.POS_ID, PropertyTypes.VEC3D);
                    if (property.isPresent()) {
                        final Vec3d computed = property.get().get();
                        final Box box = Box.of(computed, 1, 1, 1);
                        WorldRenderer.drawBox(matrices, buffer, box, 1.0F, 0, 0, 1.0F);
                    }
                }
                matrices.pop();
            }
        });
    }
}
