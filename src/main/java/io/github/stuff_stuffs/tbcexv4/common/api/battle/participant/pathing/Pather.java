package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public interface Pather<M> {
    PathCache<M> compute(PatherOptions options, BattleParticipantView participant);

    interface PathCache<M> {
        @Nullable PathNode<M> get(int x, int y, int z);

        Stream<? extends PathNode<M>> terminal();

        Stream<? extends PathNode<M>> all();
    }

    interface PathNode<M> {
        @Nullable PathNode<M> previous();

        M movement();

        int x();

        int y();

        int z();
    }
}
