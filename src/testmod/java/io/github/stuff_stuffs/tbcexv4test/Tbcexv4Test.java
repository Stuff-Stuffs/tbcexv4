package io.github.stuff_stuffs.tbcexv4test;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Api;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePos;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core.SetupEnvironmentBattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.core.StartBattleAction;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.BattleEnvironmentInitialState;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.ServerBattleWorld;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Tbcexv4Test implements ModInitializer {
    public static final BattleActionType<PlayerJoinTestBattleAction> JOIN_TEST_TYPE = new BattleActionType<>(context -> PlayerJoinTestBattleAction.CODEC);

    @Override
    public void onInitialize() {
        Registry.register(Tbcexv4Registries.BattleActionTypes.REGISTRY, Tbcexv4.id("join_test"), JOIN_TEST_TYPE);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("tbcexv4test").then(CommandManager.argument("player", EntityArgumentType.player()).executes(context -> {
                final ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                final ServerWorld world = (ServerWorld) player.getWorld();
                final ServerBattleWorld battleWorld = (ServerBattleWorld) world.getServer().getWorld(Tbcexv4.battleWorldKey(player.getWorld().getRegistryKey()));
                final int rad = 4;
                final int size = (rad * 2 + 1) * 16;
                final Optional<Battle> opt = battleWorld.battleManager().createBattle(size, size, size, Tbcexv4Registries.TurnManagerTypes.ENERGY_IN_ORDER_TURN_MANAGER_TYPE, Unit.INSTANCE);
                if (opt.isEmpty()) {
                    return 1;
                }
                final Battle battle = opt.get();
                final ChunkSectionPos center = ChunkSectionPos.from(player.getBlockPos());
                final List<PlayerJoinTestBattleAction.Entry> entries = new ArrayList<>();
                for (final SheepEntity friend : world.getEntitiesByType(EntityType.SHEEP, player.getBoundingBox().expand(rad * 12), i -> true)) {
                    final int x = (friend.getBlockX() - center.getMinX()) + rad * 16;
                    final int y = (friend.getBlockY() - center.getMinY()) + rad * 16;
                    final int z = (friend.getBlockZ() - center.getMinZ()) + rad * 16;
                    entries.add(new PlayerJoinTestBattleAction.Entry(new BattlePos(x, y, z), false));
                }
                for (final PigEntity enemy : world.getEntitiesByType(EntityType.PIG, player.getBoundingBox().expand(rad * 12), i -> true)) {
                    final int x = (enemy.getBlockX() - center.getMinX()) + rad * 16;
                    final int y = (enemy.getBlockY() - center.getMinY()) + rad * 16;
                    final int z = (enemy.getBlockZ() - center.getMinZ()) + rad * 16;
                    entries.add(new PlayerJoinTestBattleAction.Entry(new BattlePos(x, y, z), true));
                }
                battle.pushAction(new SetupEnvironmentBattleAction(BattleEnvironmentInitialState.of(world, center.add(-rad, -rad, -rad), center.add(rad, rad, rad))));
                final int x = (player.getBlockX() - center.getMinX()) + rad * 16;
                final int y = (player.getBlockY() - center.getMinY()) + rad * 16;
                final int z = (player.getBlockZ() - center.getMinZ()) + rad * 16;
                battle.pushAction(new PlayerJoinTestBattleAction(player.getUuid(), x, y, z, entries));
                battle.pushAction(new StartBattleAction());
                Tbcexv4Api.watch(player, battle);
                return 0;
            })));
        });
    }
}
