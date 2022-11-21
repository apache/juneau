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
package org.apache.juneau.dto.openapi3;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.internal.*;

import java.util.*;

/**
 * Describes a single HTTP header.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	HeaderInfo x = <jsm>headerInfo</jsm>(<js>"integer"</js>).description(<js>"The number of allowed requests in the current period"</js>);
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
 * 		<js>"description"</js>: <js>"The number of allowed requests in the current period"</js>,
 * 		<js>"type"</js>: <js>"integer"</js>
 * 	}
 * </p>
 */
@Bean(properties="description,explode,deprecated,allowEmptyValue,allowReserved,schema,example,examples,$ref,*")
@SuppressWarnings({"unchecked"})
@FluentSetters
public class HeaderInfo extends OpenApiElement {

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
	 * @return This object
	 */
	public HeaderInfo setDescription(String value) {
		description = value;
		return this;
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
	 * @return This object
	 */
	public HeaderInfo setRequired(Boolean value) {
		required = value;
		return this;
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
	 * @param value
	 * 	The new value for this property.
	 * @return This object
	 */
	public HeaderInfo setExplode(Boolean value) {
		explode = value;
		return this;
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
	 * @param value
	 * 	The new value for this property.
	 * @return This object
	 */
	public HeaderInfo setDeprecated(Boolean value) {
		deprecated = value;
		return this;
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
	 * @param value
	 * 	The new value for this property.
	 * @return This object
	 */
	public HeaderInfo setAllowEmptyValue(Boolean value) {
		allowEmptyValue = value;
		return this;
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
	 * @param value
	 * 	The new value for this property.
	 * @return This object
	 */
	public HeaderInfo setAllowReserved(Boolean value) {
		allowReserved = value;
		return this;
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
	 * @return This object
	 */
	public HeaderInfo setSchema(SchemaInfo value) {
		schema = value;
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
	 * @return This object
	 */
	@Beanp("$ref")
	public HeaderInfo setRef(String value) {
		ref = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>x-example</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Beanp("x-example")
	public Object getExample() {
		return example;
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
	 * @return This object
	 */
	public HeaderInfo setExamples(Map<String,Example> value) {
		examples = copyOf(value);
		return this;
	}

	/**
	 * Adds a single value to the <property>examples</property> property.
	 *
	 * @param name The example name.
	 * @param example The example.
	 * @return This object
	 */
	public HeaderInfo addExample(String name, Example example) {
		examples = mapBuilder(examples).sparse().add(name, example).build();
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

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
			case "description": return setDescription(stringify(value));
			case "required": return setRequired(toBoolean(value));
			case "explode": return setExplode(toBoolean(value));
			case "deprecated": return setDeprecated(toBoolean(value));
			case "allowEmptyValue": return setAllowEmptyValue(toBoolean(value));
			case "$ref": return setRef(stringify(value));
			case "schema": return setSchema(toType(value, SchemaInfo.class));
			case "x-example": return setExample(stringify(value));
			case "examples": return setExamples(mapBuilder(String.class,Example.class).sparse().addAny(value).build());
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
			.addIf(description != null, "description")
			.addIf(required != null, "required")
			.addIf(explode != null, "explode")
			.addIf(deprecated != null, "deprecated")
			.addIf(allowEmptyValue != null, "allowEmptyValue")
			.addIf(ref != null, "$ref")
			.addIf(allowReserved != null, "allowReserved")
			.addIf(schema != null, "schema")
			.addIf(example != null, "example")
			.addIf(examples != null, "examples")
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
