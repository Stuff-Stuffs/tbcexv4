package io.github.stuff_stuffs.tbcexv4.common.impl.battle.participant.damage;

import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.damage.DamageType;
import io.github.stuff_stuffs.tbcexv4.common.api.battle.participant.damage.DamageTypeGraph;
import io.github.stuff_stuffs.tbcexv4.common.api.util.TopologicalSort;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DamageTypeGraphImpl implements DamageTypeGraph {
    private final Map<DamageType, NodeImpl> nodes;

    public DamageTypeGraphImpl(final Registry<DamageType> types) {
        nodes = new Object2ReferenceOpenHashMap<>();
        for (final DamageType type : types) {
            nodes.put(type, new NodeImpl(type));
        }
        for (final Map.Entry<DamageType, NodeImpl> entry : nodes.entrySet()) {
            for (final Identifier parent : entry.getKey().parents()) {
                final DamageType type = types.get(parent);
                if (type != null) {
                    entry.getValue().directParents.add(nodes.get(type));
                }
            }
            for (final Identifier child : entry.getKey().children()) {
                final DamageType type = types.get(child);
                if (type != null) {
                    entry.getValue().directChildren.add(nodes.get(type));
                }
            }
        }
        final List<DamageType> sorted = TopologicalSort.tieBreakingSort(types.stream().toList(), (parent, child, items) -> {
            final DamageType parentType = items.get(parent);
            final DamageType childType = items.get(child);
            final Identifier parentId = types.getId(parentType);
            final Identifier childId = types.getId(childType);
            return parentType.children().contains(childId) || childType.parents().contains(parentId);
        }, (first, second, items) -> {
            final DamageType firstType = items.get(first);
            final DamageType secondType = items.get(second);
            final Identifier firstId = types.getId(firstType);
            final Identifier secondId = types.getId(secondType);
            return firstId.compareTo(secondId);
        });
        for (final DamageType type : sorted) {
            nodes.get(type).computeParents();
        }
        for (int i = sorted.size() - 1; i >= 0; i--) {
            nodes.get(sorted.get(i)).computeChildren();
        }
    }

    @Override
    public Node get(final DamageType type) {
        return nodes.get(type);
    }

    private static final class NodeImpl implements Node {
        private final DamageType delegate;
        private final Set<Node> directParents;
        private final Set<Node> directChildren;
        private final Set<Node> parents;
        private final Set<Node> children;

        private NodeImpl(final DamageType delegate) {
            this.delegate = delegate;
            directParents = new ObjectOpenHashSet<>();
            directChildren = new ObjectOpenHashSet<>();
            parents = new ObjectOpenHashSet<>();
            children = new ObjectOpenHashSet<>();
        }

        private void computeChildren() {
            children.addAll(directChildren);
            for (final Node child : directChildren) {
                addChildren(child);
            }
        }

        private void computeParents() {
            parents.addAll(directParents);
            for (final Node child : directParents) {
                addParents(child);
            }
        }

        private void addChildren(final Node child) {
            if (children.add(child)) {
                for (final Node node : child.children()) {
                    addChildren(node);
                }
            }
        }

        private void addParents(final Node parent) {
            if (parents.add(parent)) {
                for (final Node node : parent.parents()) {
                    addChildren(node);
                }
            }
        }

        @Override
        public DamageType delegate() {
            return delegate;
        }

        @Override
        public boolean isChildOf(final Node node) {
            return parents.contains(node);
        }

        @Override
        public boolean isParentOf(final Node node) {
            return children.contains(node);
        }

        @Override
        public Set<Node> children() {
            return Collections.unmodifiableSet(directChildren);
        }

        @Override
        public Set<Node> parents() {
            return Collections.unmodifiableSet(directParents);
        }
    }
}
