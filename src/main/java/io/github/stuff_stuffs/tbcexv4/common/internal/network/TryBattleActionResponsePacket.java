package io.github.stuff_stuffs.tbcexv4.common.internal.network;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record TryBattleActionResponsePacket(
        UUID requestId,
        boolean success,
        @Nullable Text desc
) implements FabricPacket {
    @Override
    public void write(final PacketByteBuf buf) {
        buf.writeUuid(requestId);
        buf.writeBoolean(success);
        if (desc != null) {
            buf.writeText(desc);
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    public PacketType<?> getType() {
        return Tbcexv4CommonNetwork.TRY_BATTLE_ACTION_RESPONSE_PACKET_TYPE;
    }

    public static TryBattleActionResponsePacket netDecode(final PacketByteBuf buf) {
        final UUID id = buf.readUuid();
        final boolean success = buf.readBoolean();
        if (success) {
            return new TryBattleActionResponsePacket(id, true, buf.readText());
        } else {
            return new TryBattleActionResponsePacket(id, false, null);
        }
    }
}
