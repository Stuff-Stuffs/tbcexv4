package io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.env.BattleEnvironmentImpl;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.BattleEnvironmentInitialState;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.function.Function;

public class SetupEnvironmentBattleAction implements BattleAction {
    public static final Function<BattleCodecContext, Codec<SetupEnvironmentBattleAction>> CODEC_FACTORY = context -> BattleEnvironmentInitialState.codec(context.dynamicRegistryManager()).xmap(SetupEnvironmentBattleAction::new, action -> action.state);
    private final BattleEnvironmentInitialState state;

    public SetupEnvironmentBattleAction(final BattleEnvironmentInitialState state) {
        this.state = state;
    }

    @Override
    public BattleActionType<?> type() {
        return Tbcexv4Registries.BattleActions.SETUP_ENVIRONMENT_TYPE;
    }

    @Override
    public void apply(final BattleState state, final BattleTracer tracer) {
        final BattleView battle = ((BattleEnvironmentImpl) state.environment()).battle();
        battle.world().runAction(world -> {
            this.state.apply(world, ChunkSectionPos.getSectionCoord(battle.worldX(0)), ChunkSectionPos.getSectionCoord(battle.worldZ(0)));
        });
    }
}
