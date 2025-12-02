/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.common.collections;

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.util.concurrent.atomic.*;

/**
 * A simple mutable long value.
 *
 * <p>
 * This class extends {@link Value}&lt;{@link Long}&gt; and adds a convenience method for incrementing
 * the value, which is useful for counting operations in lambdas and loops.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is <b>not thread-safe</b>. For concurrent access, use {@link AtomicLong} instead.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a counter</jc>
 * 	LongValue <jv>counter</jv> = LongValue.<jsm>create</jsm>();
 *
 * 	<jc>// Use in a lambda to count valid items</jc>
 * 	list.forEach(<jv>x</jv> -&gt; {
 * 		<jk>if</jk> (<jv>x</jv>.isValid()) {
 * 			<jv>counter</jv>.getAndIncrement();
 * 		}
 * 	});
 *
 * 	<jsm>log</jsm>(<js>"Valid items: "</js> + <jv>counter</jv>.get());
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonCollections">juneau-common-collections</a>
 * </ul>
 */
public class LongValue extends Value<Long> {

	/**
	 * Creates a new long value initialized to <c>0</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	LongValue <jv>counter</jv> = LongValue.<jsm>create</jsm>();
	 * 	<jsm>assertEquals</jsm>(0L, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @return A new long value.
	 */
	public static LongValue create() {
		return of(0L);
	}

	/**
	 * Creates a new long value with the specified initial value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	LongValue <jv>counter</jv> = LongValue.<jsm>of</jsm>(42L);
	 * 	<jsm>assertEquals</jsm>(42L, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @param value The initial value.
	 * @return A new long value.
	 */
	public static LongValue of(Long value) {
		return new LongValue(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value The initial value.
	 */
	public LongValue(Long value) {
		super(value);
	}

	/**
	 * Adds the specified value to the current value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	LongValue <jv>value</jv> = LongValue.<jsm>of</jsm>(10L);
	 * 	<jv>value</jv>.add(5L);
	 * 	<jsm>assertEquals</jsm>(15L, <jv>value</jv>.get());
	 * </p>
	 *
	 * @param x The value to add.
	 * @return This object.
	 */
	public LongValue add(Long x) {
		var v = get();
		set((v == null ? 0L : v) + (x == null ? 0L : x));
		return this;
	}

	/**
	 * Adds the specified value to the current value and returns the new value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	LongValue <jv>value</jv> = LongValue.<jsm>of</jsm>(10L);
	 * 	<jk>long</jk> <jv>result</jv> = <jv>value</jv>.addAndGet(5L);  <jc>// Returns 15L</jc>
	 * 	<jsm>assertEquals</jsm>(15L, <jv>value</jv>.get());
	 * </p>
	 *
	 * @param x The value to add.
	 * @return The new value after addition.
	 */
	public Long addAndGet(Long x) {
		var v = get();
		var result = (v == null ? 0L : v) + (x == null ? 0L : x);
		set(result);
		return result;
	}

	/**
	 * Decrements the value by 1.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	LongValue <jv>counter</jv> = LongValue.<jsm>of</jsm>(5L);
	 * 	<jv>counter</jv>.decrement();
	 * 	<jsm>assertEquals</jsm>(4L, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @return This object.
	 */
	public LongValue decrement() {
		var v = get();
		set((v == null ? 0L : v) - 1L);
		return this;
	}

	/**
	 * Decrements the value by 1 and returns the new value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	LongValue <jv>counter</jv> = LongValue.<jsm>of</jsm>(5L);
	 * 	<jk>long</jk> <jv>result</jv> = <jv>counter</jv>.decrementAndGet();  <jc>// Returns 4L</jc>
	 * 	<jsm>assertEquals</jsm>(4L, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @return The decremented value.
	 */
	public Long decrementAndGet() {
		var v = get();
		var result = (v == null ? 0L : v) - 1L;
		set(result);
		return result;
	}

	/**
	 * Returns the current value and then increments it.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	LongValue <jv>counter</jv> = LongValue.<jsm>of</jsm>(5L);
	 * 	<jk>long</jk> <jv>current</jv> = <jv>counter</jv>.getAndIncrement();  <jc>// Returns 5L</jc>
	 * 	<jk>long</jk> <jv>next</jv> = <jv>counter</jv>.get();                <jc>// Returns 6L</jc>
	 * </p>
	 *
	 * @return The value before it was incremented.
	 */
	public long getAndIncrement() {
		var v = get();
		set(v == null ? 1L : v + 1L);
		return v == null ? 0L : v;
	}

	/**
	 * Increments the value by 1.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	LongValue <jv>counter</jv> = LongValue.<jsm>of</jsm>(5L);
	 * 	<jv>counter</jv>.increment();
	 * 	<jsm>assertEquals</jsm>(6L, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @return This object.
	 */
	public LongValue increment() {
		var v = get();
		set((v == null ? 0L : v) + 1L);
		return this;
	}

	/**
	 * Increments the value by 1 and returns the new value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	LongValue <jv>counter</jv> = LongValue.<jsm>of</jsm>(5L);
	 * 	<jk>long</jk> <jv>result</jv> = <jv>counter</jv>.incrementAndGet();  <jc>// Returns 6L</jc>
	 * 	<jsm>assertEquals</jsm>(6L, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @return The incremented value.
	 */
	public Long incrementAndGet() {
		var v = get();
		var result = (v == null ? 0L : v) + 1L;
		set(result);
		return result;
	}

	/**
	 * Checks if the current value is equal to the specified value.
	 *
	 * <p>
	 * Uses {@link Utils#eq(Object, Object)} for deep equality comparison, which handles nulls safely.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	LongValue <jv>value</jv> = LongValue.<jsm>of</jsm>(42L);
	 * 	<jsm>assertTrue</jsm>(<jv>value</jv>.is(42L));
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.is(43L));
	 * </p>
	 *
	 * @param value The value to compare to.
	 * @return <jk>true</jk> if the current value is equal to the specified value.
	 */
	public boolean is(Long value) {
		return eq(get(), value);
	}

	/**
	 * Checks if the current value matches any of the specified values.
	 *
	 * <p>
	 * Uses {@link Utils#eq(Object, Object)} for deep equality comparison of each value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	LongValue <jv>value</jv> = LongValue.<jsm>of</jsm>(5L);
	 * 	<jsm>assertTrue</jsm>(<jv>value</jv>.isAny(3L, 5L, 7L));
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.isAny(1L, 2L));
	 * </p>
	 *
	 * @param values The values to compare to.
	 * @return <jk>true</jk> if the current value matches any of the specified values.
	 */
	public boolean isAny(Long...values) {
		assertArgNotNull("values", values);
		var current = get();
		for (var value : values)
			if (eq(current, value))
				return true;
		return false;
	}
}