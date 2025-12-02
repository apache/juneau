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
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.collections.*;

/**
 * Describes a single HTTP header.
 *
 * <p>
 * The Header Object follows the structure of the Parameter Object with the following changes: it does not have a
 * <c>name</c> field since the header name is specified in the key, and it does not have a <c>required</c> field
 * since headers are always optional in HTTP.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The Header Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>description</c> (string) - A brief description of the header (CommonMark syntax may be used)
 * 	<li><c>required</c> (boolean) - Determines whether this header is mandatory (default is <jk>false</jk>)
 * 	<li><c>deprecated</c> (boolean) - Specifies that a header is deprecated
 * 	<li><c>allowEmptyValue</c> (boolean) - Sets the ability to pass empty-valued headers
 * 	<li><c>style</c> (string) - Describes how the header value will be serialized
 * 	<li><c>explode</c> (boolean) - When true, header values of type array or object generate separate headers for each value
 * 	<li><c>allowReserved</c> (boolean) - Determines whether the header value should allow reserved characters
 * 	<li><c>schema</c> ({@link SchemaInfo}) - The schema defining the type used for the header
 * 	<li><c>example</c> (any) - Example of the header's potential value
 * 	<li><c>examples</c> (map of {@link Example}) - Examples of the header's potential value
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	HeaderInfo <jv>x</jv> = <jsm>headerInfo</jsm>(<js>"integer"</js>).description(<js>"The number of allowed requests in the current period"</js>);
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = Json.<jsm>from</jsm>(<jv>x</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	String <jv>json</jv> = <jv>x</jv>.toString();
 * </p>
 * <p class='bcode'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"description"</js>: <js>"The number of allowed requests in the current period"</js>,
 * 		<js>"type"</js>: <js>"integer"</js>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#header-object">OpenAPI Specification &gt; Header Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/describing-parameters/">OpenAPI Describing Parameters</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
public class HeaderInfo extends OpenApiElement {

	private String description, ref;
	private Boolean required, explode, deprecated, allowEmptyValue, allowReserved;
	private SchemaInfo schema;
	private Object example;
	private Map<String,Example> examples;

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

		this.description = copyFrom.description;
		this.example = copyFrom.example;
		this.allowEmptyValue = copyFrom.allowEmptyValue;
		this.schema = copyFrom.schema;
		this.allowReserved = copyFrom.allowReserved;
		this.required = copyFrom.required;
		this.ref = copyFrom.ref;
		this.explode = copyFrom.explode;
		this.deprecated = copyFrom.deprecated;
		this.examples = copyOf(copyFrom.examples, Example::copy);
	}

	/**
	 * Adds a single value to the <property>examples</property> property.
	 *
	 * @param name The example name.  Must not be <jk>null</jk>.
	 * @param example The example.  Must not be <jk>null</jk>.
	 * @return This object
	 */
	public HeaderInfo addExample(String name, Example example) {
		assertArgNotNull("name", name);
		assertArgNotNull("example", example);
		examples = mapb(String.class, Example.class).to(examples).sparse().add(name, example).build();
		return this;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public HeaderInfo copy() {
		return new HeaderInfo(this);
	}

	@Override /* Overridden from OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "description" -> toType(getDescription(), type);
			case "required" -> toType(getRequired(), type);
			case "explode" -> toType(getExplode(), type);
			case "deprecated" -> toType(getDeprecated(), type);
			case "allowEmptyValue" -> toType(getAllowEmptyValue(), type);
			case "allowReserved" -> toType(getAllowReserved(), type);
			case "$ref" -> toType(getRef(), type);
			case "schema" -> toType(getSchema(), type);
			case "x-example" -> toType(getExample(), type);
			case "examples" -> toType(getExamples(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Bean property getter:  <property>allowEmptyValue</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getAllowEmptyValue() { return allowEmptyValue; }

	/**
	 * Bean property getter:  <property>allowReserved</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getAllowReserved() { return allowReserved; }

	/**
	 * Bean property getter:  <property>deprecated</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getDeprecated() { return deprecated; }

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * <p>
	 * A short description of the header.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() { return description; }

	/**
	 * Bean property getter:  <property>x-example</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Beanp("x-example")
	public Object getExample() { return example; }

	/**
	 * Bean property getter:  <property>examples</property>.
	 *
	 * <p>
	 * The list of possible responses as they are returned from executing this operation.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Example> getExamples() { return examples; }

	/**
	 * Bean property getter:  <property>required</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExplode() { return explode; }

	/**
	 * Bean property getter:  <property>$ref</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Beanp("$ref")
	public String getRef() { return ref; }

	/**
	 * Bean property getter:  <property>required</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getRequired() { return required; }

	/**
	 * Bean property getter:  <property>schema</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public SchemaInfo getSchema() { return schema; }

	@Override /* Overridden from SwaggerElement */
	public Set<String> keySet() {
		// @formatter:off
		var s = setb(String.class)
			.addIf(nn(ref), "$ref")
			.addIf(nn(allowEmptyValue), "allowEmptyValue")
			.addIf(nn(allowReserved), "allowReserved")
			.addIf(nn(deprecated), "deprecated")
			.addIf(nn(description), "description")
			.addIf(nn(examples), "examples")
			.addIf(nn(explode), "explode")
			.addIf(nn(required), "required")
			.addIf(nn(schema), "schema")
			.addIf(nn(example), "x-example")
			.build();
		// @formatter:on
		return new MultiSet<>(s, super.keySet());
	}

	/**
	 * Resolves any <js>"$ref"</js> attributes in this element.
	 *
	 * @param openApi The swagger document containing the definitions.
	 * @param refStack Keeps track of previously-visited references so that we don't cause recursive loops.
	 * @param maxDepth
	 * 	The maximum depth to resolve references.
	 * 	<br>After that level is reached, <code>$ref</code> references will be left alone.
	 * 	<br>Useful if you have very complex models and you don't want your swagger page to be overly-complex.
	 * @return
	 * 	This object with references resolved.
	 * 	<br>May or may not be the same object.
	 */
	public HeaderInfo resolveRefs(OpenApi openApi, Deque<String> refStack, int maxDepth) {

		if (nn(ref)) {
			if (refStack.contains(ref) || refStack.size() >= maxDepth)
				return this;
			refStack.addLast(ref);
			var r = openApi.findRef(ref, HeaderInfo.class);
			r = r.resolveRefs(openApi, refStack, maxDepth);
			refStack.removeLast();
			return r;
		}
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public HeaderInfo set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "$ref" -> setRef(s(value));
			case "allowEmptyValue" -> setAllowEmptyValue(toBoolean(value));
			case "allowReserved" -> setAllowReserved(toBoolean(value));
			case "deprecated" -> setDeprecated(toBoolean(value));
			case "description" -> setDescription(s(value));
			case "examples" -> setExamples(toMapBuilder(value, String.class, Example.class).sparse().build());
			case "explode" -> setExplode(toBoolean(value));
			case "required" -> setRequired(toBoolean(value));
			case "schema" -> setSchema(toType(value, SchemaInfo.class));
			case "x-example" -> setExample(value);
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	/**
	 * Bean property setter:  <property>allowEmptyValue</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public HeaderInfo setAllowEmptyValue(Boolean value) {
		allowEmptyValue = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>allowReserved</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public HeaderInfo setAllowReserved(Boolean value) {
		allowReserved = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>deprecated</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public HeaderInfo setDeprecated(Boolean value) {
		deprecated = value;
		return this;
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
	 * @return This object
	 */
	public HeaderInfo setDescription(String value) {
		description = value;
		return this;
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
	public HeaderInfo setExample(Object value) {
		example = value;
		return this;
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
	public HeaderInfo setExamples(Map<String,Example> value) {
		examples = copyOf(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>explode</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public HeaderInfo setExplode(Boolean value) {
		explode = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>$ref</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	@Beanp("$ref")
	public HeaderInfo setRef(String value) {
		ref = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>required</property>.
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
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public HeaderInfo setRequired(Boolean value) {
		required = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>schema</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public HeaderInfo setSchema(SchemaInfo value) {
		schema = value;
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public HeaderInfo strict(Object value) {
		super.strict(value);
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	protected HeaderInfo strict() {
		super.strict();
		return this;
	}
}