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
package org.apache.juneau.dto.swagger;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Describes a single response from an API Operation.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	ResponseInfo <jv>info</jv> = <jsm>responseInfo</jsm>(<js>"A complex object array response"</js>)
 * 		.schema(
 * 			<jsm>schemaInfo</jsm>
 * 				.type(<js>"array"</js>)
 * 				.items(
 * 					<jsm>items</jsm>()
 * 						.set(<js>"$ref"</js>, <js>"#/definitions/VeryComplexType"</js>)
 * 				)
 * 		);
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.toString(<jv>info</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	<jv>json</jv> = <jv>info</jv>.toString();
 * </p>
 * <p class='bjson'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"description"</js>: <js>"A complex object array response"</js>,
 * 		<js>"schema"</js>: {
 * 			<js>"type"</js>: <js>"array"</js>,
 * 			<js>"items"</js>: {
 * 				<js>"$ref"</js>: <js>"#/definitions/VeryComplexType"</js>
 * 			}
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Overview &gt; juneau-rest-server &gt; Swagger</a>
 * </ul>
 */
@Bean(properties="description,schema,headers,examples,*")
@FluentSetters
public class ResponseInfo extends SwaggerElement {

	private String description;
	private SchemaInfo schema;
	private Map<String,HeaderInfo> headers;
	private Map<String,Object> examples;

	/**
	 * Default constructor.
	 */
	public ResponseInfo() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public ResponseInfo(ResponseInfo copyFrom) {
		super(copyFrom);

		this.description = copyFrom.description;
		this.schema = copyFrom.schema == null ? null : copyFrom.schema.copy();

		this.examples = copyOf(copyFrom.examples);

		if (copyFrom.headers == null) {
			this.headers = null;
		} else {
			this.headers = map();
			copyFrom.headers.forEach((k,v) -> this.headers.put(k, v.copy()));
		}

	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public ResponseInfo copy() {
		return new ResponseInfo(this);
	}

	/**
	 * Copies any non-null fields from the specified object to this object.
	 *
	 * @param r
	 * 	The object to copy fields from.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object.
	 */
	public ResponseInfo copyFrom(ResponseInfo r) {
		if (r != null) {
			if (r.description != null)
				description = r.description;
			if (r.schema != null)
				schema = r.schema;
			if (r.headers != null)
				headers = r.headers;
			if (r.examples != null)
				examples = r.examples;
		}
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * <p>
	 * A short description of the response.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 *
	 * <p>
	 * A short description of the response.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br><a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used for rich text representation.
	 * 	<br>Property value is required.
	 * @return This object.
	 */
	public ResponseInfo setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>examples</property>.
	 *
	 * <p>
	 * An example of the response message.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Object> getExamples() {
		return examples;
	}

	/**
	 * Bean property setter:  <property>examples</property>.
	 *
	 * <p>
	 * An example of the response message.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Keys must be MIME-type strings.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public ResponseInfo setExamples(Map<String,Object> value) {
		examples = copyOf(value);
		return this;
	}

	/**
	 * Bean property appender:  <property>examples</property>.
	 *
	 * <p>
	 * Adds a single value to the <property>examples</property> property.
	 *
	 * @param mimeType The mime-type string.
	 * @param example The example.
	 * @return This object.
	 */
	public ResponseInfo addExample(String mimeType, Object example) {
		examples =  mapBuilder(examples).sparse().add(mimeType, example).build();
		return this;
	}

	/**
	 * Bean property getter:  <property>headers</property>.
	 *
	 * <p>
	 * A list of headers that are sent with the response.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,HeaderInfo> getHeaders() {
		return headers;
	}

	/**
	 * Bean property setter:  <property>headers</property>.
	 *
	 * <p>
	 * A list of headers that are sent with the response.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public ResponseInfo setHeaders(Map<String,HeaderInfo> value) {
		headers = copyOf(value);
		return this;
	}

	/**
	 * Bean property appender:  <property>headers</property>.
	 *
	 * @param name The header name.
	 * @param header The header descriptions
	 * @return This object.
	 */
	public ResponseInfo addHeader(String name, HeaderInfo header) {
		headers = mapBuilder(headers).add(name, header).build();
		return this;
	}

	/**
	 * Returns the header information with the specified name.
	 *
	 * @param name The header name.
	 * @return The header info, or <jk>null</jk> if not found.
	 */
	public HeaderInfo getHeader(String name) {
		return getHeaders().get(name);
	}

	/**
	 * Bean property getter:  <property>schema</property>.
	 *
	 * <p>
	 * A definition of the response structure.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public SchemaInfo getSchema() {
		return schema;
	}

	/**
	 * Bean property setter:  <property>schema</property>.
	 *
	 * <p>
	 * A definition of the response structure.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>It can be a primitive, an array or an object.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public ResponseInfo setSchema(SchemaInfo value) {
		schema = value;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "description": return toType(getDescription(), type);
			case "examples": return toType(getExamples(), type);
			case "headers": return toType(getHeaders(), type);
			case "schema": return toType(getSchema(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public ResponseInfo set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "description": return setDescription(stringify(value));
			case "examples": return setExamples(mapBuilder(String.class,Object.class).sparse().addAny(value).build());
			case "headers": return setHeaders(mapBuilder(String.class,HeaderInfo.class).sparse().addAny(value).build());
			case "schema": return setSchema(toType(value, SchemaInfo.class));
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
			.addIf(description != null, "description")
			.addIf(examples != null, "examples")
			.addIf(headers != null, "headers")
			.addIf(schema != null, "schema")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

	/**
	 * Resolves any <js>"$ref"</js> attributes in this element.
	 *
	 * @param swagger The swagger document containing the definitions.
	 * @param refStack Keeps track of previously-visited references so that we don't cause recursive loops.
	 * @param maxDepth
	 * 	The maximum depth to resolve references.
	 * 	<br>After that level is reached, <c>$ref</c> references will be left alone.
	 * 	<br>Useful if you have very complex models and you don't want your swagger page to be overly-complex.
	 * @return
	 * 	This object with references resolved.
	 * 	<br>May or may not be the same object.
	 */
	public ResponseInfo resolveRefs(Swagger swagger, Deque<String> refStack, int maxDepth) {

		if (schema != null)
			schema = schema.resolveRefs(swagger, refStack, maxDepth);

		if (headers != null)
			headers.entrySet().forEach(x -> x.setValue(x.getValue().resolveRefs(swagger, refStack, maxDepth)));

		return this;
	}
}
