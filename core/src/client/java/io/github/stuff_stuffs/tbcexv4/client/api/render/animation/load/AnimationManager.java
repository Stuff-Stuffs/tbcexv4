package io.github.stuff_stuffs.tbcexv4.client.api.render.animation.load;

import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.Animation;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.BattleRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ModelRenderState;
import io.github.stuff_stuffs.tbcexv4.client.api.render.animation.state.ParticipantRenderState;
import net.minecraft.util.Identifier;

import java.util.List;

public interface AnimationManager {
    List<Animation<ModelRenderState>> lookupModelAnimation(Identifier id);

    List<Animation<ParticipantRenderState>> lookupParticipantAnimation(Identifier id);

    List<Animation<BattleRenderState>> lookupBattleAnimation(Identifier id);
}
