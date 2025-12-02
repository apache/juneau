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
package org.apache.juneau.commons.collections;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

/**
 * A simple mutable byte value.
 *
 * <p>
 * This class extends {@link Value}&lt;{@link Byte}&gt; and adds convenience methods for incrementing,
 * decrementing, and testing byte values, which are useful in lambdas and byte manipulation operations.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is <b>not thread-safe</b>. For concurrent access, use synchronization or atomic classes.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a counter</jc>
 * 	ByteValue <jv>counter</jv> = ByteValue.<jsm>create</jsm>();
 *
 * 	<jc>// Use in a lambda to count items</jc>
 * 	list.forEach(<jv>x</jv> -&gt; {
 * 		<jk>if</jk> (<jv>x</jv>.isValid()) {
 * 			<jv>counter</jv>.increment();
 * 		}
 * 	});
 *
 * 	<jsm>log</jsm>(<js>"Count: "</js> + <jv>counter</jv>.get());
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonCollections">juneau-common-collections</a>
 * </ul>
 */
public class ByteValue extends Value<Byte> {

	/**
	 * Creates a new byte value initialized to <c>0</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ByteValue <jv>counter</jv> = ByteValue.<jsm>create</jsm>();
	 * 	<jsm>assertEquals</jsm>(0, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @return A new byte value.
	 */
	public static ByteValue create() {
		return of((byte)0);
	}

	/**
	 * Creates a new byte value with the specified initial value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ByteValue <jv>value</jv> = ByteValue.<jsm>of</jsm>((<jk>byte</jk>)42);
	 * 	<jsm>assertEquals</jsm>(42, <jv>value</jv>.get());
	 * </p>
	 *
	 * @param value The initial value.
	 * @return A new byte value.
	 */
	public static ByteValue of(Byte value) {
		return new ByteValue(value);
	}

	/**
	 * Constructor.
	 */
	public ByteValue() {
		super((byte)0);
	}

	/**
	 * Constructor.
	 *
	 * @param value The initial value.
	 */
	public ByteValue(Byte value) {
		super(value == null ? 0 : value);
	}

	/**
	 * Adds the specified value to the current value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ByteValue <jv>value</jv> = ByteValue.<jsm>of</jsm>((<jk>byte</jk>)10);
	 * 	<jv>value</jv>.add((<jk>byte</jk>)5);
	 * 	<jsm>assertEquals</jsm>(15, <jv>value</jv>.get());
	 * </p>
	 *
	 * @param x The value to add.
	 * @return This object.
	 */
	public ByteValue add(Byte x) {
		set((byte)(get() + (x == null ? 0 : x)));
		return this;
	}

	/**
	 * Adds the specified value to the current value and returns the new value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ByteValue <jv>value</jv> = ByteValue.<jsm>of</jsm>((<jk>byte</jk>)10);
	 * 	<jk>byte</jk> <jv>result</jv> = <jv>value</jv>.addAndGet((<jk>byte</jk>)5);  <jc>// Returns 15</jc>
	 * 	<jsm>assertEquals</jsm>(15, <jv>value</jv>.get());
	 * </p>
	 *
	 * @param x The value to add.
	 * @return The new value after addition.
	 */
	public Byte addAndGet(Byte x) {
		set((byte)(get() + (x == null ? 0 : x)));
		return get();
	}

	/**
	 * Decrements the value by 1.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ByteValue <jv>counter</jv> = ByteValue.<jsm>of</jsm>((<jk>byte</jk>)5);
	 * 	<jv>counter</jv>.decrement();
	 * 	<jsm>assertEquals</jsm>(4, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @return This object.
	 */
	public ByteValue decrement() {
		set((byte)(get() - 1));
		return this;
	}

	/**
	 * Decrements the value by 1 and returns the new value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ByteValue <jv>counter</jv> = ByteValue.<jsm>of</jsm>((<jk>byte</jk>)5);
	 * 	<jk>byte</jk> <jv>result</jv> = <jv>counter</jv>.decrementAndGet();  <jc>// Returns 4</jc>
	 * 	<jsm>assertEquals</jsm>(4, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @return The decremented value.
	 */
	public Byte decrementAndGet() {
		set((byte)(get() - 1));
		return get();
	}

	/**
	 * Increments the value by 1.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ByteValue <jv>counter</jv> = ByteValue.<jsm>of</jsm>((<jk>byte</jk>)5);
	 * 	<jv>counter</jv>.increment();
	 * 	<jsm>assertEquals</jsm>(6, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @return This object.
	 */
	public ByteValue increment() {
		set((byte)(get() + 1));
		return this;
	}

	/**
	 * Increments the value by 1 and returns the new value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	ByteValue <jv>counter</jv> = ByteValue.<jsm>of</jsm>((<jk>byte</jk>)5);
	 * 	<jk>byte</jk> <jv>result</jv> = <jv>counter</jv>.incrementAndGet();  <jc>// Returns 6</jc>
	 * 	<jsm>assertEquals</jsm>(6, <jv>counter</jv>.get());
	 * </p>
	 *
	 * @return The incremented value.
	 */
	public Byte incrementAndGet() {
		set((byte)(get() + 1));
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
	 * 	ByteValue <jv>value</jv> = ByteValue.<jsm>of</jsm>((<jk>byte</jk>)42);
	 * 	<jsm>assertTrue</jsm>(<jv>value</jv>.is((<jk>byte</jk>)42));
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.is((<jk>byte</jk>)43));
	 * </p>
	 *
	 * @param value The value to compare to.
	 * @return <jk>true</jk> if the current value is equal to the specified value.
	 */
	public boolean is(Byte value) {
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
	 * 	ByteValue <jv>value</jv> = ByteValue.<jsm>of</jsm>((<jk>byte</jk>)5);
	 * 	<jsm>assertTrue</jsm>(<jv>value</jv>.isAny((<jk>byte</jk>)3, (<jk>byte</jk>)5, (<jk>byte</jk>)7));
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.isAny((<jk>byte</jk>)1, (<jk>byte</jk>)2));
	 * </p>
	 *
	 * @param values The values to compare to.
	 * @return <jk>true</jk> if the current value matches any of the specified values.
	 */
	public boolean isAny(Byte...values) {
		assertArgNotNull("values", values);
		var current = get();
		for (var value : values)
			if (eq(current, value))
				return true;
		return false;
	}
}
