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

/**
 * A simple mutable integer value.
 *
 * <p>
 * This class extends {@link Value}&lt;{@link Integer}&gt; and adds a convenience method for incrementing
 * the value, which is useful for counting operations in lambdas and loops.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is <b>not thread-safe</b>. For concurrent access, use {@link java.util.concurrent.atomic.AtomicInteger} instead.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a counter</jc>
 * 	IntegerValue <jv>counter</jv> = IntegerValue.<jsm>create</jsm>();
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
public class IntegerValue extends Value<Integer> {

	/**
	 * Creates a new integer value initialized to <c>0</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	IntegerValue <jv>counter</jv> = IntegerValue.<jsm>create</jsm>();
	 * 	<jsm>assertEquals</jsm>(0, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @return A new integer value.
	 */
	public static IntegerValue create() {
		return of(0);
	}

	/**
	 * Creates a new integer value with the specified initial value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	IntegerValue <jv>counter</jv> = IntegerValue.<jsm>of</jsm>(42);
	 * 	<jsm>assertEquals</jsm>(42, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @param value The initial value.
	 * @return A new integer value.
	 */
	public static IntegerValue of(Integer value) {
		return new IntegerValue(value);
	}

	/**
	 * Constructor.
	 */
	public IntegerValue() {
		super(0);
	}

	/**
	 * Constructor.
	 *
	 * @param value The initial value.
	 */
	public IntegerValue(Integer value) {
		super(value == null ? 0 : value);
	}

	/**
	 * Adds the specified value to the current value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	IntegerValue <jv>value</jv> = IntegerValue.<jsm>of</jsm>(10);
	 * 	<jv>value</jv>.add(5);
	 * 	<jsm>assertEquals</jsm>(15, <jv>value</jv>.get());
	 * </p>
	 *
	 * @param x The value to add.
	 * @return This object.
	 */
	public IntegerValue add(Integer x) {
		set(get() + (x == null ? 0 : x));
		return this;
	}

	/**
	 * Adds the specified value to the current value and returns the new value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	IntegerValue <jv>value</jv> = IntegerValue.<jsm>of</jsm>(10);
	 * 	<jk>int</jk> <jv>result</jv> = <jv>value</jv>.addAndGet(5);  <jc>// Returns 15</jc>
	 * 	<jsm>assertEquals</jsm>(15, <jv>value</jv>.get());
	 * </p>
	 *
	 * @param x The value to add.
	 * @return The new value after addition.
	 */
	public Integer addAndGet(Integer x) {
		set(get() + (x == null ? 0 : x));
		return get();
	}

	/**
	 * Decrements the value by 1.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	IntegerValue <jv>counter</jv> = IntegerValue.<jsm>of</jsm>(5);
	 * 	<jv>counter</jv>.decrement();
	 * 	<jsm>assertEquals</jsm>(4, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @return This object.
	 */
	public IntegerValue decrement() {
		set(get() - 1);
		return this;
	}

	/**
	 * Decrements the value by 1 and returns the new value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	IntegerValue <jv>counter</jv> = IntegerValue.<jsm>of</jsm>(5);
	 * 	<jk>int</jk> <jv>result</jv> = <jv>counter</jv>.decrementAndGet();  <jc>// Returns 4</jc>
	 * 	<jsm>assertEquals</jsm>(4, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @return The decremented value.
	 */
	public Integer decrementAndGet() {
		set(get() - 1);
		return get();
	}

	/**
	 * Returns the current value and then increments it.
	 *
	 * <p>
	 * This is a convenience method commonly used for counting operations in lambdas and loops.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	IntegerValue <jv>counter</jv> = IntegerValue.<jsm>of</jsm>(5);
	 * 	<jk>int</jk> <jv>current</jv> = <jv>counter</jv>.getAndIncrement();  <jc>// Returns 5</jc>
	 * 	<jk>int</jk> <jv>next</jv> = <jv>counter</jv>.get();                <jc>// Returns 6</jc>
	 * </p>
	 *
	 * @return The value before it was incremented.
	 */
	public Integer getAndIncrement() {
		var v = get();
		var result = v == null ? 0 : v;
		set(result + 1);
		return result;
	}

	/**
	 * Increments the value by 1.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	IntegerValue <jv>counter</jv> = IntegerValue.<jsm>of</jsm>(5);
	 * 	<jv>counter</jv>.increment();
	 * 	<jsm>assertEquals</jsm>(6, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @return This object.
	 */
	public IntegerValue increment() {
		set(get() + 1);
		return this;
	}

	/**
	 * Increments the value by 1 and returns the new value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	IntegerValue <jv>counter</jv> = IntegerValue.<jsm>of</jsm>(5);
	 * 	<jk>int</jk> <jv>result</jv> = <jv>counter</jv>.incrementAndGet();  <jc>// Returns 6</jc>
	 * 	<jsm>assertEquals</jsm>(6, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @return The incremented value.
	 */
	public Integer incrementAndGet() {
		set(get() + 1);
		return get();
	}

	/**
	 * Checks if the current value is equal to the specified value.
	 *
	 * <p>
	 * Uses {@link Utils#eq(Object, Object)} for deep equality comparison, which handles nulls safely.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	IntegerValue <jv>value</jv> = IntegerValue.<jsm>of</jsm>(42);
	 * 	<jsm>assertTrue</jsm>(<jv>value</jv>.is(42));
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.is(43));
	 * </p>
	 *
	 * @param value The value to compare to.
	 * @return <jk>true</jk> if the current value is equal to the specified value.
	 */
	public boolean is(Integer value) {
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
	 * 	IntegerValue <jv>value</jv> = IntegerValue.<jsm>of</jsm>(5);
	 * 	<jsm>assertTrue</jsm>(<jv>value</jv>.isAny(3, 5, 7));
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.isAny(1, 2));
	 * </p>
	 *
	 * @param values The values to compare to.
	 * @return <jk>true</jk> if the current value matches any of the specified values.
	 */
	public boolean isAny(Integer...values) {
		assertArgNotNull("values", values);
		var current = get();
		for (var value : values)
			if (eq(current, value))
				return true;
		return false;
	}
}
