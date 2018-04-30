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
import static org.apache.juneau.internal.ArrayUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.utils.*;

/**
 * Describes a single operation parameter.
 * 
 * <p>
 * A unique parameter is defined by a combination of a name and location.
 * 
 * <p>
 * There are five possible parameter types.
 * <ul class='spaced-list'>
 * 	<li><js>"path"</js> - Used together with Path Templating, where the parameter value is actually part of the
 * 		operation's URL.
 * 		This does not include the host or base path of the API.
 * 		For example, in <code>/items/{itemId}</code>, the path parameter is <code>itemId</code>.
 * 	<li><js>"query"</js> - Parameters that are appended to the URL.
 * 		For example, in <code>/items?id=###</code>, the query parameter is <code>id</code>.
 * 	<li><js>"header"</js> - Custom headers that are expected as part of the request.
 * 	<li><js>"body"</js> - The payload that's appended to the HTTP request.
 * 		Since there can only be one payload, there can only be one body parameter.
 * 		The name of the body parameter has no effect on the parameter itself and is used for documentation purposes
 * 		only.
 * 		Since Form parameters are also in the payload, body and form parameters cannot exist together for the same
 * 		operation.
 * 	<li><js>"formData"</js> - Used to describe the payload of an HTTP request when either
 * 		<code>application/x-www-form-urlencoded</code>, <code>multipart/form-data</code> or both are used as the
 * 		content type of the request (in Swagger's definition, the consumes property of an operation).
 * 		This is the only parameter type that can be used to send files, thus supporting the file type.
 * 		Since form parameters are sent in the payload, they cannot be declared together with a body parameter for the
 * 		same operation.
 * 		Form parameters have a different format based on the content-type used (for further details, consult
 * 		<code>http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4</code>):
 * 		<ul>
 * 			<li><js>"application/x-www-form-urlencoded"</js> - Similar to the format of Query parameters but as a
 * 				payload.
 * 				For example, <code>foo=1&amp;bar=swagger</code> - both <code>foo</code> and <code>bar</code> are form
 * 				parameters.
 * 				This is normally used for simple parameters that are being transferred.
 * 			<li><js>"multipart/form-data"</js> - each parameter takes a section in the payload with an internal header.
 * 				For example, for the header <code>Content-Disposition: form-data; name="submit-name"</code> the name of
 * 				the parameter is <code>submit-name</code>.
 * 				This type of form parameters is more commonly used for file transfers.
 * 		</ul>
 * 	</li>
 * </ul>
 * 
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	ParameterInfo x = <jsm>parameterInfo</jsm>(<js>"query"</js>, <js>"foo"</js>);
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
 * 		<js>"in"</js>: <js>"query"</js>,
 * 		<js>"name"</js>: <js>"foo"</js>
 * 	}
 * </p>
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../../overview-summary.html#juneau-dto.Swagger'>Overview &gt; juneau-dto &gt; Swagger</a>
 * </ul>
 */
@Bean(properties="in,name,type,description,required,schema,format,allowEmptyValue,items,collectionFormat,default,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,enum,multipleOf,x-example,x-examples,*")
public class ParameterInfo extends SwaggerElement {

	private static final String[] VALID_IN = {"query", "header", "path", "formData", "body"};
	private static final String[] VALID_TYPES = {"string", "number", "integer", "boolean", "array", "file"};
	private static final String[] VALID_COLLECTION_FORMATS = {"csv", "ssv", "tsv", "pipes", "multi"};

	private String 
		name,
		in,
		description,
		type,
		format,
		pattern,
		collectionFormat;
	private Number 
		maximum,
		minimum,
		multipleOf;
	private Integer 
		maxLength,
		minLength,
		maxItems,
		minItems;
	private Boolean 
		required,
		allowEmptyValue,
		exclusiveMaximum,
		exclusiveMinimum,
		uniqueItems;
	private SchemaInfo schema;
	private Items items;
	private Object _default;
	private List<Object> _enum;
	private Object example;
	private Map<String,String> examples;

	/**
	 * Default constructor.
	 */
	public ParameterInfo() {}
	
