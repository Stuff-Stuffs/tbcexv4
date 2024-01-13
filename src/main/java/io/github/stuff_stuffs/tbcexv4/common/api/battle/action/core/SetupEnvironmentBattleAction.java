package io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleState;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.tracer.BattleTracer;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.transaction.BattleTransactionContext;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.ServerBattleImpl;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.state.env.ServerBattleEnvironmentImpl;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4ClientDelegates;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.BattleEnvironmentInitialState;
import net.minecraft.text.Text;
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
    public void apply(final BattleState state, final BattleTransactionContext transactionContext, final BattleTracer tracer) {
        //INTERNALS AHEAD
        if (state.environment() instanceof final ServerBattleEnvironmentImpl environment && environment.battle() instanceof final ServerBattleImpl server) {
            server.world().runAction(world -> {
                this.state.apply(world, ChunkSectionPos.getSectionCoord(environment.battle().worldX(0)), ChunkSectionPos.getSectionCoord(environment.battle().worldZ(0)));
            });
        } else {
            Tbcexv4ClientDelegates.SETUP_CLIENT_ENV_DELEGATE.accept(this.state, state.environment());
        }
        transactionContext.addCloseCallback((context, result) -> {
            if (result.wasAborted()) {
                throw new RuntimeException("Tried to revert a setup action, this should be impossible!");
            }
        });
    }

    @Override
    public Text chatMessage() {
        return Text.of("Setting up battle environment!");
    }
}
