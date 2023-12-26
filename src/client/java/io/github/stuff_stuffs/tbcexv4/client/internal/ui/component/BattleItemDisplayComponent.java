package io.github.stuff_stuffs.tbcexv4.client.internal.ui.component;

import io.github.stuff_stuffs.tbcexv4.client.api.BattleItemRendererRegistry;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.inventory.item.BattleItem;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Size;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;
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
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        if(button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            spinning = !spinning;
            return true;
        }
        return super.onMouseDown(mouseX, mouseY, button);
    }

    @Override
    public boolean onMouseDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
        if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            rotation.rotateLocalY((float)(deltaX*0.05));
            rotation.rotateLocalY((float)(deltaY*0.05));
            return true;
        }
        return super.onMouseDrag(mouseX, mouseY, deltaX, deltaY, button);
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        BattleItem item = this.item;
        if(item!=null) {
            if(spinning) {
                rotation.rotateAxis(delta * 0.05F, SPIN_AXIS);
            }
            MatrixStack matrices = context.getMatrices();
            matrices.push();
            Positioning pos = positioning().get();
            matrices.translate(pos.x, pos.y, 0);
            Size size = fullSize();
            matrices.scale(1/(float)size.width(), 1/(float)size.height(), 1);
            matrices.multiply(rotation);
            BattleItemRendererRegistry.render(item, context.getVertexConsumers(), matrices);
            matrices.pop();
        }
    }

    public void item(BattleItem item) {
        this.item = item;
    }
}
