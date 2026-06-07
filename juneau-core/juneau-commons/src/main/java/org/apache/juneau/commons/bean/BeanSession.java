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
package org.apache.juneau.commons.bean;

import java.util.*;
/**
 * Minimum runtime surface that the bean-modeling layer needs from a marshalling session.
 *
 * <p>
 * {@code BeanSession} is an SPI seam between the bean-modeling runtime (in {@code commons.bean}) and the
 * marshalling stack (in {@code juneau-marshall}).  The marshalling layer's {@code MarshallingSession}
 * implements this interface so the bean-modeling types (notably {@code BeanMap} and {@code BeanPropertyMeta})
 * can request session-aware operations — type conversion, bean-map wrapping — without referencing any
 * marshalling-aware concrete types.
 *
 * <p>
 * On the commons-side construction path ({@code BeanMeta.of(Class, BeanConfigContext)}) the session is
 * <jk>null</jk> and these operations are skipped; the bean-modeling code runs in raw-reflection mode.
 *
 * <h5 class='section'>Method parameter typing:</h5>
 * <p>
 * The {@code targetType} parameters are typed as {@link Object} because the marshalling layer expresses them
 * as {@code ClassMeta<?>} — a marshalling-side type that {@code commons.bean} cannot reference.  Callers in
 * the bean-modeling layer pass through whatever opaque type-metadata handle they received; the
 * {@code MarshallingSession} implementation interprets it as a {@code ClassMeta}.
 *
 * <h5 class='topic'>Thread safety</h5>
 * Thread safety depends on implementation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link BeanConfigContext} — bean-modeling configuration counterpart.
 * </ul>
 */
public interface BeanSession {

	/**
	 * Converts the specified value to the specified target type.
	 *
	 * <p>
	 * Implementations apply any registered conversions, swaps, or coercions necessary to produce a value
	 * compatible with {@code targetType}.  On the marshalling-side, this routes through
	 * {@code MarshallingSession.convertToType(Object, ClassMeta)}.
	 *
	 * @param value The value to convert.  May be <jk>null</jk>.
	 * @param targetType
	 * 	The target type — typically a marshalling-side {@code ClassMeta<?>} handle passed through from
	 * 	a {@code BeanPropertyMeta} or {@code BeanMap}.  Must not be <jk>null</jk>.
	 * @return The converted value.
	 */
	Object convertToType(Object value, Object targetType);

	/**
	 * Converts the specified value to the specified target type, instantiating non-static inner classes against
	 * an outer instance.
	 *
	 * @param outer The outer-class instance for non-static inner classes.  May be <jk>null</jk>.
	 * @param value The value to convert.  May be <jk>null</jk>.
	 * @param targetType The target type — typically a marshalling-side {@code ClassMeta<?>}.  Must not be <jk>null</jk>.
	 * @return The converted value.
	 */
	Object convertToMemberType(Object outer, Object value, Object targetType);

	/**
	 * Parses the specified JSON-formatted character sequence into a {@link Map}.
	 *
	 * <p>
	 * Used by {@code BeanPropertyMeta.setPropertyValue} when a {@link CharSequence} value is supplied for a
	 * {@code Map}-typed property — the bean-modeling layer cannot reference the marshalling-side JSON parser
	 * directly, so the parse is delegated to the session via this SPI.
	 *
	 * <p>
	 * Implementations typically route through {@code JsonMap.ofString(value).session(this)}.
	 *
	 * @param value The JSON-formatted character sequence to parse.  Must not be <jk>null</jk>.
	 * @return The parsed map.
	 */
	@SuppressWarnings({
		"java:S1452" // Map<?,?> wildcard return intentional; the key/value types are determined by the JSON content at runtime
	})
	java.util.Map<?,?> parseToMap(CharSequence value);

	/**
	 * Parses the specified JSON-formatted character sequence into a {@link Collection}.
	 *
	 * <p>
	 * Used by {@code BeanPropertyMeta.setPropertyValue} when a {@link CharSequence} value is supplied for a
	 * {@code Collection}-typed property — the bean-modeling layer cannot reference the marshalling-side JSON
	 * parser directly, so the parse is delegated to the session via this SPI.
	 *
	 * <p>
	 * Implementations typically route through {@code new JsonList(value).setBeanSession(this)}.
	 *
	 * @param value The JSON-formatted character sequence to parse.  Must not be <jk>null</jk>.
	 * @return The parsed collection.
	 */
	@SuppressWarnings({
		"java:S1452" // Collection<?> wildcard return intentional; the element type is determined by the JSON content at runtime
	})
	java.util.Collection<?> parseToList(CharSequence value);

	/**
	 * Wraps the specified bean in a {@code BeanMap}.
	 *
	 * <p>
	 * Equivalent to {@code MarshallingSession.toBeanMap(Object)} on the marshalling-side.  The return type is
	 * typed as {@link Object} (rather than {@code BeanMap}) because the bean-modeling {@code BeanMap} runtime
	 * type currently lives in {@code juneau-marshall}; callers in {@code commons.bean} treat the returned value
	 * opaquely and pass it through to other {@code BeanSession} / bean-modeling APIs that re-narrow it.
	 *
	 * <p>
	 * The signature is generic to allow implementations (notably {@code MarshallingSession}) to satisfy this
	 * method with their existing covariant {@code <T> BeanMap<T> toBeanMap(T)} declaration without an
	 * additional bridge method.
	 *
	 * @param <T> The bean type.
	 * @param bean The bean to wrap.  Must not be <jk>null</jk>.
	 * @return A new bean map (typically a {@code BeanMap}) wrapping the supplied bean.
	 */
	<T> Object toBeanMap(T bean);
}
