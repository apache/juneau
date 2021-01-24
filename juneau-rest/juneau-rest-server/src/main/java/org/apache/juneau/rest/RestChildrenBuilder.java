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
package org.apache.juneau.rest;

import static org.apache.juneau.rest.HttpRuntimeException.*;
import static org.apache.juneau.internal.ObjectUtils.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.exception.*;

/**
 * Builder for {@link RestChildren} objects.
 */
public class RestChildrenBuilder {

	final List<RestContext> list = AList.create();

	private BeanFactory beanFactory;
	private Class<? extends RestChildren> implClass;

	/**
	 * Instantiates a {@link RestChildren} object based on the contents of this builder.
	 *
	 * @return A new {@link RestChildren} object.
	 */
	public RestChildren build() {
		try {
			Class<? extends RestChildren> ic = firstNonNull(implClass, RestChildren.class);
			return BeanFactory.of(beanFactory).addBean(RestChildrenBuilder.class, this).createBean(ic);
		} catch (Exception e) {
			throw toHttpException(e, InternalServerError.class);
		}
	}

	/**
	 * Adds a child resource to this builder.
	 *
	 * @param childContext The REST context of the child resource.
	 * @return This object (for method chaining).
	 */
	public RestChildrenBuilder add(RestContext childContext) {
		this.list.add(childContext);
		return this;
	}


	/**
	 * Specifies a {@link RestChildren} implementation subclass to use.
	 *
	 * <p>
	 * When specified, the {@link #build()} method will create an instance of that class instead of the default {@link RestChildren}.
	 *
	 * <p>
	 * The subclass must have a public constructor that takes in any of the following arguments:
	 * <ul>
	 * 	<li>{@link RestChildrenBuilder} - This object.
	 * 	<li>Any beans found in the specified {@link #beanFactory(BeanFactory) bean factory}.
	 * 	<li>Any {@link Optional} beans that may or may not be found in the specified {@link #beanFactory(BeanFactory) bean factory}.
	 * </ul>
	 *
	 * @param implClass The implementation class to build.
	 * @return This object (for method chaining).
	 */
	public RestChildrenBuilder implClass(Class<? extends RestChildren> implClass) {
		this.implClass = implClass;
		return this;
	}

	/**
	 * Specifies a {@link BeanFactory} to use when resolving constructor arguments.
	 *
	 * @param beanFactory The bean factory to use for resolving constructor arguments.
	 * @return This object (for method chaining).
	 */
	public RestChildrenBuilder beanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		return this;
	}
}
