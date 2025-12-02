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

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.utils.*;

/**
 * Utility class for efficiently converting objects between types.
 *
 * <p>
 * If the value isn't an instance of the specified type, then converts the value if possible.
 *
 * <p>
 * The following conversions are valid:
 * <table class='styled'>
 * 	<tr><th>Convert to type</th><th>Valid input value types</th><th>Notes</th></tr>
 * 	<tr>
 * 		<td>
 * 			A class that is the normal type of a registered {@link ObjectSwap}.
 * 		</td>
 * 		<td>
 * 			A value whose class matches the transformed type of that registered {@link ObjectSwap}.
 * 		</td>
 * 		<td>&nbsp;</td>
 * 	</tr>
 * 	<tr>
 * 		<td>
 * 			A class that is the transformed type of a registered {@link ObjectSwap}.
 * 		</td>
 * 		<td>
 * 			A value whose class matches the normal type of that registered {@link ObjectSwap}.
 * 		</td>
 * 		<td>&nbsp;</td>
 * 	</tr>
 * 	<tr>
 * 		<td>
 * 			{@code Number} (e.g. {@code Integer}, {@code Short}, {@code Float},...)
 * 			<br><code>Number.<jsf>TYPE</jsf></code> (e.g. <code>Integer.<jsf>TYPE</jsf></code>,
 * 			<code>Short.<jsf>TYPE</jsf></code>, <code>Float.<jsf>TYPE</jsf></code>,...)
 * 		</td>
 * 		<td>
 * 			{@code Number}, {@code String}, <jk>null</jk>
 * 		</td>
 * 		<td>
 * 			For primitive {@code TYPES}, <jk>null</jk> returns the JVM default value for that type.
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td>
 * 			{@code Map} (e.g. {@code Map}, {@code HashMap}, {@code TreeMap}, {@code JsonMap})
 * 		</td>
 * 		<td>
 * 			{@code Map}
 * 		</td>
 * 		<td>
 * 			If {@code Map} is not constructible, an {@code JsonMap} is created.
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td>
 * 			<c>Collection</c> (e.g. <c>List</c>, <c>LinkedList</c>, <c>HashSet</c>, <c>JsonList</c>)
 * 		</td>
 * 		<td>
 * 			<c>Collection&lt;Object&gt;</c>
 * 			<br><c>Object[]</c>
 * 		</td>
 * 		<td>
 * 			If <c>Collection</c> is not constructible, a <c>JsonList</c> is created.
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td>
 * 			<c>X[]</c> (array of any type X)
 * 		</td>
 * 		<td>
 * 			<c>List&lt;X&gt;</c>
 * 		</td>
 * 		<td>&nbsp;</td>
 * 	</tr>
 * 	<tr>
 * 		<td>
 * 			<c>X[][]</c> (multi-dimensional arrays)
 * 		</td>
 * 		<td>
 * 			<c>List&lt;List&lt;X&gt;&gt;</c>
 * 			<br><c>List&lt;X[]&gt;</c>
 * 			<br><c> List[]&lt;X&gt;</c>
 * 		</td>
 * 		<td>&nbsp;</td>
 * 	</tr>
 * 	<tr>
 * 		<td>
 * 			<c>Enum</c>
 * 		</td>
 * 		<td>
 * 			<c>String</c>
 * 		</td>
 * 		<td>&nbsp;</td>
 * 	</tr>
 * 	<tr>
 * 		<td>
 * 			Bean
 * 		</td>
 * 		<td>
 * 			<c>Map</c>
 * 		</td>
 * 		<td>&nbsp;</td>
 * 	</tr>
 * 	<tr>
 * 		<td>
 * 			<c>String</c>
 * 		</td>
 * 		<td>
 * 			Anything
 * 		</td>
 * 		<td>
 * 			Arrays are converted to JSON arrays
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td>
 * 			Anything with one of the following methods:
 * 			<br><code><jk>public static</jk> T fromString(String)</code>
 * 			<br><code><jk>public static</jk> T valueOf(String)</code>
 * 			<br><code><jk>public</jk> T(String)</code>
 * 		</td>
 * 		<td>
 * 			<c>String</c>
 * 		</td>
 * 		<td>
 * 			<br>
 * 		</td>
 * 	</tr>
 * </table>
 *
 */
public class ConverterUtils {

	// Session objects are usually not thread safe, but we're not using any feature
	// of bean sessions that would cause thread safety issues.
	private static final BeanSession session = BeanContext.DEFAULT_SESSION;

	/**
	 * Converts an object to a Boolean.
	 *
	 * @param o The object to convert.
	 * @return The converted object.
	 */
	public static Boolean toBoolean(Object o) {
		return toType(o, Boolean.class);
	}

	/**
	 * Converts an object to an Integer.
	 *
	 * @param o The object to convert.
	 * @return The converted object.
	 */
	public static Integer toInteger(Object o) {
		return toType(o, Integer.class);
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
	 * @param <T> The class type to convert the value to.
	 * @param value The value to convert.
	 * @param type The class type to convert the value to.
	 * @throws InvalidDataConversionException If the specified value cannot be converted to the specified type.
	 * @return The converted value.
	 */
	public static <T> T toType(Object value, Class<T> type) {
		return session.convertToType(value, type);
	}

	/**
	 * Converts the specified object to a {@link ListBuilder} with elements of the specified type.
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
	 * @return A new {@link ListBuilder} containing the converted elements.
	 */
	public static <T> ListBuilder<T> toListBuilder(Object value, Class<T> type) {
		return listb(type, GenericConverter.INSTANCE).addAny(value);
	}

	/**
	 * Converts the specified object to a {@link MapBuilder} with keys and values of the specified types.
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
	 * @return A new {@link MapBuilder} containing the converted entries.
	 */
	public static <K,V> MapBuilder<K,V> toMapBuilder(Object value, Class<K> keyType, Class<V> valueType) {
		return mapb(keyType, valueType, GenericConverter.INSTANCE).addAny(value);
	}

	/**
	 * Converts the specified object to a {@link SetBuilder} with elements of the specified type.
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
	 * @return A new {@link SetBuilder} containing the converted elements.
	 */
	public static <T> SetBuilder<T> toSetBuilder(Object value, Class<T> type) {
		return setb(type, GenericConverter.INSTANCE).addAny(value);
	}
}