	/**
	 * Copy constructor.
	 * 
	 * @param copyFrom The object to copy. 
	 */
	public ParameterInfo(ParameterInfo copyFrom) {
		super(copyFrom);
		
		this.name = copyFrom.name;
		this.in = copyFrom.in;
		this.description = copyFrom.description;
		this.type = copyFrom.type;
		this.format = copyFrom.format;
		this.pattern = copyFrom.pattern;
		this.collectionFormat = copyFrom.collectionFormat;
		this.maximum = copyFrom.maximum;
		this.minimum = copyFrom.minimum;
		this.multipleOf = copyFrom.multipleOf;
		this.maxLength = copyFrom.maxLength;
		this.minLength = copyFrom.minLength;
		this.maxItems = copyFrom.maxItems;
		this.minItems = copyFrom.minItems;
		this.required = copyFrom.required;
		this.allowEmptyValue = copyFrom.allowEmptyValue;
		this.exclusiveMaximum = copyFrom.exclusiveMaximum;
		this.exclusiveMinimum = copyFrom.exclusiveMinimum;
		this.uniqueItems = copyFrom.uniqueItems;
		this.schema = copyFrom.schema == null ? null : copyFrom.schema.copy();
		this.items = copyFrom.items == null ? null : copyFrom.items.copy();
		this._default = copyFrom._default;
		this._enum = newList(copyFrom._enum);
		this.example = copyFrom.example;
		
		this.examples = copyFrom.examples == null ? null : new LinkedHashMap<String,String>();
		if (copyFrom.examples != null)
			this.examples.putAll(copyFrom.examples);
	}
	
	/**
	 * Make a deep copy of this object.
	 * 
	 * @return A deep copy of this object. 
	 */
	public ParameterInfo copy() {
		return new ParameterInfo(this);
	}
	
	@Override /* SwaggerElement */
	protected ParameterInfo strict() {
		super.strict();
		return this;
	}

