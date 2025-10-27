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

/**
 * A simple mutable float value.
 *
 * <p>
 * This class extends {@link Value}&lt;{@link Float}&gt; and provides a convenient way to pass mutable
 * float references to lambdas, inner classes, or methods.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is <b>not thread-safe</b>.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a float value to track a running sum</jc>
 * 	FloatValue <jv>sum</jv> = FloatValue.<jsm>create</jsm>();
 *
 * 	<jc>// Use in a lambda to accumulate values</jc>
 * 	list.forEach(<jv>x</jv> -&gt; {
 * 		<jv>sum</jv>.set(<jv>sum</jv>.get() + <jv>x</jv>.floatValue());
 * 	});
 *
 * 	<jsm>log</jsm>(<js>"Total: "</js> + <jv>sum</jv>.get());
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonCollections">juneau-common-collections</a>
 * </ul>
 */
public class FloatValue extends Value<Float> {

	/**
	 * Creates a new float value initialized to <c>0.0f</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FloatValue <jv>value</jv> = FloatValue.<jsm>create</jsm>();
	 * 	<jsm>assertEquals</jsm>(0.0f, <jv>value</jv>.get());
	 * </p>
	 *
	 * @return A new float value.
	 */
	public static FloatValue create() {
		return of(0.0f);
	}

	/**
	 * Creates a new float value with the specified initial value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FloatValue <jv>value</jv> = FloatValue.<jsm>of</jsm>(3.14f);
	 * 	<jsm>assertEquals</jsm>(3.14f, <jv>value</jv>.get());
	 * </p>
	 *
	 * @param value The initial value.
	 * @return A new float value.
	 */
	public static FloatValue of(Float value) {
		return new FloatValue(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value The initial value.
	 */
	public FloatValue(Float value) {
		super(value);
	}

	/**
	 * Checks if the current value equals the specified value within the given precision.
	 *
	 * <p>
	 * This method handles <jk>null</jk> values safely and performs precision-based equality comparison
	 * using absolute difference. The comparison returns <jk>true</jk> if the absolute difference between
	 * the two values is less than or equal to the specified precision.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FloatValue <jv>value</jv> = FloatValue.<jsm>of</jsm>(3.14f);
	 * 	<jsm>assertTrue</jsm>(<jv>value</jv>.is(3.14f, 0.01f));    <jc>// Exact match within 0.01</jc>
	 * 	<jsm>assertTrue</jsm>(<jv>value</jv>.is(3.15f, 0.02f));    <jc>// Within 0.02</jc>
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.is(3.15f, 0.001f));  <jc>// Not within 0.001</jc>
	 *
	 * 	<jc>// Handles null values</jc>
	 * 	FloatValue <jv>empty</jv> = FloatValue.<jsm>create</jsm>();
	 * 	<jv>empty</jv>.set(<jk>null</jk>);
	 * 	<jsm>assertFalse</jsm>(<jv>empty</jv>.is(3.14f, 0.01f));   <jc>// Null doesn't match any value</jc>
	 *
	 * 	<jc>// Precision-based comparison</jc>
	 * 	FloatValue <jv>pi</jv> = FloatValue.<jsm>of</jsm>(3.14159f);
	 * 	<jsm>assertTrue</jsm>(<jv>pi</jv>.is(3.14f, 0.002f));      <jc>// Within 0.002 precision</jc>
	 * </p>
	 *
	 * @param other The value to compare with.
	 * @param precision The maximum allowed difference for equality. Must be non-negative.
	 * @return <jk>true</jk> if the absolute difference between the values is less than or equal to precision.
	 * @throws IllegalArgumentException if precision is negative.
	 */
	public boolean is(float other, float precision) {
		if (precision < 0) {
			throw new IllegalArgumentException("Precision must be non-negative");
		}
		var v = get();
		if (v == null) {
			return false;
		}
		return Math.abs(v - other) <= precision;
	}

	/**
	 * Checks if the current value matches any of the specified values within the given precision.
	 *
	 * <p>
	 * This method handles <jk>null</jk> values safely and performs precision-based equality comparison
	 * for each value using absolute difference.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	FloatValue <jv>value</jv> = FloatValue.<jsm>of</jsm>(3.14f);
	 * 	<jsm>assertTrue</jsm>(<jv>value</jv>.isAny(0.01f, 2.5f, 3.15f, 5.0f));   <jc>// Matches 3.15f within 0.01</jc>
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.isAny(0.001f, 1.0f, 2.0f, 5.0f));  <jc>// No match within 0.001</jc>
	 *
	 * 	<jc>// Empty array returns false</jc>
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.isAny(0.01f));
	 * </p>
	 *
	 * @param precision The maximum allowed difference for equality. Must be non-negative and is the first parameter.
	 * @param values The values to compare to.
	 * @return <jk>true</jk> if the current value matches any of the specified values within the precision.
	 * @throws IllegalArgumentException if precision is negative.
	 */
	public boolean isAny(float precision, float... values) {
		if (precision < 0) {
			throw new IllegalArgumentException("Precision must be non-negative");
		}
		var v = get();
		if (v == null)
			return false;
		for (var value : values)
			if (Math.abs(v - value) <= precision)
				return true;
		return false;
	}
}
