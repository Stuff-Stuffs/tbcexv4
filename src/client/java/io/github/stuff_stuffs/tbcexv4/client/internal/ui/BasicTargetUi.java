package io.github.stuff_stuffs.tbcexv4.client.internal.ui;

import io.github.stuff_stuffs.tbcexv4.client.api.ui.TargetUi;
import io.github.stuff_stuffs.tbcexv4.client.api.ui.TargetUiRegistry;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.Pather;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.ParticipantTarget;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.PathTarget;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.PosTarget;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target.TargetChooser;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleStateView;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

//TODO fix button distance calc
public final class BasicTargetUi {
    private record ParticipantBox(ParticipantTarget target, Box worldSpaceBox) {
    }

    private record PosBox(PosTarget target, BattlePos pos, Box worldSpaceBox) {
    }

    private record PathBox(PathTarget target, Box worldSpaceBox) {
    }

    public static void init() {
        TargetUiRegistry.register(Tbcexv4Registries.TargetTypes.POS_TARGET, (chooser, context) -> {
            final List<PosBox> boxes = new ArrayList<>();
            final Iterator<? extends PosTarget> iterator = chooser.all();
            final BattleView battle = context.battle();
            while (iterator.hasNext()) {
                final PosTarget target = iterator.next();
                final int x = battle.worldX(target.pos().x());
                final int y = battle.worldY(target.pos().y());
                final int z = battle.worldZ(target.pos().z());
                boxes.add(new PosBox(target, target.pos(), new Box(x + 0.25, y + 0.25, z + 0.25, x + 0.75, y + 0.75, z + 0.75)));
            }
            context.addRenderable(new TargetUi.WorldInteraction() {
                @Override
                public void render(final WorldRenderContext renderContext) {
                    final MatrixStack matrices = renderContext.matrixStack();
                    final Camera camera = renderContext.camera();
                    matrices.push();
                    matrices.translate(-camera.getPos().x, -camera.getPos().y, -camera.getPos().z);
                    final VertexConsumerProvider consumers = renderContext.consumers();
                    final int raycast = raycast(camera);
                    if (raycast >= 0) {
                        final PosBox target = boxes.get(raycast);
                        final Box box = target.worldSpaceBox;
                        WorldRenderer.renderFilledBox(matrices, consumers.getBuffer(RenderLayer.getDebugFilledBox()), box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, 0, 1, 0, 1);
                    }
                    final VertexConsumer buffer = consumers.getBuffer(RenderLayer.LINES);
                    for (int i = 0, size = boxes.size(); i < size; i++) {
                        final PosBox target = boxes.get(i);
                        final Box box = target.worldSpaceBox;
                        if (i != raycast) {
                            WorldRenderer.drawBox(matrices, buffer, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, 0, 1, 0, 1);
                        }
                    }
                    matrices.pop();
                }

                @Override
                public double buttonDistance() {
                    final Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
                    final int raycast = raycast(camera);
                    if (raycast < 0) {
                        return Double.NaN;
                    }
                    final PosBox box = boxes.get(raycast);
                    final double clippedX = MathHelper.clamp(camera.getPos().x, box.worldSpaceBox.minX, box.worldSpaceBox.maxX) - camera.getPos().x;
                    final double clippedY = MathHelper.clamp(camera.getPos().y, box.worldSpaceBox.minY, box.worldSpaceBox.maxY) - camera.getPos().y;
                    final double clippedZ = MathHelper.clamp(camera.getPos().z, box.worldSpaceBox.minZ, box.worldSpaceBox.maxZ) - camera.getPos().z;
                    return Math.sqrt(clippedX * clippedX + clippedY * clippedY + clippedZ * clippedZ);
                }

                @Override
                public void onButton(final TargetUi.Context context) {
                    final Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
                    final int raycast = raycast(camera);
                    if (raycast < 0) {
                        return;
                    }
                    context.acceptTarget(boxes.get(raycast).target);
                }

                @Override
                public void close() {

                }

                private int raycast(final Camera camera) {
                    final Vec3d start = camera.getPos();
                    final Vector3d transform = camera.getRotation().transform(new Vector3d(0, 0, 1));
                    final Vec3d end = start.add(new Vec3d(transform.x, transform.y, transform.z).multiply(100));
                    double best = Double.POSITIVE_INFINITY;
                    int bestIndex = -1;
                    for (int i = 0, size = boxes.size(); i < size; i++) {
                        final PosBox box = boxes.get(i);
                        final Optional<Vec3d> raycast = box.worldSpaceBox.raycast(start, end);
                        if (raycast.isPresent()) {
                            final double d2 = start.squaredDistanceTo(raycast.get());
                            if (d2 < best) {
                                best = d2;
                                bestIndex = i;
                            }
                        }
                    }
                    return bestIndex;
                }
            });
        });
        TargetUiRegistry.register(Tbcexv4Registries.TargetTypes.PATH_TARGET, (chooser, context) -> {
            final BattleView battle = context.battle();
            final List<PathBox> boxes = new ArrayList<>();
            final Iterator<? extends PathTarget> all = chooser.all();
            final Int2ObjectMap<PathBox> terminals = new Int2ObjectOpenHashMap<>();
            while (all.hasNext()) {
                final PathTarget target = all.next();
                final Pather.PathNode node = target.node();
                final int x = battle.worldX(node.x());
                final int y = battle.worldY(node.y());
                final int z = battle.worldZ(node.z());
                final PathBox box = new PathBox(target, new Box(x + 0.25, y + 0.25, z + 0.25, x + 0.75, y + 0.75, z + 0.75));
                boxes.add(box);
                if (target.terminal()) {
                    terminals.put(Pather.Paths.pack(node.x(), node.y(), node.z()), box);
                }
            }
            context.addRenderable(new TargetUi.WorldInteraction() {
                @Override
                public void render(final WorldRenderContext renderContext) {
                    final MatrixStack matrices = renderContext.matrixStack();
                    final Camera camera = renderContext.camera();
                    matrices.push();
                    matrices.translate(-camera.getPos().x, -camera.getPos().y, -camera.getPos().z);
                    final VertexConsumerProvider consumers = renderContext.consumers();
                    final VertexConsumer buffer = consumers.getBuffer(RenderLayer.LINES);
                    for (final PathBox pathBox : terminals.values()) {
                        final Box box = pathBox.worldSpaceBox;
                        WorldRenderer.drawBox(matrices, buffer, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, 0, 1, 0, 1);
                    }
                    final int raycast = raycast(camera);
                    if (raycast >= 0) {
                        final PathBox pathBox = boxes.get(raycast);
                        Pather.PathNode last = pathBox.target().node();
                        final Matrix4f pMat = matrices.peek().getPositionMatrix();
                        final Matrix3f nMat = matrices.peek().getNormalMatrix();
                        while (last.prev() != null) {
                            final Pather.PathNode prev = last.prev();
                            final float x0 = battle.worldX(last.x()) + 0.5F;
                            final float x1 = battle.worldX(prev.x()) + 0.5F;
                            final float y0 = battle.worldY(last.y()) + 0.5F;
                            final float y1 = battle.worldY(prev.y()) + 0.5F;
                            final float z0 = battle.worldZ(last.z()) + 0.5F;
                            final float z1 = battle.worldZ(prev.z()) + 0.5F;
                            buffer.vertex(pMat, x0, y0, z0).color(0.0F, 1.0F, 0.0F, 1.0F).normal(nMat, 1.0F, 0.0F, 0.0F).next();
                            buffer.vertex(pMat, x1, y1, z1).color(0.0F, 1.0F, 0.0F, 1.0F).normal(nMat, 1.0F, 0.0F, 0.0F).next();
                            last = prev;
                        }
                        final Box box = pathBox.worldSpaceBox.expand(1 / 6.0);
                        WorldRenderer.renderFilledBox(matrices, consumers.getBuffer(RenderLayer.getDebugFilledBox()), box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, 0, 1, 0, 1);
                    }
                    matrices.pop();
                }

                @Override
                public double buttonDistance() {
                    final Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
                    final int raycast = raycast(camera);
                    if (raycast < 0) {
                        return Double.NaN;
                    }
                    final PathBox box = boxes.get(raycast);
                    final double clippedX = MathHelper.clamp(camera.getPos().x, box.worldSpaceBox.minX, box.worldSpaceBox.maxX) - camera.getPos().x;
                    final double clippedY = MathHelper.clamp(camera.getPos().y, box.worldSpaceBox.minY, box.worldSpaceBox.maxY) - camera.getPos().y;
                    final double clippedZ = MathHelper.clamp(camera.getPos().z, box.worldSpaceBox.minZ, box.worldSpaceBox.maxZ) - camera.getPos().z;
                    return Math.sqrt(clippedX * clippedX + clippedY * clippedY + clippedZ * clippedZ);
                }

                @Override
                public void onButton(final TargetUi.Context context) {
                    final Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
                    final int raycast = raycast(camera);
                    if (raycast < 0) {
                        return;
                    }
                    context.acceptTarget(boxes.get(raycast).target);
                }

                @Override
                public void close() {

                }

                private int raycast(final Camera camera) {
                    final Vec3d start = camera.getPos();
                    final Vector3d transform = camera.getRotation().transform(new Vector3d(0, 0, 1));
                    final Vec3d end = start.add(new Vec3d(transform.x, transform.y, transform.z).multiply(100));
                    double best = Double.POSITIVE_INFINITY;
                    int bestIndex = -1;
                    for (int i = 0, size = boxes.size(); i < size; i++) {
                        final PathBox box = boxes.get(i);
                        final Optional<Vec3d> raycast = box.worldSpaceBox.raycast(start, end);
                        if (raycast.isPresent()) {
                            final double d2 = start.squaredDistanceTo(raycast.get());
                            if (d2 < best) {
                                best = d2;
                                bestIndex = i;
                            }
                        }
                    }
                    return bestIndex;
                }
            });
        });
        TargetUiRegistry.register(Tbcexv4Registries.TargetTypes.PARTICIPANT_TARGET, new TargetUi<ParticipantTarget>() {
            @Override
            public void targeting(final TargetChooser<ParticipantTarget> chooser, final Context context) {
                final BattleView battle = context.battle();
                final BattleStateView state = battle.state();
                final List<ParticipantBox> boxes = new ArrayList<>();
                final Iterator<? extends ParticipantTarget> all = chooser.all();
                while (all.hasNext()) {
                    final ParticipantTarget next = all.next();
                    final BattleParticipantView participant = state.participant(next.participant());
                    final BattlePos pos = participant.pos();
                    final int x = battle.worldX(pos.x());
                    final int y = battle.worldY(pos.y());
                    final int z = battle.worldZ(pos.z());
                    final Box worldSpace = participant.bounds().asBox(x, y, z);
                    boxes.add(new ParticipantBox(next, worldSpace));
                }
                context.addRenderable(new WorldInteraction() {
                    @Override
                    public void render(final WorldRenderContext renderContext) {
                        final MatrixStack matrices = renderContext.matrixStack();
                        final Camera camera = renderContext.camera();
                        matrices.push();
                        matrices.translate(-camera.getPos().x, -camera.getPos().y, -camera.getPos().z);
                        final VertexConsumerProvider consumers = renderContext.consumers();
                        final VertexConsumer buffer = consumers.getBuffer(RenderLayer.LINES);
                        final int raycast = raycast(camera);
                        for (int i = 0, size = boxes.size(); i < size; i++) {
                            final ParticipantBox box = boxes.get(i);
                            WorldRenderer.drawBox(matrices, buffer, box.worldSpaceBox, 1, raycast != i ? 1 : 0, 0, 1);
                        }
                        matrices.pop();
                    }

                    @Override
                    public double buttonDistance() {
                        final Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
                        final int raycast = raycast(camera);
                        if (raycast < 0) {
                            return Double.NaN;
                        }
                        final ParticipantBox box = boxes.get(raycast);
                        final double clippedX = MathHelper.clamp(camera.getPos().x, box.worldSpaceBox.minX, box.worldSpaceBox.maxX) - camera.getPos().x;
                        final double clippedY = MathHelper.clamp(camera.getPos().y, box.worldSpaceBox.minY, box.worldSpaceBox.maxY) - camera.getPos().y;
                        final double clippedZ = MathHelper.clamp(camera.getPos().z, box.worldSpaceBox.minZ, box.worldSpaceBox.maxZ) - camera.getPos().z;
                        return Math.sqrt(clippedX * clippedX + clippedY * clippedY + clippedZ * clippedZ);
                    }

                    @Override
                    public void onButton(final Context context) {
                        final Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
                        final int raycast = raycast(camera);
                        if (raycast < 0) {
                            return;
                        }
                        context.acceptTarget(boxes.get(raycast).target);
                    }

                    @Override
                    public void close() {

                    }

                    private int raycast(final Camera camera) {
                        final Vec3d start = camera.getPos();
                        final Vector3d transform = camera.getRotation().transform(new Vector3d(0, 0, 1));
                        final Vec3d end = start.add(new Vec3d(transform.x, transform.y, transform.z).multiply(100));
                        double best = Double.POSITIVE_INFINITY;
                        int bestIndex = -1;
                        for (int i = 0, size = boxes.size(); i < size; i++) {
                            final ParticipantBox box = boxes.get(i);
                            final Optional<Vec3d> raycast = box.worldSpaceBox.raycast(start, end);
                            if (raycast.isPresent()) {
                                final double d2 = start.squaredDistanceTo(raycast.get());
                                if (d2 < best) {
                                    best = d2;
                                    bestIndex = i;
                                }
                            }
                        }
                        return bestIndex;
                    }
                });
            }
        });
    }

    private BasicTargetUi() {
    }
}
