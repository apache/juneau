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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;

/**
 * Describes a single response from an API Operation.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
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
 * <p class='bcode w800'>
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
 * <ul class='seealso'>
 * 	<li class='link'>{@doc DtoSwagger}
 * </ul>
 */
@Bean(properties="description,schema,headers,examples,*")
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

		if (copyFrom.examples == null)
			this.examples = null;
		else
			this.examples = new LinkedHashMap<>(copyFrom.examples);

		if (copyFrom.headers == null) {
			this.headers = null;
		} else {
			this.headers = new LinkedHashMap<>();
			for (Map.Entry<String,HeaderInfo> e : copyFrom.headers.entrySet())
				this.headers.put(e.getKey(), e.getValue().copy());
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
	// description
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
	 * 	<br>{@doc ExtGFM} can be used for rich text representation.
	 * 	<br>Property value is required.
	 */
	public void setDescription(String value) {
		description = value;
	}

	/**
	 * Bean property fluent getter:  <property>description</property>.
	 *
	 * <p>
	 * A short description of the response.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> description() {
		return Optional.ofNullable(getDescription());
	}

	/**
	 * Bean property fluent setter:  <property>description</property>.
	 *
	 * <p>
	 * A short description of the response.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public ResponseInfo description(String value) {
		setDescription(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// examples
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setExamples(Map<String,Object> value) {
		examples = newMap(value);
	}

	/**
	 * Bean property appender:  <property>examples</property>.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * @return This object.
	 */
	public ResponseInfo addExamples(Map<String,Object> values) {
		examples = mapBuilder(examples).sparse().addAll(values).build();
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
	public ResponseInfo example(String mimeType, Object example) {
		examples =  mapBuilder(examples).sparse().add(mimeType, example).build();
		return this;
	}

	/**
	 * Bean property fluent getter:  <property>examples</property>.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Map<String,Object>> examples() {
		return Optional.ofNullable(getExamples());
	}

	/**
	 * Bean property fluent setter:  <property>examples</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public ResponseInfo examples(Map<String,Object> value) {
		setExamples(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>examples</property>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	examples(<js>"{'text/json':{foo:'bar'}}"</js>);
	 * </p>
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public ResponseInfo examples(String value) {
		setExamples(mapBuilder(String.class,Object.class).sparse().addJson(value).build());
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// headers
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setHeaders(Map<String,HeaderInfo> value) {
		headers = newMap(value);
	}

	/**
	 * Bean property appender:  <property>headers</property>.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public ResponseInfo addHeaders(Map<String,HeaderInfo> values) {
		headers = mapBuilder(headers).sparse().addAll(values).build();
		return this;
	}

	/**
	 * Bean property appender:  <property>headers</property>.
	 *
	 * @param name The header name.
	 * @param header The header descriptions
	 * @return This object.
	 */
	public ResponseInfo header(String name, HeaderInfo header) {
		addHeaders(Collections.singletonMap(name, header));
		return this;
	}

	/**
	 * Bean property fluent getter:  <property>headers</property>.
	 *
	 * <p>
	 * A list of headers that are sent with the response.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Map<String,HeaderInfo>> headers() {
		return Optional.ofNullable(getHeaders());
	}

	/**
	 * Bean property fluent setter:  <property>headers</property>.
	 *
	 * <p>
	 * A list of headers that are sent with the response.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public ResponseInfo headers(Map<String,HeaderInfo> value) {
		setHeaders(value);
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>headers</property>.
	 *
	 * <p>
	 * A list of headers that are sent with the response as raw JSON.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	headers(<js>"{headerName:{description:'description',...}}"</js>);
	 * </p>
	 *
	 * @param json
	 * 	The new value for this property as JSON
	 * @return This object.
	 */
	public ResponseInfo headers(String json) {
		setHeaders(mapBuilder(String.class,HeaderInfo.class).sparse().addJson(json).build());
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
	 * Returns the header information with the specified name.
	 *
	 * @param name The header name.
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<HeaderInfo> header(String name) {
		return Optional.ofNullable(getHeader(name));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// schema
	//-----------------------------------------------------------------------------------------------------------------

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
	 */
	public void setSchema(SchemaInfo value) {
		schema = value;
	}

	/**
	 * Bean property fluent getter:  <property>schema</property>.
	 *
	 * <p>
	 * A definition of the response structure.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<SchemaInfo> schema() {
		return Optional.ofNullable(getSchema());
	}

	/**
	 * Bean property setter:  <property>schema</property>.
	 *
	 * <p>
	 * A definition of the response structure.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public ResponseInfo schema(SchemaInfo value) {
		setSchema(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>schema</property>.
	 *
	 * <p>
	 * A definition of the response structure as raw JSON.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	schema(<js>"{type:'type',description:'description',...}"</js>);
	 * </p>
	 *
	 * @param json
	 * 	The new value for this property as JSON.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public ResponseInfo schema(String json) {
		setSchema(toType(json, SchemaInfo.class));
		return this;
	}


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
			case "description": return description(stringify(value));
			case "examples": return examples(mapBuilder(String.class,Object.class).sparse().addAny(value).build());
			case "headers": return headers(mapBuilder(String.class,HeaderInfo.class).sparse().addAny(value).build());
			case "schema": return schema(stringify(value));
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		ASet<String> s = ASet.<String>of()
			.appendIf(description != null, "description")
			.appendIf(examples != null, "examples")
			.appendIf(headers != null, "headers")
			.appendIf(schema != null, "schema");
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
			for (Map.Entry<String,HeaderInfo> e : headers.entrySet())
				e.setValue(e.getValue().resolveRefs(swagger, refStack, maxDepth));

		return this;
	}
}
