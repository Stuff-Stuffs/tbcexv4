package io.github.stuff_stuffs.tbcexv4.common.internal.network;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public record WatchRequestResponsePacket(@Nullable BattleHandle handle, @Nullable Info info) implements FabricPacket {
    public WatchRequestResponsePacket {
        if ((handle == null) ^ (info == null)) {
            throw new IllegalArgumentException();
        }
    }

    public static WatchRequestResponsePacket decode(final PacketByteBuf buf) {
        final boolean valid = buf.readBoolean();
        if (!valid) {
            return new WatchRequestResponsePacket(null, null);
        } else {
            final BattleHandle handle = buf.decode(NbtOps.INSTANCE, BattleHandle.CODEC);
            final int xSize = buf.readInt();
            final int ySize = buf.readInt();
            final int zSize = buf.readInt();
            final BlockPos min = buf.readBlockPos();
            final RegistryKey<World> sourceWorld = buf.readRegistryKey(RegistryKeys.WORLD);
            return new WatchRequestResponsePacket(handle, new Info(xSize, ySize, zSize, min, sourceWorld));
        }
    }

    @Override
    public void write(final PacketByteBuf buf) {
        if (handle == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.encode(NbtOps.INSTANCE, BattleHandle.CODEC, handle);
            buf.writeInt(info.xSize);
            buf.writeInt(info.ySize);
            buf.writeInt(info.zSize);
            buf.writeBlockPos(info.min);
            buf.writeRegistryKey(info.sourceWorld);
        }
    }

    @Override
    public PacketType<?> getType() {
        return Tbcexv4CommonNetwork.WATCH_REQUEST_RESPONSE_PACKET_TYPE;
    }

    public record Info(int xSize, int ySize, int zSize, BlockPos min, RegistryKey<World> sourceWorld) {
    }
}
