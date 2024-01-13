package io.github.stuff_stuffs.tbcexv4test;

import com.mojang.datafixers.FunctionType;
import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Api;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core.NoopBattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core.SetupEnvironmentBattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core.StartBattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.request.BattleActionRequest;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.request.DebugBattleActionRequest;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.ParticipantTarget;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.SingleTargetPlan;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.TargetChooser;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.TargetChoosers;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.team.BattleParticipantTeamRelation;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.state.BattleStateView;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.BattleEnvironmentInitialState;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.ServerBattleWorld;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.Optional;

public class Tbcexv4Test implements ModInitializer {
    public static final BattleActionType<PlayerJoinTestBattleAction> JOIN_TEST_TYPE = new BattleActionType<>(context -> PlayerJoinTestBattleAction.CODEC);
    public static final BattleActionType<AttackBattleAction> ATTACK_TYPE = new BattleActionType<>(context -> AttackBattleAction.CODEC);
    public static final BattleActionType<HealBattleAction> HEAL_TYPE = new BattleActionType<>(context -> HealBattleAction.CODEC);

    @Override
    public void onInitialize() {
        Registry.register(Tbcexv4Registries.BattleActions.REGISTRY, Tbcexv4.id("join_test"), JOIN_TEST_TYPE);
        Registry.register(Tbcexv4Registries.BattleActions.REGISTRY, Tbcexv4.id("attack"), ATTACK_TYPE);
        Registry.register(Tbcexv4Registries.BattleActions.REGISTRY, Tbcexv4.id("heal"), HEAL_TYPE);
        Tbcexv4Registries.DefaultPlans.register((participant, consumer) -> {
            final Optional<TargetChooser<ParticipantTarget>> chooser = TargetChoosers.hurtParticipant(participant, handle -> {
                final BattleStateView state = participant.battleState();
                return state.relation(participant.team(), state.participant(handle).team()) == BattleParticipantTeamRelation.HOSTILE;
            }, 1);
            if (chooser.isPresent()) {
                consumer.accept(new SingleTargetPlan<>(chooser.get(), (FunctionType<ParticipantTarget, BattleActionRequest>) participantTarget -> new DebugBattleActionRequest(new AttackBattleAction(participant.handle(), participantTarget.participant()))));
            }

            final Optional<TargetChooser<ParticipantTarget>> helpChooser = TargetChoosers.helpParticipant(participant, handle -> {
                final BattleStateView state = participant.battleState();
                return state.relation(participant.team(), state.participant(handle).team()) == BattleParticipantTeamRelation.ALLY;
            }, 1);
            if (helpChooser.isPresent()) {
                consumer.accept(new SingleTargetPlan<>(helpChooser.get(), (FunctionType<ParticipantTarget, BattleActionRequest>) participantTarget -> new DebugBattleActionRequest(new HealBattleAction(participant.handle(), participantTarget.participant()))));
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("tbcexv4test").then(CommandManager.argument("player", EntityArgumentType.player()).executes(context -> {
                final ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                final ServerWorld world = (ServerWorld) player.getWorld();
                final ServerBattleWorld battleWorld = (ServerBattleWorld) world.getServer().getWorld(Tbcexv4.battleWorldKey(player.getWorld().getRegistryKey()));
                final Optional<Battle> opt = battleWorld.battleManager().createBattle(144, 144, 144, Tbcexv4Registries.TurnManagerTypes.IN_ORDER_TURN_MANAGER_TYPE, Unit.INSTANCE);
                if (opt.isEmpty()) {
                    return 1;
                }
                final Battle battle = opt.get();
                final ChunkSectionPos center = ChunkSectionPos.from(player.getBlockPos());
                battle.pushAction(new SetupEnvironmentBattleAction(BattleEnvironmentInitialState.of(world, center.add(-4, -4, -4), center.add(4, 4, 4))));
                battle.pushAction(new PlayerJoinTestBattleAction(player.getUuid()));
                battle.pushAction(new StartBattleAction());
                Tbcexv4Api.watch(player, battle);
                return 0;
            })));
            dispatcher.register(CommandManager.literal("tbcexv4Advance").requires(source -> source.hasPermissionLevel(2)).then(CommandManager.argument("player", EntityArgumentType.player()).executes(context -> {
                final ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                final ServerWorld world = (ServerWorld) player.getWorld();
                final Optional<BattleHandle> watching = Tbcexv4Api.watching(player);
                if (watching.isEmpty() || !(world instanceof final ServerBattleWorld battleWorld)) {
                    return 0;
                }
                final Optional<? extends Battle> optBattle = battleWorld.battleManager().getOrLoadBattle(watching.get());
                if (optBattle.isEmpty()) {
                    return 0;
                }
                final Battle battle = optBattle.get();
                if (!battle.turnManager().currentTurn().contains(new BattleParticipantHandle(player.getUuid()))) {
                    return 1;
                }
                battle.pushAction(new NoopBattleAction(Optional.of(player.getUuid())));
                return 0;
            })));
        });
    }
}
