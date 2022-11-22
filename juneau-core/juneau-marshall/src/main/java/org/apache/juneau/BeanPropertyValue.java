// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau;

import org.apache.juneau.collections.*;

/**
 * Represents a simple bean property value and the meta-data associated with it.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class BeanPropertyValue implements Comparable<BeanPropertyValue> {

	private final BeanPropertyMeta pMeta;
	private final String name;
	private final Object value;
	private final Throwable thrown;

	/**
	 * Constructor.
	 *
	 * @param pMeta The bean property metadata.
	 * @param name The bean property name.
	 * @param value The bean property value.
	 * @param thrown The exception thrown by calling the property getter.
	 */
	public BeanPropertyValue(BeanPropertyMeta pMeta, String name, Object value, Throwable thrown) {
		this.pMeta = pMeta;
		this.name = name;
		this.value = value;
		this.thrown = thrown;
	}

	/**
	 * Returns the bean property metadata.
	 *
	 * @return The bean property metadata.
	 */
	public final BeanPropertyMeta getMeta() {
		return pMeta;
	}

	/**
	 * Returns the bean property metadata.
	 *
	 * @return The bean property metadata.
	 */
	public final ClassMeta<?> getClassMeta() {
		return pMeta.getClassMeta();
	}

	/**
	 * Returns the bean property name.
	 *
	 * @return The bean property name.
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Returns the bean property value.
	 *
	 * @return The bean property value.
	 */
	public final Object getValue() {
		return value;
	}

	/**
	 * Returns the exception thrown by calling the property getter.
	 *
	 * @return The exception thrown by calling the property getter.
	 */
	public final Throwable getThrown() {
		return thrown;
	}

	@Override /* Comparable */
	public int compareTo(BeanPropertyValue o) {
		return name.compareTo(o.name);
	}

	@Override /* Object */
	public String toString() {
		return JsonMap.create()
			.append("name", name)
			.append("value", value)
			.append("type", pMeta.getClassMeta().getInnerClass().getSimpleName())
			.toString();
	}
}
