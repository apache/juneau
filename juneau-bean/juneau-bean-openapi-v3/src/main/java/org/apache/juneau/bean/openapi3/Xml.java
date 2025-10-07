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
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.common.internal.Utils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;

/**
 * A metadata object that allows for more fine-tuned XML model definitions.
 *
 * <p>
 * The Xml Object is a metadata object that allows for more fine-tuned XML model definitions. When using arrays, 
 * XML element names are not inferred (for singular/plural forms) and the name property should be used to add that 
 * information. This object is used to control how schema properties are serialized to XML.
 *
 * <h5 class='section'>OpenAPI Specification:</h5>
 * <p>
 * The Xml Object is composed of the following fields:
 * <ul class='spaced-list'>
 * 	<li><c>name</c> (string) - Replaces the name of the element/attribute used for the described schema property
 * 	<li><c>namespace</c> (string) - The URI of the namespace definition
 * 	<li><c>prefix</c> (string) - The prefix to be used for the name
 * 	<li><c>attribute</c> (boolean) - Declares whether the property definition translates to an attribute instead of an element
 * 	<li><c>wrapped</c> (boolean) - May be used only for an array definition. Signifies whether the array is wrapped
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	Xml <jv>x</jv> = <jsm>xml</jsm>().setName(<js>"book"</js>).setNamespace(<js>"http://example.com/schema"</js>);
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
 * 		<js>"name"</js>: <js>"book"</js>,
 * 		<js>"namespace"</js>: <js>"http://example.com/schema"</js>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://spec.openapis.org/oas/v3.0.0#xml-object">OpenAPI Specification &gt; XML Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/data-models/representing-xml/">OpenAPI Representing XML</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanOpenApi3">juneau-bean-openapi-v3</a>
 * </ul>
 */
@FluentSetters
public class Xml extends OpenApiElement {

	private String
		name,
		namespace,
		prefix;
	private Boolean
		attribute,
		wrapped;

	/**
	 * Default constructor.
	 */
	public Xml() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	public Xml(Xml copyFrom) {
		super(copyFrom);

		this.name = copyFrom.name;
		this.namespace = copyFrom.namespace;
		this.prefix = copyFrom.prefix;
		this.attribute = copyFrom.attribute;
		this.wrapped = copyFrom.wrapped;
	}

	/**
	 * Make a deep copy of this object.
	 *
	 * @return A deep copy of this object.
	 */
	public Xml copy() {
		return new Xml(this);
	}

	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * <p>
	 * Replaces the name of the element/attribute used for the described schema property.
	 *
	 * <p>
	 * When defined within the Items Object (<code>items</code>), it will affect the name of the individual XML elements
	 * within the list.
	 * <br>When defined alongside <code>type</code> being array (outside the <code>items</code>), it will affect the
	 * wrapping element and only if wrapped is <jk>true</jk>.
	 * <br>If wrapped is <jk>false</jk>, it will be ignored.
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
	 * Replaces the name of the element/attribute used for the described schema property.
	 *
	 * <p>
	 * When defined within the Items Object (<code>items</code>), it will affect the name of the individual XML elements
	 * within the list.
	 * <br>When defined alongside <code>type</code> being array (outside the <code>items</code>), it will affect the
	 * wrapping element and only if wrapped is <jk>true</jk>.
	 * <br>If wrapped is <jk>false</jk>, it will be ignored.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Xml setName(String value) {
		name = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>namespace</property>.
	 *
	 * <p>
	 * The URL of the namespace definition. Value SHOULD be in the form of a URL.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * Bean property setter:  <property>namespace</property>.
	 *
	 * <p>
	 * The URL of the namespace definition. Value SHOULD be in the form of a URL.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Xml setNamespace(String value) {
		namespace = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>prefix</property>.
	 *
	 * <p>
	 * The prefix to be used for the name.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * Bean property setter:  <property>prefix</property>.
	 *
	 * <p>
	 * The prefix to be used for the name.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Xml setPrefix(String value) {
		prefix = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>attribute</property>.
	 *
	 * <p>
	 * Declares whether the property definition translates to an attribute instead of an element.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getAttribute() {
		return attribute;
	}

	/**
	 * Bean property setter:  <property>attribute</property>.
	 *
	 * <p>
	 * Declares whether the property definition translates to an attribute instead of an element.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Default value is <jk>false</jk>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Xml setAttribute(Boolean value) {
		attribute = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>wrapped</property>.
	 *
	 * <p>
	 * MAY be used only for an array definition.
	 *
	 * <p>
	 * Signifies whether the array is wrapped (for example,
	 * <code>&lt;books&gt;&lt;book/&gt;&lt;book/&gt;&lt;books&gt;</code>) or unwrapped
	 * (<code>&lt;book/&gt;&lt;book/&gt;</code>).
	 * <br>The definition takes effect only when defined alongside <code>type</code> being <code>array</code>
	 * (outside the <code>items</code>).
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getWrapped() {
		return wrapped;
	}

	/**
	 * Bean property setter:  <property>wrapped</property>.
	 *
	 * <p>
	 * MAY be used only for an array definition.
	 *
	 * <p>
	 * Signifies whether the array is wrapped (for example,
	 * <code>&lt;books&gt;&lt;book/&gt;&lt;book/&gt;&lt;books&gt;</code>) or unwrapped
	 * (<code>&lt;book/&gt;&lt;book/&gt;</code>).
	 * <br>The definition takes effect only when defined alongside <code>type</code> being <code>array</code>
	 * (outside the <code>items</code>).
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Xml setWrapped(Boolean value) {
		this.wrapped = value;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* OpenApiElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "name" -> toType(getName(), type);
			case "namespace" -> toType(getNamespace(), type);
			case "prefix" -> toType(getPrefix(), type);
			case "attribute" -> toType(getAttribute(), type);
			case "wrapped" -> toType(getWrapped(), type);
			default -> super.get(property, type);
		};
	}

	@Override /* OpenApiElement */
	public Xml set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "attribute" -> setAttribute(toBoolean(value));
			case "name" -> setName(Utils.s(value));
			case "namespace" -> setNamespace(Utils.s(value));
			case "prefix" -> setPrefix(Utils.s(value));
			case "wrapped" -> setWrapped(toBoolean(value));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
	}

	@Override /* OpenApiElement */
	public Set<String> keySet() {
		var s = setBuilder(String.class)
			.addIf(attribute != null, "attribute")
			.addIf(name != null, "name")
			.addIf(namespace != null, "namespace")
			.addIf(prefix != null, "prefix")
			.addIf(wrapped != null, "wrapped")
			.build();
		return new MultiSet<>(s, super.keySet());
	}

	// <FluentSetters>

	@Override /* GENERATED - do not modify */
	public Xml strict() {
		super.strict();
		return this;
	}

	@Override /* GENERATED - do not modify */
	public Xml strict(Object value) {
		super.strict(value);
		return this;
	}

	// </FluentSetters>
}