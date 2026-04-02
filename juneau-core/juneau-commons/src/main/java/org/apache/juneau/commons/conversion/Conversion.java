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
 * Functional interface representing a single type conversion used by {@link CachingConverter}.
 *
 * <p>
 * The {@code args} parameter carries any type arguments needed for parameterized output types.
 * For example, when converting to a {@link List}, {@code args[0]} would be the element type.
 * For a {@link Map}, {@code args[0]} is the key type and {@code args[1]} is the value type.
 *
 * <p>
 * A single cached {@code Conversion} function can serve all parameterizations of the same raw output
 * type by accepting different {@code args} values at runtime.
 *
 * @param <I> The input type.
 * @param <O> The output type.
 */
@FunctionalInterface
public interface Conversion<I, O> {

	/**
	 * Converts the input object to the output type.
	 *
	 * @param in The input object.
	 * @param memberOf The outer instance for non-static inner class construction, or <jk>null</jk>.
	 * @param session The converter session providing contextual objects such as {@link java.util.TimeZone} or
	 * 	{@link java.util.Locale}, or <jk>null</jk> if no session is available.
	 * @param args Optional type arguments for parameterized output types (e.g. element type for collections).
	 * @return The converted object.
	 */
	O to(I in, Object memberOf, ConverterSession session, Class<?>... args);
}
