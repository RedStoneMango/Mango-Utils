/*
 * Copyright (c) 2025 RedStoneMango
 *
 * This file is licensed under the MIT License.
 * You may use, copy, modify, and distribute this file under the terms of the MIT License.
 * See the LICENSE file or https://opensource.org/licenses/MIT for full text.
 */

package io.github.redstonemango.mangoutils.tuple;

import io.github.redstonemango.mangoutils.function.TriConsumer;
import io.github.redstonemango.mangoutils.function.TriFunction;
import io.github.redstonemango.mangoutils.function.TriPredicate;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * A tuple of three elements.
 * <p>
 * This class represents an immutable 3-tuple (also known as a triple), holding three values of potentially different types.
 * It provides utility methods for mapping, testing, transforming, and consuming the elements.
 *
 * @param <T> the type of the first element
 * @param <U> the type of the second element
 * @param <V> the type of the third element
 *
 * @author RedStoneMango
 */
public class Tuple3<T, U, V> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The first element of the tuple.
     */
    public final T first;

    /**
     * The second element of the tuple.
     */
    public final U second;

    /**
     * The third element of the tuple.
     */
    public final V third;

    /**
     * Constructs a new {@code Tuple3} with the specified values.
     *
     * @param first  the first element
     * @param second the second element
     * @param third  the third element
     *
     * @see #of(Object, Object, Object)
     */
    public Tuple3(T first, U second, V third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    /**
     * Returns the first element of the tuple.
     *
     * @return the first element
     */
    public T getFirst() {
        return first;
    }

    /**
     * Returns the second element of the tuple.
     *
     * @return the second element
     */
    public U getSecond() {
        return second;
    }

    /**
     * Returns the third element of the tuple.
     *
     * @return the third element
     */
    public V getThird() {
        return third;
    }

    /**
     * Maps each element of the tuple using the provided functions.
     *
     * @param f1  function to map the first element
     * @param f2  function to map the second element
     * @param f3  function to map the third element
     * @param <Q> the type of the mapped first element
     * @param <R> the type of the mapped second element
     * @param <S> the type of the mapped third element
     * @return a new tuple with the mapped values
     */
    public <Q, R, S> Tuple3<Q, R, S> map(Function<? super T, ? extends Q> f1,
                                         Function<? super U, ? extends R> f2,
                                         Function<? super V, ? extends S> f3) {
        return new Tuple3<>(f1.apply(first), f2.apply(second), f3.apply(third));
    }

    /**
     * Applies a function to the tuple's values to produce a new tuple.
     *
     * @param f   the function to apply
     * @param <Q> the type of the new first element
     * @param <R> the type of the new second element
     * @param <S> the type of the new third element
     * @return a new tuple resulting from the function
     */
    public <Q, R, S> Tuple3<Q, R, S> flatMap(TriFunction<? super T, ? super U, ? super V, Tuple3<Q, R, S>> f) {
        return f.apply(first, second, third);
    }

    /**
     * Maps the first element of the tuple using the given function.
     *
     * @param f   function to apply to the first element
     * @param <R> the new type of the first element
     * @return a new tuple with the mapped first element
     */
    public <R> Tuple3<R, U, V> mapFirst(Function<? super T, ? extends R> f) {
        return new Tuple3<>(f.apply(first), second, third);
    }

    /**
     * Maps the second element of the tuple using the given function.
     *
     * @param f   function to apply to the second element
     * @param <R> the new type of the second element
     * @return a new tuple with the mapped second element
     */
    public <R> Tuple3<T, R, V> mapSecond(Function<? super U, ? extends R> f) {
        return new Tuple3<>(first, f.apply(second), third);
    }

    /**
     * Maps the third element of the tuple using the given function.
     *
     * @param f   function to apply to the third element
     * @param <R> the new type of the third element
     * @return a new tuple with the mapped third element
     */
    public <R> Tuple3<T, U, R> mapThird(Function<? super V, ? extends R> f) {
        return new Tuple3<>(first, second, f.apply(third));
    }

    /**
     * Transforms this tuple into a different type using the given function.
     *
     * @param f   function to apply to the entire tuple
     * @param <R> the result type
     * @return the result of applying the function
     */
    public <R> R transform(Function<? super Tuple3<T, U, V>, ? extends R> f) {
        return f.apply(this);
    }

    /**
     * Applies the given {@link TriConsumer} to the elements of the tuple.
     *
     * @param consumer the consumer to apply
     */
    public void applyTo(TriConsumer<? super T, ? super U, ? super V> consumer) {
        consumer.accept(first, second, third);
    }

    /**
     * Applies a {@link Consumer} to the first element.
     *
     * @param consumer the consumer to apply
     */
    public void applyToFirst(Consumer<? super T> consumer) {
        consumer.accept(first);
    }

    /**
     * Applies a {@link Consumer} to the second element.
     *
     * @param consumer the consumer to apply
     */
    public void applyToSecond(Consumer<? super U> consumer) {
        consumer.accept(second);
    }

    /**
     * Applies a {@link Consumer} to the third element.
     *
     * @param consumer the consumer to apply
     */
    public void applyToThird(Consumer<? super V> consumer) {
        consumer.accept(third);
    }

    /**
     * Applies a {@link BiConsumer} to the first and second elements.
     *
     * @param consumer the consumer to apply
     */
    public void applyToFirstPart(BiConsumer<? super T, ? super U> consumer) {
        consumer.accept(first, second);
    }

    /**
     * Applies a {@link BiConsumer} to the second and third elements.
     *
     * @param consumer the consumer to apply
     */
    public void applyToSecondPart(BiConsumer<? super U, ? super V> consumer) {
        consumer.accept(second, third);
    }

    /**
     * Applies a {@link BiConsumer} to the first and third elements.
     *
     * @param consumer the consumer to apply
     */
    public void applyToOuterElements(BiConsumer<? super T, ? super V> consumer) {
        consumer.accept(first, third);
    }

    /**
     * Tests the elements using a {@link TriPredicate}.
     *
     * @param predicate the predicate to test all elements
     * @return {@code true} if the predicate matches; {@code false} otherwise
     */
    public boolean test(TriPredicate<? super T, ? super U, ? super V> predicate) {
        return predicate.test(first, second, third);
    }

    /**
     * Tests the first element using a {@link Predicate}.
     *
     * @param predicate the predicate to test the first element
     * @return {@code true} if the predicate matches; {@code false} otherwise
     */
    public boolean testFirst(Predicate<? super T> predicate) {
        return predicate.test(first);
    }

    /**
     * Tests the second element using a {@link Predicate}.
     *
     * @param predicate the predicate to test the second element
     * @return {@code true} if the predicate matches; {@code false} otherwise
     */
    public boolean testSecond(Predicate<? super U> predicate) {
        return predicate.test(second);
    }

    /**
     * Tests the third element using a {@link Predicate}.
     *
     * @param predicate the predicate to test the third element
     * @return {@code true} if the predicate matches; {@code false} otherwise
     */
    public boolean testThird(Predicate<? super V> predicate) {
        return predicate.test(third);
    }

    /**
     * Tests the first and second elements using a {@link BiPredicate}.
     *
     * @param predicate the predicate to apply to the first and second elements
     * @return {@code true} if the predicate matches; {@code false} otherwise
     */
    public boolean testFirstPart(BiPredicate<? super T, ? super U> predicate) {
        return predicate.test(first, second);
    }

    /**
     * Tests the second and third elements using a {@link BiPredicate}.
     *
     * @param predicate the predicate to apply to the second and third elements
     * @return {@code true} if the predicate matches; {@code false} otherwise
     */
    public boolean testSecondPart(BiPredicate<? super U, ? super V> predicate) {
        return predicate.test(second, third);
    }

    /**
     * Tests the first and third elements using a {@link BiPredicate}.
     *
     * @param predicate the predicate to apply to the first and third elements
     * @return {@code true} if the predicate matches; {@code false} otherwise
     */
    public boolean testOuterElements(BiPredicate<? super T, ? super V> predicate) {
        return predicate.test(first, third);
    }

    /**
     * Returns a new tuple with the elements reversed.
     *
     * @return a new {@code Tuple3} with the third, second, and first elements
     */
    public Tuple3<V, U, T> reverseOrder() {
        return new Tuple3<>(third, second, first);
    }

    /**
     * Swaps the first and second elements of the tuple.
     *
     * @return a new {@code Tuple3} with the first and second elements swapped
     */
    public Tuple3<U, T, V> swapFirstPart() {
        return new Tuple3<>(second, first, third);
    }

    /**
     * Swaps the second and third elements of the tuple.
     *
     * @return a new {@code Tuple3} with the second and third elements swapped
     */
    public Tuple3<T, V, U> swapSecondPart() {
        return new Tuple3<>(first, third, second);
    }

    /**
     * Returns a new tuple with the elements reversed.
     * <p>
     * This method delegates to {@link #reverseOrder()}
     * </p>
     *
     * @return a new {@code Tuple3} with the third, second, and first elements
     */
    public Tuple3<V, U, T> swapOuterElements() {
        return reverseOrder();
    }

    /**
     * Converts the tuple to a {@link List} of its elements.
     *
     * @return a list containing the three elements
     */
    public List<Object> toList() {
        return List.of(first, second, third);
    }

    /**
     * Converts the tuple to a {@link Stream} of its elements.
     *
     * @return a stream containing the three elements
     */
    public Stream<Object> toStream() {
        return Stream.of(first, second, third);
    }

    /**
     * Indicates whether this tuple is equal to another object.
     * Two tuples are equal if all corresponding elements are equal.
     *
     * @param obj the object to compare to
     * @return {@code true} if equal; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof Tuple3<?, ?, ?> tuple)) return false;
        return Objects.equals(first, tuple.first) &&
                Objects.equals(second, tuple.second) &&
                Objects.equals(third, tuple.third);
    }

    /**
     * Returns a hash code based on all three elements.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(first, second, third);
    }

    /**
     * Returns a string representation of the tuple.
     *
     * @return a string in the form {@code (first, second, third)}
     */
    @Override
    public String toString() {
        return "(" + first + ", " + second + ", " + third + ")";
    }

    /**
     * Creates a new {@code Tuple3} with the specified elements.
     *
     * @param first  the first element
     * @param second the second element
     * @param third  the third element
     * @param <T>    the type of the first element
     * @param <U>    the type of the second element
     * @param <V>    the type of the third element
     * @return a new {@code Tuple3} containing the given elements
     */
    public static <T, U, V> Tuple3<T, U, V> of(T first, U second, V third) {
        return new Tuple3<>(first, second, third);
    }
}