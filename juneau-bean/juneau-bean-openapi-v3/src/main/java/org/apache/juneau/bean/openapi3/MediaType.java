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
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.collections.*;

/**
 * Provides schema and examples for the media type identified by its key.
 *
 * <p>
 * Each Media Type Object provides schema and examples for the media type identified by its key (e.g., <js>"application/json"</js>).
 * Media types are used in request bodies and response content to describe the structure and format of the data being sent or received.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The Media Type Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>schema</c> ({@link SchemaInfo}) - The schema defining the content
 * 	<li><c>example</c> (any) - Example of the media type (mutually exclusive with <c>examples</c>)
 * 	<li><c>examples</c> (map of {@link Example}) - Examples of the media type (mutually exclusive with <c>example</c>)
 * 	<li><c>encoding</c> (map of {@link Encoding}) - A map between a property name and its encoding information
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a MediaType for JSON content</jc>
 * 	MediaType <jv>mediaType</jv> = <jk>new</jk> MediaType()
 * 		.setSchema(
 * 			<jk>new</jk> SchemaInfo()
 * 				.setType(<js>"object"</js>)
 * 				.setProperties(
 * 					JsonMap.<jsm>of</jsm>(
 * 						<js>"id"</js>, <jk>new</jk> SchemaInfo().setType(<js>"integer"</js>),
 * 						<js>"name"</js>, <jk>new</jk> SchemaInfo().setType(<js>"string"</js>)
 * 					)
 * 				)
 * 		)
 * 		.setExample(
 * 			JsonMap.<jsm>of</jsm>(<js>"id"</js>, 123, <js>"name"</js>, <js>"Fluffy"</js>)
 * 		);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#media-type-object">OpenAPI Specification &gt; Media Type Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/media-types/">OpenAPI Media Types</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class MediaType extends OpenApiElement {

	// Argument name constants for assertArgNotNull
	private static final String ARG_example = "example";
	private static final String ARG_key = "key";
	private static final String ARG_name = "name";
	private static final String ARG_property = "property";
	private static final String ARG_value = "value";

	// Property name constants
	private static final String PROP_encoding = "encoding";
	private static final String PROP_examples = "examples";
	private static final String PROP_schema = "schema";
	private static final String PROP_xExample = "x-example";

	private SchemaInfo schema;
	private Object example;
	private Map<String,Example> examples = map();
	private Map<String,Encoding> encoding = map();

	/**
	 * Default constructor.
	 */
	public MediaType() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public MediaType(MediaType copyFrom) {
		super(copyFrom);

		this.schema = copyFrom.schema;
		this.example = copyFrom.example;
		if (nn(copyFrom.examples))
			examples.putAll(copyOf(copyFrom.examples, Example::copy));
		if (nn(copyFrom.encoding))
			encoding.putAll(copyOf(copyFrom.encoding, Encoding::copy));
	}

	/**
	 * Adds one or more values to the <property>variables</property> property.
	 *
	 * @param key The mapping key.  Must not be <jk>null</jk>.
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return This object
	 */
	public MediaType addEncoding(String key, Encoding value) {
		assertArgNotNull(ARG_key, key);
		assertArgNotNull(ARG_value, value);
		encoding.put(key, value);
		return this;
	}

	/**
	 * Adds a single value to the <property>examples</property> property.
	 *
	 * @param name The example name.  Must not be <jk>null</jk>.
	 * @param example The example.  Must not be <jk>null</jk>.
	 * @return This object
	 */
	public MediaType addExample(String name, Example example) {
		assertArgNotNull(ARG_name, name);
		assertArgNotNull(ARG_example, example);
		examples.put(name, example);
		return this;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public MediaType copy() {
		return new MediaType(this);
	}

	@Override /* Overridden from OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull(ARG_property, property);
		return switch (property) {
			case PROP_encoding -> toType(getEncoding(), type);
			case PROP_examples -> toType(getExamples(), type);
			case PROP_schema -> toType(getSchema(), type);
			case PROP_xExample -> toType(getExample(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Bean property getter:  <property>variables</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Encoding> getEncoding() { return nullIfEmpty(encoding); }

	/**
	 * Bean property getter:  <property>x-example</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Beanp("x-example")
	public Object getExample() { return example; }

	/**
	 * Bean property getter:  <property>examples</property>.
	 *
	 * <p>
	 * The list of possible responses as they are returned from executing this operation.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Example> getExamples() { return nullIfEmpty(examples); }

	/**
	 * Bean property getter:  <property>schema</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public SchemaInfo getSchema() { return schema; }

	@Override /* Overridden from OpenApiElement */
	public Set<String> keySet() {
		// @formatter:off
		var s = setb(String.class)
			.addIf(ne(encoding), PROP_encoding)
			.addIf(ne(examples), PROP_examples)
			.addIf(nn(schema), PROP_schema)
			.addIf(nn(example), PROP_xExample)
			.build();
		// @formatter:on
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from OpenApiElement */
	public MediaType set(String property, Object value) {
		assertArgNotNull(ARG_property, property);
		return switch (property) {
			case PROP_encoding -> setEncoding(toMapBuilder(value, String.class, Encoding.class).sparse().build());
			case PROP_examples -> setExamples(toMapBuilder(value, String.class, Example.class).sparse().build());
			case PROP_schema -> setSchema(toType(value, SchemaInfo.class));
			case PROP_xExample -> setExample(value);
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	/**
	 * Bean property setter:  <property>variables</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public MediaType setEncoding(Map<String,Encoding> value) {
		encoding.clear();
		if (nn(value))
			encoding.putAll(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>examples</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	@Beanp("x-example")
	public MediaType setExample(Object value) {
		example = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>headers</property>.
	 *
	 * <p>
	 * A list of examples that are sent with the response.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public MediaType setExamples(Map<String,Example> value) {
		examples.clear();
		if (nn(value))
			examples.putAll(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>schema</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public MediaType setSchema(SchemaInfo value) {
		schema = value;
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public MediaType strict(Object value) {
		super.strict(value);
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	protected MediaType strict() {
		super.strict();
		return this;
	}
}