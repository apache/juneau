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
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.common.internal.Utils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

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
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi3</a>
 * </ul>
 */
@FluentSetters
public class MediaType extends OpenApiElement{
	private SchemaInfo schema;
	private Object example;
	private Map<String,Example> examples;
	private Map<String,Encoding> encoding;

	/**
	 * Default constructor.
	 */
	public MediaType() { }

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public MediaType(MediaType copyFrom) {
		super(copyFrom);

		this.schema = copyFrom.schema;
		this.example = copyFrom.example;
		this.examples = copyOf(copyFrom.examples, Example::copy);
		this.encoding = copyOf(copyFrom.encoding, Encoding::copy);
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public MediaType copy() {
		return new MediaType(this);
	}

	@Override /* OpenApiElement */
	protected MediaType strict() {
		super.strict();
		return this;
	}

	@Override /* GENERATED - do not modify */
	public MediaType strict(Object value) {
		super.strict(value);
		return this;
	}

	/**
	 * Bean property getter:  <property>schema</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public SchemaInfo getSchema() {
		return schema;
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

	/**
	 * Bean property getter:  <property>x-example</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Beanp("x-example")
	public Object getExample() {
		return example;
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
	 * Bean property getter:  <property>variables</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String, Encoding> getEncoding() {
		return encoding;
	}

	/**
	 * Bean property setter:  <property>variables</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public MediaType setEncoding(Map<String, Encoding> value) {
		encoding = copyOf(value);
		return this;
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
		assertArgNotNull("key", key);
		assertArgNotNull("value", value);
		encoding = mapBuilder(encoding).sparse().add(key, value).build();
		return this;
	}

	/**
	 * Bean property getter:  <property>examples</property>.
	 *
	 * <p>
	 * The list of possible responses as they are returned from executing this operation.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Example> getExamples() {
		return examples;
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
		examples = copyOf(value);
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
		assertArgNotNull("name", name);
		assertArgNotNull("example", example);
		examples = mapBuilder(examples).add(name, example).build();
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "encoding" -> toType(getEncoding(), type);
			case "examples" -> toType(getExamples(), type);
			case "schema" -> toType(getSchema(), type);
			case "x-example" -> toType(getExample(), type);
			default -> super.get(property, type);
		};
	}

	@Override /* OpenApiElement */
	public MediaType set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "encoding" -> setEncoding(mapBuilder(String.class,Encoding.class).sparse().addAny(value).build());
			case "examples" -> setExamples(mapBuilder(String.class,Example.class).sparse().addAny(value).build());
			case "schema" -> setSchema(toType(value, SchemaInfo.class));
			case "x-example" -> setExample(value);
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	@Override /* OpenApiElement */
	public Set<String> keySet() {
		var s = setBuilder(String.class)
			.addIf(schema != null, "schema")
			.addIf(example != null, "x-example")
			.addIf(encoding != null, "encoding")
			.addIf(examples != null, "examples")
			.build();
		return new MultiSet<>(s, super.keySet());
	}
}