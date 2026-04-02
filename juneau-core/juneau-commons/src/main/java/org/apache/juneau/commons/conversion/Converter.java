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

import java.lang.reflect.*;

/**
 * Interface for converting objects between types.
 *
 * <p>
 * Use {@link BasicConverter#INSTANCE} for a default singleton implementation that supports
 * a wide range of common type conversions without requiring a {@code BeanContext} or {@code BeanSession}.
 */
public interface Converter {

	/**
	 * Returns <jk>true</jk> if this converter can convert from the specified input type to the specified output type.
	 *
	 * @param inType The input type.
	 * @param outType The output type.
	 * @return <jk>true</jk> if a conversion path exists.
	 */
	default boolean canConvert(Class<?> inType, Class<?> outType) {
		return true;
	}

	/**
	 * Converts the specified object to the specified type.
	 *
	 * <p>
	 * Returns <jk>null</jk> only when the input object is <jk>null</jk>.
	 * Throws {@link InvalidConversionException} when no conversion path exists.
	 * Use {@link #canConvert(Class, Class)} to pre-check if uncertain.
	 *
	 * @param o The object to convert.
	 * @param type The type to convert to.
	 * @param <T> The type to convert to.
	 * @return The converted object, or <jk>null</jk> if the input is <jk>null</jk>.
	 * @throws InvalidConversionException If no conversion path exists from the input type to the target type.
	 */
	<T> T to(Object o, Class<T> type);

	/**
	 * Converts the specified object to the specified parameterized type.
	 *
	 * <p>
	 * This method allows conversion to complex parameterized types such as collections and maps.
	 * The <c>args</c> parameter specifies the type parameters of the main type.
	 *
	 * <p>
	 * Returns <jk>null</jk> only when the input object is <jk>null</jk>.
	 * Throws {@link InvalidConversionException} when no conversion path exists.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Convert to List&lt;String&gt;</jc>
	 * 	List&lt;String&gt; <jv>list</jv> = <jv>converter</jv>.to(<jv>o</jv>, List.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Convert to Map&lt;String,Integer&gt;</jc>
	 * 	Map&lt;String,Integer&gt; <jv>map</jv> = <jv>converter</jv>.to(<jv>o</jv>, Map.<jk>class</jk>, String.<jk>class</jk>, Integer.<jk>class</jk>);
	 * </p>
	 *
	 * @param o The object to convert.
	 * @param mainType The main type to convert to.
	 * @param args The type parameters of the main type.
	 * @param <T> The type to convert to.
	 * @return The converted object, or <jk>null</jk> if the input is <jk>null</jk>.
	 * @throws InvalidConversionException If no conversion path exists from the input type to the target type.
	 */
	<T> T to(Object o, Type mainType, Type...args);
}
