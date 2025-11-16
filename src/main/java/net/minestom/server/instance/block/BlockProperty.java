package net.minestom.server.instance.block;

import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Optional;

public sealed interface BlockProperty<T extends Comparable<T>> extends BlockProperties permits BlockPropertyImpl,
        BlockProperty.Boolean, BlockProperty.Enum, BlockProperty.Integer {

    String name();

    @Unmodifiable
    List<T> values();

    boolean contains(T value);

    Optional<T> parse(String value);

    sealed interface Integer extends BlockProperty<java.lang.Integer> permits BlockPropertyImpl.IntImpl {
        int min();
        int max();
    }

    sealed interface Boolean extends BlockProperty<java.lang.Boolean> permits BlockPropertyImpl.BooleanImpl {
    }

    sealed interface Enum<E extends java.lang.Enum<E>> extends BlockProperty<E> permits BlockPropertyImpl.EnumImpl {
    }

}
