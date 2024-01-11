package io.github.stuff_stuffs.tbcexv4.client.internal.ui.component;

import io.github.stuff_stuffs.tbcexv4.client.api.BattleItemRendererRegistry;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItem;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFW;

public class BattleItemDisplayComponent extends BaseComponent {
    private static final Vector3fc SPIN_AXIS = new Vector3f(0, 1, 0);
    private final Quaternionf rotation;
    private boolean spinning = true;
    private @Nullable BattleItem item = null;

    public BattleItemDisplayComponent() {
        rotation = new Quaternionf();
    }

    @Override
    public boolean onMouseDown(final double mouseX, final double mouseY, final int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            spinning = !spinning;
            return true;
        }
        return super.onMouseDown(mouseX, mouseY, button);
    }

    @Override
    public boolean onMouseDrag(final double mouseX, final double mouseY, final double deltaX, final double deltaY, final int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            rotation.rotateLocalY((float) (deltaX * 0.05));
            rotation.rotateLocalX((float) (deltaY * 0.05));
            return true;
        }
        return super.onMouseDrag(mouseX, mouseY, deltaX, deltaY, button);
    }

    @Override
    public void draw(final OwoUIDrawContext context, final int mouseX, final int mouseY, final float partialTicks, final float delta) {
        final BattleItem item = this.item;
        if (item != null) {
            if (spinning) {
                rotation.rotateAxis(delta * 0.05F, SPIN_AXIS);
            }
            final MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(x() + width() * 0.5F, y() + height() * 0.5F, 0);
            final int scale = Math.min(width(), height());
            matrices.scale((float) scale, (float) scale, (float) scale);
            matrices.multiply(rotation);
            final Quaternionf quaternionf = new Quaternionf().fromAxisAngleDeg(0, 1, 0, -45);
            matrices.multiplyPositionMatrix(new Matrix4f().set(quaternionf));
            BattleItemRendererRegistry.render(item, context.getVertexConsumers(), matrices);
            context.getVertexConsumers().drawCurrentLayer();
            matrices.pop();
        }
    }

    public void item(final @Nullable BattleItem item) {
        this.item = item;
    }
}
