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
package org.apache.juneau.dto.openapi;

import org.apache.juneau.annotation.Bean;
import org.apache.juneau.annotation.BeanProperty;
import org.apache.juneau.dto.swagger.ResponseInfo;
import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.internal.MultiSet;
import org.apache.juneau.internal.StringUtils;
import org.apache.juneau.utils.ASet;

import java.util.*;

import static org.apache.juneau.internal.ArrayUtils.contains;
import static org.apache.juneau.internal.BeanPropertyUtils.*;

/**
 * Describes a single HTTP header.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	HeaderInfo x = <jsm>headerInfo</jsm>(<js>"integer"</js>).description(<js>"The number of allowed requests in the current period"</js>);
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String json = JsonSerializer.<jsf>DEFAULT</jsf>.toString(x);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	String json = x.toString();
 * </p>
 * <p class='bcode w800'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"description"</js>: <js>"The number of allowed requests in the current period"</js>,
 * 		<js>"type"</js>: <js>"integer"</js>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'>{@doc juneau-dto.Swagger}
 * </ul>
 */
@Bean(properties="description,explode,deprecated,allowEmptyValue,allowReserved,schema,example,examples,$ref,*")
@SuppressWarnings({"unchecked"})
public class HeaderInfo extends OpenApiElement {

	private static final String[] VALID_TYPES = {"string", "number", "integer", "boolean", "array"};
	private static final String[] VALID_COLLECTION_FORMATS = {"csv","ssv","tsv","pipes","multi"};

	private String
		description,
		ref;
	private Boolean
		required,
		explode,
		deprecated,
		allowEmptyValue,
		allowReserved;
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
		if (copyFrom.examples == null)
			this.examples = null;
		else
			this.examples = new LinkedHashMap<>();
			for (Map.Entry<String,Example> e : copyFrom.examples.entrySet())
				this.examples.put(e.getKey(),	e.getValue().copy());
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public HeaderInfo copy() {
		return new HeaderInfo(this);
	}

