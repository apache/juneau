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

import java.util.concurrent.atomic.*;

/**
 * A simple mutable boolean value.
 *
 * <p>
 * This class extends {@link Value}&lt;{@link Boolean}&gt; and provides a convenient way to pass mutable
 * boolean references to lambdas, inner classes, or methods. Unlike {@link Flag}, this class supports three
 * states: <c>true</c>, <c>false</c>, and <c>null</c>.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is <b>not thread-safe</b>. For concurrent access, use {@link AtomicBoolean} instead.
 * 	<li class='note'>
 * 		This class supports three states (<c>true</c>, <c>false</c>, <c>null</c>). If you only need
 * 		two states (<c>true</c>/<c>false</c>), consider using {@link Flag} instead.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a boolean value to track if a condition was met</jc>
 * 	BooleanValue <jv>found</jv> = BooleanValue.<jsm>create</jsm>();
 *
 * 	<jc>// Use in a lambda</jc>
 * 	list.forEach(<jv>x</jv> -&gt; {
 * 		<jk>if</jk> (<jv>x</jv>.matches(criteria)) {
 * 			<jv>found</jv>.set(<jk>true</jk>);
 * 		}
 * 	});
 *
 * 	<jk>if</jk> (<jv>found</jv>.isTrue()) {
 * 		<jsm>log</jsm>(<js>"Match found!"</js>);
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsCollections">juneau-commons-collections</a>
 * 	<li class='jc'>{@link Flag}
 * </ul>
 */
public class BooleanValue extends Value<Boolean> {

	/**
	 * Creates a new boolean value initialized to <c>false</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	BooleanValue <jv>value</jv> = BooleanValue.<jsm>create</jsm>();
	 * 	<jsm>assertEquals</jsm>(<jk>false</jk>, <jv>value</jv>.get());
	 * </p>
	 *
	 * @return A new boolean value.
	 */
	public static BooleanValue create() {
		return of(false);
	}

	/**
	 * Creates a new boolean value with the specified initial value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	BooleanValue <jv>value</jv> = BooleanValue.<jsm>of</jsm>(<jk>true</jk>);
	 * 	<jsm>assertEquals</jsm>(<jk>true</jk>, <jv>value</jv>.get());
	 * </p>
	 *
	 * @param value The initial value.
	 * @return A new boolean value.
	 */
	public static BooleanValue of(Boolean value) {
		return new BooleanValue(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value The initial value.
	 */
	public BooleanValue(Boolean value) {
		super(value);
	}

	/**
	 * Checks if the current value is equal to the specified value.
	 *
	 * <p>
	 * Uses {@link Utils#eq(Object, Object)} for deep equality comparison, which handles nulls safely.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	BooleanValue <jv>value</jv> = BooleanValue.<jsm>of</jsm>(<jk>true</jk>);
	 * 	<jsm>assertTrue</jsm>(<jv>value</jv>.is(<jk>true</jk>));
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.is(<jk>false</jk>));
	 *
	 * 	<jc>// Handles null safely</jc>
	 * 	BooleanValue <jv>empty</jv> = BooleanValue.<jsm>of</jsm>(<jk>null</jk>);
	 * 	<jsm>assertTrue</jsm>(<jv>empty</jv>.is(<jk>null</jk>));
	 * </p>
	 *
	 * @param value The value to compare to.
	 * @return <jk>true</jk> if the current value is equal to the specified value.
	 */
	public boolean is(Boolean value) {
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
	 * 	BooleanValue <jv>value</jv> = BooleanValue.<jsm>of</jsm>(<jk>true</jk>);
	 * 	<jsm>assertTrue</jsm>(<jv>value</jv>.isAny(<jk>true</jk>, <jk>null</jk>));
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.isAny(<jk>false</jk>));
	 * </p>
	 *
	 * @param values The values to compare to.
	 * @return <jk>true</jk> if the current value matches any of the specified values.
	 */
	public boolean isAny(Boolean...values) {
		assertArgNotNull("values", values);
		var current = get();
		for (var value : values)
			if (eq(current, value))
				return true;
		return false;
	}

	/**
	 * Returns <c>true</c> if the value is not set to <c>true</c> (i.e., it's <c>false</c> or <c>null</c>).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	BooleanValue <jv>value</jv> = BooleanValue.<jsm>of</jsm>(<jk>false</jk>);
	 * 	<jk>if</jk> (<jv>value</jv>.isNotTrue()) {
	 * 		<jsm>log</jsm>(<js>"Value is not true"</js>);
	 * 	}
	 * </p>
	 *
	 * @return <c>true</c> if the value is not set to <c>true</c>, <c>false</c> otherwise.
	 */
	public boolean isNotTrue() { return ! isTrue(); }

	/**
	 * Returns <c>true</c> if the value is set to <c>true</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	BooleanValue <jv>value</jv> = BooleanValue.<jsm>of</jsm>(<jk>true</jk>);
	 * 	<jk>if</jk> (<jv>value</jv>.isTrue()) {
	 * 		<jsm>log</jsm>(<js>"Value is true"</js>);
	 * 	}
	 * </p>
	 *
	 * @return <c>true</c> if the value is set to <c>true</c>, <c>false</c> otherwise (including when <c>null</c>).
	 */
	public boolean isTrue() { return Boolean.TRUE.equals(get()); }
}
