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

import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.common.utils.*;
import org.apache.juneau.internal.*;

/**
 * information for Examples object.
 *
 * <p>
 * The Example Object provides an example of a media type. The example object is mutually exclusive of the examples
 * object. Furthermore, if referencing a schema which contains an example, the example value shall override the example
 * provided by the schema.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The Example Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>summary</c> (string) - Short description for the example
 * 	<li><c>description</c> (string) - Long description for the example. CommonMark syntax MAY be used for rich text representation
 * 	<li><c>value</c> (any) - Embedded literal example. The value field and externalValue field are mutually exclusive
 * 	<li><c>externalValue</c> (string) - A URI that points to the literal example. This provides the capability to reference
 * 		examples that cannot easily be included in JSON or YAML documents. The value field and externalValue field are mutually exclusive
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	Example <jv>x</jv> = <jsm>example</jsm>()
 * 		.setSummary(<js>"User example"</js>)
 * 		.setValue(<js>"John Doe"</js>);
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = Json.<jsm>from</jsm>(<jv>x</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	<jv>json</jv> = <jv>x</jv>.toString();
 * </p>
 * <p class='bcode'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"summary"</js>: <js>"User example"</js>,
 * 		<js>"value"</js>: <js>"John Doe"</js>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#example-object">OpenAPI Specification &gt; Example Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/adding-examples/">OpenAPI Adding Examples</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
public class Example extends OpenApiElement {

	private String summary;
	private String description;
	private String externalValue;
	private Object value;

	/**
	 * Default constructor.
	 */
	public Example() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Example(Example copyFrom) {
		super(copyFrom);

		this.summary = copyFrom.summary;
		this.description = copyFrom.description;
		this.externalValue = copyFrom.externalValue;
		this.value = copyFrom.value;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Example copy() {
		return new Example(this);
	}

	/**
	 * Bean property getter:  <property>summary</property>.
	 *
	 * <p>
	 * The identifying name of the contact person/organization.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getSummary() {
		return summary;
	}

	/**
	 * Bean property setter:  <property>summary</property>.
	 *
	 * <p>
	 * The identifying name of the contact person/organization.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Example setSummary(String value) {
		summary = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * <p>
	 * The URL pointing to the contact information.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Example setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>externalValue</property>.
	 *
	 * <p>
	 * The email address of the contact person/organization.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getExternalValue() {
		return externalValue;
	}

	/**
	 * Bean property setter:  <property>externalValue</property>.
	 *
	 * <p>
	 * The email address of the contact person/organization.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>MUST be in the format of an email address.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Example setExternalValue(String value) {
		externalValue = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>default</property>.
	 *
	 * <p>
	 * Declares the value of the parameter that the server will use if none is provided, for example a <js>"count"</js>
	 * to control the number of results per page might default to 100 if not supplied by the client in the request.
	 *
	 * (Note: <js>"value"</js> has no meaning for required parameters.)
	 * Unlike JSON Schema this value MUST conform to the defined <code>type</code> for this parameter.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Bean property setter:  <property>value</property>.
	 *
	 * <p>
	 * Declares the value of the parameter that the server will use if none is provided, for example a <js>"count"</js>
	 * to control the number of results per page might default to 100 if not supplied by the client in the request.
	 * (Note: <js>"default"</js> has no meaning for required parameters.)
	 * Unlike JSON Schema this value MUST conform to the defined <code>type</code> for this parameter.
	 *
	 * @param val The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Example setValue(Object val) {
		value = val;
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "description" -> toType(getDescription(), type);
			case "externalValue" -> toType(getExternalValue(), type);
			case "summary" -> toType(getSummary(), type);
			case "value" -> toType(getValue(), type);
			default -> super.get(property, type);
		};
	}

	@Override /* Overridden from OpenApiElement */
	public Example set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "description" -> setDescription(Utils.s(value));
			case "externalValue" -> setExternalValue(Utils.s(value));
			case "summary" -> setSummary(Utils.s(value));
			case "value" -> setValue(value);
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	@Override /* Overridden from OpenApiElement */
	public Set<String> keySet() {
		var s = setBuilder(String.class)
			.addIf(description != null, "description")
			.addIf(externalValue != null, "externalValue")
			.addIf(summary != null, "summary")
			.addIf(value != null, "value")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from OpenApiElement */
	public Example strict() {
		super.strict();
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public Example strict(Object value) {
		super.strict(value);
		return this;
	}
}