package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.plan.target;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.pathing.Pather;

public record PathTarget(Pather.PathNode node, boolean terminal) implements Target {
}
