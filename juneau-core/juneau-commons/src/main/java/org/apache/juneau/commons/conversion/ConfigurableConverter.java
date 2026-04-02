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
import java.util.concurrent.*;

/**
 * A {@link BasicConverter} subclass that supports runtime registration of custom type conversions.
 *
 * <p>
 * Use {@link #add(Class, Class, Conversion)} to register a custom {@link Conversion} function for a specific
 * input/output type pair before the first conversion for that pair is requested.
 * Registered conversions take priority over the built-in {@link BasicConverter} reflection logic.
 *
 * <p>
 * This class is intended to be instantiated and held as a field (e.g., on a {@code BeanContext}) so that
 * custom conversions can be injected at configuration time.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	ConfigurableConverter <jv>converter</jv> = <jk>new</jk> ConfigurableConverter()
 * 		.add(String.<jk>class</jk>, MyBean.<jk>class</jk>, (<jv>in</jv>, <jv>memberOf</jv>, <jv>args</jv>) -> MyBean.fromString(<jv>in</jv>));
 *
 * 	MyBean <jv>bean</jv> = <jv>converter</jv>.to(<js>"value"</js>, MyBean.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is thread-safe provided that all {@link #add} calls complete before the converter is shared across
 * threads. Registering conversions concurrently with active {@link #to} calls is also safe due to the underlying
 * {@link ConcurrentHashMap}, but registered conversions may not be visible immediately if the cache has already
 * been populated for that type pair.
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BasicConverter}
 * 	<li class='jc'>{@link Conversion}
 * </ul>
 */
@SuppressWarnings({
	"unchecked" // Type erasure requires unchecked casts in registry lookup
})
public class ConfigurableConverter extends BasicConverter {

	private final Map<Class<?>, Map<Class<?>, Conversion<?,?>>> registered = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 */
	public ConfigurableConverter() {}

	/**
	 * Registers a custom conversion function for the specified input/output type pair.
	 *
	 * <p>
	 * The registered conversion takes priority over the built-in {@link BasicConverter} reflection logic.
	 * Registrations should be made before the converter is shared across threads or before the first conversion
	 * for the given type pair is requested.
	 *
	 * @param <I> The input type.
	 * @param <O> The output type.
	 * @param inType The input type class.
	 * @param outType The output type class.
	 * @param conversion The conversion function to register.
	 * @return This object.
	 */
	public <I, O> ConfigurableConverter add(Class<I> inType, Class<O> outType, Conversion<I, O> conversion) {
		registered
			.computeIfAbsent(inType, k -> new ConcurrentHashMap<>())
			.put(outType, conversion);
		return this;
	}

	@Override
	protected <I, O> Conversion<I, O> findConversion(Class<I> inType, Class<O> outType) {
		var inner = registered.get(inType);
		if (inner != null) {
			var fn = (Conversion<I, O>) inner.get(outType);
			if (fn != null)
				return fn;
		}
		return super.findConversion(inType, outType);
	}
}
