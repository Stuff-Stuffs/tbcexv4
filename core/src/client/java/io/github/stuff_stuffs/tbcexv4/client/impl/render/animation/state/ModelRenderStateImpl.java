package io.github.stuff_stuffs.tbcexv4.client.impl.render.animation.state;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.AnimationContext;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.property.Property;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.RenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.renderer.ModelRenderer;
import io.github.stuff_stuffs.tbcexv4.client.api.render.renderer.ModelRendererRegistry;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ModelRenderStateImpl extends RenderStateImpl implements ModelRenderState {
    private static final Quaternionf SCRATCH_ROTATION = new Quaternionf();
    private final String id;
    private final TimedContainer<String, ModelRenderStateImpl> timedContainer;
    private final RenderState parent;
    private final List<ModelRenderStateImpl> cached;

    final Matrix4f transform;
    final Matrix4f transformInverted;
    final Matrix3f normal;
    final Matrix3f normalInverted;
    private int cachedFlags = 0;

    public ModelRenderStateImpl(final String id, final RenderState parent) {
        this.id = id;
        this.parent = parent;
        timedContainer = new TimedContainer<>(k -> new ModelRenderStateImpl(k, this));
        cached = new ArrayList<>();
        transform = new Matrix4f();
        transformInverted = new Matrix4f();
        normal = new Matrix3f();
        normalInverted = new Matrix3f();
    }

    @Override
    public void checkpoint() {
        super.checkpoint();
        timedContainer.checkpoint();
    }

    @Override
    public int update(final double time) {
        super.update(time);
        int flags = timedContainer.update(time);
        cached.clear();
        for (final String id : timedContainer.children(time)) {
            cached.add(timedContainer.get(id, time));
        }
        final ModelRenderer renderer = getProperty(RENDERER).get();
        if (renderer != ModelRendererRegistry.NOOP_RENDERER) {
            flags |= (getProperty(LAST_INVERSION).get() ? 2 : 1);
        }
        cachedFlags = flags;
        return flags;
    }

    public void updateMatrices() {
        if (cachedFlags == 0) {
            return;
        }
        if ((cachedFlags & 1) == 1) {
            transform.identity();
            normal.identity();
            if (parent instanceof final ModelRenderStateImpl impl) {
                transform.mul(impl.transform);
                normal.mul(impl.normal);
            } else if (parent instanceof final ParticipantRenderStateImpl impl) {
                transform.mul(impl.transform);
                normal.mul(impl.normal);
            }
            updateMatrices0(transform, normal);
        }
        if ((cachedFlags & 2) == 2) {
            transformInverted.identity();
            normalInverted.identity();
            if (parent instanceof final ModelRenderStateImpl impl) {
                transformInverted.mul(impl.transformInverted);
                normalInverted.mul(impl.normalInverted);
            } else if (parent instanceof final ParticipantRenderStateImpl impl) {
                transformInverted.mul(impl.transformInverted);
                normalInverted.mul(impl.normalInverted);
            }
            updateMatrices0(transformInverted, normalInverted);
        }
        for (final ModelRenderStateImpl state : cached) {
            state.updateMatrices();
        }
    }

    private void updateMatrices0(final Matrix4f transform, final Matrix3f normal) {
        final Optional<ModelRenderState.ModelData> opt = getProperty(ModelRenderState.MODEL_DATA).get();
        final Vec3d translation = getProperty(ModelRenderState.TRANSLATION).get();
        final Quaternionfc rotation = getProperty(ModelRenderState.ROTATION).get();
        final Vec3d scale = getProperty(ModelRenderState.SCALE).get();
        if (opt.isPresent()) {
            final ModelRenderState.ModelData modelData = opt.get();
            final Vec3d position = modelData.position();
            transform.translate((float) position.x, (float) position.y, (float) position.z);
        }
        transform.translate((float) translation.x, (float) translation.y, (float) translation.z);
        rotation.get(SCRATCH_ROTATION);
        if (opt.isPresent()) {
            final ModelRenderState.ModelData modelData = opt.get();
            SCRATCH_ROTATION.mul(modelData.rotation());
        }
        transform.rotate(SCRATCH_ROTATION);
        normal.rotate(SCRATCH_ROTATION);
        transform.scale((float) scale.x, (float) scale.y, (float) scale.z);
        if (scale.x == scale.y && scale.y == scale.z) {
            if (scale.x < 0.0F) {
                normal.scale(-1.0F);
            }
        } else {
            final float sx = 1.0F / (float) scale.x;
            final float sy = 1.0F / (float) scale.y;
            final float sz = 1.0F / (float) scale.z;
            final float scalar = MathHelper.fastInverseCbrt(sx * sy * sz);
            normal.scale(scalar * sx, scalar * sy, scalar * sz);
        }
    }

    @Override
    public void cleanup(final AnimationContext context, final double time) {
        super.cleanup(context, time);
        timedContainer.clear(context, time);
    }

    public List<? extends ModelRenderState> cached() {
        return cached;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Optional<ModelRenderState> getChild(final String id, final double time) {
        return Optional.ofNullable(timedContainer.get(id, time));
    }

    @Override
    public Result<Animation.TimedEvent, Unit> addChild(final String id, final double time, final AnimationContext context) {
        return timedContainer.add(id, time, context);
    }

    @Override
    public Result<Animation.TimedEvent, Unit> removeChild(final String id, final double time, final AnimationContext context) {
        final ModelRenderStateImpl state = timedContainer.get(id, time);
        if (state != null) {
            final double last = state.lastAbove(Property.ReservationLevel.IDLE);
            if (last > time) {
                return Result.failure(Unit.INSTANCE);
            }
        }
        return timedContainer.remove(id, time, context);
    }

    @Override
    public Set<String> children(final double time) {
        return timedContainer.children(time);
    }

    @Override
    public List<ModelRenderState> getChildren(final String id, final double time) {
        final List<ModelRenderState> list = new ArrayList<>();
        for (final String child : children(time)) {
            final Optional<ModelRenderState> opt = getChild(child, time);
            if (opt.isEmpty()) {
                continue;
            }
            final ModelRenderState childState = opt.get();
            if (child.equals(id)) {
                list.add(childState);
            }
            list.addAll(childState.getChildren(id, time));
        }
        return list;
    }

    @Override
    public void multiply(final MatrixStack matrices) {
        final MatrixStack.Entry peek = matrices.peek();
        peek.getPositionMatrix().mul(transform);
        peek.getNormalMatrix().mul(normal);
    }

    @Override
    public void multiplyInverted(final MatrixStack matrices) {
        final MatrixStack.Entry peek = matrices.peek();
        peek.getPositionMatrix().mul(transformInverted);
        peek.getNormalMatrix().mul(normalInverted);
    }

    @Override
    public RenderState parent() {
        return parent;
    }
}
