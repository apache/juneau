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
package org.apache.juneau.html;

import org.apache.juneau.commons.http.MediaType;
import java.lang.annotation.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.xml.*;

/**
 * Serializes POJO metamodels to HTML.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Accept</c> types:  <bc>text/html+schema</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/html</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Essentially the same as {@link HtmlSerializer}, except serializes the POJO metamodel instead of the model itself.
 *
 * <p>
 * Produces output that describes the POJO metamodel similar to an XML schema document.
 *
 * <p>
 * The easiest way to create instances of this class is through the {@link HtmlSerializer#getSchemaSerializer()},
 * which will create a schema serializer with the same settings as the originating serializer.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HtmlBasics">HTML Basics</a>

 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for HtmlSchemaSerializer hierarchy
	"java:S115"  // Constants use UPPER_snakeCase convention (e.g., PROP_generator)
})
public class HtmlSchemaSerializer extends HtmlSerializer {

	// Property name constants
	private static final String PROP_generator = "generator";

	/**
	 * Builder class.
	 */
	public static class Builder extends HtmlSerializer.Builder<Builder> {

		private static final Cache<HashKey,HtmlSchemaSerializer> CACHE = Cache.of(HashKey.class, HtmlSchemaSerializer.class).build();

		JsonSchemaGenerator.Builder generatorBuilder;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("text/html");
			accept("text/html+schema");
			generatorBuilder = JsonSchemaGenerator.create().marshallingContext(marshallingContext());
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			generatorBuilder = copyFrom.generatorBuilder.copy().marshallingContext(marshallingContext());
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(HtmlSchemaSerializer copyFrom) {
			super(copyFrom);
			generatorBuilder = copyFrom.generator.copy().marshallingContext(marshallingContext());
		}

		/**
		 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Add descriptions.
		 *
		 * <p>
		 * Identifies which categories of types that descriptions should be automatically added to generated schemas.
		 * <p>
		 * The description is the result of calling {@link ClassMeta#getName()}.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#addDescriptionsTo(TypeCategory...)}
		 * </ul>
		 *
		 * @param values
		 * 	The values to add to this setting.
		 * 	<br>The default is an empty string.
		 * @return This object.
		 */
		public Builder addDescriptionsTo(TypeCategory...values) {
			generatorBuilder.addDescriptionsTo(values);
			return this;
		}

		/**
		 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Add examples.
		 *
		 * <p>
		 * Identifies which categories of types that examples should be automatically added to generated schemas.
		 * <p>
		 * The examples come from calling {@link ClassMeta#getExample(MarshallingSession,JsonParserSession)} which in turn gets examples
		 * from the following:
		 * <ul class='javatree'>
		 * 	<li class='ja'>{@link Example}
		 * 	<li class='ja'>{@link Marshalled#example() Marshalled(example)}
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#addExamplesTo(TypeCategory...)}
		 * </ul>
		 *
		 * @param values
		 * 	The values to add to this setting.
		 * 	<br>The default is an empty string.
		 * @return This object.
		 */
		public Builder addExamplesTo(TypeCategory...values) {
			generatorBuilder.addExamplesTo(values);
			return this;
		}

		/**
		 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Allow nested descriptions.
		 *
		 * <p>
		 * Identifies whether nested descriptions are allowed in schema definitions.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#allowNestedDescriptions()}
		 * </ul>
		 *
		 * @return This object.
		 */
		public Builder allowNestedDescriptions() {
			generatorBuilder.allowNestedDescriptions();
			return this;
		}

		/**
		 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Allow nested examples.
		 *
		 * <p>
		 * Identifies whether nested examples are allowed in schema definitions.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#allowNestedExamples()}
		 * </ul>
		 *
		 * @return This object.
		 */
		public Builder allowNestedExamples() {
			generatorBuilder.allowNestedExamples();
			return this;
		}

		/**
		 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Schema definition mapper.
		 *
		 * <p>
		 * Interface to use for converting Bean classes to definition IDs and URIs.
		 * <p>
		 * Used primarily for defining common definition sections for beans in Swagger JSON.
		 * <p>
		 * This setting is ignored if {@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#useBeanDefs()} is not enabled.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link org.apache.juneau.jsonschema.JsonSchemaGenerator.Builder#beanDefMapper(Class)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is {@link org.apache.juneau.jsonschema.BasicBeanDefMapper}.
		 * @return This object.
		 */
		public Builder beanDefMapper(Class<? extends MarshallingDefMapper> value) {
			generatorBuilder.beanDefMapper(value);
			return this;
		}

		@Override /* Overridden from Context.Builder */
		public HtmlSchemaSerializer build() {
			return cache(CACHE).build(HtmlSchemaSerializer.class);
		}

		@Override /* Overridden from Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Overridden from Context.Builder */
		public HashKey hashKey() {
			// @formatter:off
			return HashKey.of(
				super.hashKey(),
				generatorBuilder.hashKey()
			);
			// @formatter:on
		}

		/**
		 * <i><l>HtmlSchemaSerializer</l> configuration property:&emsp;</i>  Use bean definitions.
		 *
		 * <p>
		 * When enabled, schemas on beans will be serialized as the following:
		 * <p class='bjson'>
		 * 	{
		 * 		type: <js>'object'</js>,
		 * 		<js>'$ref'</js>: <js>'#/definitions/TypeId'</js>
		 * 	}
		 * </p>
		 *
		 * @return This object.
		 */
		public Builder useBeanDefs() {
			generatorBuilder.useBeanDefs();
			return this;
		}


	}

	/** Default serializer, with whitespace. */
	public static class Readable extends HtmlSchemaSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Readable(Builder builder) {
			super(builder.useWhitespace());
		}
	}

	/** Default serializer, single quotes, simple mode. */
	public static class Simple extends HtmlSchemaSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Simple(Builder builder) {
			super(builder.quoteChar('\''));
		}
	}

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static class SimpleReadable extends HtmlSchemaSerializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public SimpleReadable(Builder builder) {
			super(builder.quoteChar('\'').useWhitespace());
		}
	}

	/** Default serializer, all default settings.*/
	public static final HtmlSchemaSerializer DEFAULT = new HtmlSchemaSerializer(create());
	/** Default serializer, all default settings.*/
	public static final HtmlSchemaSerializer DEFAULT_READABLE = new Readable(create());

	/** Default serializer, single quotes, simple mode. */
	public static final HtmlSchemaSerializer DEFAULT_SIMPLE = new Simple(create());

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static final HtmlSchemaSerializer DEFAULT_SIMPLE_READABLE = new SimpleReadable(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	final JsonSchemaGenerator generator;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this serializer.
	 */
	public HtmlSchemaSerializer(Builder builder) {
		super(builder.detectRecursions().ignoreRecursions());

		generator = builder.generatorBuilder.build();
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public HtmlSchemaSerializerSession.Builder createSession() {
		return HtmlSchemaSerializerSession.create(this);
	}

	@Override /* Overridden from Context */
	public HtmlSchemaSerializerSession getSession() { return createSession().build(); }

	@Override /* Overridden from HtmlSerializer */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_generator, generator);
	}

	JsonSchemaGenerator getGenerator() { return generator; }
}