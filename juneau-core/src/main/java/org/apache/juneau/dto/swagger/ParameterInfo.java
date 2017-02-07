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
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;

/**
 * Describes a single operation parameter.
 * <p>
 * A unique parameter is defined by a combination of a name and location.
 * <p>
 * There are five possible parameter types.
 * <ul>
 * 	<li><js>"path"</js> - Used together with Path Templating, where the parameter value is actually part of the operation's URL.
 * 		This does not include the host or base path of the API.
 * 		For example, in <code>/items/{itemId}</code>, the path parameter is <code>itemId</code>.
 * 	<li><js>"query"</js> - Parameters that are appended to the URL.
 * 		For example, in <code>/items?id=###</code>, the query parameter is <code>id</code>.
 * 	<li><js>"header"</js> - Custom headers that are expected as part of the request.
 * 	<li><js>"body"</js> - The payload that's appended to the HTTP request.
 * 		Since there can only be one payload, there can only be one body parameter.
 * 		The name of the body parameter has no effect on the parameter itself and is used for documentation purposes only.
 * 		Since Form parameters are also in the payload, body and form parameters cannot exist together for the same operation.
 * 	<li><js>"formData"</js> - Used to describe the payload of an HTTP request when either <code>application/x-www-form-urlencoded</code>, <code>multipart/form-data</code> or both are used as the content type of the request (in Swagger's definition, the consumes property of an operation).
 * 		This is the only parameter type that can be used to send files, thus supporting the file type.
 * 		Since form parameters are sent in the payload, they cannot be declared together with a body parameter for the same operation.
 * 		Form parameters have a different format based on the content-type used (for further details, consult <code>http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4</code>):
 * 		<ul>
 * 			<li><js>"application/x-www-form-urlencoded"</js> - Similar to the format of Query parameters but as a payload.
 * 				For example, <code>foo=1&amp;bar=swagger</code> - both <code>foo</code> and <code>bar</code> are form parameters.
 * 				This is normally used for simple parameters that are being transferred.
 * 			<li><js>"multipart/form-data"</js> - each parameter takes a section in the payload with an internal header.
 * 				For example, for the header <code>Content-Disposition: form-data; name="submit-name"</code> the name of the parameter is <code>submit-name</code>.
 * 				This type of form parameters is more commonly used for file transfers.
 * 		</ul>
 * 	</li>
 * </ul>
 */
@Bean(properties="in,name,type,description,required,schema,format,allowEmptyValue,items,collectionFormat,default,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,enum,multipleOf")
public class ParameterInfo {

	private static final String[] VALID_IN = {"query", "header", "path", "formData", "body"};
	private static final String[] VALID_TYPES = {"string", "number", "integer", "boolean", "array", "file"};
	private static final String[] VALID_COLLECTION_FORMATS = {"csv", "ssv", "tsv", "pipes", "multi"};

	private String name;
	private String in;
	private String description;
	private Boolean required;
	private SchemaInfo schema;
	private String type;
	private String format;
	private Boolean allowEmptyValue;
	private Items items;
	private String collectionFormat;
	private Object _default;
	private Number maximum;
	private Boolean exclusiveMaximum;
	private Number minimum;
	private Boolean exclusiveMinimum;
	private Integer maxLength;
	private Integer minLength;
	private String pattern;
	private Integer maxItems;
	private Integer minItems;
	private Boolean uniqueItems;
	private List<Object> _enum;
	private Number multipleOf;
	private boolean strict;

	/**
	 * Convenience method for creating a new Parameter object.
	 *
	 * @param in Required. The location of the parameter.
	 * 	Possible values are <js>"query"</js>, <js>"header"</js>, <js>"path"</js>, <js>"formData"</js> or <js>"body"</js>.
	 * @param name Required. The name of the parameter.
	 * 	Parameter names are case sensitive.
	 * 	If <code>in</code> is <js>"path"</js>, the <code>name</code> field MUST correspond to the associated path segment from the <code>path</code> field in the <a class="doclink" href="http://swagger.io/specification/#pathsObject">Paths Object</a>.
	 * 	See <a class="doclink" href="http://swagger.io/specification/#pathTemplating">Path Templating</a> for further information.
	 * 	For all other cases, the name corresponds to the parameter name used based on the <code>in</code> property.
	 * @return A new Parameter object.
	 */
	public static ParameterInfo create(String in, String name) {
		return new ParameterInfo().setIn(in).setName(name);
	}

