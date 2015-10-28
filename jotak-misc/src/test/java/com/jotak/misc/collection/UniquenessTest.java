package com.jotak.misc.collection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.assertj.core.groups.Tuple;
import org.junit.Test;

/**
 * @author jtakvorian
 */
public class UniquenessTest {

    @Test
    public void shouldHaveCustomUniqueness() {
        final List<Something> inputList = ImmutableList.of(
                new Something(1, 2),
                new Something(1, 2),
                new Something(1, 3),
                new Something(2, 2)
        );

        // Custom Uniqueness constraint: check field 1 only
        final List<Something> uniqueField1 = Uniqueness.from(inputList)
                .constraintOn(ImmutableList.of(Something::getField1))
                .asList();
        assertThat(uniqueField1).hasSize(2).extracting("field1").containsOnly(1, 2);

        // Custom Uniqueness constraint: check field 2 only
        final List<Something> uniqueField2 = Uniqueness.from(inputList)
                .constraintOn(ImmutableList.of(Something::getField2))
                .asList();
        assertThat(uniqueField2).hasSize(2).extracting("field2").containsOnly(2, 3);
    }

    @Test
    public void shouldHaveCustomUniquenessOnAllFields() {
        final List<Something> inputList = ImmutableList.of(
                new Something(1, 2),
                new Something(1, 2),
                new Something(1, 3),
                new Something(2, 2)
        );

        // Custom Uniqueness constraint: all fields
        final List<Something> allFields = Uniqueness.from(inputList)
                .constraintOn(ImmutableList.of(Something::getField1, Something::getField2))
                .asList();
        assertThat(allFields).hasSize(3).extracting("field1", "field2").containsOnly(
                Tuple.tuple(1, 2),
                Tuple.tuple(1, 3),
                Tuple.tuple(2, 2)
        );
    }

    @Test
    public void shouldNotHaveCustomUniqueness() {
        final List<Something> inputList = ImmutableList.of(
                new Something(1, 2),
                new Something(1, 2),
                new Something(1, 3),
                new Something(2, 2)
        );

        // Custom Uniqueness without any parameter reuse standard equals/hashCode
        final List<Something> allFields = Uniqueness.from(inputList).asList();
        assertThat(allFields).hasSize(3).extracting("field1", "field2").containsOnly(
                Tuple.tuple(1, 2),
                Tuple.tuple(1, 3),
                Tuple.tuple(2, 2)
        );
    }

    @Test
    public void shouldHaveCustomUniquenessOnNoField() {
        final List<Something> inputList = ImmutableList.of(
                new Something(1, 2),
                new Something(1, 2),
                new Something(1, 3),
                new Something(2, 2)
        );

        // Custom Uniqueness constraint: no field
        final List<Something> allFields = Uniqueness.from(inputList).constraintOn(ImmutableList.of()).asList();
        assertThat(allFields).hasSize(1);
    }

    @Test
    public void shouldHaveCustomUniquenessWithPartialEquals() {
        final List<SomethingWithPartialEquals> inputList = ImmutableList.of(
                new SomethingWithPartialEquals(1, 2),
                new SomethingWithPartialEquals(1, 2),
                new SomethingWithPartialEquals(1, 3),
                new SomethingWithPartialEquals(2, 2)
        );

        // Custom Uniqueness constraint: check field 1 only
        final List<SomethingWithPartialEquals> uniqueField1 = Uniqueness.from(inputList)
                .constraintOn(ImmutableList.of(SomethingWithPartialEquals::getField1))
                .asList();
        assertThat(uniqueField1).hasSize(2).extracting("field1").containsOnly(1, 2);

        // Custom Uniqueness constraint: check field 2 only
        final List<SomethingWithPartialEquals> uniqueField2 = Uniqueness.from(inputList)
                .constraintOn(ImmutableList.of(SomethingWithPartialEquals::getField2))
                .asList();
        assertThat(uniqueField2).hasSize(2).extracting("field2").containsOnly(2, 3);
    }

