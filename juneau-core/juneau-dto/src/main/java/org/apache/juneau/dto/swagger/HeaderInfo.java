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

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.marshaller.*;

/**
 * Describes a single HTTP header.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	HeaderInfo <jv>headerInfo</jv> = <jsm>headerInfo</jsm>(<js>"integer"</js>).description(<js>"The number of allowed requests in the current period"</js>);
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.toString(<jv>headerInfo</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	<jv>json</jv> = <jv>headerInfo</jv>.toString();
 * </p>
 * <p class='bjson'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"description"</js>: <js>"The number of allowed requests in the current period"</js>,
 * 		<js>"type"</js>: <js>"integer"</js>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Overview &gt; juneau-rest-server &gt; Swagger</a>
 * </ul>
 */
@Bean(properties="description,type,format,items,collectionFormat,default,maximum,exclusiveMaximum,minimum,exclusiveMinimum,maxLength,minLength,pattern,maxItems,minItems,uniqueItems,enum,multipleOf,$ref,example,*")
@SuppressWarnings({"unchecked"})
@FluentSetters
public class HeaderInfo extends SwaggerElement {

	private static final String[] VALID_TYPES = {"string", "number", "integer", "boolean", "array"};
	private static final String[] VALID_COLLECTION_FORMATS = {"csv","ssv","tsv","pipes","multi"};

	private String
		description,
		type,
		format,
		collectionFormat,
		pattern,
		ref;
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
		exclusiveMaximum,
		exclusiveMinimum,
		uniqueItems;
	private Items items;
	private Object _default;
	private Set<Object> _enum;
	private Object example;

