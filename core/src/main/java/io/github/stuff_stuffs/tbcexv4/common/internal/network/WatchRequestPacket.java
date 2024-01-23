package io.github.stuff_stuffs.tbcexv4.common.internal.network;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import org.jetbrains.annotations.Nullable;

public record WatchRequestPacket(@Nullable BattleHandle handle) implements FabricPacket {
    public WatchRequestPacket(final PacketByteBuf buf) {
        this(buf.readBoolean() ? buf.decode(NbtOps.INSTANCE, BattleHandle.CODEC) : null);
    }

    @Override
    public void write(final PacketByteBuf buf) {
        if (handle == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.encode(NbtOps.INSTANCE, BattleHandle.CODEC, handle);
        }
    }

    @Override
    public PacketType<?> getType() {
        return Tbcexv4CommonNetwork.WATCH_REQUEST_PACKET_TYPE;
    }
}
