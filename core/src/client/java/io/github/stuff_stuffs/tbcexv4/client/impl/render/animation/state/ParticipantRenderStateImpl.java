package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.Property;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ParticipantRenderState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class ParticipantRenderStateImpl extends RenderStateImpl implements ParticipantRenderState {
    private final BattleParticipantHandle id;
    private final ModelRenderStateImpl rootModel;
    private final BattleRenderState parent;
    final Matrix4f transform;
    final Matrix4f transformInverted;
    final Matrix3f normal;
    final Matrix3f normalInverted;

    public ParticipantRenderStateImpl(final BattleParticipantHandle id, final BattleRenderState parent) {
        super();
        this.id = id;
        this.parent = parent;
        rootModel = createRoot();
        transform = new Matrix4f();
        transformInverted = new Matrix4f();
        normal = new Matrix3f();
        normalInverted = new Matrix3f();
    }

    protected ModelRenderStateImpl createRoot() {
        return new ModelRenderStateImpl("root", this);
    }

    @Override
    public void clearUpTo(final double time) {
        super.clearUpTo(time);
        rootModel.clearUpTo(time);
    }

    @Override
    public int update(final double time) {
        super.update(time);
        final int flags = rootModel.update(time);
        final Vec3d position = getProperty(ParticipantRenderState.POSITION).get();
        if ((flags & 1) == 1) {
            transform.identity();
            normal.identity();
            transform.translate((float) position.x, (float) position.y, (float) position.z);
        }
        if ((flags & 2) == 2) {
            transformInverted.identity();
            normalInverted.identity();
            transformInverted.translate((float) position.x, (float) position.y, (float) position.z);
            transformInverted.scale(-1, -1, 1);
            normalInverted.scale(-1, -1, 1);
        }
        rootModel.updateMatrices();
        return flags;
    }

    @Override
    public double lastAbove(final double t, final Property.@Nullable ReservationLevel level) {
        return Math.max(super.lastAbove(t, level), rootModel.lastAbove(t, level));
    }

    @Override
    public BattleParticipantHandle id() {
        return id;
    }

    @Override
    public ModelRenderState modelRoot() {
        return rootModel;
    }

    @Override
    public BattleRenderState parent() {
        return parent;
    }

    @Override
    public void cleanup(final AnimationContext context, final double time) {
        super.cleanup(context, time);
        rootModel.cleanup(context, time);
    }
}
