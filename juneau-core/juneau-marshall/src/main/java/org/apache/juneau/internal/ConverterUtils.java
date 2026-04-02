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
package org.apache.juneau.internal;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.util.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.conversion.BasicConverter;
import org.apache.juneau.parser.*;
import org.apache.juneau.utils.*;

/**
 * Utility class for common type conversions and building typed collections and maps.
 */
public class ConverterUtils {

	/**
	 * Prevents instantiation.
	 */
	private ConverterUtils() {}

	/**
	 * Converts an object to a Boolean.
	 *
	 * @param o The object to convert.
	 * @return The converted object.
	 */
	public static Boolean toBoolean(Object o) {
		return BasicConverter.INSTANCE.to(o, Boolean.class);
	}

	/**
	 * Converts an object to an Integer.
	 *
	 * @param o The object to convert.
	 * @return The converted object.
	 */
	public static Integer toInteger(Object o) {
		return BasicConverter.INSTANCE.to(o, Integer.class);
	}

	/**
	 * Converts an object to a Number.
	 *
	 * @param o The object to convert.
	 * @return The converted object.
	 */
	public static Number toNumber(Object o) {
		if (o == null)
			return null;
		if (o instanceof Number o2)
			return o2;
		try {
			return parseNumber(o.toString(), null);
		} catch (ParseException e) {
			throw toRex(e);
		}
	}

	/**
	 * Converts the specified object to the specified type.
	 *
	 * <p>
	 * Uses the full {@link BeanContextConverter} to support bean-aware conversions such as
	 * {@link java.util.Map} to bean, bean to bean, and {@link org.apache.juneau.swap.ObjectSwap} transforms.
	 *
	 * @param <T> The class type to convert the value to.
	 * @param value The value to convert.
	 * @param type The class type to convert the value to.
	 * @return The converted value.
	 */
	public static <T> T toType(Object value, Class<T> type) {
		return BeanContextConverter.INSTANCE.to(value, type);
	}

	/**
	 * Converts the specified object to a {@link Lists} with elements of the specified type.
	 *
	 * <p>
	 * The input value can be any of the following:
	 * <ul>
	 * 	<li>An array of objects convertible to the element type
	 * 	<li>A {@link Collection} of objects convertible to the element type
	 * 	<li>A single object convertible to the element type (creates a list with one element)
	 * 	<li>A JSON array string that can be parsed into objects of the element type
	 * </ul>
	 *
	 * @param <T> The element type.
	 * @param value The value to convert. Can be <jk>null</jk>.
	 * @param type The element type class.
	 * @return A new {@link Lists} containing the converted elements.
	 */
	public static <T> Lists<T> toListBuilder(Object value, Class<T> type) {
		return listb(type).elementFunction(o -> BeanContextConverter.INSTANCE.to(o, type)).addAny(value);
	}

	/**
	 * Converts the specified object to a {@link Maps} with keys and values of the specified types.
	 *
	 * <p>
	 * The input value can be any of the following:
	 * <ul>
	 * 	<li>A {@link java.util.Map Map} with entries convertible to the key/value types
	 * 	<li>A JSON object string that can be parsed into a map with the specified key/value types
	 * 	<li>An object with bean properties that can be converted to map entries
	 * </ul>
	 *
	 * @param <K> The key type.
	 * @param <V> The value type.
	 * @param value The value to convert. Can be <jk>null</jk>.
	 * @param keyType The key type class.
	 * @param valueType The value type class.
	 * @return A new {@link Maps} containing the converted entries.
	 */
	public static <K,V> Maps<K,V> toMapBuilder(Object value, Class<K> keyType, Class<V> valueType) {
		return mapb(keyType, valueType)
			.keyFunction(o -> BeanContextConverter.INSTANCE.to(o, keyType))
			.valueFunction(o -> BeanContextConverter.INSTANCE.to(o, valueType))
			.addAny(value);
	}

	/**
	 * Converts the specified object to a {@link Sets} with elements of the specified type.
	 *
	 * <p>
	 * The input value can be any of the following:
	 * <ul>
	 * 	<li>An array of objects convertible to the element type
	 * 	<li>A {@link Collection} of objects convertible to the element type
	 * 	<li>A single object convertible to the element type (creates a set with one element)
	 * 	<li>A JSON array string that can be parsed into objects of the element type
	 * </ul>
	 *
	 * <p>
	 * Duplicate elements (after conversion) will be automatically removed as per {@link java.util.Set Set} semantics.
	 *
	 * @param <T> The element type.
	 * @param value The value to convert. Can be <jk>null</jk>.
	 * @param type The element type class.
	 * @return A new {@link Sets} containing the converted elements.
	 */
	public static <T> Sets<T> toSetBuilder(Object value, Class<T> type) {
		return setb(type).elementFunction(o -> BeanContextConverter.INSTANCE.to(o, type)).addAny(value);
	}
}