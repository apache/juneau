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

/**
 * A pluggable strategy for discovering {@link Conversion} functions between type pairs.
 *
 * <p>
 * Implementations are passed to the {@link ConfigurableConverter} constructor and are consulted in order before
 * the built-in {@link BasicConverter} reflection logic.
 *
 * <p>
 * Return <jk>null</jk> from {@link #find} to indicate that this finder has no conversion for the
 * given type pair; the next registered finder (or the built-in logic) will be tried instead.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	ConfigurableConverter <jv>converter</jv> = <jk>new</jk> ConfigurableConverter(
 * 		(<jv>inType</jv>, <jv>outType</jv>) -> {
 * 			<jk>if</jk> (<jv>inType</jv> == String.<jk>class</jk> &amp;&amp; <jv>outType</jv> == MyBean.<jk>class</jk>)
 * 				<jk>return</jk> (<jv>in</jv>, <jv>memberOf</jv>, <jv>session</jv>, <jv>args</jv>) -> MyBean.fromString((<jk>String</jk>) <jv>in</jv>);
 * 			<jk>return null</jk>;
 * 		}
 * 	);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ConfigurableConverter}
 * 	<li class='jc'>{@link Conversion}
 * </ul>
 */
@FunctionalInterface
public interface ConversionFinder {

	/**
	 * Returns a {@link Conversion} for the specified input/output type pair, or <jk>null</jk> if this finder
	 * has no conversion for the pair.
	 *
	 * @param inType The input type class.
	 * @param outType The output type class.
	 * @return A {@link Conversion} function, or <jk>null</jk> if no conversion is available.
	 */
	@SuppressWarnings({
		"java:S1452" // Wildcards unavoidable; concrete I/O types are only known at runtime
	})
	Conversion<?,?> findConversion(Class<?> inType, Class<?> outType);
}
