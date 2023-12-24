package io.github.stuff_stuffs.tbcexv4.common.api.battle.state.attachment;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import net.minecraft.text.Text;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public final class BattleAttachmentType<T extends BattleAttachment> {
    private final BiFunction<BattleParticipantView, T, Text> name;
    private final BiPredicate<BattleParticipantView, T> visible;

    public BattleAttachmentType(final BiFunction<BattleParticipantView, T, Text> name, final BiPredicate<BattleParticipantView, T> visible) {
        this.name = name;
        this.visible = visible;
    }

    public Text name(final BattleParticipantView participant, final T value) {
        return name.apply(participant, value);
    }

    public boolean visible(final BattleParticipantView participant, final T value) {
        return visible.test(participant, value);
    }
}
