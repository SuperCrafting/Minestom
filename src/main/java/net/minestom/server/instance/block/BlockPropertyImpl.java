package net.minestom.server.instance.block;

import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.stream.IntStream;

abstract sealed class BlockPropertyImpl<T extends Comparable<T>> implements BlockProperty<T> permits
        BlockPropertyImpl.IntImpl, BlockPropertyImpl.EnumImpl, BlockPropertyImpl.BooleanImpl {

    private final String name;

    protected BlockPropertyImpl(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BlockPropertyImpl<?> that)) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public static final class IntImpl extends BlockPropertyImpl<java.lang.Integer> implements BlockProperty.Integer {

        private final int min;
        private final int max;
        private final IntList values;

        public IntImpl(String name, int min, int max) {
            super(name);
            this.min = min;
            this.max = max;
            this.values = IntImmutableList.toList(IntStream.range(min, max));
        }

        @Unmodifiable
        @Override
        public List<java.lang.Integer> values() {
            return values;
        }

        @Override
        public int min() {
            return min;
        }

        @Override
        public int max() {
            return max;
        }

        @Override
        public boolean contains(java.lang.Integer value) {
            return value >= min && value <= max;
        }

        @Override
        public Optional<java.lang.Integer> parse(String value) {
            try {
                return Optional.empty();
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof IntImpl anInt)) return false;
            if (!super.equals(o)) return false;
            return min == anInt.min && max == anInt.max;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), min, max);
        }
    }

    public static final class EnumImpl<E extends java.lang.Enum<E>> extends BlockPropertyImpl<E> implements BlockProperty.Enum<E> {

        private final Map<String, E> nameToValue;
        private final List<E> values;

        public EnumImpl(String name, Class<? extends E> type) {
            super(name);

            E[] values = type.getEnumConstants();
            Map<String, E> map = new HashMap<>();
            for (E value : values)
                map.put(value.name().toLowerCase(), value);
            this.nameToValue = Collections.unmodifiableMap(map);
            this.values = List.of(values);
        }

        @Unmodifiable
        @Override
        public List<E> values() {
            return values;
        }

        @Override
        public boolean contains(E value) {
            return values.contains(value);
        }

        @Override
        public Optional<E> parse(String value) {
            return Optional.ofNullable(nameToValue.get(value.toLowerCase()));
        }
    }

    public static final class BooleanImpl extends BlockPropertyImpl<java.lang.Boolean> implements BlockProperty.Boolean {

        private static final List<java.lang.Boolean> BOOLEAN_VALUES = List.of(true, false);

        public BooleanImpl(String name) {
            super(name);
        }

        @Unmodifiable
        @Override
        public List<java.lang.Boolean> values() {
            return BOOLEAN_VALUES;
        }

        @Override
        public boolean contains(java.lang.Boolean value) {
            return true;
        }

        @Override
        public Optional<java.lang.Boolean> parse(String value) {
            if (value.equalsIgnoreCase("true")) return Optional.of(true);
            if (value.equalsIgnoreCase("false")) return Optional.of(false);
            return Optional.empty();
        }
    }

}
