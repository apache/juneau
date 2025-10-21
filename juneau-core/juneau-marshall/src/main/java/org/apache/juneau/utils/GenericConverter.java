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
package org.apache.juneau.utils;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;

/**
 * Generic object converter implementation.
 *
 * <p>
 * A simple implementation of the {@link Converter} interface that delegates to the default
 * {@link BeanContext} session for type conversion.
 *
 * <p>
 * This converter provides a convenient way to convert objects between different types using
 * the same conversion logic that's used throughout the Juneau framework.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Convert a string to an integer</jc>
 * 	Integer <jv>result</jv> = GenericConverter.INSTANCE.convertTo(Integer.<jk>class</jk>, <js>"123"</js>);
 * 	<jc>// result = 123</jc>
 *
 * 	<jc>// Convert a map to a bean</jc>
 * 	MyBean <jv>bean</jv> = GenericConverter.INSTANCE.convertTo(MyBean.<jk>class</jk>, <jv>map</jv>);
 * </p>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * This class is thread-safe. The singleton instance can be safely shared across multiple threads.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jm'>{@link Converter}
 * 	<li class='jm'>{@link BeanContext#DEFAULT_SESSION}
 * 	<li class='jm'>{@link BeanSession#convertToType(Object, Class)}
 * </ul>
 */
public class GenericConverter implements Converter {

	/**
	 * Singleton instance of the generic converter.
	 *
	 * <p>
	 * This instance can be safely shared across multiple threads and reused for all conversion operations.
	 */
	public static final GenericConverter INSTANCE = new GenericConverter();

	/**
	 * Converts the specified object to the specified type.
	 *
	 * <p>
	 * This method delegates to the default {@link BeanContext} session for the actual conversion logic.
	 * It supports all the same conversion types that are supported by the framework's bean conversion system.
	 *
	 * <p>
	 * Supported conversions include:
	 * <ul>
	 * 	<li>Primitive types and their wrapper classes
	 * 	<li>String to Number conversions
	 * 	<li>Map to Bean conversions
	 * 	<li>Collection to Array conversions
	 * 	<li>Enum conversions
	 * 	<li>Object swap conversions
	 * 	<li>And many more...
	 * </ul>
	 *
	 * @param <T> The target type to convert to.
	 * @param type The target class type.
	 * @param o The object to convert.
	 * @return The converted object, or <jk>null</jk> if the input object is <jk>null</jk>.
	 * @throws InvalidDataConversionException If the object cannot be converted to the specified type.
	 */
	@Override
	public <T> T convertTo(Class<T> type, Object o) {
		return BeanContext.DEFAULT_SESSION.convertToType(o, type);
	}
}
