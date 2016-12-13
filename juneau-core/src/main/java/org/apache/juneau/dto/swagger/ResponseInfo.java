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

import java.util.*;

import org.apache.juneau.annotation.*;

/**
 * Describes a single response from an API Operation.
 *
 * <h6 class='topic'>Example:</h6>
 * <p class='bcode'>
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
 */
@Bean(properties="description,schema,headers,examples")
public class ResponseInfo {

	private String description;
	private SchemaInfo schema;
	private Map<String,HeaderInfo> headers;
	private Map<String,Object> examples;

	/**
	 * Convenience method for creating a new Response object.
	 *
	 * @param description A short description of the response.
	 * 	<a href='https://help.github.com/articles/github-flavored-markdown'>GFM syntax</a> can be used for rich text representation.
	 * @return A new Header object.
	 */
	public static ResponseInfo create(String description) {
		return new ResponseInfo().setDescription(description);
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 * <p>
	 * Required. A short description of the response.
	 * <a href='https://help.github.com/articles/github-flavored-markdown'>GFM syntax</a> can be used for rich text representation.
	 *
	 * @return The value of the <property>description</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 * <p>
	 * Required. A short description of the response.
	 * <a href='https://help.github.com/articles/github-flavored-markdown'>GFM syntax</a> can be used for rich text representation.
	 *
	 * @param description The new value for the <property>description</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ResponseInfo setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Bean property getter:  <property>schema</property>.
	 * <p>
	 * A definition of the response structure.
	 * It can be a primitive, an array or an object.
	 * If this field does not exist, it means no content is returned as part of the response.
	 * As an extension to the <a href='http://swagger.io/specification/#schemaObject'>Schema Object</a>, its root type value may also be <js>"file"</js>.
	 * This SHOULD be accompanied by a relevant produces mime-type.
	 *
	 * @return The value of the <property>schema</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public SchemaInfo getSchema() {
		return schema;
	}

	/**
	 * Bean property setter:  <property>schema</property>.
	 * <p>
	 * A definition of the response structure.
	 * It can be a primitive, an array or an object.
	 * If this field does not exist, it means no content is returned as part of the response.
	 * As an extension to the <a href='http://swagger.io/specification/#schemaObject'>Schema Object</a>, its root type value may also be <js>"file"</js>.
	 * This SHOULD be accompanied by a relevant produces mime-type.
	 *
	 * @param schema The new value for the <property>schema</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ResponseInfo setSchema(SchemaInfo schema) {
		this.schema = schema;
		return this;
	}

	/**
	 * Bean property getter:  <property>headers</property>.
	 * <p>
	 * A list of headers that are sent with the response.
	 *
	 * @return The value of the <property>headers</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,HeaderInfo> getHeaders() {
		return headers;
	}

	/**
	 * Bean property setter:  <property>headers</property>.
	 * <p>
	 * A list of headers that are sent with the response.
	 *
	 * @param headers The new value for the <property>headers</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ResponseInfo setHeaders(Map<String,HeaderInfo> headers) {
		this.headers = headers;
		return this;
	}

	/**
	 * Bean property adder:  <property>headers</property>.
	 * <p>
	 * A list of headers that are sent with the response.
	 *
	 * @param name The header name.
	 * @param header The header descriptions
	 * @return This object (for method chaining).
	 */
	public ResponseInfo addHeader(String name, HeaderInfo header) {
		if (headers == null)
			headers = new TreeMap<String,HeaderInfo>();
		headers.put(name, header);
		return this;
	}

	/**
	 * Bean property getter:  <property>examples</property>.
	 * <p>
	 * An example of the response message.
	 * Keys must be MIME-type strings.
	 *
	 * @return The value of the <property>examples</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Object> getExamples() {
		return examples;
	}

	/**
	 * Bean property setter:  <property>examples</property>.
	 * <p>
	 * An example of the response message.
	 * Keys must be MIME-type strings.
	 *
	 * @param examples The new value for the <property>examples</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ResponseInfo setExamples(Map<String,Object> examples) {
		this.examples = examples;
		return this;
	}

	/**
	 * Bean property adder:  <property>examples</property>.
	 * <p>
	 * An example of the response message.
	 *
	 * @param mimeType The mimeType of the example.
	 * @param example The example output.
	 * @return This object (for method chaining).
	 */
	public ResponseInfo addExample(String mimeType, Object example) {
		if (examples == null)
			examples = new TreeMap<String,Object>();
		examples.put(mimeType, example);
		return this;
	}
}
