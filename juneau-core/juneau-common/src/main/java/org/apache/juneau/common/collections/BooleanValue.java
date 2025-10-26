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
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonCollections">juneau-common-collections</a>
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
}
