package io.github.stuff_stuffs.tbcexv4test;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Api;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core.SetupEnvironmentBattleAction;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.BattleEnvironmentInitialState;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.ServerBattleWorld;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.Optional;

public class Tbcexv4Test implements ModInitializer {
    public static final BattleActionType<PlayerJoinTestBattleAction> JOIN_TEST_TYPE = new BattleActionType<>(context -> PlayerJoinTestBattleAction.CODEC);

    @Override
    public void onInitialize() {
        Registry.register(Tbcexv4Registries.BattleActions.REGISTRY, Tbcexv4.id("join_test"), JOIN_TEST_TYPE);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("tbcexv4test").then(CommandManager.argument("player", EntityArgumentType.player()).executes(new Command<ServerCommandSource>() {
            @Override
            public int run(final CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
                final ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                final ServerWorld world = (ServerWorld) player.getWorld();
                final ServerBattleWorld battleWorld = (ServerBattleWorld) world.getServer().getWorld(Tbcexv4.battleWorldKey(player.getWorld().getRegistryKey()));
                final Optional<Battle> opt = battleWorld.battleManager().createBattle(144, 144, 144);
                if (opt.isEmpty()) {
                    return 1;
                }
                final Battle battle = opt.get();
                final ChunkSectionPos center = ChunkSectionPos.from(player.getBlockPos());
                battle.pushAction(new SetupEnvironmentBattleAction(BattleEnvironmentInitialState.of(world, center.add(-4, -4, -4), center.add(4, 4, 4))));
                battle.pushAction(new PlayerJoinTestBattleAction(player.getUuid()));
                Tbcexv4Api.watch(player, battle);
                return 0;
            }
        }))));
    }
}
