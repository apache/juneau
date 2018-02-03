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

import static org.apache.juneau.internal.BeanPropertyUtils.*;
import java.util.*;

import org.apache.juneau.annotation.*;

/**
 * Describes a single response from an API Operation.
 * 
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	ResponseInfo x = <jsm>responseInfo</jsm>(<js>"A complex object array response"</js>)
 * 		.schema(
 * 			<jsm>schemaInfo</jsm>
 * 				.type(<js>"array"</js>)
 * 				.items(
 * 					<jsm>items<jsm>()
 * 						.set(<js>"$ref"</js>, <js>"#/definitions/VeryComplexType"</js>)
 * 				)
 * 		);
 * 
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String json = JsonSerializer.<jsf>DEFAULT</jsf>.toString(x);
 * 
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	String json = x.toString();
 * </p>
 * <p class='bcode'>
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
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../../overview-summary.html#juneau-dto.Swagger'>Overview &gt; juneau-dto &gt; Swagger</a>
 * </ul>
 */
@Bean(properties="description,schema,headers,examples,*")
public class ResponseInfo extends SwaggerElement {

	private String description;
	private SchemaInfo schema;
	private Map<String,HeaderInfo> headers;
	private Map<String,Object> examples;

	/**
	 * Copies any non-null fields from the specified object to this object.
	 * 
	 * @param r 
	 * 	The object to copy fields from.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
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
	 * @return This object (for method chaining).
	 */
	public ResponseInfo setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Same as {@link #setDescription(String)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toString()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ResponseInfo description(Object value) {
		return setDescription(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>schema</property>.
	 * 
	 * <p>
	 * A definition of the response structure.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		If this field does not exist, it means no content is returned as part of the response.
	 * 	<li>
	 * 		As an extension to the <a class="doclink" href="http://swagger.io/specification/#schemaObject">Schema Object</a>,
	 * 		its root type value may also be <js>"file"</js>.
	 * 	<li>
	 * 		This SHOULD be accompanied by a relevant produces mime-type.
	 * </ul>
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		If this field does not exist, it means no content is returned as part of the response.
	 * 	<li>
	 * 		As an extension to the <a class="doclink" href="http://swagger.io/specification/#schemaObject">Schema Object</a>,
	 * 		its root type value may also be <js>"file"</js>.
	 * 	<li>
	 * 		This SHOULD be accompanied by a relevant produces mime-type.
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>It can be a primitive, an array or an object.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ResponseInfo setSchema(SchemaInfo value) {
		schema = value;
		return this;
	}

	/**
	 * Same as {@link #setSchema(SchemaInfo)}.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li>{@link SchemaInfo}
	 * 		<li><code>String</code> - JSON object representation of {@link SchemaInfo}
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode'>
	 * 	schema(<js>"{type:'type',description:'description',...}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ResponseInfo schema(Object value) {
		return setSchema(toType(value, SchemaInfo.class));
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
	 * @return This object (for method chaining).
	 */
	public ResponseInfo setHeaders(Map<String,HeaderInfo> value) {
		headers = newMap(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>headers</property> property.
	 * 
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public ResponseInfo addHeaders(Map<String,HeaderInfo> values) {
		headers = addToMap(headers, values);
		return this;
	}

	/**
	 * Adds a single value to the <property>headers</property> property.
	 * 
	 * @param name The header name.
	 * @param header The header descriptions
	 * @return This object (for method chaining).
	 */
	public ResponseInfo header(String name, HeaderInfo header) {
		addHeaders(Collections.singletonMap(name, header));
		return this;
	}

	/**
	 * Adds one or more values to the <property>headers</property> property.
	 * 
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><code>Map&lt;String,{@link HeaderInfo}|String&gt;</code>
	 * 		<li><code>String</code> - JSON object representation of <code>Map&lt;String,{@link HeaderInfo}&gt;</code>
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode'>
	 * 	headers(<js>"{headerName:{description:'description',...}}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public ResponseInfo headers(Object...values) {
		headers = addToMap(headers, values, String.class, HeaderInfo.class);
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
	 * @return This object (for method chaining).
	 */
	public ResponseInfo setExamples(Map<String,Object> value) {
		examples = newMap(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>examples</property> property.
	 * 
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public ResponseInfo addExamples(Map<String,Object> values) {
		examples = addToMap(examples, values);
		return this;
	}

	/**
	 * Adds a single value to the <property>examples</property> property.
	 * 
	 * @param mimeType The mime-type string.
	 * @param example The example.
	 * @return This object (for method chaining).
	 */
	public ResponseInfo example(String mimeType, Object example) {
		examples = addToMap(examples, mimeType, example);
		return this;
	}

	/**
	 * Adds one or more values to the <property>examples</property> property.
	 * 
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><code>Map&lt;String,Object&gt;</code>
	 * 		<li><code>String</code> - JSON object representation of <code>Map&lt;String,Object&gt;</code>
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode'>
	 * 	examples(<js>"{'text/json':{foo:'bar'}}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public ResponseInfo examples(Object...values) {
		examples = addToMap(examples, values, String.class, Object.class);
		return this;
	}

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "description": return toType(getDescription(), type);
			case "schema": return toType(getSchema(), type);
			case "headers": return toType(getHeaders(), type);
			case "examples": return toType(getExamples(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public ResponseInfo set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "description": return description(value);
			case "schema": return schema(value);
			case "headers": return setHeaders(null).headers(value);
			case "examples": return setExamples(null).examples(value);
			default: 
				super.set(property, value);
				return this;
		}
	}
}