	@Override /* OpenApiElement */
	protected HeaderInfo strict() {
		super.strict();
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
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setDescription(String value) {
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
	public HeaderInfo description(Object value) {
		return setDescription(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>required</property>.
	 *
	 * <p>
	 * The type of the object.
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
	 * The type of the object.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='doctree'>
	 * 	<li class='extlink'>{@doc SwaggerDataTypes}
	 * </ul>
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
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setRequired(Boolean value) {
		required = value;
		return this;
	}

	/**
	 * Same as {@link #setRequired(Boolean)}
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toBoolean()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo required(Object value) {
		return setRequired(toBoolean(value));
	}


	/**
	 * Bean property getter:  <property>required</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getExplode() {
		return explode;
	}

	/**
	 * Bean property setter:  <property>explode</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='doctree'>
	 * 	<li class='extlink'>{@doc SwaggerDataTypes}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setExplode(Boolean value) {
		explode = value;
		return this;
	}

	/**
	 * Same as {@link #setExplode(Boolean)}
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toBoolean()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo explode(Object value) {
		return setExplode(toBoolean(value));
	}

	/**
	 * Bean property getter:  <property>deprecated</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getDeprecated() {
		return deprecated;
	}

	/**
	 * Bean property setter:  <property>deprecated</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='doctree'>
	 * 	<li class='extlink'>{@doc SwaggerDataTypes}
	 * </ul>
	 *
	 * @param value
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setDeprecated(Boolean value) {
		deprecated = value;
		return this;
	}

	/**
	 * Same as {@link #setDeprecated(Boolean)}
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toBoolean()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo deprecated(Object value) {
		return setDeprecated(toBoolean(value));
	}

	/**
	 * Bean property getter:  <property>allowEmptyValue</property>.
	 *
	 * <p>
	 * The type of the object.
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
	 * The type of the object.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='doctree'>
	 * 	<li class='extlink'>{@doc SwaggerDataTypes}
	 * </ul>
	 *
	 * @param value
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setAllowEmptyValue(Boolean value) {
		allowEmptyValue = value;
		return this;
	}

	/**
	 * Same as {@link #setDeprecated(Boolean)}
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toBoolean()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo allowEmptyValue(Object value) {
		return setAllowEmptyValue(toBoolean(value));
	}

	/**
	 * Bean property getter:  <property>allowReserved</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getAllowReserved() {
		return allowReserved;
	}

	/**
	 * Bean property setter:  <property>allowReserved</property>.
	 *
	 * <p>
	 * The type of the object.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='doctree'>
	 * 	<li class='extlink'>{@doc SwaggerDataTypes}
	 * </ul>
	 *
	 * @param value
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setAllowReserved(Boolean value) {
		allowReserved = value;
		return this;
	}

	/**
	 * Same as {@link #setDeprecated(Boolean)}
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toBoolean()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo allowReserved(Object value) {
		return setAllowReserved(toBoolean(value));
	}

	/**
	 * Bean property getter:  <property>schema</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public SchemaInfo getSchema() {
		return schema;
	}

	/**
	 * Bean property setter:  <property>schema</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setSchema(SchemaInfo value) {
		schema = value;
		return this;
	}

	/**
	 * Same as {@link #setSchema(SchemaInfo)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Valid types:
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo schema(Object value) {
		return setSchema(toType(value, SchemaInfo.class));
	}


	/**
	 * Bean property getter:  <property>$ref</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@BeanProperty("$ref")
	public String getRef() {
		return ref;
	}

	/**
	 * Returns <jk>true</jk> if this object has a <js>"$ref"</js> attribute.
	 *
	 * @return <jk>true</jk> if this object has a <js>"$ref"</js> attribute.
	 */
	public boolean hasRef() {
		return ref != null;
	}

	/**
	 * Bean property setter:  <property>$ref</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("$ref")
	public HeaderInfo setRef(Object value) {
		ref = StringUtils.asString(value);
		return this;
	}

	/**
	 * Same as {@link #setRef(Object)}.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo ref(Object value) {
		return setRef(value);
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
	 * Bean property setter:  <property>examples</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("x-example")
	public HeaderInfo setExample(Object value) {
		example = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>examples</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo example(Object value) {
		example = value;
		return this;
	}


	/**
	 * Bean property getter:  <property>examples</property>.
	 *
	 * <p>
	 * The list of possible responses as they are returned from executing this operation.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,Example> getExamples() {
		return examples;
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
	 * @return This object (for method chaining).
	 */
	public HeaderInfo setExamples(Map<String,Example> value) {
		examples = newMap(value);
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
	public HeaderInfo addExamples(Map<String,Example> values) {
		examples = addToMap(examples, values);
		return this;
	}

	/**
	 * Adds a single value to the <property>examples</property> property.
	 *
	 * @param name The example name.
	 * @param example The example.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo example(String name, Example example) {
		addExamples(Collections.singletonMap(name, example));
		return this;
	}
	/**
	 * Adds one or more values to the <property>examples</property> property.
	 *
	 * @param values
	 * 	The values to add to this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li><code>Map&lt;String,{@link org.apache.juneau.dto.swagger.HeaderInfo}|String&gt;</code>
	 * 		<li><code>String</code> - JSON object representation of <code>Map&lt;String,{@link org.apache.juneau.dto.swagger.HeaderInfo}&gt;</code>
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode w800'>
	 * 	headers(<js>"{headerName:{description:'description',...}}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public HeaderInfo examples(Object...values) {
		examples = addToMap(examples,values, String.class, Example.class);
		return this;
	}


	@Override /* OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "description": return (T)getDescription();
			case "required": return toType(getRequired(), type);
			case "explode": return toType(getExplode(), type);
			case "deprecated": return toType(getDeprecated(), type);
			case "allowEmptyValue": return toType(getAllowEmptyValue(), type);
			case "allowReserved": return toType(getAllowReserved(), type);
			case "$ref": return toType(getRef(), type);
			case "schema": return toType(getSchema(), type);
			case "x-example": return toType(getExample(), type);
			case "examples": return toType(getExamples(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* OpenApiElement */
	public HeaderInfo set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "description": return description(value);
			case "required": return required(value);
			case "explode": return explode(value);
			case "deprecated": return deprecated(value);
			case "allowEmptyValue": return allowEmptyValue(value);
			case "$ref": return ref(value);
			case "schema": return schema(value);
			case "x-example": return example(value);
			case "examples": return setExamples(null).examples(value);
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		ASet<String> s = new ASet<String>()
			.appendIf(description != null, "description")
			.appendIf(required != null, "required")
			.appendIf(explode != null, "explode")
			.appendIf(deprecated != null, "deprecated")
			.appendIf(allowEmptyValue != null, "allowEmptyValue")
			.appendIf(ref != null, "$ref")
			.appendIf(allowReserved != null, "allowReserved")
			.appendIf(schema != null, "schema")
			.appendIf(example != null, "example")
			.appendIf(examples != null, "examples");
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
	public HeaderInfo resolveRefs(Swagger swagger, Deque<String> refStack, int maxDepth) {

		if (ref != null) {
			if (refStack.contains(ref) || refStack.size() >= maxDepth)
				return this;
			refStack.addLast(ref);
			HeaderInfo r = swagger.findRef(ref, HeaderInfo.class).resolveRefs(swagger, refStack, maxDepth);
			refStack.removeLast();
			return r;
		}
		return this;
	}
}
