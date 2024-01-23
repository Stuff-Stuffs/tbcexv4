package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing;

import com.mojang.datafixers.FunctionType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.BattleParticipantView;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.function.Consumer;

public interface NeighbourFinder {
    Event<Gather> GATHER_EVENT = EventFactory.createArrayBacked(Gather.class, (FunctionType<Gather[], Gather>) gatherers -> (Gather) (participant, consumer) -> {
        for (Gather gatherer : gatherers) {
            gatherer.gather(participant, consumer);
        }
    });

    Instance apply(PatherOptions options, BattleParticipantView participant);

    interface Instance {
        void find(Pather.PathNode previous, NeighbourData data, Consumer<Pather.PathNode> consumer);
    }

    interface NeighbourData {
        double cost(int x, int y, int z);
    }

    public interface Gather {
        void gather(BattleParticipantView participant, Consumer<NeighbourFinder> consumer);
    }
}
