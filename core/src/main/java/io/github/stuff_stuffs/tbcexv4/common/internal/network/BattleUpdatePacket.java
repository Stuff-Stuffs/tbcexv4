package io.github.stuff_stuffs.tbcexv4.common.internal.network;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtTagSizeTracker;
import net.minecraft.network.PacketByteBuf;

public record BattleUpdatePacket(BattleHandle handle, int index, NbtElement element) implements FabricPacket {
    public BattleUpdatePacket(final PacketByteBuf buf) {
        this(buf.decode(NbtOps.INSTANCE, BattleHandle.CODEC), buf.readInt(), buf.readNbt(NbtTagSizeTracker.ofUnlimitedBytes()));
    }

    @Override
    public void write(final PacketByteBuf buf) {
        buf.encode(NbtOps.INSTANCE, BattleHandle.CODEC, handle);
        buf.writeInt(index);
        buf.writeNbt(element);
    }

    @Override
    public PacketType<?> getType() {
        return Tbcexv4CommonNetwork.BATTLE_UPDATE_PACKET_TYPE;
    }
}
