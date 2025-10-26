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
}