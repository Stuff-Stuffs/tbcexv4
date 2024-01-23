package io.github.stuff_stuffs.tbcexv4.common.internal.network;

import io.github.stuff_stuffs.tbcexv4.common.api.Tbcexv4Registries;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleAction;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.action.BattleActionType;
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
    private final BattleActionType<?> type;
    private final NbtElement action;
    private final UUID requestId;
    private final UUID handle;

    public <T extends BattleAction> TryBattleActionPacket(final T value, final UUID id, final UUID handle, final BattleCodecContext context) {

        //noinspection unchecked
        final BattleActionType<T> type = (BattleActionType<T>) value.type();
        this.type = type;
        action = type.codec(context).encodeStart(NbtOps.INSTANCE, value).getOrThrow(false, Tbcexv4.LOGGER::error);
        requestId = id;
        this.handle = handle;
    }

    private TryBattleActionPacket(final BattleActionType<?> type, final NbtElement element, final UUID id, final UUID handle) {
        this.type = type;
        action = element;
        requestId = id;
        this.handle = handle;
    }

    public UUID requestId() {
        return requestId;
    }

    public UUID handle() {
        return handle;
    }

    @Override
    public void write(final PacketByteBuf buf) {
        buf.writeRegistryValue(Tbcexv4Registries.BattleActionTypes.REGISTRY, type);
        buf.writeNbt(action);
        buf.writeUuid(requestId);
        buf.writeUuid(handle);
    }

    @Override
    public PacketType<?> getType() {
        return Tbcexv4CommonNetwork.TRY_BATTLE_ACTION_PACKET_TYPE;
    }

    public Optional<? extends BattleAction> decode(final BattleCodecContext context) {
        return type.codec(context).parse(NbtOps.INSTANCE, action).result();
    }

    public static TryBattleActionPacket netDecode(final PacketByteBuf buf) {
        final BattleActionType<?> type = buf.readRegistryValue(Tbcexv4Registries.BattleActionTypes.REGISTRY);
        if (type == null) {
            throw new RuntimeException();
        }
        return new TryBattleActionPacket(type, buf.readNbt(NbtTagSizeTracker.ofUnlimitedBytes()), buf.readUuid(), buf.readUuid());
    }
}