    @Test
    public void shouldHaveCustomUniquenessWithPartialEqualsChangedBySet() {
        final List<SomethingWithPartialEquals> inputList = ImmutableList.of(
                new SomethingWithPartialEquals(1, 2),
                new SomethingWithPartialEquals(1, 2),
                new SomethingWithPartialEquals(1, 3)
        );

        // Custom Uniqueness constraint: check field 2 only
        // Here, the Uniqueness API should provide items <1,2> and <1,3>
        // until it's transformed as a Set => since object SomethingWithPartialEquals "equals" only checks for field 1,
        // we only get 1 element at the end (see "asSet" javadoc)
        final Set<SomethingWithPartialEquals> uniqueField = Uniqueness.from(inputList)
                .constraintOn(ImmutableList.of(SomethingWithPartialEquals::getField2))
                .asSet();
        assertThat(uniqueField).hasSize(1);
    }

    @Test
    public void shouldHaveCustomUniquenessWithEqualsHashcode() {
        final List<Something> inputList = ImmutableList.of(
                new Something(1, 2),
                new Something(1, 2),
                new Something(1, 3),
                new Something(2, 2)
        );

        // Custom Uniqueness constraint: check field 1 only
        final List<Something> uniqueField1 = Uniqueness.from(inputList)
                .withEquals((that, other) -> that.getField1() == ((Something) other).getField1())
                .withHashcode(Something::getField1)
                .asList();
        assertThat(uniqueField1).hasSize(2).extracting("field1").containsOnly(1, 2);
    }

    @Test
    public void shouldGetWrappedObjectWithEqualsHashcode() {
        final List<Something> inputList = ImmutableList.of(
                new Something(1, 2),
                new Something(1, 2),
                new Something(1, 3),
                new Something(2, 2)
        );
        // Create a wrapped "Something" with equals/hashCode on field1 only
        final Set<Uniqueness.Wrap<Something>> result = inputList.stream()
                .map(something -> Uniqueness.wrapObject(
                        something,
                        (that, other) -> that.getField1() == ((Something) other).getField1(),
                        Something::getField1
                ))
                .collect(Collectors.toSet());

        assertThat(result).hasSize(2).extracting("wrapped").extracting("field1").containsOnly(1, 2);
    }

    @Test
    public void shouldGetWrappedObjectWithAllFields() {
        final List<Something> inputList = ImmutableList.of(
                new Something(1, 2),
                new Something(1, 2),
                new Something(1, 3),
                new Something(2, 2)
        );
        // Create a wrapped "Something" with equals/hashCode on field1 and field2
        final Set<Uniqueness.Wrap<Something>> result = inputList.stream()
                .map(something -> Uniqueness.wrapObject(
                        something,
                        Arrays.asList(Something::getField1, Something::getField2)
                ))
                .collect(Collectors.toSet());

        assertThat(result).hasSize(3).extracting("wrapped").extracting("field1", "field2").containsOnly(
                Tuple.tuple(1, 2),
                Tuple.tuple(1, 3),
                Tuple.tuple(2, 2));
    }

    @Test
    public void shouldGetWrappedObjectWithOneField() {
        final List<Something> inputList = ImmutableList.of(
                new Something(1, 2),
                new Something(1, 2),
                new Something(1, 3),
                new Something(2, 2)
        );
        // Create a wrapped "Something" with equals/hashCode on field2 only
        final Set<Uniqueness.Wrap<Something>> result = inputList.stream()
                .map(something -> Uniqueness.wrapObject(
                        something,
                        Collections.singletonList(Something::getField2)
                ))
                .collect(Collectors.toSet());

        assertThat(result).hasSize(2).extracting("wrapped").extracting("field2").containsOnly(2, 3);
    }

    private static final class Something {
        private final int field1;
        private final int field2;

        private Something(final int field1, final int field2) {
            this.field1 = field1;
            this.field2 = field2;
        }

        public int getField1() {
            return field1;
        }

        public int getField2() {
            return field2;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Something something = (Something) o;
            return Objects.equals(field1, something.field1) &&
                    Objects.equals(field2, something.field2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(field1, field2);
        }
    }

    private static final class SomethingWithPartialEquals {
        private final int field1;
        private final int field2;

        private SomethingWithPartialEquals(final int field1, final int field2) {
            this.field1 = field1;
            this.field2 = field2;
        }

        public int getField1() {
            return field1;
        }

        public int getField2() {
            return field2;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final SomethingWithPartialEquals something = (SomethingWithPartialEquals) o;
            return Objects.equals(field1, something.field1);
        }

        @Override
        public int hashCode() {
            return Objects.hash(field1);
        }
    }
}