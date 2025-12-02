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

/**
 * A simple mutable double value.
 *
 * <p>
 * This class extends {@link Value}&lt;{@link Double}&gt; and provides a convenient way to pass mutable
 * double references to lambdas, inner classes, or methods.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is <b>not thread-safe</b>.
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a double value to track a running sum</jc>
 * 	DoubleValue <jv>sum</jv> = DoubleValue.<jsm>create</jsm>();
 *
 * 	<jc>// Use in a lambda to accumulate values</jc>
 * 	list.forEach(<jv>x</jv> -&gt; {
 * 		<jv>sum</jv>.set(<jv>sum</jv>.get() + <jv>x</jv>.doubleValue());
 * 	});
 *
 * 	<jsm>log</jsm>(<js>"Total: "</js> + <jv>sum</jv>.get());
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonCollections">juneau-common-collections</a>
 * </ul>
 */
public class DoubleValue extends Value<Double> {

	/**
	 * Creates a new double value initialized to <c>0.0</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	DoubleValue <jv>value</jv> = DoubleValue.<jsm>create</jsm>();
	 * 	<jsm>assertEquals</jsm>(0.0, <jv>value</jv>.get());
	 * </p>
	 *
	 * @return A new double value.
	 */
	public static DoubleValue create() {
		return of(0.0);
	}

	/**
	 * Creates a new double value with the specified initial value.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	DoubleValue <jv>value</jv> = DoubleValue.<jsm>of</jsm>(3.14159);
	 * 	<jsm>assertEquals</jsm>(3.14159, <jv>value</jv>.get());
	 * </p>
	 *
	 * @param value The initial value.
	 * @return A new double value.
	 */
	public static DoubleValue of(Double value) {
		return new DoubleValue(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value The initial value.
	 */
	public DoubleValue(Double value) {
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
	 * 	DoubleValue <jv>value</jv> = DoubleValue.<jsm>of</jsm>(3.14159);
	 * 	<jsm>assertTrue</jsm>(<jv>value</jv>.is(3.14, 0.01));      <jc>// Within 0.01</jc>
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.is(3.14, 0.001));    <jc>// Not within 0.001</jc>
	 *
	 * 	<jc>// Handles null values</jc>
	 * 	DoubleValue <jv>empty</jv> = DoubleValue.<jsm>create</jsm>();
	 * 	<jv>empty</jv>.set(<jk>null</jk>);
	 * 	<jsm>assertFalse</jsm>(<jv>empty</jv>.is(3.14, 0.01));     <jc>// Null doesn't match any value</jc>
	 *
	 * 	<jc>// Floating-point precision example</jc>
	 * 	DoubleValue <jv>calc</jv> = DoubleValue.<jsm>of</jsm>(0.1 + 0.2);  <jc>// Actually 0.30000000000000004</jc>
	 * 	<jsm>assertTrue</jsm>(<jv>calc</jv>.is(0.3, 0.000001));    <jc>// Handles rounding errors</jc>
	 * </p>
	 *
	 * @param other The value to compare with.
	 * @param precision The maximum allowed difference for equality. Must be non-negative.
	 * @return <jk>true</jk> if the absolute difference between the values is less than or equal to precision.
	 * @throws IllegalArgumentException if precision is negative.
	 */
	public boolean is(double other, double precision) {
		assertArg(precision >= 0, "Precision must be non-negative");
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
	 * 	DoubleValue <jv>value</jv> = DoubleValue.<jsm>of</jsm>(3.14159);
	 * 	<jsm>assertTrue</jsm>(<jv>value</jv>.isAny(0.01, 2.5, 3.15, 5.0));   <jc>// Matches 3.15 within 0.01</jc>
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.isAny(0.001, 1.0, 2.0, 5.0));  <jc>// No match within 0.001</jc>
	 *
	 * 	<jc>// Empty array returns false</jc>
	 * 	<jsm>assertFalse</jsm>(<jv>value</jv>.isAny(0.01));
	 * </p>
	 *
	 * @param precision The maximum allowed difference for equality. Must be non-negative and is the first parameter.
	 * @param values The values to compare to.
	 * @return <jk>true</jk> if the current value matches any of the specified values within the precision.
	 * @throws IllegalArgumentException if precision is negative.
	 */
	public boolean isAny(double precision, double...values) {
		assertArg(precision >= 0, "Precision must be non-negative");
		assertArgNotNull("values", values);
		var v = get();
		if (v == null)
			return false;
		for (var value : values)
			if (Math.abs(v - value) <= precision)
				return true;
		return false;
	}
}