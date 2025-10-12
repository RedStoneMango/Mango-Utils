/*
 * Copyright (c) 2025 RedStoneMango
 *
 * This file is licensed under the MIT License.
 * You may use, copy, modify, and distribute this file under the terms of the MIT License.
 * See the LICENSE file or https://opensource.org/licenses/MIT for full text.
 */

package io.github.redstonemango.mangoutils.tuple;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * A tuple of two elements.
 * <p>
 * This class represents an immutable 2-tuple (also known as a pair), holding three values of potentially different types.
 * It provides various utility methods for transforming, consuming, and testing its elements.
 *
 * @param <T> the type of the first element
 * @param <U> the type of the second element
 *
 * @author RedStoneMango
 */
public class Tuple2<T, U> implements Serializable {

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
     * Constructs a new {@code Tuple2} with the specified values.
     *
     * @param first  the first element
     * @param second the second element
     *
     * @see #of(Object, Object)
     */
    public Tuple2(T first, U second) {
        this.first = first;
        this.second = second;
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
     * Maps both elements of the tuple using the given functions.
     *
     * @param f1 the function to apply to the first element
     * @param f2 the function to apply to the second element
     * @param <R> the type of the mapped first element
     * @param <S> the type of the mapped second element
     * @return a new {@code Tuple2} with the mapped values
     */
    public <R, S> Tuple2<R, S> map(Function<? super T, ? extends R> f1, Function<? super U, ? extends S> f2) {
        return new Tuple2<>(f1.apply(first), f2.apply(second));
    }

    /**
     * Applies the given function to this tuple, producing a new tuple.
     *
     * @param f   the function to apply
     * @param <R> the type of the first element in the returned tuple
     * @param <S> the type of the second element in the returned tuple
     * @return the result of applying the function
     */
    public <R, S> Tuple2<R, S> flatMap(BiFunction<? super T, ? super U, Tuple2<R, S>> f) {
        return f.apply(first, second);
    }

    /**
     * Maps the first element using the given function.
     *
     * @param f   the function to apply to the first element
     * @param <R> the type of the mapped first element
     * @return a new {@code Tuple2} with the transformed first element
     */
    public <R> Tuple2<R, U> mapFirst(Function<? super T, ? extends R> f) {
        return new Tuple2<>(f.apply(first), second);
    }

    /**
     * Maps the second element using the given function.
     *
     * @param f   the function to apply to the second element
     * @param <R> the type of the mapped second element
     * @return a new {@code Tuple2} with the transformed second element
     */
    public <R> Tuple2<T, R> mapSecond(Function<? super U, ? extends R> f) {
        return new Tuple2<>(first, f.apply(second));
    }

    /**
     * Transforms the entire tuple using the given function.
     *
     * @param f   the function to apply to the tuple
     * @param <R> the result type
     * @return the result of the transformation
     */
    public <R> R transform(Function<? super Tuple2<T, U>, ? extends R> f) {
        return f.apply(this);
    }

    /**
     * Applies a {@link BiConsumer} to the tuple's elements.
     *
     * @param consumer the consumer to apply
     */
    public void applyTo(BiConsumer<? super T, ? super U> consumer) {
        consumer.accept(first, second);
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
     * Tests the elements of the tuple using the given predicate.
     *
     * @param predicate the predicate to test the tuple's elements
     * @return {@code true} if the predicate matches, otherwise {@code false}
     */
    public boolean test(BiPredicate<? super T, ? super U> predicate) {
        return predicate.test(first, second);
    }

    /**
     * Tests the first element using the given predicate.
     *
     * @param predicate the predicate to apply to the first element
     * @return {@code true} if the predicate matches, otherwise {@code false}
     */
    public boolean testFirst(Predicate<? super T> predicate) {
        return predicate.test(first);
    }

    /**
     * Tests the second element using the given predicate.
     *
     * @param predicate the predicate to apply to the second element
     * @return {@code true} if the predicate matches, otherwise {@code false}
     */
    public boolean testSecond(Predicate<? super U> predicate) {
        return predicate.test(second);
    }

    /**
     * Returns a new tuple with the first and second elements swapped.
     *
     * @return a new {@code Tuple2} with swapped elements
     */
    public Tuple2<U, T> swap() {
        return new Tuple2<>(second, first);
    }

    /**
     * Converts the tuple to a {@link List} of two elements.
     *
     * @return a list containing the elements of the tuple
     */
    public List<Object> toList() {
        return List.of(first, second);
    }

    /**
     * Converts the tuple to a {@link Stream} of two elements.
     *
     * @return a stream containing the elements of the tuple
     */
    public Stream<Object> toStream() {
        return Stream.of(first, second);
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Two {@code Tuple2} instances are equal if their corresponding elements are equal.
     *
     * @param obj the reference object with which to compare
     * @return {@code true} if this object is the same as the obj argument; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof Tuple2<?, ?> tuple)) return false;
        return Objects.equals(first, tuple.first) && Objects.equals(second, tuple.second);
    }

    /**
     * Returns a hash code value for the tuple.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    /**
     * Returns a string representation of the tuple.
     *
     * @return a string in the form {@code (first, second)}
     */
    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    /**
     * Creates a new {@code Tuple2} with the given elements.
     *
     * @param first  the first element
     * @param second the second element
     * @param <T>    the type of the first element
     * @param <U>    the type of the second element
     * @return a new {@code Tuple2} containing the provided elements
     *
     * @see #Tuple2(Object, Object)
     */
    public static <T, U> Tuple2<T, U> of(T first, U second) {
        return new Tuple2<>(first, second);
    }
}