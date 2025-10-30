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
package org.apache.juneau.bean.swagger;

import static org.apache.juneau.common.utils.AssertionUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.common.collections.*;
import org.apache.juneau.marshaller.*;

/**
 * A limited subset of JSON-Schema's items object.
 *
 * <p>
 * The Items Object is a limited subset of JSON-Schema's items object for Swagger 2.0. It is used by parameter
 * definitions that are not located in "body" to describe the type of items in an array. This is particularly useful
 * for query parameters, path parameters, and header parameters that accept arrays.
 *
 * <h5 class='section'>Swagger Specification:</h5>
 * <p>
 * The Items Object supports the following fields from JSON Schema:
 * <ul class='spaced-list'>
 * 	<li><c>type</c> (string, REQUIRED) - The data type. Values: <js>"string"</js>, <js>"number"</js>, <js>"integer"</js>, <js>"boolean"</js>, <js>"array"</js>
 * 	<li><c>format</c> (string) - The format modifier (e.g., <js>"int32"</js>, <js>"int64"</js>, <js>"float"</js>, <js>"double"</js>, <js>"date"</js>, <js>"date-time"</js>)
 * 	<li><c>items</c> ({@link Items}) - Required if type is <js>"array"</js>. Describes the type of items in the array
 * 	<li><c>collectionFormat</c> (string) - How multiple values are formatted. Values: <js>"csv"</js>, <js>"ssv"</js>, <js>"tsv"</js>, <js>"pipes"</js>, <js>"multi"</js>
 * 	<li><c>default</c> (any) - The default value
 * 	<li><c>maximum</c> (number), <c>exclusiveMaximum</c> (boolean), <c>minimum</c> (number), <c>exclusiveMinimum</c> (boolean) - Numeric constraints
 * 	<li><c>maxLength</c> (integer), <c>minLength</c> (integer), <c>pattern</c> (string) - String constraints
 * 	<li><c>maxItems</c> (integer), <c>minItems</c> (integer), <c>uniqueItems</c> (boolean) - Array constraints
 * 	<li><c>enum</c> (array) - Possible values for this item
 * 	<li><c>multipleOf</c> (number) - Must be a multiple of this value
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	Items <jv>items</jv> = <jsm>items</jsm>(<js>"string"</js>).minLength(2);
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = Json.<jsm>from</jsm>(<jv>items</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	<jv>json</jv> = <jv>items</jv>.toString();
 * </p>
 * <p class='bjson'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"type"</js>: <js>"string"</js>,
 * 		<js>"minLength"</js>: 2
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/specification/v2/#items-object">Swagger 2.0 Specification &gt; Items Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/2-0/describing-parameters/">Swagger Describing Parameters</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanSwagger2">juneau-bean-swagger-v2</a>
 * </ul>
 */
public class Items extends SwaggerElement {

	private static final String[] VALID_TYPES = { "string", "number", "integer", "boolean", "array" };
	private static final String[] VALID_COLLECTION_FORMATS = { "csv", "ssv", "tsv", "pipes", "multi" };

	private String type, format, collectionFormat, pattern, ref;
	private Number maximum, minimum, multipleOf;
	private Integer maxLength, minLength, maxItems, minItems;
	private Boolean exclusiveMaximum, exclusiveMinimum, uniqueItems;
	private Items items;  // NOSONAR - Intentional naming.
	private Object _default;  // NOSONAR - Intentional naming.
	private Set<Object> _enum;  // NOSONAR - Intentional naming.

	/**
	 * Default constructor.
	 */
	public Items() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Items(Items copyFrom) {
		super(copyFrom);

		this.collectionFormat = copyFrom.collectionFormat;
		this._default = copyFrom._default;
		this._enum = copyOf(copyFrom._enum);
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
		this.ref = copyFrom.ref;
		this.type = copyFrom.type;
		this.uniqueItems = copyFrom.uniqueItems;
	}

