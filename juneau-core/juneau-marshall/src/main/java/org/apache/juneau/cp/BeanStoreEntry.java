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
package org.apache.juneau.cp;

import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;

import java.util.function.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;

/**
 * Represents a bean in a {@link BeanStore}.
 *
 * <p>
 * A bean entry consists of the following:
 * <ul>
 * 	<li>A class type.
 * 	<li>A bean or bean supplier that returns an instance of the class type.  This can be a subclass of the type.
 * 	<li>An optional name.
 * </ul>
 *
 * @param <T> The bean type.
 */
public class BeanStoreEntry<T> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param <T> The class type to associate with the bean.
	 * @param type The class type to associate with the bean.
	 * @param bean The bean supplier.
	 * @param name Optional name to associate with the bean.  Can be <jk>null</jk>.
	 * @return A new bean store entry.
	 */
	public static <T> BeanStoreEntry<T> create(Class<T> type, Supplier<T> bean, String name) {
		return new BeanStoreEntry<>(type, bean, name);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	final Supplier<T> bean;
	final Class<T> type;
	final String name;

	/**
	 * Constructor.
	 *
	 * @param type The class type to associate with the bean.
	 * @param bean The bean supplier.
	 * @param name Optional name to associate with the bean.  Can be <jk>null</jk>.
	 */
	protected BeanStoreEntry(Class<T> type, Supplier<T> bean, String name) {
		this.bean = assertArgNotNull("bean", bean);
		this.type = assertArgNotNull("type", type);
		this.name = nullIfEmpty(name);
	}

	/**
	 * Returns <jk>true</jk> if this bean is exactly of the specified type.
	 *
	 * @param type The class to check.  Returns <jk>false</jk> if <jk>null</jk>.
	 * @return <jk>true</jk> if this bean is exactly of the specified type.
	 */
	public boolean matches(Class<?> type) {
		return this.type.equals(type);
	}

	/**
	 * Returns <jk>true</jk> if this bean is exactly of the specified type and has the specified name.
	 *
	 * @param type The class to check.  Returns <jk>false</jk> if <jk>null</jk>.
	 * @param name The name to check.  Can be <jk>null</jk> to only match if name of entry is <jk>null</jk>.
	 * @return <jk>true</jk> if this bean is exactly of the specified type and has the specified name.
	 */
	public boolean matches(Class<?> type, String name) {
		name = nullIfEmpty(name);
		return matches(type) && StringUtils.eq(this.name, name);
	}

	/**
	 * Returns the bean associated with this entry.
	 *
	 * @return The bean associated with this entry.
	 */
	public T get() {
		return bean.get();
	}

	/**
	 * Returns the type this bean is associated with.
	 *
	 * @return The type this bean is associated with.
	 */
	public Class<T> getType() {
		return type;
	}

	/**
	 * Returns the name associated with this entry.
	 *
	 * @return the name associated with this entry.  <jk>null</jk> if no name is associated.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the properties in this object as a simple map for debugging purposes.
	 *
	 * @return The properties in this object as a simple map.
	 */
	protected JsonMap properties() {
		return filteredMap()
			.append("type", simpleClassName(getType()))
			.append("bean", ObjectUtils.identity(get()))
			.append("name", getName());
	}
}