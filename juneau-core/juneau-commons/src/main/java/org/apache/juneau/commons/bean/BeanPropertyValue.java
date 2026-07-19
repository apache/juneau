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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import org.apache.juneau.commons.collections.*;

/**
 * Represents a simple bean property value and the meta-data associated with it.
 *
 * <h5 class='topic'>Thread safety</h5>
 *
 * Instances are immutable and thread-safe.
 */
@SuppressWarnings({
	"java:S115",   // Constants use UPPER_snakeCase convention (e.g., PROP_name)
	"java:S1452"  // Wildcard required - Class<?> for property type metadata
})
public class BeanPropertyValue implements Comparable<BeanPropertyValue> {

	// Property name constants
	private static final String PROP_name = "name";
	private static final String PROP_value = "value";
	private static final String PROP_type = "type";

	private final BeanPropertyMeta pMeta;
	private final String name;
	private final Object value;
	private final Throwable thrown;

	/**
	 * Constructor.
	 *
	 * @param pMeta The bean property metadata.
	 * @param name The bean property name.
	 * @param value The bean property value.  Can be <jk>null</jk>.
	 * @param thrown The exception thrown by calling the property getter.  Can be <jk>null</jk> if no exception was thrown.
	 */
	public BeanPropertyValue(BeanPropertyMeta pMeta, String name, Object value, Throwable thrown) {
		this.pMeta = pMeta;
		this.name = name;
		this.value = value;
		this.thrown = thrown;
	}

	@Override /* Overridden from Comparable */
	public int compareTo(BeanPropertyValue o) {
		return name.compareTo(o.name);
	}

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		return o instanceof BeanPropertyValue o2 && compareTo(o2) == 0;
	}

	@Override /* Overridden from Object */
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	/**
	 * Returns the {@link BeanInfo} of the bean property.
	 *
	 * <p>
	 * Returns the bean-modeling-side SPI type ({@link BeanInfo}).  Marshalling-side callers that need the
	 * {@code ClassMeta} narrowing must cast — the concrete instance in-tree is always a
	 * {@code ClassMeta}.
	 *
	 * @return The bean property type info.
	 */
	public final BeanInfo<?> getBeanInfo() { return pMeta.getBeanInfo(); }

	/**
	 * Returns the bean property metadata.
	 *
	 * @return The bean property metadata.
	 */
	public final BeanPropertyMeta getMeta() { return pMeta; }

	/**
	 * Returns the bean property name.
	 *
	 * @return The bean property name.
	 */
	public final String getName() { return name; }

	/**
	 * Returns the exception thrown by calling the property getter.
	 *
	 * @return The exception thrown by calling the property getter.
	 */
	public final Throwable getThrown() { return thrown; }

	/**
	 * Returns the bean property value.
	 *
	 * @return The bean property value.
	 */
	public final Object getValue() { return value; }

	/**
	 * Returns a property map view of this object.
	 *
	 * @return A property map containing the name, value, and type of this bean property.
	 */
	public FluentMap<String,Object> properties() {
		// @formatter:off
		return filteredBeanPropertyMap()
			.a(PROP_name, name)
			.a(PROP_value, value)
			.a(PROP_type, cns(pMeta.getBeanInfo()));
		// @formatter:on
	}

	@Override /* Overridden from Object */
	public String toString() {
		return r(properties());
	}
}