	/**
	 * Same as {@link #create(String, String)} except methods will throw runtime exceptions if you attempt
	 * to pass in invalid values per the Swagger spec.
	 *
	 * @param in Required. The location of the parameter.
	 * 	Possible values are <js>"query"</js>, <js>"header"</js>, <js>"path"</js>, <js>"formData"</js> or <js>"body"</js>.
	 * @param name Required. The name of the parameter.
	 * 	Parameter names are case sensitive.
	 * 	If <code>in</code> is <js>"path"</js>, the <code>name</code> field MUST correspond to the associated path segment from the <code>path</code> field in the <a class="doclink" href="http://swagger.io/specification/#pathsObject">Paths Object</a>.
	 * 	See <a class="doclink" href="http://swagger.io/specification/#pathTemplating">Path Templating</a> for further information.
	 * 	For all other cases, the name corresponds to the parameter name used based on the <code>in</code> property.
	 * @return A new Parameter object.
	 */
	public static ParameterInfo createStrict(String in, String name) {
		return new ParameterInfo().setStrict().setIn(in).setName(name);
	}

	private ParameterInfo setStrict() {
		this.strict = true;
		return this;
	}

	/**
	 * Bean property getter:  <property>name</property>.
	 * <p>
	 * Required. The name of the parameter.
	 * Parameter names are case sensitive.
	 * If <code>in</code> is <js>"path"</js>, the <code>name</code> field MUST correspond to the associated path segment from the <code>path</code> field in the <a class="doclink" href="http://swagger.io/specification/#pathsObject">Paths Object</a>.
	 * See <a class="doclink" href="http://swagger.io/specification/#pathTemplating">Path Templating</a> for further information.
	 * For all other cases, the name corresponds to the parameter name used based on the <code>in</code> property.
	 *
	 * @return The value of the <property>name</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Bean property setter:  <property>name</property>.
	 * <p>
	 * Required. The name of the parameter.
	 * Parameter names are case sensitive.
	 * If <code>in</code> is <js>"path"</js>, the <code>name</code> field MUST correspond to the associated path segment from the <code>path</code> field in the <a class="doclink" href="http://swagger.io/specification/#pathsObject">Paths Object</a>.
	 * See <a class="doclink" href="http://swagger.io/specification/#pathTemplating">Path Templating</a> for further information.
	 * For all other cases, the name corresponds to the parameter name used based on the <code>in</code> property.
	 *
	 * @param name The new value for the <property>name</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setName(String name) {
		if (! "body".equals(in))
			this.name = name;
		return this;
	}

	/**
	 * Bean property getter:  <property>in</property>.
	 * <p>
	 * Required. The location of the parameter.
	 * Possible values are <js>"query"</js>, <js>"header"</js>, <js>"path"</js>, <js>"formData"</js> or <js>"body"</js>.
	 *
	 * @return The value of the <property>in</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getIn() {
		return in;
	}

	/**
	 * Bean property setter:  <property>in</property>.
	 * <p>
	 * Required. The location of the parameter.
	 * Possible values are <js>"query"</js>, <js>"header"</js>, <js>"path"</js>, <js>"formData"</js> or <js>"body"</js>.
	 *
	 * @param in The new value for the <property>in</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setIn(String in) {
		if (strict && ! ArrayUtils.contains(in, VALID_IN))
			throw new RuntimeException("Invalid value passed in to setIn(String).  Value='"+in+"', valid values=" + JsonSerializer.DEFAULT_LAX.toString(VALID_IN));
		this.in = in;
		if ("path".equals(in))
			required = true;
		return this;
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 * <p>
	 * A brief description of the parameter.
	 * This could contain examples of use.
	 * <a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used for rich text representation.
	 *
	 * @return The value of the <property>description</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 * <p>
	 * A brief description of the parameter.
	 * This could contain examples of use.
	 * <a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used for rich text representation.
	 *
	 * @param description The new value for the <property>description</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * Bean property getter:  <property>required</property>.
	 * <p>
	 * Determines whether this parameter is mandatory.
	 * If the parameter is <code>in</code> <js>"path"</js>, this property is required and its value MUST be <jk>true</jk>.
	 * Otherwise, the property MAY be included and its default value is <jk>false</jk>.
	 *
	 * @return The value of the <property>required</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Boolean getRequired() {
		return required;
	}

	/**
	 * Bean property setter:  <property>required</property>.
	 * <p>
	 * Determines whether this parameter is mandatory.
	 * If the parameter is <code>in</code> <js>"path"</js>, this property is required and its value MUST be <jk>true</jk>.
	 * Otherwise, the property MAY be included and its default value is <jk>false</jk>.
	 *
	 * @param required The new value for the <property>required</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setRequired(Boolean required) {
		this.required = required;
		return this;
	}

	/**
	 * Bean property getter:  <property>schema</property>.
	 * <p>
	 * Required. The schema defining the type used for the body parameter.
	 *
	 * @return The value of the <property>schema</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public SchemaInfo getSchema() {
		return schema;
	}

	/**
	 * Bean property setter:  <property>schema</property>.
	 * <p>
	 * Required. The schema defining the type used for the body parameter.
	 *
	 * @param schema The new value for the <property>schema</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setSchema(SchemaInfo schema) {
		this.schema = schema;
		return this;
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 * <p>
	 * Required. The type of the parameter.
	 * Since the parameter is not located at the request body, it is limited to simple types (that is, not an object).
	 * The value MUST be one of <js>"string"</js>, <js>"number"</js>, <js>"integer"</js>, <js>"boolean"</js>, <js>"array"</js> or <js>"file"</js>.
	 * If type is <js>"file"</js>, the <code>consumes</code> MUST be either <js>"multipart/form-data"</js>, <js>"application/x-www-form-urlencoded"</js> or both and the parameter MUST be <code>in</code> <js>"formData"</js>.
	 *
	 * @return The value of the <property>type</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Bean property setter:  <property>type</property>.
	 * <p>
	 * Required. The type of the parameter.
	 * Since the parameter is not located at the request body, it is limited to simple types (that is, not an object).
	 * The value MUST be one of <js>"string"</js>, <js>"number"</js>, <js>"integer"</js>, <js>"boolean"</js>, <js>"array"</js> or <js>"file"</js>.
	 * If type is <js>"file"</js>, the <code>consumes</code> MUST be either <js>"multipart/form-data"</js>, <js>"application/x-www-form-urlencoded"</js> or both and the parameter MUST be <code>in</code> <js>"formData"</js>.
	 *
	 * @param type The new value for the <property>type</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setType(String type) {
		if (strict && ! ArrayUtils.contains(type, VALID_TYPES))
			throw new RuntimeException("Invalid value passed in to setType(String).  Value='"+type+"', valid values=" + JsonSerializer.DEFAULT_LAX.toString(VALID_TYPES));
		this.type = type;
		return this;
	}

	/**
	 * Bean property getter:  <property>format</property>.
	 * <p>
	 * The extending format for the previously mentioned type.
	 * See <a class="doclink" href="http://swagger.io/specification/#dataTypeFormat">Data Type Formats</a> for further details.
	 *
	 * @return The value of the <property>format</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * Bean property setter:  <property>format</property>.
	 * <p>
	 * The extending format for the previously mentioned type.
	 * See <a class="doclink" href="http://swagger.io/specification/#dataTypeFormat">Data Type Formats</a> for further details.
	 *
	 * @param format The new value for the <property>format</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setFormat(String format) {
		this.format = format;
		return this;
	}

	/**
	 * Bean property getter:  <property>allowEmptyValue</property>.
	 * <p>
	 * Sets the ability to pass empty-valued parameters.
	 * This is valid only for either <code>query</code> or <code>formData</code> parameters and allows you to send a parameter with a name only or an empty value.
	 * Default value is <jk>false</jk>.
	 *
	 * @return The value of the <property>allowEmptyValue</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Boolean getAllowEmptyValue() {
		return allowEmptyValue;
	}

	/**
	 * Bean property setter:  <property>allowEmptyValue</property>.
	 * <p>
	 * Sets the ability to pass empty-valued parameters.
	 * This is valid only for either <code>query</code> or <code>formData</code> parameters and allows you to send a parameter with a name only or an empty value.
	 * Default value is <jk>false</jk>.
	 *
	 * @param allowEmptyValue The new value for the <property>allowEmptyValue</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setAllowEmptyValue(Boolean allowEmptyValue) {
		this.allowEmptyValue = allowEmptyValue;
		return this;
	}

	/**
	 * Bean property getter:  <property>items</property>.
	 * <p>
	 * Required if <code>type</code> is <js>"array"</js>.
	 * Describes the type of items in the array.
	 *
	 * @return The value of the <property>items</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Items getItems() {
		return items;
	}

	/**
	 * Bean property setter:  <property>items</property>.
	 * <p>
	 * Required if <code>type</code> is <js>"array"</js>.
	 * Describes the type of items in the array.
	 *
	 * @param items The new value for the <property>items</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setItems(Items items) {
		this.items = items;
		return this;
	}

	/**
	 * Bean property getter:  <property>collectionFormat</property>.
	 * <p>
	 * Determines the format of the array if type array is used.
	 * <p>
	 * Possible values are:
	 * <ul>
	 * 	<li><code>csv</code> - comma separated values <code>foo,bar</code>.
	 * 	<li><code>ssv</code> - space separated values <code>foo bar</code>.
	 * 	<li><code>tsv</code> - tab separated values <code>foo\tbar</code>.
	 * 	<li><code>pipes</code> - pipe separated values <code>foo|bar</code>.
	 * 	<li><code>multi</code> - corresponds to multiple parameter instances instead of multiple values for a single instance <code>foo=bar&amp;foo=baz</code>.
	 * 		This is valid only for parameters <code>in</code> <js>"query"</js> or <js>"formData"</js>.
	 * </ul>
	 * <p>
	 * Default value is <code>csv</code>.
	 *
	 * @return The value of the <property>collectionFormat</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getCollectionFormat() {
		return collectionFormat;
	}

	/**
	 * Bean property setter:  <property>collectionFormat</property>.
	 * <p>
	 * Determines the format of the array if type array is used.
	 * <p>
	 * Possible values are:
	 * <ul>
	 * 	<li><code>csv</code> - comma separated values <code>foo,bar</code>.
	 * 	<li><code>ssv</code> - space separated values <code>foo bar</code>.
	 * 	<li><code>tsv</code> - tab separated values <code>foo\tbar</code>.
	 * 	<li><code>pipes</code> - pipe separated values <code>foo|bar</code>.
	 * 	<li><code>multi</code> - corresponds to multiple parameter instances instead of multiple values for a single instance <code>foo=bar&amp;foo=baz</code>.
	 * 		This is valid only for parameters <code>in</code> <js>"query"</js> or <js>"formData"</js>.
	 * </ul>
	 * <p>
	 * Default value is <code>csv</code>.
	 *
	 * @param collectionFormat The new value for the <property>collectionFormat</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setCollectionFormat(String collectionFormat) {
		if (strict && ! ArrayUtils.contains(collectionFormat, VALID_COLLECTION_FORMATS))
			throw new RuntimeException("Invalid value passed in to setCollectionFormat(String).  Value='"+collectionFormat+"', valid values=" + JsonSerializer.DEFAULT_LAX.toString(VALID_COLLECTION_FORMATS));
		this.collectionFormat = collectionFormat;
		return this;
	}

	/**
	 * Bean property getter:  <property>default</property>.
	 * <p>
	 * Declares the value of the parameter that the server will use if none is provided, for example a <js>"count"</js> to control the number of results per page might default to 100 if not supplied by the client in the request.
	 * (Note: <js>"default"</js> has no meaning for required parameters.)
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor101">http://json-schema.org/latest/json-schema-validation.html#anchor101</a>.
	 * Unlike JSON Schema this value MUST conform to the defined <code>type</code> for this parameter.
	 *
	 * @return The value of the <property>default</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Object getDefault() {
		return _default;
	}

	/**
	 * Bean property setter:  <property>default</property>.
	 * <p>
	 * Declares the value of the parameter that the server will use if none is provided, for example a <js>"count"</js> to control the number of results per page might default to 100 if not supplied by the client in the request.
	 * (Note: <js>"default"</js> has no meaning for required parameters.)
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor101">http://json-schema.org/latest/json-schema-validation.html#anchor101</a>.
	 * Unlike JSON Schema this value MUST conform to the defined <code>type</code> for this parameter.
	 *
	 * @param _default The new value for the <property>default</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setDefault(Object _default) {
		this._default = _default;
		return this;
	}

	/**
	 * Bean property getter:  <property>maximum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor17">http://json-schema.org/latest/json-schema-validation.html#anchor17</a>.
	 *
	 * @return The value of the <property>maximum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Number getMaximum() {
		return maximum;
	}

	/**
	 * Bean property setter:  <property>maximum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor17">http://json-schema.org/latest/json-schema-validation.html#anchor17</a>.
	 *
	 * @param maximum The new value for the <property>maximum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setMaximum(Number maximum) {
		this.maximum = maximum;
		return this;
	}

	/**
	 * Bean property getter:  <property>exclusiveMaximum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor17">http://json-schema.org/latest/json-schema-validation.html#anchor17</a>.
	 *
	 * @return The value of the <property>exclusiveMaximum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExclusiveMaximum() {
		return exclusiveMaximum;
	}

	/**
	 * Bean property setter:  <property>exclusiveMaximum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor17">http://json-schema.org/latest/json-schema-validation.html#anchor17</a>.
	 *
	 * @param exclusiveMaximum The new value for the <property>exclusiveMaximum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setExclusiveMaximum(Boolean exclusiveMaximum) {
		this.exclusiveMaximum = exclusiveMaximum;
		return this;
	}

	/**
	 * Bean property getter:  <property>minimum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor21">http://json-schema.org/latest/json-schema-validation.html#anchor21</a>.
	 *
	 * @return The value of the <property>minimum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Number getMinimum() {
		return minimum;
	}

	/**
	 * Bean property setter:  <property>minimum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor21">http://json-schema.org/latest/json-schema-validation.html#anchor21</a>.
	 *
	 * @param minimum The new value for the <property>minimum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setMinimum(Number minimum) {
		this.minimum = minimum;
		return this;
	}

	/**
	 * Bean property getter:  <property>exclusiveMinimum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor21">http://json-schema.org/latest/json-schema-validation.html#anchor21</a>.
	 *
	 * @return The value of the <property>exclusiveMinimum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExclusiveMinimum() {
		return exclusiveMinimum;
	}

	/**
	 * Bean property setter:  <property>exclusiveMinimum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor21">http://json-schema.org/latest/json-schema-validation.html#anchor21</a>.
	 *
	 * @param exclusiveMinimum The new value for the <property>exclusiveMinimum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setExclusiveMinimum(Boolean exclusiveMinimum) {
		this.exclusiveMinimum = exclusiveMinimum;
		return this;
	}

	/**
	 * Bean property getter:  <property>maxLength</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor26">http://json-schema.org/latest/json-schema-validation.html#anchor26</a>.
	 *
	 * @return The value of the <property>maxLength</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxLength() {
		return maxLength;
	}

	/**
	 * Bean property setter:  <property>maxLength</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor26">http://json-schema.org/latest/json-schema-validation.html#anchor26</a>.
	 *
	 * @param maxLength The new value for the <property>maxLength</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setMaxLength(Integer maxLength) {
		this.maxLength = maxLength;
		return this;
	}

	/**
	 * Bean property getter:  <property>minLength</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor29">http://json-schema.org/latest/json-schema-validation.html#anchor29</a>.
	 *
	 * @return The value of the <property>minLength</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinLength() {
		return minLength;
	}

	/**
	 * Bean property setter:  <property>minLength</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor29">http://json-schema.org/latest/json-schema-validation.html#anchor29</a>.
	 *
	 * @param minLength The new value for the <property>minLength</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setMinLength(Integer minLength) {
		this.minLength = minLength;
		return this;
	}

	/**
	 * Bean property getter:  <property>pattern</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor33">http://json-schema.org/latest/json-schema-validation.html#anchor33</a>.
	 *
	 * @return The value of the <property>pattern</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getPattern() {
		return pattern;
	}

	/**
	 * Bean property setter:  <property>pattern</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor33">http://json-schema.org/latest/json-schema-validation.html#anchor33</a>.
	 *
	 * @param pattern The new value for the <property>pattern</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setPattern(String pattern) {
		this.pattern = pattern;
		return this;
	}

	/**
	 * Bean property getter:  <property>maxItems</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor42">http://json-schema.org/latest/json-schema-validation.html#anchor42</a>.
	 *
	 * @return The value of the <property>maxItems</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxItems() {
		return maxItems;
	}

	/**
	 * Bean property setter:  <property>maxItems</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor42">http://json-schema.org/latest/json-schema-validation.html#anchor42</a>.
	 *
	 * @param maxItems The new value for the <property>maxItems</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setMaxItems(Integer maxItems) {
		this.maxItems = maxItems;
		return this;
	}

	/**
	 * Bean property getter:  <property>minItems</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor45">http://json-schema.org/latest/json-schema-validation.html#anchor45</a>.
	 *
	 * @return The value of the <property>minItems</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinItems() {
		return minItems;
	}

	/**
	 * Bean property setter:  <property>minItems</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor45">http://json-schema.org/latest/json-schema-validation.html#anchor45</a>.
	 *
	 * @param minItems The new value for the <property>minItems</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setMinItems(Integer minItems) {
		this.minItems = minItems;
		return this;
	}

	/**
	 * Bean property getter:  <property>uniqueItems</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor49">http://json-schema.org/latest/json-schema-validation.html#anchor49</a>.
	 *
	 * @return The value of the <property>uniqueItems</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Boolean getUniqueItems() {
		return uniqueItems;
	}

	/**
	 * Bean property setter:  <property>uniqueItems</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor49">http://json-schema.org/latest/json-schema-validation.html#anchor49</a>.
	 *
	 * @param uniqueItems The new value for the <property>uniqueItems</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setUniqueItems(Boolean uniqueItems) {
		this.uniqueItems = uniqueItems;
		return this;
	}

	/**
	 * Bean property getter:  <property>enum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor76">http://json-schema.org/latest/json-schema-validation.html#anchor76</a>.
	 *
	 * @return The value of the <property>enum</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public List<Object> getEnum() {
		return _enum;
	}

	/**
	 * Bean property setter:  <property>enum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor76">http://json-schema.org/latest/json-schema-validation.html#anchor76</a>.
	 *
	 * @param _enum The new value for the <property>enum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setEnum(List<Object> _enum) {
		this._enum = _enum;
		return this;
	}

	/**
	 * Bean property adder:  <property>enum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor76">http://json-schema.org/latest/json-schema-validation.html#anchor76</a>.
	 *
	 * @param _enum The new values to add to the <property>enum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("hiding")
	public ParameterInfo addEnum(Object..._enum) {
		return addEnum(Arrays.asList(_enum));
	}

	/**
	 * Bean property adder:  <property>enum</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor76">http://json-schema.org/latest/json-schema-validation.html#anchor76</a>.
	 *
	 * @param _enum The new values to add to the <property>enum</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("hiding")
	public ParameterInfo addEnum(Collection<Object> _enum) {
		if (this._enum == null)
			this._enum = new LinkedList<Object>();
		this._enum.addAll(_enum);
		return this;
	}

	/**
	 * Bean property getter:  <property>multipleOf</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor14">http://json-schema.org/latest/json-schema-validation.html#anchor14</a>.
	 *
	 * @return The value of the <property>multipleOf</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Number getMultipleOf() {
		return multipleOf;
	}

	/**
	 * Bean property setter:  <property>multipleOf</property>.
	 * <p>
	 * See <a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor14">http://json-schema.org/latest/json-schema-validation.html#anchor14</a>.
	 *
	 * @param multipleOf The new value for the <property>multipleOf</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setMultipleOf(Number multipleOf) {
		this.multipleOf = multipleOf;
		return this;
	}
}
