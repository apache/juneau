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

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.internal.*;

/**
 * Used to aid in serialization, deserialization, and validation.
 *
 * <p>
 * The Discriminator Object is used to aid in serialization, deserialization, and validation. It adds support for
 * polymorphism by allowing schemas to be discriminated based on the value of a specific property. This is particularly
 * useful when working with inheritance hierarchies in object-oriented programming.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The Discriminator Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>propertyName</c> (string, REQUIRED) - The name of the property in the payload that will hold the discriminator value
 * 	<li><c>mapping</c> (map of strings) - An object to hold mappings between payload values and schema names or references
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	Discriminator <jv>x</jv> = <jsm>discriminator</jsm>()
 * 		.setPropertyName(<js>"petType"</js>)
 * 		.setMapping(<jsm>map</jsm>(<js>"dog"</js>, <js>"#/components/schemas/Dog"</js>, <js>"cat"</js>, <js>"#/components/schemas/Cat"</js>));
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
 * 		<js>"propertyName"</js>: <js>"petType"</js>,
 * 		<js>"mapping"</js>: {
 * 			<js>"dog"</js>: <js>"#/components/schemas/Dog"</js>,
 * 			<js>"cat"</js>: <js>"#/components/schemas/Cat"</js>
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#discriminator-object">OpenAPI Specification &gt; Discriminator Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/data-models/inheritance-and-polymorphism/">OpenAPI Inheritance and Polymorphism</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
public class Discriminator extends OpenApiElement {

	private String propertyName;
	private Map<String,String> mapping;

	/**
	 * Default constructor.
	 */
	public Discriminator() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Discriminator(Discriminator copyFrom) {
		super(copyFrom);

		this.propertyName = copyFrom.propertyName;
		this.mapping = copyOf(copyFrom.mapping);
	}

	/**
	 * Adds one or more values to the <property>mapping</property> property.
	 *
	 * @param key The key.  Must not be <jk>null</jk>.
	 * @param value The value.  Must not be <jk>null</jk>.
	 * @return This object
	 */
	public Discriminator addMapping(String key, String value) {
		assertArgNotNull("key", key);
		assertArgNotNull("value", value);
		mapping = mapBuilder(mapping).sparse().add(key, value).build();
		return this;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Discriminator copy() {
		return new Discriminator(this);
	}

	@Override /* Overridden from OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "propertyName" -> toType(getPropertyName(), type);
			case "mapping" -> toType(getMapping(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Bean property getter:  <property>mapping</property>.
	 *
	 * <p>
	 * The URL for the target documentation.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Map<String,String> getMapping() { return mapping; }

	/**
	 * Bean property getter:  <property>propertyName</property>.
	 *
	 * <p>
	 * A short description of the target documentation.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getPropertyName() { return propertyName; }

	@Override /* Overridden from OpenApiElement */
	public Set<String> keySet() {
		var s = setBuilder(String.class)
			.addIf(mapping != null, "mapping")
			.addIf(propertyName != null, "propertyName")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from OpenApiElement */
	public Discriminator set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "mapping" -> setMapping(mapBuilder(String.class, String.class).sparse().addAny(value).build());
			case "propertyName" -> setPropertyName(Utils.s(value));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	/**
	 * Bean property setter:  <property>mapping</property>.
	 *
	 * <p>
	 * The URL for the target documentation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * 	<br>URIs defined by {@link UriResolver} can be used for values.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Discriminator setMapping(Map<String,String> value) {
		mapping = copyOf(value);
		return this;
	}

	/**
	 * Bean property setter:  <property>propertyName</property>.
	 *
	 * <p>
	 * A short description of the target documentation.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Discriminator setPropertyName(String value) {
		propertyName = value;
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public Discriminator strict() {
		super.strict();
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public Discriminator strict(Object value) {
		super.strict(value);
		return this;
	}
}