	/**
	 * Copies any non-null fields from the specified object to this object.
	 * 
	 * @param p 
	 * 	The object to copy fields from.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo copyFrom(ParameterInfo p) {
		if (p != null) {
			if (p.name != null)
				name = p.name;
			if (p.in != null)
				in = p.in;
			if (p.description != null)
				description = p.description;
			if (p.type != null)
				type = p.type;
			if (p.format != null)
				format = p.format;
			if (p.pattern != null)
				pattern = p.pattern;
			if (p.collectionFormat != null)
				collectionFormat = p.collectionFormat;
			if (p.maximum != null)
				maximum = p.maximum;
			if (p.minimum != null)
				minimum = p.minimum;
			if (p.multipleOf != null)
				multipleOf = p.multipleOf;
			if (p.maxLength != null)
				maxLength = p.maxLength;
			if (p.minLength != null)
				minLength = p.minLength;
			if (p.maxItems != null)
				maxItems = p.maxItems;
			if (p.minItems != null)
				minItems = p.minItems;
			if (p.required != null)
				required = p.required;
			if (p.allowEmptyValue != null)
				allowEmptyValue = p.allowEmptyValue;
			if (p.exclusiveMaximum != null)
				exclusiveMaximum = p.exclusiveMaximum;
			if (p.exclusiveMinimum != null)
				exclusiveMinimum = p.exclusiveMinimum;
			if (p.uniqueItems != null)
				uniqueItems = p.uniqueItems;
			if (p.schema != null)
				schema = p.schema;
			if (p.items != null)
				items = p.items;
			if (p._default != null)
				_default = p._default;
			if (p._enum != null)
				_enum = p._enum;
			if (p.example != null)
				example = p.examples;
			if (p.examples != null)
				examples = p.examples;
		}
		return this;
	}
	
	/**
	 * Bean property getter:  <property>name</property>.
	 * 
	 * <p>
	 * The name of the parameter.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Parameter names are case sensitive.
	 * 	<li>
	 * 		If <code>in</code> is <js>"path"</js>, the <code>name</code> field MUST correspond to the associated path segment
	 * 		from the <code>path</code> field in the <a class="doclink" href="http://swagger.io/specification/#pathsObject">Paths Object</a>.
	 * 	<li>
	 * 		For all other cases, the name corresponds to the parameter name used based on the <code>in</code> property.
	 * </ul>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://swagger.io/specification/#pathTemplating">Path Templating</a>
	 * </ul>
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Bean property setter:  <property>name</property>.
	 * 
	 * <p>
	 * The name of the parameter.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Parameter names are case sensitive.
	 * 	<li>
	 * 		If <code>in</code> is <js>"path"</js>, the <code>name</code> field MUST correspond to the associated path segment
	 * 		from the <code>path</code> field in the <a class="doclink" href="http://swagger.io/specification/#pathsObject">Paths Object</a>.
	 * 	<li>
	 * 		For all other cases, the name corresponds to the parameter name used based on the <code>in</code> property.
	 * </ul>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://swagger.io/specification/#pathTemplating">Path Templating</a>
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setName(String value) {
		if (! "body".equals(in))
			name = value;
		return this;
	}

	/**
	 * Same as {@link #setName(String)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toString()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo name(Object value) {
		return setName(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>in</property>.
	 * 
	 * <p>
	 * The location of the parameter.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getIn() {
		return in;
	}

	/**
	 * Bean property setter:  <property>in</property>.
	 * 
	 * <p>
	 * The location of the parameter.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"query"</js>
	 * 		<li><js>"header"</js>
	 * 		<li><js>"path"</js>
	 * 		<li><js>"formData"</js>
	 * 		<li><js>"body"</js>
	 * 	</ul>
	 * 	<br>Property value is required.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setIn(String value) {
		if (isStrict() && ! contains(value, VALID_IN))
			throw new FormattedRuntimeException(
				"Invalid value passed in to setIn(String).  Value=''{0}'', valid values={1}",
				value, VALID_IN
			);
		in = value;
		if ("path".equals(value))
			required = true;
		return this;
	}

	/**
	 * Same as {@link #setIn(String)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toString()</code>.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"query"</js>
	 * 		<li><js>"header"</js>
	 * 		<li><js>"path"</js>
	 * 		<li><js>"formData"</js>
	 * 		<li><js>"body"</js>
	 * 	</ul>
	 * 	<br>Property value is required.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo in(Object value) {
		return setIn(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 * 
	 * <p>
	 * A brief description of the parameter.
	 * <br>This could contain examples of use.
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
	 * A brief description of the parameter.
	 * <br>This could contain examples of use.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br><a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used for rich text representation.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Same as {@link #setDescription(String)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br><a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used for rich text representation.
	 * 	<br>Non-String values will be converted to String using <code>toString()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo description(Object value) {
		return setDescription(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>required</property>.
	 * 
	 * <p>
	 * Determines whether this parameter is mandatory.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getRequired() {
		return required;
	}

	/**
	 * Bean property setter:  <property>required</property>.
	 * 
	 * <p>
	 * Determines whether this parameter is mandatory.
	 * 
	 * <p>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>If the parameter is <code>in</code> <js>"path"</js>, this property is required and its value MUST be <jk>true</jk>.
	 * 	<br>Otherwise, the property MAY be included and its default value is <jk>false</jk>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setRequired(Boolean value) {
		required = value;
		return this;
	}

	/**
	 * Same as {@link #setRequired(Boolean)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-boolean values will be converted to boolean using <code>Boolean.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>If the parameter is <code>in</code> <js>"path"</js>, this property is required and its value MUST be <jk>true</jk>.
	 * 	<br>Otherwise, the property MAY be included and its default value is <jk>false</jk>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo required(Object value) {
		return setRequired(toBoolean(value));
	}

	/**
	 * Bean property getter:  <property>schema</property>.
	 * 
	 * <p>
	 * The schema defining the type used for the body parameter.
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
	 * The schema defining the type used for the body parameter.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setSchema(SchemaInfo value) {
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
	 * 	<br>Property value is required.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo schema(Object value) {
		return setSchema(toType(value, SchemaInfo.class));
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 * 
	 * <p>
	 * The type of the parameter.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Bean property setter:  <property>type</property>.
	 * 
	 * <p>
	 * The type of the parameter.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"string"</js>
	 * 		<li><js>"number"</js>
	 * 		<li><js>"integer"</js>
	 * 		<li><js>"boolean"</js>
	 * 		<li><js>"array"</js>
	 * 		<li><js>"file"</js>
	 * 	</ul>
	 * 	<br>If type is <js>"file"</js>, the <code>consumes</code> MUST be either <js>"multipart/form-data"</js>, <js>"application/x-www-form-urlencoded"</js> 
	 * 		or both and the parameter MUST be <code>in</code> <js>"formData"</js>.
	 * 	<br>Property value is required.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setType(String value) {
		if (isStrict() && ! contains(value, VALID_TYPES))
			throw new FormattedRuntimeException(
				"Invalid value passed in to setType(String).  Value=''{0}'', valid values={1}",
				value, VALID_TYPES
			);
		type = value;
		return this;
	}

	/**
	 * Same as {@link #setType(String)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toString()</code>.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"string"</js>
	 * 		<li><js>"number"</js>
	 * 		<li><js>"integer"</js>
	 * 		<li><js>"boolean"</js>
	 * 		<li><js>"array"</js>
	 * 		<li><js>"file"</js>
	 * 	</ul>
	 * 	<br>If type is <js>"file"</js>, the <code>consumes</code> MUST be either <js>"multipart/form-data"</js>, <js>"application/x-www-form-urlencoded"</js> 
	 * 		or both and the parameter MUST be <code>in</code> <js>"formData"</js>.
	 * 	<br>Property value is required.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo type(Object value) {
		return setType(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>format</property>.
	 * 
	 * <p>
	 * The extending format for the previously mentioned type.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://swagger.io/specification/#dataTypeFormat">Data Type Formats</a>
	 * </ul>
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * Bean property setter:  <property>format</property>.
	 * 
	 * <p>
	 * The extending format for the previously mentioned type.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://swagger.io/specification/#dataTypeFormat">Data Type Formats</a>
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setFormat(String value) {
		format = value;
		return this;
	}

	/**
	 * Same as {@link #setFormat(String)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toString()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo format(Object value) {
		return setFormat(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>allowEmptyValue</property>.
	 * 
	 * <p>
	 * Sets the ability to pass empty-valued parameters.
	 * 
	 * <p>
	 * This is valid only for either <code>query</code> or <code>formData</code> parameters and allows you to send a
	 * parameter with a name only or an empty value.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getAllowEmptyValue() {
		return allowEmptyValue;
	}

	/**
	 * Bean property setter:  <property>allowEmptyValue</property>.
	 * 
	 * <p>
	 * Sets the ability to pass empty-valued parameters.
	 * 
	 * <p>
	 * This is valid only for either <code>query</code> or <code>formData</code> parameters and allows you to send a
	 * parameter with a name only or an empty value.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * 	<br>Default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setAllowEmptyValue(Boolean value) {
		allowEmptyValue = value;
		return this;
	}

	/**
	 * Same as {@link #setAllowEmptyValue(Boolean)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-boolean values will be converted to boolean using <code>Boolean.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * 	<br>Default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo allowEmptyValue(Object value) {
		return setAllowEmptyValue(toBoolean(value));
	}

	/**
	 * Bean property getter:  <property>items</property>.
	 * 
	 * <p>
	 * Describes the type of items in the array.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Items getItems() {
		return items;
	}

	/**
	 * Bean property setter:  <property>items</property>.
	 * 
	 * <p>
	 * Describes the type of items in the array.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Property value is required if <code>type</code> is <js>"array"</js>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setItems(Items value) {
		items = value;
		return this;
	}

	/**
	 * Same as {@link #setItems(Items)}.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Property value is required if <code>type</code> is <js>"array"</js>.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li>{@link Items}
	 * 		<li><code>String</code> - JSON object representation of {@link Items}
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode'>
	 * 	items(<js>"{type:'type',format:'format',...}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo items(Object value) {
		return setItems(toType(value, Items.class));
	}

	/**
	 * Bean property getter:  <property>collectionFormat</property>.
	 * 
	 * <p>
	 * Determines the format of the array if type array is used.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getCollectionFormat() {
		return collectionFormat;
	}

	/**
	 * Bean property setter:  <property>collectionFormat</property>.
	 * 
	 * <p>
	 * Determines the format of the array if type array is used.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"csv"</js> (default) - comma separated values <code>foo,bar</code>.
	 * 		<li><js>"ssv"</js> - space separated values <code>foo bar</code>.
	 * 		<li><js>"tsv"</js> - tab separated values <code>foo\tbar</code>.
	 * 		<li><js>"pipes"</js> - pipe separated values <code>foo|bar</code>.
	 * 		<li><js>"multi"</js> - corresponds to multiple parameter instances instead of multiple values for a single
	 * 			instance <code>foo=bar&amp;foo=baz</code>.
	 * 			<br>This is valid only for parameters <code>in</code> <js>"query"</js> or <js>"formData"</js>.
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setCollectionFormat(String value) {
		if (isStrict() && ! contains(value, VALID_COLLECTION_FORMATS))
			throw new FormattedRuntimeException(
				"Invalid value passed in to setCollectionFormat(String).  Value=''{0}'', valid values={1}",
				value, VALID_COLLECTION_FORMATS
			);
		collectionFormat = value;
		return this;
	}

	/**
	 * Same as {@link #setCollectionFormat(String)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toString()</code>.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"csv"</js> (default) - comma separated values <code>foo,bar</code>.
	 * 		<li><js>"ssv"</js> - space separated values <code>foo bar</code>.
	 * 		<li><js>"tsv"</js> - tab separated values <code>foo\tbar</code>.
	 * 		<li><js>"pipes"</js> - pipe separated values <code>foo|bar</code>.
	 * 		<li><js>"multi"</js> - corresponds to multiple parameter instances instead of multiple values for a single
	 * 			instance <code>foo=bar&amp;foo=baz</code>.
	 * 			<br>This is valid only for parameters <code>in</code> <js>"query"</js> or <js>"formData"</js>.
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo collectionFormat(Object value) {
		return setCollectionFormat(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>default</property>.
	 * 
	 * <p>
	 * Declares the value of the parameter that the server will use if none is provided, for example a <js>"count"</js>
	 * to control the number of results per page might default to 100 if not supplied by the client in the request.
	 * 
	 * (Note: <js>"default"</js> has no meaning for required parameters.)
	 * Unlike JSON Schema this value MUST conform to the defined <code>type</code> for this parameter.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor101">http://json-schema.org/latest/json-schema-validation.html#anchor101</a>
	 * </ul>
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Object getDefault() {
		return _default;
	}

	/**
	 * Bean property setter:  <property>default</property>.
	 * 
	 * <p>
	 * Declares the value of the parameter that the server will use if none is provided, for example a <js>"count"</js>
	 * to control the number of results per page might default to 100 if not supplied by the client in the request.
	 * (Note: <js>"default"</js> has no meaning for required parameters.)
	 * Unlike JSON Schema this value MUST conform to the defined <code>type</code> for this parameter.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor101">http://json-schema.org/latest/json-schema-validation.html#anchor101</a>
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setDefault(Object value) {
		_default = value;
		return this;
	}

	/**
	 * Same as {@link #setDefault(Object)}.
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo _default(Object value) {
		return setDefault(value);
	}

	/**
	 * Bean property getter:  <property>maximum</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor17">http://json-schema.org/latest/json-schema-validation.html#anchor17</a>
	 * </ul>
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMaximum() {
		return maximum;
	}

	/**
	 * Bean property setter:  <property>maximum</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor17">http://json-schema.org/latest/json-schema-validation.html#anchor17</a>
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setMaximum(Number value) {
		maximum = value;
		return this;
	}

	/**
	 * Same as {@link #setMaximum(Number)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-Number values will be converted to Number using <code>toString()</code> then best number match.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo maximum(Object value) {
		return setMaximum(toNumber(value));
	}

	/**
	 * Bean property getter:  <property>exclusiveMaximum</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor17">http://json-schema.org/latest/json-schema-validation.html#anchor17</a>
	 * </ul>
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExclusiveMaximum() {
		return exclusiveMaximum;
	}

	/**
	 * Bean property setter:  <property>exclusiveMaximum</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor17">http://json-schema.org/latest/json-schema-validation.html#anchor17</a>
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setExclusiveMaximum(Boolean value) {
		exclusiveMaximum = value;
		return this;
	}

	/**
	 * Same as {@link #setExclusiveMaximum(Boolean)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-boolean values will be converted to boolean using <code>Boolean.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo exclusiveMaximum(Object value) {
		return setExclusiveMaximum(toBoolean(value));
	}

	/**
	 * Bean property getter:  <property>minimum</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor21">http://json-schema.org/latest/json-schema-validation.html#anchor21</a>
	 * </ul>
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMinimum() {
		return minimum;
	}

	/**
	 * Bean property setter:  <property>minimum</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor21">http://json-schema.org/latest/json-schema-validation.html#anchor21</a>
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setMinimum(Number value) {
		minimum = value;
		return this;
	}

	/**
	 * Same as {@link #setMinimum(Number)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-Number values will be converted to Number using <code>toString()</code> then best number match.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo minimum(Object value) {
		return setMinimum(toNumber(value));
	}

	/**
	 * Bean property getter:  <property>exclusiveMinimum</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor21">http://json-schema.org/latest/json-schema-validation.html#anchor21</a>
	 * </ul>
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExclusiveMinimum() {
		return exclusiveMinimum;
	}

	/**
	 * Bean property setter:  <property>exclusiveMinimum</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor21">http://json-schema.org/latest/json-schema-validation.html#anchor21</a>
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setExclusiveMinimum(Boolean value) {
		exclusiveMinimum = value;
		return this;
	}

	/**
	 * Same as {@link #setExclusiveMinimum(Boolean)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-boolean values will be converted to boolean using <code>Boolean.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo exclusiveMinimum(Object value) {
		return setExclusiveMinimum(toBoolean(value));
	}

	/**
	 * Bean property getter:  <property>maxLength</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor26">http://json-schema.org/latest/json-schema-validation.html#anchor26</a>
	 * </ul>
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxLength() {
		return maxLength;
	}

	/**
	 * Bean property setter:  <property>maxLength</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor26">http://json-schema.org/latest/json-schema-validation.html#anchor26</a>
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setMaxLength(Integer value) {
		maxLength = value;
		return this;
	}

	/**
	 * Same as {@link #setMaxLength(Integer)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-Integer values will be converted to Integer using <code>Integer.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo maxLength(Object value) {
		return setMaxLength(toInteger(value));
	}

	/**
	 * Bean property getter:  <property>minLength</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor29">http://json-schema.org/latest/json-schema-validation.html#anchor29</a>
	 * </ul>
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinLength() {
		return minLength;
	}

	/**
	 * Bean property setter:  <property>minLength</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor29">http://json-schema.org/latest/json-schema-validation.html#anchor29</a>
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setMinLength(Integer value) {
		minLength = value;
		return this;
	}

	/**
	 * Same as {@link #setMinLength(Integer)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-Integer values will be converted to Integer using <code>Integer.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo minLength(Object value) {
		return setMinLength(toInteger(value));
	}

	/**
	 * Bean property getter:  <property>pattern</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor33">http://json-schema.org/latest/json-schema-validation.html#anchor33</a>
	 * </ul>
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getPattern() {
		return pattern;
	}

	/**
	 * Bean property setter:  <property>pattern</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor33">http://json-schema.org/latest/json-schema-validation.html#anchor33</a>
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setPattern(String value) {
		pattern = value;
		return this;
	}

	/**
	 * Same as {@link #setPattern(String)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toString()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo pattern(Object value) {
		return setPattern(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>maxItems</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor42">http://json-schema.org/latest/json-schema-validation.html#anchor42</a>
	 * </ul>
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxItems() {
		return maxItems;
	}

	/**
	 * Bean property setter:  <property>maxItems</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor42">http://json-schema.org/latest/json-schema-validation.html#anchor42</a>
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setMaxItems(Integer value) {
		maxItems = value;
		return this;
	}

	/**
	 * Same as {@link #setMaxItems(Integer)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-Integer values will be converted to Integer using <code>Integer.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo maxItems(Object value) {
		return setMaxItems(toInteger(value));
	}

	/**
	 * Bean property getter:  <property>minItems</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor45">http://json-schema.org/latest/json-schema-validation.html#anchor45</a>
	 * </ul>
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinItems() {
		return minItems;
	}

	/**
	 * Bean property setter:  <property>minItems</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor45">http://json-schema.org/latest/json-schema-validation.html#anchor45</a>
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setMinItems(Integer value) {
		minItems = value;
		return this;
	}

	/**
	 * Same as {@link #setMinItems(Integer)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-Integer values will be converted to Integer using <code>Integer.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo minItems(Object value) {
		return setMinItems(toInteger(value));
	}

	/**
	 * Bean property getter:  <property>uniqueItems</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor49">http://json-schema.org/latest/json-schema-validation.html#anchor49</a>
	 * </ul>
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getUniqueItems() {
		return uniqueItems;
	}

	/**
	 * Bean property setter:  <property>uniqueItems</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor49">http://json-schema.org/latest/json-schema-validation.html#anchor49</a>
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setUniqueItems(Boolean value) {
		uniqueItems = value;
		return this;
	}

	/**
	 * Same as {@link #setUniqueItems(Boolean)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-boolean values will be converted to boolean using <code>Boolean.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo uniqueItems(Object value) {
		return setUniqueItems(toBoolean(value));
	}

	/**
	 * Bean property getter:  <property>enum</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor76">http://json-schema.org/latest/json-schema-validation.html#anchor76</a>
	 * </ul>
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<Object> getEnum() {
		return _enum;
	}

	/**
	 * Bean property setter:  <property>enum</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor76">http://json-schema.org/latest/json-schema-validation.html#anchor76</a>
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setEnum(Collection<Object> value) {
		_enum = newList(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>enum</property> property.
	 * 
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo addEnum(Collection<Object> value) {
		_enum = addToList(_enum, value);
		return this;
	}
	
	/**
	 * Adds one or more values to the <property>enum</property> property.
	 * 
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><code>Object</code>
	 * 		<li><code>Collection&lt;Object&gt;</code>
	 * 		<li><code>String</code> - JSON array representation of <code>Collection&lt;Object&gt;</code>
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode'>
	 * 	_enum(<js>"['foo','bar']"</js>);
	 * 			</p>
	 * 		<li><code>String</code> - Individual values
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode'>
	 * 	_enum(<js>"foo"</js>, <js>"bar"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo _enum(Object...values) {
		_enum = addToList(_enum, values, Object.class);
		return this;
	}

	/**
	 * Bean property getter:  <property>multipleOf</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor14">http://json-schema.org/latest/json-schema-validation.html#anchor14</a>
	 * </ul>
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMultipleOf() {
		return multipleOf;
	}

	/**
	 * Bean property setter:  <property>multipleOf</property>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='extlink'><a class="doclink" href="http://json-schema.org/latest/json-schema-validation.html#anchor14">http://json-schema.org/latest/json-schema-validation.html#anchor14</a>
	 * </ul>
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo setMultipleOf(Number value) {
		multipleOf = value;
		return this;
	}

	/**
	 * Same as {@link #setMultipleOf(Number)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-Number values will be converted to Number using <code>toString()</code> then best number match.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo multipleOf(Object value) {
		return setMultipleOf(toNumber(value));
	}

	/**
	 * Bean property getter:  <property>x-example</property>.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@BeanProperty("x-example")
	public Object getExample() {
		return example;
	}

	/**
	 * Bean property setter:  <property>x-example</property>.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("x-example")
	public ParameterInfo setExample(Object value) {
		example = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>x-example</property>.
	 * 
	 * @param value The property value.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo example(Object value) {
		example = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>x-examples</property>.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@BeanProperty("x-examples")
	public Map<String,String> getExamples() {
		return examples;
	}

	/**
	 * Bean property setter:  <property>x-examples</property>.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("x-examples")
	public ParameterInfo setExamples(Map<String,String> value) {
		examples = newMap(value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>x-examples</property> property.
	 * 
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo addExamples(Map<String,String> values) {
		examples = addToMap(examples, values);
		return this;
	}

	/**
	 * Adds a single value to the <property>x-examples</property> property.
	 * 
	 * @param name The extra property name.
	 * @param value The extra property value.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo example(String name, String value) {
		examples = addToMap(examples, name, value);
		return this;
	}

	/**
	 * Adds one or more values to the <property>x-examples</property> property.
	 * 
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><code>Map&lt;String,String&gt;</code>
	 * 		<li><code>String</code> - JSON object representation of <code>Map&lt;String,Object&gt;</code>
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode'>
	 * 	examples(<js>"{'text/json':'{foo:\\'bar\\'}'}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public ParameterInfo examples(Object...values) {
		examples = addToMap(examples, values, String.class, String.class);
		return this;
	}

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "name": return toType(getName(), type);
			case "in": return toType(getIn(), type);
			case "description": return toType(getDescription(), type);
			case "required": return toType(getRequired(), type);
			case "schema": return toType(getSchema(), type);
			case "type": return toType(getType(), type);
			case "format": return toType(getFormat(), type);
			case "allowEmptyValue": return toType(getAllowEmptyValue(), type);
			case "items": return toType(getItems(), type);
			case "collectionFormat": return toType(getCollectionFormat(), type);
			case "default": return toType(getDefault(), type);
			case "maximum": return toType(getMaximum(), type);
			case "exclusiveMaximum": return toType(getExclusiveMaximum(), type);
			case "minimum": return toType(getMinimum(), type);
			case "exclusiveMinimum": return toType(getExclusiveMinimum(), type);
			case "maxLength": return toType(getMaxLength(), type);
			case "minLength": return toType(getMinLength(), type);
			case "pattern": return toType(getPattern(), type);
			case "maxItems": return toType(getMaxItems(), type);
			case "minItems": return toType(getMinItems(), type);
			case "uniqueItems": return toType(getUniqueItems(), type);
			case "enum": return toType(getEnum(), type);
			case "multipleOf": return toType(getMultipleOf(), type);
			case "x-example": return toType(getExample(), type);
			case "x-examples": return toType(getExamples(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public ParameterInfo set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "name": return name(value);
			case "in": return in(value);
			case "description": return description(value);
			case "required": return required(value);
			case "schema": return schema(value);
			case "type": return type(value);
			case "format": return format(value);
			case "allowEmptyValue": return allowEmptyValue(value);
			case "items": return items(value);
			case "collectionFormat": return collectionFormat(value);
			case "default": return _default(value);
			case "maximum": return maximum(value);
			case "exclusiveMaximum": return exclusiveMaximum(value);
			case "minimum": return minimum(value);
			case "exclusiveMinimum": return exclusiveMinimum(value);
			case "maxLength": return maxLength(value);
			case "minLength": return minLength(value);
			case "pattern": return pattern(value);
			case "maxItems": return maxItems(value);
			case "minItems": return minItems(value);
			case "uniqueItems": return uniqueItems(value);
			case "enum": return setEnum(null)._enum(value);
			case "multipleOf": return multipleOf(value);
			case "x-example": return example(value);
			case "x-examples": return examples(value);
			default: 
				super.set(property, value);
				return this;
		}
	}
	
	@Override /* SwaggerElement */
	public Set<String> keySet() {
		ASet<String> s = new ASet<String>()
			.appendIf(name != null, "name")
			.appendIf(in != null, "in")
			.appendIf(description != null, "description")
			.appendIf(required != null, "required")
			.appendIf(schema != null, "schema")
			.appendIf(type != null, "type")
			.appendIf(format != null, "format")
			.appendIf(allowEmptyValue != null, "allowEmptyValue")
			.appendIf(items != null, "items")
			.appendIf(collectionFormat != null, "collectionFormat")
			.appendIf(_default != null, "default")
			.appendIf(maximum != null, "maximum")
			.appendIf(exclusiveMaximum != null, "exclusiveMaximum")
			.appendIf(minimum != null, "minimum")
			.appendIf(exclusiveMinimum != null, "exclusiveMinimum")
			.appendIf(maxLength != null, "maxLength")
			.appendIf(minLength != null, "minLength")
			.appendIf(pattern != null, "pattern")
			.appendIf(maxItems != null, "maxItems")
			.appendIf(minItems != null, "minItems")
			.appendIf(uniqueItems != null, "uniqueItems")
			.appendIf(_enum != null, "enum")
			.appendIf(multipleOf != null, "multipleOf")
			.appendIf(example != null, "x-example")
			.appendIf(examples != null, "x-examples");
		return new MultiSet<>(s, super.keySet());
	}

	/**
	 * Resolves any <js>"$ref"</js> attributes in this element.
	 * 
	 * @param swagger The swagger document containing the definitions.
	 * @param refStack Keeps track of previously-visited references so that we don't cause recursive loops.
	 * @param maxDepth 
	 * 	The maximum depth to resolve references. 
	 * 	<br>After that level is reached, <code>$ref</code> references will be left alone.
	 * 	<br>Useful if you have very complex models and you don't want your swagger page to be overly-complex.
	 * @return 
	 * 	This object with references resolved.
	 * 	<br>May or may not be the same object.
	 */
	public ParameterInfo resolveRefs(Swagger swagger, Deque<String> refStack, int maxDepth) {
		
		if (schema != null)
			schema = schema.resolveRefs(swagger, refStack, maxDepth);
		
		if (items != null)
			items = items.resolveRefs(swagger, refStack, maxDepth);

		return this;
	}
}
