package io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.damage;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Set;

public record DamageType(Text name, Text description, Set<Identifier> parents, Set<Identifier> children) {
}
