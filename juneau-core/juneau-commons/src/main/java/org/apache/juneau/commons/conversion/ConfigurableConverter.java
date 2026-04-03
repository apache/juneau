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
 * A {@link BasicConverter} subclass that supports pluggable type conversion discovery via {@link ConversionFinder}.
 *
 * <p>
 * Pass one or more {@link ConversionFinder} instances to the constructor. Finders are consulted in order before
 * falling back to the built-in {@link BasicConverter} reflection logic.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	ConfigurableConverter <jv>converter</jv> = <jk>new</jk> ConfigurableConverter(
 * 		(<jv>in</jv>, <jv>out</jv>) -> <jv>in</jv> == String.<jk>class</jk> &amp;&amp; <jv>out</jv> == MyBean.<jk>class</jk>
 * 			? (<jv>s</jv>, <jv>memberOf</jv>, <jv>session</jv>, <jv>args</jv>) -> MyBean.fromString((<jk>String</jk>) <jv>s</jv>)
 * 			: <jk>null</jk>
 * 	);
 *
 * 	MyBean <jv>bean</jv> = <jv>converter</jv>.to(<js>"value"</js>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BasicConverter}
 * 	<li class='jc'>{@link ConversionFinder}
 * 	<li class='jc'>{@link Conversion}
 * </ul>
 */
public class ConfigurableConverter extends BasicConverter {

	private final List<ConversionFinder> finders;

	/**
	 * Constructor.
	 *
	 * @param finders Optional {@link ConversionFinder} instances consulted in order before registered
	 *   type-pair conversions and the built-in {@link BasicConverter} reflection logic.
	 */
	public ConfigurableConverter(ConversionFinder... finders) {
		this.finders = List.of(finders);
	}

	/**
	 * Returns <jk>true</jk> if any registered {@link ConversionFinder} can convert the specified type pair.
	 *
	 * <p>
	 * This only checks the registered finders, not the built-in {@link BasicConverter} reflection logic.
	 *
	 * @param inType The input type class.
	 * @param outType The output type class.
	 * @return <jk>true</jk> if a finder-provided conversion exists for the specified type pair.
	 */
	public boolean hasCustomConversion(Class<?> inType, Class<?> outType) {
		for (var finder : finders)
			if (finder.find(inType, outType) != null)
				return true;
		return false;
	}

	@Override
	@SuppressWarnings({
		"unchecked" // Type safety guaranteed by ConversionFinder contract
	})
	protected <I, O> Conversion<I, O> findConversion(Class<I> inType, Class<O> outType) {
		for (var finder : finders) {
			var fn = (Conversion<I, O>) finder.find(inType, outType);
			if (fn != null)
				return fn;
		}
		return super.findConversion(inType, outType);
	}
}
