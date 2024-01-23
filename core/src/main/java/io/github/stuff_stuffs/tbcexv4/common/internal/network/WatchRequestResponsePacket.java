package io.github.stuff_stuffs.tbcexv4.common.internal.network;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.Battle;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleCodecContext;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.impl.battle.ServerBattleImpl;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record WatchRequestResponsePacket(@Nullable BattleHandle handle, @Nullable Info info) implements FabricPacket {
    public WatchRequestResponsePacket {
        if ((handle == null) ^ (info == null)) {
            throw new IllegalArgumentException();
        }
    }

    public static WatchRequestResponsePacket createEmpty() {
        return new WatchRequestResponsePacket(null, null);
    }

    public static WatchRequestResponsePacket create(final Battle battle) {
        final NbtCompound turnManagerContainerNbt = ((ServerBattleImpl) battle).turnManagerContainer.toNbt(BattleCodecContext.create(((ServerBattleImpl) battle).world().getRegistryManager()));
        return new WatchRequestResponsePacket(
                battle.handle(),
                new WatchRequestResponsePacket.Info(
                        battle.xSize(),
                        battle.ySize(),
                        battle.zSize(),
                        new BlockPos(
                                battle.worldX(0),
                                battle.worldY(0),
                                battle.worldZ(0)
                        ),
                        battle.state().sourceWorld(),
                        turnManagerContainerNbt
                )
        );
    }

    public Optional<ServerBattleImpl.TurnManagerContainer<?>> decodeTurnManager(final BattleCodecContext context) {
        if (info == null) {
            return Optional.empty();
        }
        return ServerBattleImpl.TurnManagerContainer.fromNbt(context, info.turnManagerContainer);
    }

    public static WatchRequestResponsePacket decode(final PacketByteBuf buf) {
        final boolean valid = buf.readBoolean();
        if (!valid) {
            return createEmpty();
        } else {
            final BattleHandle handle = buf.decode(NbtOps.INSTANCE, BattleHandle.CODEC);
            final int xSize = buf.readInt();
            final int ySize = buf.readInt();
            final int zSize = buf.readInt();
            final BlockPos min = buf.readBlockPos();
            final RegistryKey<World> sourceWorld = buf.readRegistryKey(RegistryKeys.WORLD);
            return new WatchRequestResponsePacket(handle, new Info(xSize, ySize, zSize, min, sourceWorld, buf.readNbt()));
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
            buf.writeNbt(info.turnManagerContainer);
        }
    }

    @Override
    public PacketType<?> getType() {
        return Tbcexv4CommonNetwork.WATCH_REQUEST_RESPONSE_PACKET_TYPE;
    }

    public record Info(int xSize, int ySize, int zSize, BlockPos min, RegistryKey<World> sourceWorld,
                       NbtCompound turnManagerContainer) {
    }
}
