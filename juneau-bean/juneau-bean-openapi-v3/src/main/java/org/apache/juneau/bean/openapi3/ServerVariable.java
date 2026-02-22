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

import org.apache.juneau.commons.collections.*;

/**
 * The ServerVariable Object represents a server variable for server URL template substitution. Server variables can be
 * used to define different server environments (e.g., development, staging, production) with different base URLs,
 * ports, or other variable parts of the server URL.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The ServerVariable Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>enum</c> (array of any) - An enumeration of string values to be used if the substitution options are from a limited set
 * 	<li><c>default</c> (string, REQUIRED) - The default value to use for substitution, which SHALL be sent if an alternate value is not supplied
 * 	<li><c>description</c> (string) - An optional description for the server variable. CommonMark syntax MAY be used for rich text representation
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	ServerVariable <jv>x</jv> = <jsm>serverVariable</jsm>()
 * 		.setDefault(<js>"api"</js>)
 * 		.setEnum(<js>"api"</js>, <js>"staging"</js>, <js>"dev"</js>)
 * 		.setDescription(<js>"Environment to use"</js>);
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
 * 		<js>"default"</js>: <js>"api"</js>,
 * 		<js>"enum"</js>: [<js>"api"</js>, <js>"staging"</js>, <js>"dev"</js>],
 * 		<js>"description"</js>: <js>"Environment to use"</js>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#server-variable-object">OpenAPI Specification &gt; Server Variable Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/api-host-and-base-path/">OpenAPI API Host and Base Path</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S115", // Constants use UPPER_snakeCase naming convention
	"java:S116", // Field name uses trailing underscore (default_) to avoid Java keyword conflict
})
public class ServerVariable extends OpenApiElement {

	// Argument name constants for assertArgNotNull
	private static final String ARG_property = "property";

	// Property name constants
	private static final String PROP_default = "default";
	private static final String PROP_description = "description";
	private static final String PROP_enum = "enum";

	private List<Object> enum_ = list();
	private String default_;
	private String description;

	/**
	 * Default constructor.
	 */
	public ServerVariable() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public ServerVariable(ServerVariable copyFrom) {
		super(copyFrom);

		if (nn(copyFrom.enum_))
			this.enum_.addAll(copyOf(copyFrom.enum_));
		this.default_ = copyFrom.default_;
		this.description = copyFrom.description;
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
	 * 	enum_(<js>"['foo','bar']"</js>);
	 * 			</p>
	 * 		<li><code>String</code> - Individual values
	 * 			<h5 class='figure'>Example:</h5>
	 * 			<p class='bcode'>
	 * 	enum_(<js>"foo"</js>, <js>"bar"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Ignored if <jk>null</jk>.
	 * @return This object
	 */
	public ServerVariable addEnum(Object...values) {
		if (nn(values))
			for (var v : values)
				if (nn(v))
					enum_.add(v);
		return this;
	}

	/**
	 * Make a deep copy of this object.
	 * @return A deep copy of this object.
	 */
	public ServerVariable copy() {
		return new ServerVariable(this);
	}

	@Override /* Overridden from OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull(ARG_property, property);
		return switch (property) {
			case PROP_enum -> toType(getEnum(), type);
			case PROP_default -> toType(getDefault(), type);
			case PROP_description -> toType(getDescription(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Bean property getter:  <property>default</property>.
	 *
	 * <p>
	 * Declares the value of the item that the server will use if none is provided.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"default"</js> has no meaning for required items.
	 * 	<li>
	 * 		Unlike JSON Schema this value MUST conform to the defined <code>type</code> for the data type.
	 * </ul>
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDefault() { return default_; }

	/**
	 * Bean property getter:  <property>description</property>.
	 *
	 * <p>
	 * Declares the value of the item that the server will use if none is provided.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"description"</js> has no meaning for required items.
	 * 	<li>
	 * 		Unlike JSON Schema this value MUST conform to the defined <code>type</code> for the data type.
	 * </ul>
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() { return description; }

	/**
	 * Bean property getter:  <property>enum</property>.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public List<Object> getEnum() { return nullIfEmpty(enum_); }

	@Override /* Overridden from OpenApiElement */
	public Set<String> keySet() {
		// @formatter:off
		var s = setb(String.class)
			.addIf(nn(default_), PROP_default)
			.addIf(nn(description), PROP_description)
			.addIf(ne(enum_), PROP_enum)
			.build();
		// @formatter:on
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from OpenApiElement */
	public ServerVariable set(String property, Object value) {
		assertArgNotNull(ARG_property, property);
		return switch (property) {
			case PROP_default -> setDefault(s(value));
			case PROP_description -> setDescription(s(value));
			case PROP_enum -> setEnum(listb(Object.class).addAny(value).sparse().build());
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	/**
	 * Bean property setter:  <property>default</property>.
	 *
	 * <p>
	 * Declares the value of the item that the server will use if none is provided.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"default"</js> has no meaning for required items.
	 * 	<li>
	 * 		Unlike JSON Schema this value MUST conform to the defined <code>type</code> for the data type.
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public ServerVariable setDefault(String value) {
		default_ = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 *
	 * <p>
	 * Declares the value of the item that the server will use if none is provided.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"description"</js> has no meaning for required items.
	 * 	<li>
	 * 		Unlike JSON Schema this value MUST conform to the defined <code>type</code> for the data type.
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public ServerVariable setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Bean property setter:  <property>enum</property>.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public ServerVariable setEnum(Collection<Object> value) {
		enum_.clear();
		if (nn(value))
			enum_.addAll(value);
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	public ServerVariable strict(Object value) {
		super.strict(value);
		return this;
	}

	@Override /* Overridden from OpenApiElement */
	protected ServerVariable strict() {
		super.strict();
		return this;
	}
}