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
package org.apache.juneau.rest.swagger;

import static org.apache.juneau.internal.CollectionUtils.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.http.response.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.svl.*;

/**
 * Interface for retrieving Swagger on a REST resource.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Swagger</a>
 * </ul>
 */
public interface SwaggerProvider {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Represents no SwaggerProvider.
	 *
	 * <p>
	 * Used on annotation to indicate that the value should be inherited from the parent class, and
	 * ultimately {@link BasicSwaggerProvider} if not specified at any level.
	 */
	public abstract class Void implements SwaggerProvider {};

	/**
	 * Static creator.
	 *
	 * @param beanStore The bean store to use for creating beans.
	 * @return A new builder for this object.
	 */
	public static Builder create(BeanStore beanStore) {
		return new Builder(beanStore);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	public class Builder {

		final BeanStore beanStore;
		Class<?> resourceClass;
		Supplier<VarResolver> varResolver;
		Supplier<JsonSchemaGenerator> jsonSchemaGenerator;
		Supplier<Messages> messages;
		Supplier<FileFinder> fileFinder;
		BeanCreator<SwaggerProvider> creator;

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BeanStore beanStore) {
			this.beanStore = beanStore;
			this.creator = beanStore.createBean(SwaggerProvider.class).type(BasicSwaggerProvider.class).builder(Builder.class, this);
		}

		/**
		 * Creates a new {@link SwaggerProvider} object from this builder.
		 *
		 * <p>
		 * Instantiates an instance of the {@link #type(Class) implementation class} or
		 * else {@link BasicSwaggerProvider} if implementation class was not specified.
		 *
		 * @return A new {@link SwaggerProvider} object.
		 */
		public SwaggerProvider build() {
			try {
				return creator.run();
			} catch (Exception e) {
				throw new InternalServerError(e);
			}
		}

		/**
		 * Returns the var resolver in this builder if it's been specified.
		 *
		 * @return The var resolver.
		 */
		public Optional<VarResolver> varResolver() {
			return optional(varResolver).map(x -> x.get());
		}

		/**
		 * Returns the JSON schema generator in this builder if it's been specified.
		 *
		 * @return The JSON schema generator.
		 */
		public Optional<JsonSchemaGenerator> jsonSchemaGenerator() {
			return optional(jsonSchemaGenerator).map(x -> x.get());
		}

		/**
		 * Returns the messages in this builder if it's been specified.
		 *
		 * @return The messages.
		 */
		public Optional<Messages> messages() {
			return optional(messages).map(x -> x.get());
		}

		/**
		 * Returns the file finder in this builder if it's been specified.
		 *
		 * @return The file finder.
		 */
		public Optional<FileFinder> fileFinder() {
			return optional(fileFinder).map(x -> x.get());
		}

		/**
		 * Specifies the default implementation class if not specified via {@link #type(Class)}.
		 *
		 * @return The default implementation class if not specified via {@link #type(Class)}.
		 */
		protected Class<? extends SwaggerProvider> getDefaultType() {
			return BasicSwaggerProvider.class;
		}

		/**
		 * Specifies a subclass of {@link SwaggerProvider} to create when the {@link #build()} method is called.
		 *
		 * @param value The new value for this setting.
		 * @return  This object.
		 */
		public Builder type(Class<? extends SwaggerProvider> value) {
			creator.type(value == null ? BasicSwaggerProvider.class : value);
			return this;
		}

		/**
		 * Specifies the variable resolver to use for the {@link SwaggerProvider} object.
		 *
		 * @param value The new value for this setting.
		 * @return  This object.
		 */
		public Builder varResolver(Supplier<VarResolver> value) {
			varResolver = value;
			return this;
		}

		/**
		 * Specifies the JSON-schema generator to use for the {@link SwaggerProvider} object.
		 *
		 * @param value The new value for this setting.
		 * @return  This object.
		 */
		public Builder jsonSchemaGenerator(Supplier<JsonSchemaGenerator> value) {
			jsonSchemaGenerator = value;
			return this;
		}

		/**
		 * Specifies the messages to use for the {@link SwaggerProvider} object.
		 *
		 * @param value The new value for this setting.
		 * @return  This object.
		 */
		public Builder messages(Supplier<Messages> value) {
			messages = value;
			return this;
		}

		/**
		 * Specifies the file-finder to use for the {@link SwaggerProvider} object.
		 *
		 * @param value The new value for this setting.
		 * @return  This object.
		 */
		public Builder fileFinder(Supplier<FileFinder> value) {
			fileFinder = value;
			return this;
		}

		/**
		 * Specifies an already-instantiated bean for the {@link #build()} method too return.
		 *
		 * @param value The new value for this setting.
		 * @return  This object.
		 */
		public Builder impl(SwaggerProvider value) {
			creator.impl(value);
			return this;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the Swagger associated with the specified {@link Rest}-annotated class.
	 *
	 * @param context The context of the {@link Rest}-annotated class.
	 * @param locale The request locale.
	 * @return A new {@link Swagger} DTO object.
	 * @throws Exception If an error occurred producing the Swagger.
	 */
	public Swagger getSwagger(RestContext context, Locale locale) throws Exception;

}
