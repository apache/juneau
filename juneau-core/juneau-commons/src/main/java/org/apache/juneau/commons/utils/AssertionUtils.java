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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.ObjectUtils.*;

/**
 * Membership assertion ({@link #assertOneOf(Object, Object...) assertOneOf}).
 *
 * <p>
 * The argument and state checks that used to live here moved to {@link Shorts} in 10.0:
 * <c>req*</c> (throwing {@link IllegalArgumentException}) and <c>chk*</c> (throwing {@link IllegalStateException}).
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jk>import static</jk> org.apache.juneau.commons.utils.AssertionUtils.*;
 *
 * 	<jc>// Returns the value if it matches one of the expected values; otherwise throws an AssertionError</jc>
 * 	String <jv>mode</jv> = <jsm>assertOneOf</jsm>(<jv>input</jv>, <js>"fast"</js>, <js>"safe"</js>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 *   <li class='link'><a class="doclink" href='../../../../../index.html#juneau-commons.utils'>Overview &gt; juneau-commons.utils</a>
 * </ul>
 *
 * @see Shorts
 */
public class AssertionUtils {

	/**
	 * Prevents instantiation.
	 */
	private AssertionUtils() {}

	/**
	 * Throws an {@link AssertionError} if the specified actual value is not one of the expected values.
	 *
	 * @param <T> The value type.
	 * @param actual The actual value.  Can be <jk>null</jk> (matches a <jk>null</jk> entry in the expected values).
	 * @param expected The expected values.  Must not be <jk>null</jk> (unguarded — a <jk>null</jk> array throws {@link NullPointerException}).
	 * @return The actual value if it matches one of the expected values.
	 * @throws AssertionError if the value is not one of the expected values.
	 */
	@SafeVarargs
	public static final <T> T assertOneOf(T actual, T...expected) {
		// Q:  Is this dead code?  Can this method and class be removed entirely?
		for (var e : expected) {
			if (equal(actual, e))
				return actual;
		}
		throw new AssertionError("Invalid value specified: " + actual);
	}
}
