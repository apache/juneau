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
package org.apache.juneau.commons.conversion;

import java.util.*;

/**
 * Provides contextual objects to {@link Conversion} functions at call time.
 *
 * <p>
 * A {@code ConverterSession} is passed through the {@link Conversion#to(Object, Object, ConverterSession, Class[])}
 * method and allows conversion lambdas to retrieve session-scoped objects such as a {@link java.util.TimeZone},
 * {@link java.util.Locale}, or media type without requiring those objects to be captured in the lambda closure.
 *
 * <p>
 * Implementations look up contextual objects by type (and optionally by name). If no matching object is
 * registered for the requested type the method returns {@link Optional#empty()}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// A conversion that uses the session's TimeZone.</jc>
 * 	BeanContext <jv>bc</jv> = BeanContext
 * 		.<jsm>create</jsm>()
 * 		.addConverter(String.<jk>class</jk>, Calendar.<jk>class</jk>, (<jv>in</jv>, <jv>memberOf</jv>, <jv>session</jv>, <jv>args</jv>) -&gt; {
 * 			var <jv>tz</jv> = <jv>session</jv>.get(TimeZone.<jk>class</jk>).orElse(TimeZone.<jsm>getDefault</jsm>());
 * 			<jc>// ... parse string using tz ...</jc>
 * 		})
 * 		.build();
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Conversion}
 * 	<li class='jc'>{@link Converter}
 * </ul>
 */
public interface ConverterSession {

	/**
	 * Returns a contextual object of the specified type, or {@link Optional#empty()} if none is registered.
	 *
	 * @param <T> The object type.
	 * @param c The object type to look up.
	 * @return The contextual object wrapped in an {@link Optional}, or {@link Optional#empty()} if not available.
	 */
	<T> Optional<T> get(Class<T> c);

	/**
	 * Returns a named contextual object of the specified type, or {@link Optional#empty()} if none is registered.
	 *
	 * <p>
	 * The default implementation ignores the name and delegates to {@link #get(Class)}.
	 * Override this method when multiple objects of the same type need to be distinguished by name.
	 *
	 * @param <T> The object type.
	 * @param name The object name, or <jk>null</jk> to match any.
	 * @param c The object type to look up.
	 * @return The contextual object wrapped in an {@link Optional}, or {@link Optional#empty()} if not available.
	 */
	default <T> Optional<T> get(String name, Class<T> c) {
		return get(c);
	}
}
