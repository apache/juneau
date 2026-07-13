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
package org.apache.juneau.rest.server.openapi;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.bean.openapi3.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.cp.*;
import org.apache.juneau.marshall.jsonschema.*;
import org.apache.juneau.rest.server.*;

/**
 * Interface for retrieving an {@link OpenApi} 3.1 document from a REST resource.
 *
 * <p>
 * Sibling of {@link org.apache.juneau.rest.server.swagger.SwaggerProvider} for OpenAPI 3.1 emission. Both
 * providers coexist; the {@code @Rest(openApiProvider=...)} attribute selects the implementation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ApiDocsMixins">OpenAPI 3.1 Server Emission</a>
 * </ul>
 */
public interface OpenApiProvider {

	/**
	 * Builder class for {@link OpenApiProvider} instances.
	 */
	public class Builder {

		final BeanStore beanStore;
		Class<?> resourceClass;
		Supplier<VarResolver> varResolver;
		Supplier<JsonSchemaGenerator> jsonSchemaGenerator;
		Supplier<Messages> messages;
		Supplier<FileFinder> fileFinder;
		private OpenApiProvider impl;
		private Class<? extends OpenApiProvider> implType;

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BeanStore beanStore) {
			this.beanStore = beanStore;
		}

		/**
		 * Creates a new {@link OpenApiProvider} object from this builder.
		 *
		 * <p>
		 * Instantiates an instance of the {@link #type(Class) implementation class} or
		 * else {@link BasicOpenApiProvider} if implementation class was not specified.
		 *
		 * @return A new {@link OpenApiProvider} object.
		 */
		public OpenApiProvider build() {
			try {
				if (impl != null)
					return impl;
				var t = implType != null ? implType : BasicOpenApiProvider.class;
				return BeanInstantiator.of(OpenApiProvider.class, beanStore)
					.type(t)
					.noBuilder()
					.addBean(Builder.class, this)
					.run();
			} catch (Exception e) {
				throw new InternalServerError(e);
			}
		}

		/**
		 * Returns the file finder in this builder if it's been specified.
		 *
		 * @return The file finder.
		 */
		public Optional<FileFinder> fileFinder() {
			return o(fileFinder).map(Supplier::get);
		}

		/**
		 * Specifies the file-finder to use for the {@link OpenApiProvider} object.
		 *
		 * @param value The new value for this setting.
		 * @return  This object.
		 */
		public Builder fileFinder(Supplier<FileFinder> value) {
			fileFinder = value;
			return this;
		}

		/**
		 * Specifies an already-instantiated bean for the {@link #build()} method to return.
		 *
		 * @param value The new value for this setting.
		 * @return  This object.
		 */
		public Builder impl(OpenApiProvider value) {
			this.impl = value;
			return this;
		}

		/**
		 * Returns the JSON schema generator in this builder if it's been specified.
		 *
		 * @return The JSON schema generator.
		 */
		public Optional<JsonSchemaGenerator> jsonSchemaGenerator() {
			return o(jsonSchemaGenerator).map(Supplier::get);
		}

		/**
		 * Specifies the JSON-schema generator to use for the {@link OpenApiProvider} object.
		 *
		 * @param value The new value for this setting.
		 * @return  This object.
		 */
		public Builder jsonSchemaGenerator(Supplier<JsonSchemaGenerator> value) {
			jsonSchemaGenerator = value;
			return this;
		}

		/**
		 * Returns the messages in this builder if it's been specified.
		 *
		 * @return The messages.
		 */
		public Optional<Messages> messages() {
			return o(messages).map(Supplier::get);
		}

		/**
		 * Specifies the messages to use for the {@link OpenApiProvider} object.
		 *
		 * @param value The new value for this setting.
		 * @return  This object.
		 */
		public Builder messages(Supplier<Messages> value) {
			messages = value;
			return this;
		}

		/**
		 * Specifies a subclass of {@link OpenApiProvider} to create when the {@link #build()} method is called.
		 *
		 * @param value The new value for this setting.
		 * @return  This object.
		 */
		public Builder type(Class<? extends OpenApiProvider> value) {
			implType = o(value).isPresent() ? value : BasicOpenApiProvider.class;
			return this;
		}

		/**
		 * Returns the var resolver in this builder if it's been specified.
		 *
		 * @return The var resolver.
		 */
		public Optional<VarResolver> varResolver() {
			return o(varResolver).map(Supplier::get);
		}

		/**
		 * Specifies the variable resolver to use for the {@link OpenApiProvider} object.
		 *
		 * @param value The new value for this setting.
		 * @return  This object.
		 */
		public Builder varResolver(Supplier<VarResolver> value) {
			varResolver = value;
			return this;
		}

		/**
		 * Specifies the default implementation class if not specified via {@link #type(Class)}.
		 *
		 * @return The default implementation class if not specified via {@link #type(Class)}.
		 */
		protected Class<? extends OpenApiProvider> getDefaultType() { return BasicOpenApiProvider.class; }
	}

	/**
	 * Represents no OpenApiProvider.
	 *
	 * <p>
	 * Used on annotation to indicate that the value should be inherited from the parent class, and
	 * ultimately {@link BasicOpenApiProvider} if not specified at any level.
	 */
	public abstract class Void implements OpenApiProvider {}

	/**
	 * Static creator.
	 *
	 * @param beanStore The bean store to use for creating beans.
	 * @return A new builder for this object.
	 */
	static Builder create(BeanStore beanStore) {
		return new Builder(beanStore);
	}

	/**
	 * Returns the OpenAPI 3.1 document associated with the specified {@link Rest}-annotated class.
	 *
	 * @param context The context of the {@link Rest}-annotated class.
	 * @param locale The request locale.
	 * @return A new {@link OpenApi} DTO object.
	 * @throws Exception If an error occurred producing the document.
	 */
	@SuppressWarnings({
		"java:S112" // throws Exception intentional - callback/lifecycle method
	})
	OpenApi getOpenApi(RestContext context, Locale locale) throws Exception;
}
