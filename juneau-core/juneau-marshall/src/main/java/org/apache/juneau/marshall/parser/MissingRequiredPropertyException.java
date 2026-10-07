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
package org.apache.juneau.marshall.parser;

import java.util.*;

import org.apache.juneau.commons.bean.*;

/**
 * Thrown when parser input (or the source map of a {@code Map}-to-bean conversion) does not contain one or more
 * properties marked {@link BeanProp#required() @BeanProp(required=true)}.
 *
 * <p>
 * The check runs once per bean, after all input properties have been read and before the bean is constructed, so
 * records and constructor-argument beans get this exception instead of a constructor failure.  An explicit
 * <jk>null</jk> value counts as present.
 *
 * <p>
 * This is a {@link ParseException}, so REST {@code @Content} parameters map it to <c>400 Bad Request</c>.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>public record</jk> Person(<ja>@BeanProp</ja>(required=<jk>true</jk>) String <jv>name</jv>, Integer <jv>age</jv>) {}
 *
 * 	<jk>try</jk> {
 * 		JsonParser.<jsf>DEFAULT</jsf>.read(<js>"{\"age\":30}"</js>, Person.<jk>class</jk>);
 * 	} <jk>catch</jk> (MissingRequiredPropertyException <jv>e</jv>) {
 * 		<jv>e</jv>.getBeanClass();       <jc>// Person.class</jc>
 * 		<jv>e</jv>.getPropertyNames();   <jc>// [name]</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link BeanProp#required()}
 * </ul>
 *
 * @serial exclude
 */
@SuppressWarnings("java:S110") // Inherited hierarchy depth from ParseException; acceptable for a specific parse failure.
public class MissingRequiredPropertyException extends ParseException {

	private static final long serialVersionUID = 1L;

	/**
	 * Throws this exception if the bean map is missing required properties.
	 *
	 * <p>
	 * Parser sessions call this through {@code ParserSession.checkRequired(BeanMap)}.  Conversion code that has no
	 * parser session passes <jk>null</jk>, which omits the input position from the message.
	 *
	 * @param <T> The bean type.
	 * @param session The current parser session, or <jk>null</jk> if not parsing.
	 * @param m The bean map to check.  Can be <jk>null</jk>.
	 * @return The same bean map, for chaining.
	 * @throws MissingRequiredPropertyException If a required property was never written.
	 */
	public static <T> BeanMap<T> check(ParserSession session, BeanMap<T> m) {
		if (m == null)
			return null;
		var missing = m.getMissingRequiredProperties();
		if (! missing.isEmpty())
			throw new MissingRequiredPropertyException(session, m.getMeta().getClassInfo().inner(), missing);
		return m;
	}

	/**
	 * Finds a {@link MissingRequiredPropertyException} in the cause chain of a throwable.
	 *
	 * <p>
	 * Bean property setters wrap failures from nested conversions, so conversion code uses this to re-surface the
	 * original exception (which is a {@link ParseException} and maps to HTTP 400).
	 *
	 * @param t The throwable to inspect.  Can be <jk>null</jk>.
	 * @return The first exception of this type in the chain (including <c>t</c> itself), or <jk>null</jk> if none.
	 */
	public static MissingRequiredPropertyException find(Throwable t) {
		for (var i = 0; t != null && i < 32; i++, t = t.getCause())
			if (t instanceof MissingRequiredPropertyException e)
				return e;
		return null;
	}

	private final Class<?> beanClass;
	private final String[] propertyNames;  // Array (not List) so the exception stays serializable.

	/**
	 * Constructor.
	 *
	 * @param session The parser session, or <jk>null</jk>.  Used to add the input position to the message.
	 * @param beanClass The bean class.
	 * @param propertyNames The missing property names.
	 */
	public MissingRequiredPropertyException(ParserSession session, Class<?> beanClass, List<String> propertyNames) {
		super(session, "Missing required properties on bean class '%s': %s", beanClass.getSimpleName(), propertyNames);
		this.beanClass = beanClass;
		this.propertyNames = propertyNames.toArray(String[]::new);
	}

	/**
	 * Returns the bean class whose required properties were missing.
	 *
	 * @return The bean class.  Never <jk>null</jk>.
	 */
	public Class<?> getBeanClass() { return beanClass; }

	/**
	 * Returns the names of the missing required properties, in bean property order.
	 *
	 * @return An unmodifiable list.  Never <jk>null</jk> or empty.
	 */
	public List<String> getPropertyNames() { return List.of(propertyNames); }
}
