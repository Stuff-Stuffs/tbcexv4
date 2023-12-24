package io.github.stuff_stuffs.tbcexv4.common.api;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattlePhase;
import io.github.stuff_stuffs.tbcexv4.common.internal.ServerPlayerExtensions;
import io.github.stuff_stuffs.tbcexv4.common.internal.network.WatchRequestResponsePacket;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.ServerBattleWorld;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public final class Tbcexv4Api {
    public static Optional<BattleHandle> watching(final ServerPlayerEntity entity) {
        return Optional.ofNullable(((ServerPlayerExtensions) entity).tbcexv4$watching());
    }

    public static void watch(final ServerPlayerEntity entity, @Nullable final Battle battle) {
        if (battle == null) {
            ((ServerPlayerExtensions) entity).tbcev4$setWatching(null);
            ServerPlayNetworking.send(entity, WatchRequestResponsePacket.createEmpty());
        } else {
            ((ServerPlayerExtensions) entity).tbcev4$setWatching(battle.handle());
            ((ServerPlayerExtensions) entity).tbcexv4$setWatchIndex(0);
            ServerPlayNetworking.send(entity, WatchRequestResponsePacket.create(battle));
        }
    }

    public static Set<BattleHandle> controlling(final ServerPlayerEntity entity) {
        final Set<BattleHandle> activeHandles = new ObjectOpenHashSet<>();
        for (final ServerWorld world : entity.getServer().getWorlds()) {
            if (world instanceof ServerBattleWorld battleWorld) {
                final Set<BattleHandle> handles = battleWorld.battleManager().unresolvedBattles(entity.getUuid());
                for (final BattleHandle handle : handles) {
                    final Optional<? extends Battle> battle = battleWorld.battleManager().getOrLoadBattle(handle);
                    if (battle.isPresent() && battle.get().phase() != BattlePhase.FINISHED) {
                        activeHandles.add(handle);
                    }
                }
            }
        }
        return activeHandles;
    }

    private Tbcexv4Api() {
    }
}
