package io.github.stuff_stuffs.tbcexv4.common.mixin;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.BattleHandle;
import io.github.stuff_stuffs.tbcexv4.common.internal.ServerPlayerExtensions;
import io.github.stuff_stuffs.tbcexv4.common.internal.Tbcexv4InternalEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity implements ServerPlayerExtensions {
    @Unique
    private @Nullable BattleHandle tbcev4$watching = null;

    @Override
    public @Nullable BattleHandle tbcexv4$watching() {
        return tbcev4$watching;
    }

    @Override
    public void tbcev4$setWatching(@Nullable final BattleHandle handle) {
        @Nullable final BattleHandle prev = tbcev4$watching;
        tbcev4$watching = handle;
        if (!Objects.equals(prev, handle)) {
            Tbcexv4InternalEvents.BATTLE_WATCH_EVENT.invoker().onWatch(prev, handle, (ServerPlayerEntity) (Object) this);
        }
    }
}
