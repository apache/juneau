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
 * A simple mutable string value.
 *
 * <p>
 * This class extends {@link Value}&lt;{@link String}&gt; and adds convenience methods for string
 * equality testing, which are useful in lambdas and conditional logic.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is <b>not thread-safe</b>. For concurrent access, use synchronization or atomic classes.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a mutable string</jc>
 * 	StringValue <jv>name</jv> = StringValue.<jsm>of</jsm>(<js>"John"</js>);
 *
 * 	<jc>// Check equality</jc>
 * 	<jk>if</jk> (<jv>name</jv>.is(<js>"John"</js>)) {
 * 		<jsm>System</jsm>.<jsf>out</jsf>.println(<js>"Name is John"</js>);
 * 	}
 *
 * 	<jc>// Check multiple values</jc>
 * 	<jk>if</jk> (<jv>name</jv>.isAny(<js>"John"</js>, <js>"Jane"</js>, <js>"Bob"</js>)) {
 * 		<jsm>System</jsm>.<jsf>out</jsf>.println(<js>"Name found"</js>);
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonCollections">juneau-common-collections</a>
 * </ul>
 */
public class StringValue extends Value<String> {

	/**
	 * Creates a new empty string value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	StringValue <jv>name</jv> = StringValue.<jsm>create</jsm>();
	 * 	<jsm>assertNull</jsm>(<jv>name</jv>.get());
	 * </p>
	 *
	 * @return A new string value.
	 */
	public static StringValue create() {
		return of(null);
	}

	/**
	 * Creates a new string value with the specified initial value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	StringValue <jv>name</jv> = StringValue.<jsm>of</jsm>(<js>"Hello"</js>);
	 * 	<jsm>assertEquals</jsm>(<js>"Hello"</js>, <jv>name</jv>.get());
	 * </p>
	 *
	 * @param value The initial value.
	 * @return A new string value.
	 */
	public static StringValue of(String value) {
		return new StringValue(value);
	}

	/**
	 * Constructor.
	 */
	public StringValue() {
		super(null);
	}

	/**
	 * Constructor.
	 *
	 * @param value The initial value.
	 */
	public StringValue(String value) {
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
	 * 	StringValue <jv>name</jv> = StringValue.<jsm>of</jsm>(<js>"John"</js>);
	 * 	<jsm>assertTrue</jsm>(<jv>name</jv>.is(<js>"John"</js>));
	 * 	<jsm>assertFalse</jsm>(<jv>name</jv>.is(<js>"Jane"</js>));
	 *
	 * 	<jc>// Handles null safely</jc>
	 * 	StringValue <jv>empty</jv> = StringValue.<jsm>create</jsm>();
	 * 	<jsm>assertTrue</jsm>(<jv>empty</jv>.is(<jk>null</jk>));
	 * </p>
	 *
	 * @param value The value to compare to.
	 * @return <jk>true</jk> if the current value is equal to the specified value.
	 */
	public boolean is(String value) {
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
	 * 	StringValue <jv>name</jv> = StringValue.<jsm>of</jsm>(<js>"John"</js>);
	 * 	<jsm>assertTrue</jsm>(<jv>name</jv>.isAny(<js>"John"</js>, <js>"Jane"</js>, <js>"Bob"</js>));
	 * 	<jsm>assertFalse</jsm>(<jv>name</jv>.isAny(<js>"Alice"</js>, <js>"Charlie"</js>));
	 *
	 * 	<jc>// Empty array returns false</jc>
	 * 	<jsm>assertFalse</jsm>(<jv>name</jv>.isAny());
	 * </p>
	 *
	 * @param values The values to compare to.
	 * @return <jk>true</jk> if the current value matches any of the specified values.
	 */
	public boolean isAny(String...values) {
		assertArgNotNull("values", values);
		var current = get();
		for (var value : values)
			if (eq(current, value))
				return true;
		return false;
	}
}
