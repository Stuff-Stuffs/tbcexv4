package io.github.stuff_stuffs.tbcexv4.common.internal.network;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantHandle;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;

import java.util.Map;
import java.util.Set;

public record ControllingBattleUpdatePacket(
        Map<BattleHandle, Set<BattleParticipantHandle>> handles
) implements FabricPacket {
    @Override
    public void write(final PacketByteBuf buf) {
        buf.writeInt(handles.size());
        for (final Map.Entry<BattleHandle, Set<BattleParticipantHandle>> entry : handles.entrySet()) {
            buf.encode(NbtOps.INSTANCE, BattleHandle.CODEC, entry.getKey());
            final Set<BattleParticipantHandle> set = entry.getValue();
            buf.writeInt(set.size());
            for (final BattleParticipantHandle handle : set) {
                buf.encode(NbtOps.INSTANCE, BattleParticipantHandle.CODEC, handle);
            }
        }
    }

    @Override
    public PacketType<?> getType() {
        return Tbcexv4CommonNetwork.CONTROLLING_BATTLE_UPDATE_PACKET_TYPE;
    }

    public static ControllingBattleUpdatePacket decode(final PacketByteBuf buf) {
        final int size = buf.readInt();
        final Map<BattleHandle, Set<BattleParticipantHandle>> handles = new Object2ReferenceOpenHashMap<>();
        for (int i = 0; i < size; i++) {
            final BattleHandle handle = buf.decode(NbtOps.INSTANCE, BattleHandle.CODEC);
            final int controllingSize = buf.readInt();
            final Set<BattleParticipantHandle> set = new ObjectOpenHashSet<>(controllingSize);
            for (int j = 0; j < controllingSize; j++) {
                set.add(buf.decode(NbtOps.INSTANCE, BattleParticipantHandle.CODEC));
            }
            handles.put(handle, set);
        }
        return new ControllingBattleUpdatePacket(handles);
    }
}
