package io.github.stuff_stuffs.tbcexv4.common.internal.network;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.internal.ServerPlayerExtensions;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.ServerBattleWorld;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public final class Tbcexv4CommonNetwork {
    public static final PacketType<WatchRequestPacket> WATCH_REQUEST_PACKET_TYPE = PacketType.create(Tbcexv4.id("watch_request"), WatchRequestPacket::new);
    public static final PacketType<WatchRequestResponsePacket> WATCH_REQUEST_RESPONSE_PACKET_TYPE = PacketType.create(Tbcexv4.id("watch_request_response"), WatchRequestResponsePacket::decode);
    public static final PacketType<BattleUpdatePacket> BATTLE_UPDATE_PACKET_TYPE = PacketType.create(Tbcexv4.id("battle_update"), BattleUpdatePacket::new);

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(WATCH_REQUEST_PACKET_TYPE, (packet, player, responseSender) -> {
            final ServerWorld serverWorld = player.server.getWorld(Tbcexv4.BATTLE_WORLD_KEY);
            if (!(serverWorld instanceof final ServerBattleWorld world)) {
                throw new RuntimeException();
            }
            if (packet.handle() == null) {
                ((ServerPlayerExtensions) player).tbcev4$setWatching(null);
                responseSender.sendPacket(new WatchRequestResponsePacket(null, null));
            } else {
                final Optional<? extends Battle> opt = world.battleManager().getOrLoadBattle(packet.handle());
                if (opt.isEmpty()) {
                    ((ServerPlayerExtensions) player).tbcev4$setWatching(null);
                    responseSender.sendPacket(new WatchRequestResponsePacket(null, null));
                } else {
                    ((ServerPlayerExtensions) player).tbcev4$setWatching(packet.handle());
                    final Battle battle = opt.get();
                    responseSender.sendPacket(new WatchRequestResponsePacket(packet.handle(), new WatchRequestResponsePacket.Info(battle.xSize(), battle.ySize(), battle.zSize(), new BlockPos(battle.worldX(0), battle.worldY(0), battle.worldZ(0)), battle.state().sourceWorld())));
                }
            }
        });
    }

    private Tbcexv4CommonNetwork() {
    }
}
