package io.github.stuff_stuffs.tbcexv4.common.internal.network;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKeys;

import java.util.ArrayList;
import java.util.List;

public record ControllingBattleUpdatePacket(List<BattleHandle> handles) implements FabricPacket {
    @Override
    public void write(final PacketByteBuf buf) {
        buf.writeInt(handles.size());
        for (final BattleHandle handle : handles) {
            buf.writeRegistryKey(handle.sourceWorld());
            buf.writeUuid(handle.id());
        }
    }

    @Override
    public PacketType<?> getType() {
        return Tbcexv4CommonNetwork.CONTROLLING_BATTLE_UPDATE_PACKET_TYPE;
    }

    public static ControllingBattleUpdatePacket decode(final PacketByteBuf buf) {
        final int size = buf.readInt();
        final List<BattleHandle> handles = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            handles.add(new BattleHandle(buf.readRegistryKey(RegistryKeys.WORLD), buf.readUuid()));
        }
        return new ControllingBattleUpdatePacket(handles);
    }
}
