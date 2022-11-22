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
import static org.apache.juneau.internal.ArrayUtils.contains;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.marshaller.*;

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
 * 		For example, in <c>/items/{itemId}</c>, the path parameter is <c>itemId</c>.
 * 	<li><js>"query"</js> - Parameters that are appended to the URL.
 * 		For example, in <c>/items?id=###</c>, the query parameter is <c>id</c>.
 * 	<li><js>"header"</js> - Custom headers that are expected as part of the request.
 * 	<li><js>"body"</js> - The payload that's appended to the HTTP request.
 * 		Since there can only be one payload, there can only be one body parameter.
 * 		The name of the body parameter has no effect on the parameter itself and is used for documentation purposes
 * 		only.
 * 		Since Form parameters are also in the payload, body and form parameters cannot exist together for the same
 * 		operation.
 * 	<li><js>"formData"</js> - Used to describe the payload of an HTTP request when either
 * 		<c>application/x-www-form-urlencoded</c>, <c>multipart/form-data</c> or both are used as the
 * 		content type of the request (in Swagger's definition, the consumes property of an operation).
 * 		This is the only parameter type that can be used to send files, thus supporting the file type.
 * 		Since form parameters are sent in the payload, they cannot be declared together with a body parameter for the
 * 		same operation.
 * 		Form parameters have a different format based on the content-type used (for further details, consult
 * 		<c>http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4</c>):
 * 		<ul>
 * 			<li><js>"application/x-www-form-urlencoded"</js> - Similar to the format of Query parameters but as a
 * 				payload.
 * 				For example, <c>foo=1&amp;bar=swagger</c> - both <c>foo</c> and <c>bar</c> are form
 * 				parameters.
 * 				This is normally used for simple parameters that are being transferred.
 * 			<li><js>"multipart/form-data"</js> - each parameter takes a section in the payload with an internal header.
 * 				For example, for the header <c>Content-Disposition: form-data; name="submit-name"</c> the name of
 * 				the parameter is <c>submit-name</c>.
 * 				This type of form parameters is more commonly used for file transfers.
 * 		</ul>
 * 	</li>
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	ParameterInfo <jv>info</jv> = <jsm>parameterInfo</jsm>(<js>"query"</js>, <js>"foo"</js>);
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
 * 		<js>"in"</js>: <js>"query"</js>,
 * 		<js>"name"</js>: <js>"foo"</js>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Overview &gt; juneau-rest-server &gt; Swagger</a>
 * </ul>
 */
@Bean(properties="in,name,type,description,required,schema,format,allowEmptyValue,items,collectionFormat,default,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,enum,multipleOf,*")
@FluentSetters
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
	private Set<Object> _enum;
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

		this.allowEmptyValue = copyFrom.allowEmptyValue;
		this.collectionFormat = copyFrom.collectionFormat;
		this._default = copyFrom._default;
		this.description = copyFrom.description;
		this._enum = copyOf(copyFrom._enum);
		this.example = copyFrom.example;
		this.exclusiveMaximum = copyFrom.exclusiveMaximum;
		this.exclusiveMinimum = copyFrom.exclusiveMinimum;
		this.format = copyFrom.format;
		this.in = copyFrom.in;
		this.items = copyFrom.items == null ? null : copyFrom.items.copy();
		this.maximum = copyFrom.maximum;
		this.maxItems = copyFrom.maxItems;
		this.maxLength = copyFrom.maxLength;
		this.minimum = copyFrom.minimum;
		this.minItems = copyFrom.minItems;
		this.minLength = copyFrom.minLength;
		this.multipleOf = copyFrom.multipleOf;
		this.name = copyFrom.name;
		this.pattern = copyFrom.pattern;
		this.required = copyFrom.required;
		this.schema = copyFrom.schema == null ? null : copyFrom.schema.copy();
		this.type = copyFrom.type;
		this.uniqueItems = copyFrom.uniqueItems;

		this.examples = copyOf(copyFrom.examples);
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
	 * @return This object.
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
				example = p.example;
			if (p.examples != null)
				examples = p.examples;
		}
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>allowEmptyValue</property>.
	 *
	 * <p>
	 * Sets the ability to pass empty-valued parameters.
	 *
	 * <p>
	 * This is valid only for either <c>query</c> or <c>formData</c> parameters and allows you to send a
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
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * 	<br>Default is <jk>false</jk>.
	 * @return This object.
	 */
	public ParameterInfo setAllowEmptyValue(Boolean value) {
		allowEmptyValue = value;
		return this;
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
	 * 		<li><js>"csv"</js> (default) - comma separated values <c>foo,bar</c>.
	 * 		<li><js>"ssv"</js> - space separated values <c>foo bar</c>.
	 * 		<li><js>"tsv"</js> - tab separated values <c>foo\tbar</c>.
	 * 		<li><js>"pipes"</js> - pipe separated values <c>foo|bar</c>.
	 * 		<li><js>"multi"</js> - corresponds to multiple parameter instances instead of multiple values for a single
	 * 			instance <c>foo=bar&amp;foo=baz</c>.
	 * 			<br>This is valid only for parameters <c>in</c> <js>"query"</js> or <js>"formData"</js>.
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public ParameterInfo setCollectionFormat(String value) {
		if (isStrict() && ! contains(value, VALID_COLLECTION_FORMATS))
			throw new BasicRuntimeException(
				"Invalid value passed in to setCollectionFormat(String).  Value=''{0}'', valid values={1}",
				value, Json5.of(VALID_COLLECTION_FORMATS)
			);
		collectionFormat = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>default</property>.
	 *
	 * <p>
	 * Declares the value of the parameter that the server will use if none is provided, for example a <js>"count"</js>
	 * to control the number of results per page might default to 100 if not supplied by the client in the request.
	 *
	 * (Note: <js>"default"</js> has no meaning for required parameters.)
	 * Unlike JSON Schema this value MUST conform to the defined <c>type</c> for this parameter.
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
	 * Unlike JSON Schema this value MUST conform to the defined <c>type</c> for this parameter.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public ParameterInfo setDefault(Object value) {
		_default = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * <p>
	 * A brief description of the parameter.
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
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br><a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used for rich text representation.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public ParameterInfo setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>enum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<Object> getEnum() {
		return _enum;
	}

	/**
	 * Bean property setter:  <property>enum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public ParameterInfo setEnum(Collection<Object> value) {
		_enum = setFrom(value);
		return this;
	}

	/**
	 * Bean property appender:  <property>enum</property>.
	 *
	 * @param value
	 * 	The values to add to this property.
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object.
	 */
	public ParameterInfo addEnum(Object...value) {
		_enum = setBuilder(_enum).sparse().add(value).build();
		return this;
	}

	/**
	 * Bean property fluent setter:  <property>enum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Strings can be JSON arrays.
	 * @return This object.
	 */
	public ParameterInfo setEnum(Object...value) {
		setEnum(setBuilder(Object.class).sparse().addAny(value).build());
		return this;
	}

	/**
	 * Bean property getter:  <property>exclusiveMaximum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExclusiveMaximum() {
		return exclusiveMaximum;
	}

	/**
	 * Bean property setter:  <property>exclusiveMaximum</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public ParameterInfo setExclusiveMaximum(Boolean value) {
		exclusiveMaximum = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>exclusiveMinimum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExclusiveMinimum() {
		return exclusiveMinimum;
	}

	/**
	 * Bean property setter:  <property>exclusiveMinimum</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public ParameterInfo setExclusiveMinimum(Boolean value) {
		exclusiveMinimum = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>format</property>.
	 *
	 * <p>
	 * The extending format for the previously mentioned type.
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
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public ParameterInfo setFormat(String value) {
		format = value;
		return this;
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
	 * @return This object.
	 */
	public ParameterInfo setIn(String value) {
		if (isStrict() && ! contains(value, VALID_IN))
			throw new BasicRuntimeException(
				"Invalid value passed in to setIn(String).  Value=''{0}'', valid values={1}",
				value, Json5.of(VALID_IN)
			);
		in = value;
		if ("path".equals(value))
			required = true;
		return this;
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
	 * 	<br>Property value is required if <c>type</c> is <js>"array"</js>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public ParameterInfo setItems(Items value) {
		items = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>maximum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMaximum() {
		return maximum;
	}

	/**
	 * Bean property setter:  <property>maximum</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public ParameterInfo setMaximum(Number value) {
		maximum = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>maxItems</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxItems() {
		return maxItems;
	}

	/**
	 * Bean property setter:  <property>maxItems</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public ParameterInfo setMaxItems(Integer value) {
		maxItems = value;
		return this;
	}


	/**
	 * Bean property getter:  <property>maxLength</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxLength() {
		return maxLength;
	}

	/**
	 * Bean property setter:  <property>maxLength</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public ParameterInfo setMaxLength(Integer value) {
		maxLength = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>minimum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMinimum() {
		return minimum;
	}

	/**
	 * Bean property setter:  <property>minimum</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public ParameterInfo setMinimum(Number value) {
		minimum = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>minItems</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinItems() {
		return minItems;
	}

	/**
	 * Bean property setter:  <property>minItems</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public ParameterInfo setMinItems(Integer value) {
		minItems = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>minLength</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinLength() {
		return minLength;
	}

	/**
	 * Bean property setter:  <property>minLength</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public ParameterInfo setMinLength(Integer value) {
		minLength = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>multipleOf</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMultipleOf() {
		return multipleOf;
	}

	/**
	 * Bean property setter:  <property>multipleOf</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public ParameterInfo setMultipleOf(Number value) {
		multipleOf = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * <p>
	 * The name of the parameter.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Parameter names are case sensitive.
	 * 	<li class='note'>
	 * 		If <c>in</c> is <js>"path"</js>, the <c>name</c> field MUST correspond to the associated path segment
	 * 		from the <c>path</c> field in the <a class="doclink" href="https://swagger.io/specification/v2#pathsObject">Paths Object</a>.
	 * 	<li class='note'>
	 * 		For all other cases, the name corresponds to the parameter name used based on the <c>in</c> property.
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
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * @return This object.
	 */
	public ParameterInfo setName(String value) {
		if (! "body".equals(in))
			name = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>pattern</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getPattern() {
		return pattern;
	}

	/**
	 * Bean property setter:  <property>pattern</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>This string SHOULD be a valid regular expression.
	 * @return This object.
	 */
	public ParameterInfo setPattern(String value) {
		pattern = value;
		return this;
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
	 * @param value
	 * 	The new value for this property.
	 * 	<br>If the parameter is <c>in</c> <js>"path"</js>, this property is required and its value MUST be <jk>true</jk>.
	 * 	<br>Otherwise, the property MAY be included and its default value is <jk>false</jk>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public ParameterInfo setRequired(Boolean value) {
		required = value;
		return this;
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
	 * @return This object.
	 */
	public ParameterInfo setSchema(SchemaInfo value) {
		schema = value;
		return this;
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
	 * 	<br>If type is <js>"file"</js>, the <c>consumes</c> MUST be either <js>"multipart/form-data"</js>, <js>"application/x-www-form-urlencoded"</js>
	 * 		or both and the parameter MUST be <c>in</c> <js>"formData"</js>.
	 * 	<br>Property value is required.
	 * @return This object.
	 */
	public ParameterInfo setType(String value) {
		if (isStrict() && ! contains(value, VALID_TYPES))
			throw new BasicRuntimeException(
				"Invalid value passed in to setType(String).  Value=''{0}'', valid values={1}",
				value, Json5.of(VALID_TYPES)
			);
		type = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>uniqueItems</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getUniqueItems() {
		return uniqueItems;
	}

	/**
	 * Bean property setter:  <property>uniqueItems</property>.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public ParameterInfo setUniqueItems(Boolean value) {
		uniqueItems = value;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "allowEmptyValue": return toType(getAllowEmptyValue(), type);
			case "collectionFormat": return toType(getCollectionFormat(), type);
			case "default": return toType(getDefault(), type);
			case "description": return toType(getDescription(), type);
			case "enum": return toType(getEnum(), type);
			case "exclusiveMaximum": return toType(getExclusiveMaximum(), type);
			case "exclusiveMinimum": return toType(getExclusiveMinimum(), type);
			case "format": return toType(getFormat(), type);
			case "in": return toType(getIn(), type);
			case "items": return toType(getItems(), type);
			case "maximum": return toType(getMaximum(), type);
			case "maxItems": return toType(getMaxItems(), type);
			case "maxLength": return toType(getMaxLength(), type);
			case "minimum": return toType(getMinimum(), type);
			case "minItems": return toType(getMinItems(), type);
			case "minLength": return toType(getMinLength(), type);
			case "multipleOf": return toType(getMultipleOf(), type);
			case "name": return toType(getName(), type);
			case "pattern": return toType(getPattern(), type);
			case "required": return toType(getRequired(), type);
			case "schema": return toType(getSchema(), type);
			case "type": return toType(getType(), type);
			case "uniqueItems": return toType(getUniqueItems(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public ParameterInfo set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "allowEmptyValue": return setAllowEmptyValue(toBoolean(value));
			case "collectionFormat": return setCollectionFormat(stringify(value));
			case "default": return setDefault(value);
			case "description": return setDescription(stringify(value));
			case "enum": return setEnum(value);
			case "exclusiveMaximum": return setExclusiveMaximum(toBoolean(value));
			case "exclusiveMinimum": return setExclusiveMinimum(toBoolean(value));
			case "format": return setFormat(stringify(value));
			case "in": return setIn(stringify(value));
			case "items": return setItems(toType(value, Items.class));
			case "maximum": return setMaximum(toNumber(value));
			case "maxItems": return setMaxItems(toInteger(value));
			case "maxLength": return setMaxLength(toInteger(value));
			case "minimum": return setMinimum(toNumber(value));
			case "minItems": return setMinItems(toInteger(value));
			case "minLength": return setMinLength(toInteger(value));
			case "multipleOf": return setMultipleOf(toNumber(value));
			case "name": return setName(stringify(value));
			case "pattern": return setPattern(stringify(value));
			case "required": return setRequired(toBoolean(value));
			case "schema": return setSchema(toType(value, SchemaInfo.class));
			case "type": return setType(stringify(value));
			case "uniqueItems": return setUniqueItems(toBoolean(value));
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
			.addIf(allowEmptyValue != null, "allowEmptyValue")
			.addIf(collectionFormat != null, "collectionFormat")
			.addIf(_default != null, "default")
			.addIf(description != null, "description")
			.addIf(_enum != null, "enum")
			.addIf(example != null, "example")
			.addIf(examples != null, "examples")
			.addIf(exclusiveMaximum != null, "exclusiveMaximum")
			.addIf(exclusiveMinimum != null, "exclusiveMinimum")
			.addIf(format != null, "format")
			.addIf(in != null, "in")
			.addIf(items != null, "items")
			.addIf(maximum != null, "maximum")
			.addIf(maxItems != null, "maxItems")
			.addIf(maxLength != null, "maxLength")
			.addIf(minimum != null, "minimum")
			.addIf(minItems != null, "minItems")
			.addIf(minLength != null, "minLength")
			.addIf(multipleOf != null, "multipleOf")
			.addIf(name != null, "name")
			.addIf(pattern != null, "pattern")
			.addIf(required != null, "required")
			.addIf(schema != null, "schema")
			.addIf(type != null, "type")
			.addIf(uniqueItems != null, "uniqueItems")
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
	public ParameterInfo resolveRefs(Swagger swagger, Deque<String> refStack, int maxDepth) {

		if (schema != null)
			schema = schema.resolveRefs(swagger, refStack, maxDepth);

		if (items != null)
			items = items.resolveRefs(swagger, refStack, maxDepth);

		return this;
	}
}
