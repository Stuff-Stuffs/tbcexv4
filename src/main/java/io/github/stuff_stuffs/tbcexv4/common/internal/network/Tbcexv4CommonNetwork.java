package io.github.stuff_stuffs.tbcexv4.common.internal.network;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Api;
import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipant;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.attachment.BattleParticipantPlayerControllerAttachmentView;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.turn.TurnManager;
import io.github.stuff_stuffs.tbcexv4.common.api.util.Result;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.ServerBattleImpl;
import io.github.stuff_stuffs.tbcexv4.common.internal.ServerPlayerExtensions;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import io.github.stuff_stuffs.tbcexv4.common.internal.world.ServerBattleWorld;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.Optional;

public final class Tbcexv4CommonNetwork {
    public static final PacketType<WatchRequestPacket> WATCH_REQUEST_PACKET_TYPE = PacketType.create(Tbcexv4.id("watch_request"), WatchRequestPacket::new);
    public static final PacketType<WatchRequestResponsePacket> WATCH_REQUEST_RESPONSE_PACKET_TYPE = PacketType.create(Tbcexv4.id("watch_request_response"), WatchRequestResponsePacket::decode);
    public static final PacketType<BattleUpdatePacket> BATTLE_UPDATE_PACKET_TYPE = PacketType.create(Tbcexv4.id("battle_update"), BattleUpdatePacket::new);
    public static final PacketType<ControllingBattleUpdatePacket> CONTROLLING_BATTLE_UPDATE_PACKET_TYPE = PacketType.create(Tbcexv4.id("controlling_battles"), ControllingBattleUpdatePacket::decode);
    public static final PacketType<TryBattleActionPacket> TRY_BATTLE_ACTION_PACKET_TYPE = PacketType.create(Tbcexv4.id("try_action"), TryBattleActionPacket::netDecode);
    public static final PacketType<TryBattleActionResponsePacket> TRY_BATTLE_ACTION_RESPONSE_PACKET_TYPE = PacketType.create(Tbcexv4.id("try_action_response"), TryBattleActionResponsePacket::netDecode);

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(WATCH_REQUEST_PACKET_TYPE, (packet, player, responseSender) -> {
            if (packet.handle() == null) {
                clearWatching(player, responseSender);
            } else {
                final RegistryKey<World> key = Tbcexv4.battleWorldKey(packet.handle().sourceWorld());
                final ServerWorld serverWorld = player.server.getWorld(key);
                if (!(serverWorld instanceof final ServerBattleWorld world)) {
                    clearWatching(player, responseSender);
                    return;
                }
                final Optional<? extends Battle> opt = world.battleManager().getOrLoadBattle(packet.handle());
                if (opt.isEmpty()) {
                    clearWatching(player, responseSender);
                } else {
                    Tbcexv4Api.watch(player, opt.get());
                }
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(TRY_BATTLE_ACTION_PACKET_TYPE, (packet, player, responseSender) -> {
            final BattleHandle handle = ((ServerPlayerExtensions) player).tbcexv4$watching();
            if (handle == null) {
                responseSender.sendPacket(new TryBattleActionResponsePacket(packet.requestId(), false, Text.of("You are not in a battle!")));
                return;
            }
            final ServerWorld world = player.server.getWorld(Tbcexv4.battleWorldKey(handle.sourceWorld()));
            if (!(world instanceof final ServerBattleWorld battleWorld)) {
                throw new RuntimeException();
            }
            final Optional<? extends Battle> opt = battleWorld.battleManager().getOrLoadBattle(handle);
            if (opt.isEmpty()) {
                responseSender.sendPacket(new TryBattleActionResponsePacket(packet.requestId(), false, Text.of("Unknown Battle!")));
                return;
            }
            final Battle battle = opt.get();
            final TurnManager manager = battle.turnManager();
            final BattleParticipantHandle participantHandle = new BattleParticipantHandle(packet.handle());
            final BattleParticipant participant = battle.state().participant(participantHandle);
            if (participant == null) {
                responseSender.sendPacket(new TryBattleActionResponsePacket(packet.requestId(), false, Text.of("Unknown participant!")));
                return;
            }
            final Optional<BattleParticipantPlayerControllerAttachmentView> view = participant.attachmentView(Tbcexv4Registries.BattleParticipantAttachmentTypes.PLAYER_CONTROLLED);
            if (view.isEmpty() || !player.getUuid().equals(view.get().controllerId())) {
                responseSender.sendPacket(new TryBattleActionResponsePacket(packet.requestId(), false, Text.of("Cannot control participant!")));
                return;
            }
            if (manager.currentTurn().contains(participantHandle)) {
                final Optional<? extends BattleAction> decoded = packet.decode(BattleCodecContext.create(battleWorld.getRegistryManager()));
                if (decoded.isPresent()) {
                    final Result<Unit, Text> result = tryApply(decoded.get(), battle, player);
                    if (result instanceof final Result.Failure<Unit, Text> failure) {
                        responseSender.sendPacket(new TryBattleActionResponsePacket(packet.requestId(), false, failure.error()));
                    } else {
                        responseSender.sendPacket(new TryBattleActionResponsePacket(packet.requestId(), true, null));
                    }
                } else {
                    responseSender.sendPacket(new TryBattleActionResponsePacket(packet.requestId(), false, Text.of("Error during action decoding!")));
                }
            } else {
                responseSender.sendPacket(new TryBattleActionResponsePacket(packet.requestId(), false, Text.of("Not your turn!")));
            }
        });
    }

    private static Result<Unit, Text> tryApply(final BattleAction action, final Battle battle, final ServerPlayerEntity source) {
        final Result<Unit, Text> check = ((ServerBattleImpl) battle).check(Optional.of(source.getUuid()), action);
        if (check instanceof Result.Success<Unit, Text>) {
            battle.pushAction(action);
        }
        return check;
    }

    public static void clearWatching(final ServerPlayerEntity entity, final PacketSender responseSender) {
        ((ServerPlayerExtensions) entity).tbcev4$setWatching(null);
        responseSender.sendPacket(WatchRequestResponsePacket.createEmpty());
    }

    private Tbcexv4CommonNetwork() {
    }
}
