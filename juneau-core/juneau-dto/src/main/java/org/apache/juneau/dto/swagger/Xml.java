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

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * A metadata object that allows for more fine-tuned XML model definitions.
 *
 * <p>
 * When using arrays, XML element names are not inferred (for singular/plural forms) and the name property should be
 * used to add that information.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	Xml <jv>xml</jv> = <jsm>xml</jsm>()
 * 		.name(<js>"foo"</js>)
 * 		.namespace(<js>"http://foo"</js>)
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = JsonSerializer.<jsf>DEFAULT</jsf>.toString(<jv>xml</jv>);
 *
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 *  <jv>json</jv> = <jv>xml</jv>.toString();
 * </p>
 * <p class='bjson'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"name"</js>: <js>"foo"</js>,
 * 		<js>"namespace"</js>: <js>"http://foo"</js>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Overview &gt; juneau-rest-server &gt; Swagger</a>
 * </ul>
 */
@Bean(properties="name,namespace,prefix,attribute,wrapped,*")
@FluentSetters
public class Xml extends SwaggerElement {

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

		this.attribute = copyFrom.attribute;
		this.name = copyFrom.name;
		this.namespace = copyFrom.namespace;
		this.prefix = copyFrom.prefix;
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

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

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
	 * @return This object.
	 */
	public Xml setAttribute(Boolean value) {
		attribute = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * <p>
	 * The name of the element/attribute used for the described schema property.
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
	 * The name of the element/attribute used for the described schema property.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Xml setName(String value) {
		name = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>namespace</property>.
	 *
	 * <p>
	 * The URL of the namespace definition.
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
	 * The URL of the namespace definition.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
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
	 * @return This object.
	 */
	public Xml setPrefix(String value) {
		prefix = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>wrapped</property>.
	 *
	 * <p>
	 * Signifies whether the array is wrapped (for example,
	 * <c>&lt;books&gt;&lt;book/&gt;&lt;book/&gt;&lt;books&gt;</c>) or unwrapped
	 * (<c>&lt;book/&gt;&lt;book/&gt;</c>).
	 * <br>The definition takes effect only when defined alongside <c>type</c> being <c>array</c>
	 * (outside the <c>items</c>).
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getWrapped() {
		return wrapped;
	}

	/**
	 * Bean property setter:  <property>wrapped</property>.
	 *
	 *
	 * <p>
	 * Signifies whether the array is wrapped (for example,
	 * <c>&lt;books&gt;&lt;book/&gt;&lt;book/&gt;&lt;books&gt;</c>) or unwrapped
	 * (<c>&lt;book/&gt;&lt;book/&gt;</c>).
	 * <br>The definition takes effect only when defined alongside <c>type</c> being <c>array</c>
	 * (outside the <c>items</c>).
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Xml setWrapped(Boolean value) {
		this.wrapped = value;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "attribute": return toType(getAttribute(), type);
			case "name": return toType(getName(), type);
			case "namespace": return toType(getNamespace(), type);
			case "prefix": return toType(getPrefix(), type);
			case "wrapped": return toType(getWrapped(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public Xml set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "attribute": return setAttribute(toBoolean(value));
			case "name": return setName(stringify(value));
			case "namespace": return setNamespace(stringify(value));
			case "prefix": return setPrefix(stringify(value));
			case "wrapped": return setWrapped(toBoolean(value));
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* SwaggerElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
			.addIf(attribute != null, "attribute")
			.addIf(name != null, "name")
			.addIf(namespace != null, "namespace")
			.addIf(prefix != null, "prefix")
			.addIf(wrapped != null, "wrapped")
			.build();
		return new MultiSet<>(s, super.keySet());
	}
}
