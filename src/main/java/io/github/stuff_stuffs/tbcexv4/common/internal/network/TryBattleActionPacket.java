package io.github.stuff_stuffs.tbcexv4.common.internal.network;

import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.request.BattleActionRequest;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.request.BattleActionRequestType;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtTagSizeTracker;
import net.minecraft.network.PacketByteBuf;

import java.util.Optional;
import java.util.UUID;

public final class TryBattleActionPacket implements FabricPacket {
    private final BattleActionRequestType<?> type;
    private final UUID requestId;
    private final NbtElement element;

    public <T extends BattleActionRequest> TryBattleActionPacket(final T value, final UUID id, final BattleCodecContext context) {
        type = value.type();
        requestId = id;
        //noinspection unchecked
        element = ((BattleActionRequestType<T>) type).codec(context).encodeStart(NbtOps.INSTANCE, value).getOrThrow(false, Tbcexv4.LOGGER::error);
    }

    private TryBattleActionPacket(final BattleActionRequestType<?> type, final UUID id, final NbtElement element) {
        this.type = type;
        requestId = id;
        this.element = element;
    }

    @Override
    public void write(final PacketByteBuf buf) {
        buf.writeRegistryValue(Tbcexv4Registries.BattleActionRequestTypes.REGISTRY, type);
        buf.writeUuid(requestId);
        buf.writeNbt(element);
    }

    @Override
    public PacketType<?> getType() {
        return Tbcexv4CommonNetwork.TRY_BATTLE_ACTION_PACKET_TYPE;
    }

    public Optional<? extends BattleActionRequest> decode(final BattleCodecContext context) {
        return type.codec(context).parse(NbtOps.INSTANCE, element).result();
    }

    public static TryBattleActionPacket netDecode(final PacketByteBuf buf) {
        final BattleActionRequestType<?> type = buf.readRegistryValue(Tbcexv4Registries.BattleActionRequestTypes.REGISTRY);
        if (type == null) {
            throw new RuntimeException();
        }
        return new TryBattleActionPacket(type, buf.readUuid(), buf.readNbt(NbtTagSizeTracker.ofUnlimitedBytes()));
    }
}
