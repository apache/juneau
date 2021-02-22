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

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Builder for {@link BeanFactory} objects.
 */
@FluentSetters
public class BeanFactoryBuilder {

	private Class<? extends BeanFactory> implClass;
	Object outer;
	BeanFactory parent;

	/**
	 * Create a new {@link BeanFactory} using this builder.
	 *
	 * @return A new {@link BeanFactory}
	 */
	public BeanFactory build() {
		try {
			Class<? extends BeanFactory> ic = isConcrete(implClass) ? implClass : getDefaultImplClass();
			return new BeanFactory().addBeans(BeanFactoryBuilder.class, this).createBean(ic);
		} catch (ExecutableException e) {
			throw new RuntimeException(e.getCause().getMessage(), e.getCause());
		}
	}

	/**
	 * Specifies the default implementation class if not specified via {@link #implClass(Class)}.
	 *
	 * @return The default implementation class if not specified via {@link #implClass(Class)}.
	 */
	protected Class<? extends BeanFactory> getDefaultImplClass() {
		return BeanFactory.class;
	}

	/**
	 * Specifies a subclass of {@link BeanFactory} to create when the {@link #build()} method is called.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public BeanFactoryBuilder implClass(Class<? extends BeanFactory> value) {
		this.implClass = value;
		return this;
	}

	/**
	 * Specifies the parent bean factory.
	 *
	 * <p>
	 * Bean searches are performed recursively up this parent chain.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public BeanFactoryBuilder parent(BeanFactory value) {
		this.parent = value;
		return this;
	}

	/**
	 * Specifies the outer bean context.
	 *
	 * <p>
	 * Used when calling {@link BeanFactory#createBean(Class)} on a non-static inner class.
	 * This should be the instance of the outer object such as the servlet object when constructing inner classes
	 * of the servlet class.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public BeanFactoryBuilder outer(Object value) {
		this.outer = value;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>
}
