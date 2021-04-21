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

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.ExceptionUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Builder for {@link BeanStore} objects.
 */
@FluentSetters
public class BeanStoreBuilder {

	private Class<? extends BeanStore> implClass;
	Object outer;
	BeanStore parent;

	/**
	 * Create a new {@link BeanStore} using this builder.
	 *
	 * @return A new {@link BeanStore}
	 */
	public BeanStore build() {
		try {
			Class<? extends BeanStore> ic = isConcrete(implClass) ? implClass : getDefaultImplClass();
			return new BeanStore().addBeans(BeanStoreBuilder.class, this).createBean(ic);
		} catch (ExecutableException e) {
			throw runtimeException(e);
		}
	}

	/**
	 * Specifies the default implementation class if not specified via {@link #implClass(Class)}.
	 *
	 * @return The default implementation class if not specified via {@link #implClass(Class)}.
	 */
	protected Class<? extends BeanStore> getDefaultImplClass() {
		return BeanStore.class;
	}

	/**
	 * Specifies a subclass of {@link BeanStore} to create when the {@link #build()} method is called.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public BeanStoreBuilder implClass(Class<? extends BeanStore> value) {
		this.implClass = value;
		return this;
	}

	/**
	 * Specifies the parent bean store.
	 *
	 * <p>
	 * Bean searches are performed recursively up this parent chain.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public BeanStoreBuilder parent(BeanStore value) {
		this.parent = value;
		return this;
	}

	/**
	 * Specifies the outer bean context.
	 *
	 * <p>
	 * Used when calling {@link BeanStore#createBean(Class)} on a non-static inner class.
	 * This should be the instance of the outer object such as the servlet object when constructing inner classes
	 * of the servlet class.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public BeanStoreBuilder outer(Object value) {
		this.outer = value;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>
}
