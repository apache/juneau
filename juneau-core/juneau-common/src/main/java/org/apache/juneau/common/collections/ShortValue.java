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
 * A simple mutable short value.
 *
 * <p>
 * This class extends {@link Value}&lt;{@link Short}&gt; and adds a convenience method for incrementing
 * the value, which is useful for counting operations in lambdas and loops.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is <b>not thread-safe</b>. For concurrent access, use {@link AtomicInteger} instead.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a counter</jc>
 * 	ShortValue <jv>counter</jv> = ShortValue.<jsm>create</jsm>();
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
public class ShortValue extends Value<Short> {

	/**
	 * Creates a new short value initialized to <c>0</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ShortValue <jv>counter</jv> = ShortValue.<jsm>create</jsm>();
	 * 	<jsm>assertEquals</jsm>(0, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @return A new short value.
	 */
	public static ShortValue create() {
		return of((short)0);
	}

	/**
	 * Creates a new short value with the specified initial value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ShortValue <jv>counter</jv> = ShortValue.<jsm>of</jsm>((<jk>short</jk>)42);
	 * 	<jsm>assertEquals</jsm>(42, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @param value The initial value.
	 * @return A new short value.
	 */
	public static ShortValue of(Short value) {
		return new ShortValue(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value The initial value.
	 */
	public ShortValue(Short value) {
		super(value);
	}

	/**
	 * Returns the current value and then increments it.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ShortValue <jv>counter</jv> = ShortValue.<jsm>of</jsm>((<jk>short</jk>)5);
	 * 	<jk>short</jk> <jv>current</jv> = <jv>counter</jv>.getAndIncrement();  <jc>// Returns 5</jc>
	 * 	<jk>short</jk> <jv>next</jv> = <jv>counter</jv>.get();                <jc>// Returns 6</jc>
	 * </p>
	 *
	 * @return The value before it was incremented.
	 */
	public short getAndIncrement() {
		var v = get();
		set(v == null ? 1 : (short)(v + 1));
		return v == null ? 0 : v;
	}

	/**
	 * Increments the value by 1.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ShortValue <jv>counter</jv> = ShortValue.<jsm>of</jsm>((<jk>short</jk>)5);
	 * 	<jv>counter</jv>.increment();
	 * 	<jsm>assertEquals</jsm>(6, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @return This object.
	 */
	public ShortValue increment() {
		var v = get();
		set((short)((v == null ? 0 : v) + 1));
		return this;
	}

	/**
	 * Decrements the value by 1.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ShortValue <jv>counter</jv> = ShortValue.<jsm>of</jsm>((<jk>short</jk>)5);
	 * 	<jv>counter</jv>.decrement();
	 * 	<jsm>assertEquals</jsm>(4, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @return This object.
	 */
	public ShortValue decrement() {
		var v = get();
		set((short)((v == null ? 0 : v) - 1));
		return this;
	}

	/**
	 * Increments the value by 1 and returns the new value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ShortValue <jv>counter</jv> = ShortValue.<jsm>of</jsm>((<jk>short</jk>)5);
	 * 	<jk>short</jk> <jv>result</jv> = <jv>counter</jv>.incrementAndGet();  <jc>// Returns 6</jc>
	 * 	<jsm>assertEquals</jsm>(6, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @return The incremented value.
	 */
	public Short incrementAndGet() {
		var v = get();
		var result = (short)((v == null ? 0 : v) + 1);
		set(result);
		return result;
	}

	/**
	 * Decrements the value by 1 and returns the new value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ShortValue <jv>counter</jv> = ShortValue.<jsm>of</jsm>((<jk>short</jk>)5);
	 * 	<jk>short</jk> <jv>result</jv> = <jv>counter</jv>.decrementAndGet();  <jc>// Returns 4</jc>
	 * 	<jsm>assertEquals</jsm>(4, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @return The decremented value.
	 */
	public Short decrementAndGet() {
		var v = get();
		var result = (short)((v == null ? 0 : v) - 1);
		set(result);
		return result;
	}

	/**
	 * Adds the specified value to the current value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ShortValue <jv>value</jv> = ShortValue.<jsm>of</jsm>((<jk>short</jk>)10);
	 * 	<jv>value</jv>.add((<jk>short</jk>)5);
	 * 	<jsm>assertEquals</jsm>(15, <jv>value</jv>.get());
	 * </p>
	 *
	 * @param x The value to add.
	 * @return This object.
	 */
	public ShortValue add(Short x) {
		var v = get();
		set((short)((v == null ? 0 : v) + (x == null ? 0 : x)));
		return this;
	}

	/**
	 * Adds the specified value to the current value and returns the new value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ShortValue <jv>value</jv> = ShortValue.<jsm>of</jsm>((<jk>short</jk>)10);
	 * 	<jk>short</jk> <jv>result</jv> = <jv>value</jv>.addAndGet((<jk>short</jk>)5);  <jc>// Returns 15</jc>
	 * 	<jsm>assertEquals</jsm>(15, <jv>value</jv>.get());
	 * </p>
	 *
	 * @param x The value to add.
	 * @return The new value after addition.
	 */
	public Short addAndGet(Short x) {
		var v = get();
		var result = (short)((v == null ? 0 : v) + (x == null ? 0 : x));
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
	 * 	ShortValue <jv>value</jv> = ShortValue.<jsm>of</jsm>((<jk>short</jk>)42);
	 * 	<jsm>assertTrue</jsm>(<jv>value</jv>.is((<jk>short</jk>)42));
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.is((<jk>short</jk>)43));
	 * </p>
	 *
	 * @param value The value to compare to.
	 * @return <jk>true</jk> if the current value is equal to the specified value.
	 */
	public boolean is(Short value) {
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
	 * 	ShortValue <jv>value</jv> = ShortValue.<jsm>of</jsm>((<jk>short</jk>)5);
	 * 	<jsm>assertTrue</jsm>(<jv>value</jv>.isAny((<jk>short</jk>)3, (<jk>short</jk>)5, (<jk>short</jk>)7));
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.isAny((<jk>short</jk>)1, (<jk>short</jk>)2));
	 * </p>
	 *
	 * @param values The values to compare to.
	 * @return <jk>true</jk> if the current value matches any of the specified values.
	 */
	public boolean isAny(Short...values) {
		assertArgNotNull("values", values);
		var current = get();
		for (var value : values)
			if (eq(current, value))
				return true;
		return false;
	}
}
