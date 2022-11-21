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

import org.apache.juneau.annotation.Bean;
import org.apache.juneau.internal.*;

import java.util.Set;

/**
 * A metadata object that allows for more fine-tuned XML model definitions.
 *
 * When using arrays, XML element names are not inferred (for singular/plural forms) and the name property should be
 * used to add that information.
 */
@Bean(properties="name,namespace,prefix,attribute,wrapped,*")
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
		if (property == null)
			return null;
		switch (property) {
			case "name": return toType(getName(), type);
			case "namespace": return toType(getNamespace(), type);
			case "prefix": return toType(getPrefix(), type);
			case "attribute": return toType(getAttribute(), type);
			case "wrapped": return toType(getWrapped(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* OpenApiElement */
	public Xml set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "name": return setName(stringify(value));
			case "namespace": return setNamespace(stringify(value));
			case "prefix": return setPrefix(stringify(value));
			case "attribute": return setAttribute(toBoolean(value));
			case "wrapped": return setWrapped(toBoolean(value));
			default:
				super.set(property, value);
				return this;
		}
	}

	@Override /* OpenApiElement */
	public Set<String> keySet() {
		Set<String> s = setBuilder(String.class)
			.addIf(name != null, "name")
			.addIf(namespace != null, "namespace")
			.addIf(prefix != null, "prefix")
			.addIf(attribute != null, "attribute")
			.addIf(wrapped != null, "wrapped")
			.build();
		return new MultiSet<>(s, super.keySet());
	}
}
