package com.jotak.misc.collection;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Uniqueness can be used to easily transform a collection into a copy without duplicates, with custom hashcode / equality functions<br/>
 * Basically, it avoids having to create a wrapping class for your object when you want to override hashCode and equals.
 *
 * @author jtakvorian
 */
public final class Uniqueness<T> {

    private final Collection<T> from;
    private Equalsor<T> equalsor;
    private Function<T, Integer> hashCode;

    private Uniqueness(final Collection<T> from) {
        this.from = checkNotNull(from);
        // Default uniqueness: use parameter T's equals / hashCode
        equalsor = Object::equals;
        hashCode = Object::hashCode;
    }

    /**
     * Initiate with an input collection. You should then call either {@link Uniqueness#withEquals(Equalsor)}+{@link Uniqueness#withHashcode(Function)} or {@link Uniqueness#constraintOn(Collection)}.
     *
     * @param from the collection to work from
     * @param <T>  parameter of the collection
     * @return kind of builder
     */
    public static <T> Uniqueness<T> from(final Collection<T> from) {
        return new Uniqueness<>(from);
    }

    /**
     * Provide a custom equals lambda. Exclusive with {@link Uniqueness#constraintOn(Collection)}.
     *
     * @param equals custom equals
     * @return the builder
     */
    public Uniqueness<T> withEquals(final Equalsor<T> equals) {
        this.equalsor = equals;
        return this;
    }

    /**
     * Provide a custom hashCode lambda. Exclusive with {@link Uniqueness#constraintOn(Collection)}.
     *
     * @param hashCode custom hashCode
     * @return the builder
     */
    public Uniqueness<T> withHashcode(final Function<T, Integer> hashCode) {
        this.hashCode = hashCode;
        return this;
    }

    /**
     * Provide a list of fields mapping. Default "equals" and "hashCode" functions will be generated from these fields.<br/>
     * So this call is exclusive with {@link Uniqueness#withEquals(Equalsor)} and {@link Uniqueness#withHashcode(Function)}.<br/>
     *
     * @param fieldsMap list of fields mapping to use as uniqueness constraint
     * @return the builder
     */
    public Uniqueness<T> constraintOn(final Collection<Function<T, ?>> fieldsMap) {
        equalsor = getEqualsorFromFields(fieldsMap);
        hashCode = getHashCodeFromFields(fieldsMap);
        return this;
    }

    private static <T> Equalsor<T> getEqualsorFromFields(final Collection<Function<T, ?>> fieldsMap) {
        return (that, other) -> {
            if (that == other) {
                return true;
            }
            if (other == null || that.getClass() != other.getClass()) {
                return false;
            }
            final T otherT = (T) other;
            for (final Function<T, ?> f : fieldsMap) {
                if (!Objects.equals(f.apply(that), f.apply(otherT))) {
                    return false;
                }
            }
            return true;
        };
    }

    private static <T> Function<T, Integer> getHashCodeFromFields(final Collection<Function<T, ?>> fieldsMap) {
        return t -> {
            final Object[] objs = new Object[fieldsMap.size()];
            int i = 0;
            for (final Function<T, ?> f : fieldsMap) {
                objs[i++] = f.apply(t);
            }
            return Objects.hash(objs);
        };
    }

    /**
     * Get the result as a stream. All duplicates are removed from the input collection, according to the provided uniqueness constraints
     *
     * @return result as stream (non-terminated)
     */
    public Stream<T> asStream() {
        checkNotNull(equalsor);
        checkNotNull(hashCode);
        return from.stream()
                .map(PrivateWrap::new)
                .collect(Collectors.toSet())
                .stream()
                .map(Wrap::getWrapped);
    }

    /**
     * Get the result as a list. All duplicates are removed from the input collection, according to the provided uniqueness constraints
     *
     * @return result as list
     */
    public List<T> asList() {
        return asStream().collect(Collectors.toList());
    }

    /**
     * Get the result as a set. All duplicates are removed from the input collection, according to the provided uniqueness constraints.<br/>
     * WARNING: beware that by transforming the result into a Set, the actual "equals" and "hashCode" of the parametrized type will be implicitely invoked.<br/>
     * So the Set <i>can</i> contain less elements than you may expect.
     *
     * @return result as set
     */
    public Set<T> asSet() {
        return asStream().collect(Collectors.toSet());
    }

    public static <T> Wrap<T> wrapObject(final T obj, final Equalsor<T> equals, final Function<T, Integer> hashCode) {
        return new Wrap<>(obj, equals, hashCode);
    }

    public static <T> Wrap<T> wrapObject(final T obj, final Collection<Function<T, ?>> fieldsMap) {
        return new Wrap<>(
                obj,
                getEqualsorFromFields(fieldsMap),
                getHashCodeFromFields(fieldsMap));
    }

    /**
     * Custom equality checker
     *
     * @param <T> type of the input colection content
     */
    public interface Equalsor<T> {
        boolean check(T that, Object other);
    }

    private final class PrivateWrap extends Wrap<T> {
        private PrivateWrap(final T wrapped) {
            super(wrapped, equalsor, hashCode);
        }
    }

    public static class Wrap<T> {
        private final T wrapped;
        private final Equalsor<T> equalsor;
        private final Function<T, Integer> hashCode;

        Wrap(final T wrapped, final Equalsor<T> equalsor, final Function<T, Integer> hashCode) {
            this.wrapped = wrapped;
            this.equalsor = equalsor;
            this.hashCode = hashCode;
        }

        public T getWrapped() {
            return wrapped;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            return equalsor.check(wrapped, ((Wrap) o).getWrapped());
        }

        @Override
        public int hashCode() {
            return hashCode.apply(wrapped);
        }
    }
}
