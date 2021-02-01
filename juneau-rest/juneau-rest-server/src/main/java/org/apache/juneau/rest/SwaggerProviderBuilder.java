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
import static org.apache.juneau.internal.ClassUtils.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.svl.*;

/**
 * Builder class for {@link SwaggerProvider} objects.
 */
public class SwaggerProviderBuilder {

	private Class<? extends SwaggerProvider> implClass;

	BeanFactory beanFactory;
	Class<?> resourceClass;
	VarResolver varResolver;
	JsonSchemaGenerator jsonSchemaGenerator;
	Messages messages;
	FileFinder fileFinder;

	/**
	 * Creates a new {@link SwaggerProvider} object from this builder.
	 *
	 * <p>
	 * Instantiates an instance of the {@link #implClass(Class) implementation class} or
	 * else {@link BasicSwaggerProvider} if implementation class was not specified.
	 *
	 * @return A new {@link SwaggerProvider} object.
	 */
	public SwaggerProvider build() {
		try {
			Class<? extends SwaggerProvider> ic = isConcrete(implClass) ? implClass : getDefaultImplClass();
			return BeanFactory.of(beanFactory).addBeans(SwaggerProviderBuilder.class, this).createBean(ic);
		} catch (Exception e) {
			throw toHttpException(e, InternalServerError.class);
		}
	}

	/**
	 * Specifies the default implementation class if not specified via {@link #implClass(Class)}.
	 *
	 * @return The default implementation class if not specified via {@link #implClass(Class)}.
	 */
	protected Class<? extends SwaggerProvider> getDefaultImplClass() {
		return BasicSwaggerProvider.class;
	}

	/**
	 * Specifies the bean factory to use for instantiating the {@link SwaggerProvider} object.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	public SwaggerProviderBuilder beanFactory(BeanFactory value) {
		this.beanFactory = value;
		return this;
	}

	/**
	 * Specifies a subclass of {@link SwaggerProvider} to create when the {@link #build()} method is called.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	public SwaggerProviderBuilder implClass(Class<? extends SwaggerProvider> value) {
		this.implClass = value;
		return this;
	}

	/**
	 * Specifies the variable resolver to use for the {@link SwaggerProvider} object.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	public SwaggerProviderBuilder varResolver(VarResolver value) {
		this.varResolver = value;
		return this;
	}

	/**
	 * Specifies the JSON-schema generator to use for the {@link SwaggerProvider} object.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	public SwaggerProviderBuilder jsonSchemaGenerator(JsonSchemaGenerator value) {
		this.jsonSchemaGenerator = value;
		return this;
	}

	/**
	 * Specifies the messages to use for the {@link SwaggerProvider} object.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	public SwaggerProviderBuilder messages(Messages value) {
		this.messages = value;
		return this;
	}

	/**
	 * Specifies the file-finder to use for the {@link SwaggerProvider} object.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	public SwaggerProviderBuilder fileFinder(FileFinder value) {
		this.fileFinder = value;
		return this;
	}
}