	/**
	 * Default constructor.
	 */
	public HeaderInfo() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public HeaderInfo(HeaderInfo copyFrom) {
		super(copyFrom);

		this.collectionFormat = copyFrom.collectionFormat;
		this._default = copyFrom._default;
		this.description = copyFrom.description;
		this._enum = copyOf(copyFrom._enum);
		this.example = copyFrom.example;
		this.exclusiveMaximum = copyFrom.exclusiveMaximum;
		this.exclusiveMinimum = copyFrom.exclusiveMinimum;
		this.format = copyFrom.format;
		this.items = copyFrom.items == null ? null : copyFrom.items.copy();
		this.maximum = copyFrom.maximum;
		this.maxItems = copyFrom.maxItems;
		this.maxLength = copyFrom.maxLength;
		this.minimum = copyFrom.minimum;
		this.minItems = copyFrom.minItems;
		this.minLength = copyFrom.minLength;
		this.multipleOf = copyFrom.multipleOf;
		this.pattern = copyFrom.pattern;
		this.type = copyFrom.type;
		this.uniqueItems = copyFrom.uniqueItems;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public HeaderInfo copy() {
		return new HeaderInfo(this);
	}

	@Override /* SwaggerElement */
	protected HeaderInfo strict() {
		super.strict();
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

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
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setCollectionFormat(String value) {
		if (isStrict() && ! ArrayUtils.contains(value, VALID_COLLECTION_FORMATS))
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
	 * Declares the value of the header that the server will use if none is provided.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		<js>"default"</js> has no meaning for required items.
	 * 	<li class='note'>
	 * 		Unlike JSON Schema this value MUST conform to the defined <c>type</c> for the header.
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
	 * Declares the value of the header that the server will use if none is provided.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		<js>"default"</js> has no meaning for required items.
	 * 	<li class='note'>
	 * 		Unlike JSON Schema this value MUST conform to the defined <c>type</c> for the header.
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setDefault(Object value) {
		_default = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * <p>
	 * A short description of the header.
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
	 * A short description of the header.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setDescription(String value) {
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
	public HeaderInfo setEnum(Collection<Object> value) {
		_enum = setFrom(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>enum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object.
	 */
	public HeaderInfo setEnum(Object...value) {
		return setEnum(Arrays.asList(value));
	}

	/**
	 * Bean property fluent setter:  <property>enum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Strings can be JSON arrays.
	 * @return This object.
	 */
	public HeaderInfo addEnum(Object...value) {
		setEnum(setBuilder(_enum).sparse().add(value).build());
		return this;
	}

	/**
	 * Bean property getter:  <property>example</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Object getExample() {
		return example;
	}

	/**
	 * Bean property setter:  <property>example</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setExample(Object value) {
		example = value;
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
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setExclusiveMaximum(Boolean value) {
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
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setExclusiveMinimum(Boolean value) {
		exclusiveMinimum = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>format</property>.
	 *
	 * <p>
	 * The extending format for the previously mentioned <c>type</c>.
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
	 * The extending format for the previously mentioned <c>type</c>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setFormat(String value) {
		format = value;
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
	public HeaderInfo setItems(Items value) {
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
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setMaximum(Number value) {
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
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setMaxItems(Integer value) {
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
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setMaxLength(Integer value) {
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
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setMinimum(Number value) {
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
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setMinItems(Integer value) {
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
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setMinLength(Integer value) {
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
	public HeaderInfo setMultipleOf(Number value) {
		multipleOf = value;
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
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setPattern(String value) {
		pattern = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>$ref</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Beanp("$ref")
	public String getRef() {
		return ref;
	}

	/**
	 * Bean property setter:  <property>$ref</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	@Beanp("$ref")
	public HeaderInfo setRef(String value) {
		ref = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * <p>
	 * The type of the object.
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
	 * The type of the object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li><js>"string"</js>
	 * 		<li><js>"number"</js>
	 * 		<li><js>"integer"</js>
	 * 		<li><js>"boolean"</js>
	 * 		<li><js>"array"</js>
	 * 	</ul>
	 * @return This object.
	 */
	public HeaderInfo setType(String value) {
		if (isStrict() && ! ArrayUtils.contains(value, VALID_TYPES))
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
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public HeaderInfo setUniqueItems(Boolean value) {
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
			case "collectionFormat": return toType(getCollectionFormat(), type);
			case "default": return toType(getDefault(), type);
			case "description": return (T)getDescription();
			case "enum": return toType(getEnum(), type);
			case "example": return toType(getExample(), type);
			case "exclusiveMaximum": return toType(getExclusiveMaximum(), type);
			case "exclusiveMinimum": return toType(getExclusiveMinimum(), type);
			case "format": return toType(getFormat(), type);
			case "items": return toType(getItems(), type);
			case "maximum": return toType(getMaximum(), type);
			case "maxItems": return toType(getMaxItems(), type);
			case "maxLength": return toType(getMaxLength(), type);
			case "minimum": return toType(getMinimum(), type);
			case "minItems": return toType(getMinItems(), type);
			case "minLength": return toType(getMinLength(), type);
			case "multipleOf": return toType(getMultipleOf(), type);
			case "pattern": return toType(getPattern(), type);
			case "$ref": return toType(getRef(), type);
			case "type": return toType(getType(), type);
			case "uniqueItems": return toType(getUniqueItems(), type);
		default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public HeaderInfo set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "collectionFormat": return setCollectionFormat(stringify(value));
			case "default": return setDefault(value);
			case "description": return setDescription(stringify(value));
			case "enum": return setEnum(setBuilder(Object.class).sparse().addAny(value).build());
			case "example": return setExample(value);
			case "exclusiveMaximum": return setExclusiveMaximum(toBoolean(value));
			case "exclusiveMinimum": return setExclusiveMinimum(toBoolean(value));
			case "format": return setFormat(stringify(value));
			case "items": return setItems(toType(value, Items.class));
			case "maximum": return setMaximum(toNumber(value));
			case "maxItems": return setMaxItems(toInteger(value));
			case "maxLength": return setMaxLength(toInteger(value));
			case "minimum": return setMinimum(toNumber(value));
			case "minItems": return setMinItems(toInteger(value));
			case "minLength": return setMinLength(toInteger(value));
			case "multipleOf": return setMultipleOf(toNumber(value));
			case "pattern": return setPattern(stringify(value));
			case "$ref": return setRef(stringify(value));
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
			.addIf(collectionFormat != null, "collectionFormat")
			.addIf(_default != null, "default")
			.addIf(description != null, "description")
			.addIf(_enum != null, "enum")
			.addIf(example != null, "example")
			.addIf(exclusiveMaximum != null, "exclusiveMaximum")
			.addIf(exclusiveMinimum != null, "exclusiveMinimum")
			.addIf(format != null, "format")
			.addIf(items != null, "items")
			.addIf(maximum != null, "maximum")
			.addIf(maxItems != null, "maxItems")
			.addIf(maxLength != null, "maxLength")
			.addIf(minimum != null, "minimum")
			.addIf(minItems != null, "minItems")
			.addIf(minLength != null, "minLength")
			.addIf(multipleOf != null, "multipleOf")
			.addIf(pattern != null, "pattern")
			.addIf(ref != null, "$ref")
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
	public HeaderInfo resolveRefs(Swagger swagger, Deque<String> refStack, int maxDepth) {

		if (ref != null) {
			if (refStack.contains(ref) || refStack.size() >= maxDepth)
				return this;
			refStack.addLast(ref);
			HeaderInfo r = swagger.findRef(ref, HeaderInfo.class).resolveRefs(swagger, refStack, maxDepth);
			refStack.removeLast();
			return r;
		}

		if (items != null)
			items = items.resolveRefs(swagger, refStack, maxDepth);

		return this;
	}
}