	/**
	 * Bean property fluent setter:  <property>enum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>String values can be JSON arrays.
	 * @return This object.
	 */
	public Items addEnum(Object...value) {
		setEnum(setb(Object.class).to(_enum).sparse().add(value).build());
		return this;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Items copy() {
		return new Items(this);
	}

	@Override /* Overridden from SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "collectionFormat" -> toType(getCollectionFormat(), type);
			case "default" -> toType(getDefault(), type);
			case "enum" -> toType(getEnum(), type);
			case "exclusiveMaximum" -> toType(getExclusiveMaximum(), type);
			case "exclusiveMinimum" -> toType(getExclusiveMinimum(), type);
			case "format" -> toType(getFormat(), type);
			case "items" -> toType(getItems(), type);
			case "maximum" -> toType(getMaximum(), type);
			case "maxItems" -> toType(getMaxItems(), type);
			case "maxLength" -> toType(getMaxLength(), type);
			case "minimum" -> toType(getMinimum(), type);
			case "minItems" -> toType(getMinItems(), type);
			case "minLength" -> toType(getMinLength(), type);
			case "multipleOf" -> toType(getMultipleOf(), type);
			case "pattern" -> toType(getPattern(), type);
			case "$ref" -> toType(getRef(), type);
			case "type" -> toType(getType(), type);
			case "uniqueItems" -> toType(getUniqueItems(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Bean property getter:  <property>collectionFormat</property>.
	 *
	 * <p>
	 * Determines the format of the array if type array is used.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getCollectionFormat() { return collectionFormat; }

	/**
	 * Bean property getter:  <property>default</property>.
	 *
	 * <p>
	 * Declares the value of the item that the server will use if none is provided.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		<js>"default"</js> has no meaning for required items.
	 * 	<li class='note'>
	 * 		Unlike JSON Schema this value MUST conform to the defined <c>type</c> for the data type.
	 * </ul>
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Object getDefault() { return _default; }

	/**
	 * Bean property getter:  <property>enum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Set<Object> getEnum() { return _enum; }

	/**
	 * Bean property getter:  <property>exclusiveMaximum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExclusiveMaximum() { return exclusiveMaximum; }

	/**
	 * Bean property getter:  <property>exclusiveMinimum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExclusiveMinimum() { return exclusiveMinimum; }

	/**
	 * Bean property getter:  <property>format</property>.
	 *
	 * <p>
	 * The extending format for the previously mentioned <c>type</c>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getFormat() { return format; }

	/**
	 * Bean property getter:  <property>items</property>.
	 *
	 * <p>
	 * Describes the type of items in the array.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Items getItems() { return items; }

	/**
	 * Bean property getter:  <property>maximum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMaximum() { return maximum; }

	/**
	 * Bean property getter:  <property>maxItems</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxItems() { return maxItems; }

	/**
	 * Bean property getter:  <property>maxLength</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMaxLength() { return maxLength; }

	/**
	 * Bean property getter:  <property>minimum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMinimum() { return minimum; }

	/**
	 * Bean property getter:  <property>minItems</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinItems() { return minItems; }

	/**
	 * Bean property getter:  <property>minLength</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Integer getMinLength() { return minLength; }

	/**
	 * Bean property getter:  <property>multipleOf</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Number getMultipleOf() { return multipleOf; }

	/**
	 * Bean property getter:  <property>pattern</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getPattern() { return pattern; }

	/**
	 * Bean property getter:  <property>$ref</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Beanp("$ref")
	public String getRef() { return ref; }

	/**
	 * Bean property getter:  <property>type</property>.
	 *
	 * <p>
	 * The internal type of the array.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getType() { return type; }

	/**
	 * Bean property getter:  <property>uniqueItems</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getUniqueItems() { return uniqueItems; }

	@Override /* Overridden from SwaggerElement */
	public Set<String> keySet() {
		// @formatter:off
		var s = setb(String.class)
			.addIf(nn(collectionFormat), "collectionFormat")
			.addIf(nn(_default), "default")
			.addIf(nn(_enum), "enum")
			.addIf(nn(exclusiveMaximum), "exclusiveMaximum")
			.addIf(nn(exclusiveMinimum), "exclusiveMinimum")
			.addIf(nn(format), "format")
			.addIf(nn(items), "items")
			.addIf(nn(maximum), "maximum")
			.addIf(nn(maxItems), "maxItems")
			.addIf(nn(maxLength), "maxLength")
			.addIf(nn(minimum), "minimum")
			.addIf(nn(minItems), "minItems")
			.addIf(nn(minLength), "minLength")
			.addIf(nn(multipleOf), "multipleOf")
			.addIf(nn(pattern), "pattern")
			.addIf(nn(ref), "$ref")
			.addIf(nn(type), "type")
			.addIf(nn(uniqueItems), "uniqueItems")
			.build();
		// @formatter:on
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
	public Items resolveRefs(Swagger swagger, Deque<String> refStack, int maxDepth) {

		if (nn(ref)) {
			if (refStack.contains(ref) || refStack.size() >= maxDepth)
				return this;
			refStack.addLast(ref);
			var r = swagger.findRef(ref, Items.class).resolveRefs(swagger, refStack, maxDepth);
			refStack.removeLast();
			return r;
		}

		if (nn(items))
			items = items.resolveRefs(swagger, refStack, maxDepth);

		return this;
	}

	@Override /* Overridden from SwaggerElement */
	public Items set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "collectionFormat" -> setCollectionFormat(s(value));
			case "default" -> setDefault(value);
			case "enum" -> setEnum(listb(Object.class).addAny(value).sparse().build());
			case "exclusiveMaximum" -> setExclusiveMaximum(toBoolean(value));
			case "exclusiveMinimum" -> setExclusiveMinimum(toBoolean(value));
			case "format" -> setFormat(s(value));
			case "items" -> setItems(toType(value, Items.class));
			case "maximum" -> setMaximum(toNumber(value));
			case "maxItems" -> setMaxItems(toInteger(value));
			case "maxLength" -> setMaxLength(toInteger(value));
			case "minimum" -> setMinimum(toNumber(value));
			case "minItems" -> setMinItems(toInteger(value));
			case "minLength" -> setMinLength(toInteger(value));
			case "multipleOf" -> setMultipleOf(toNumber(value));
			case "pattern" -> setPattern(s(value));
			case "$ref" -> setRef(s(value));
			case "type" -> setType(s(value));
			case "uniqueItems" -> setUniqueItems(toBoolean(value));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
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
	public Items setCollectionFormat(String value) {
		if (isStrict() && ! contains(value, VALID_COLLECTION_FORMATS))
			throw runtimeException("Invalid value passed in to setCollectionFormat(String).  Value=''{0}'', valid values={1}", value, Json5.of(VALID_COLLECTION_FORMATS));
		collectionFormat = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>default</property>.
	 *
	 * <p>
	 * Declares the value of the item that the server will use if none is provided.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		<js>"default"</js> has no meaning for required items.
	 * 	<li class='note'>
	 * 		Unlike JSON Schema this value MUST conform to the defined <c>type</c> for the data type.
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Items setDefault(Object value) {
		_default = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>enum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Items setEnum(Collection<Object> value) {
		_enum = toSet(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>enum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Items setEnum(Object...value) {
		return setEnum(l(value));
	}

	/**
	 * Bean property setter:  <property>exclusiveMaximum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Items setExclusiveMaximum(Boolean value) {
		exclusiveMaximum = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>exclusiveMinimum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Items setExclusiveMinimum(Boolean value) {
		exclusiveMinimum = value;
		return this;
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
	public Items setFormat(String value) {
		format = value;
		return this;
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
	public Items setItems(Items value) {
		items = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>maximum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Items setMaximum(Number value) {
		maximum = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>maxItems</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Items setMaxItems(Integer value) {
		maxItems = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>maxLength</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Items setMaxLength(Integer value) {
		maxLength = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>minimum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Items setMinimum(Number value) {
		minimum = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>minItems</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Items setMinItems(Integer value) {
		minItems = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>minLength</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Items setMinLength(Integer value) {
		minLength = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>multipleOf</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Items setMultipleOf(Number value) {
		multipleOf = value;
		return this;
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
	public Items setPattern(String value) {
		pattern = value;
		return this;
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
	public Items setRef(String value) {
		ref = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>type</property>.
	 *
	 * <p>
	 * The internal type of the array.
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
	 * 	</ul>
	 * 	<br>Property value is required.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Items setType(String value) {
		if (isStrict() && ! contains(value, VALID_TYPES))
			throw runtimeException("Invalid value passed in to setType(String).  Value=''{0}'', valid values={1}", value, Json5.of(VALID_TYPES));
		type = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>uniqueItems</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Items setUniqueItems(Boolean value) {
		uniqueItems = value;
		return this;
	}

	@Override /* Overridden from SwaggerElement */
	public Items strict() {
		super.strict();
		return this;
	}

	/**
	 * Sets strict mode on this bean.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-boolean values will be converted to boolean using <code>Boolean.<jsm>valueOf</jsm>(value.toString())</code>.
	 * 	<br>Can be <jk>null</jk> (interpreted as <jk>false</jk>).
	 * @return This object.
	 */
	@Override
	public Items strict(Object value) {
		super.strict(value);
		return this;
	}
}