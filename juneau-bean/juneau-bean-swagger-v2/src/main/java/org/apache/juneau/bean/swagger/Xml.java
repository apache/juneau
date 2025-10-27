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
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.internal.ConverterUtils.*;

import java.util.*;

import org.apache.juneau.common.collections.*;

/**
 * A metadata object that allows for more fine-tuned XML model definitions.
 *
 * <p>
 * The Xml Object is a metadata object that allows for more fine-tuned XML model definitions in Swagger 2.0. When using
 * arrays, XML element names are not inferred (for singular/plural forms) and the name property should be used to add
 * that information. This object is used to control how schema properties are serialized to XML.
 *
 * <h5 class='section'>Swagger Specification:</h5>
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
 * <p class='bjava'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	Xml <jv>xml</jv> = <jsm>xml</jsm>()
 * 		.name(<js>"foo"</js>)
 * 		.namespace(<js>"http://foo"</js>)
 *
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String <jv>json</jv> = Json.<jsm>from</jsm>(<jv>xml</jv>);
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
 * 	<li class='link'><a class="doclink" href="https://swagger.io/specification/v2/#xml-object">Swagger 2.0 Specification &gt; XML Object</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/docs/specification/2-0/describing-models/">Swagger Describing Models</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanSwagger2">juneau-bean-swagger-v2</a>
 * </ul>
 */
public class Xml extends SwaggerElement {

	private String name, namespace, prefix;
	private Boolean attribute, wrapped;

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

	@Override /* Overridden from SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "attribute" -> toType(getAttribute(), type);
			case "name" -> toType(getName(), type);
			case "namespace" -> toType(getNamespace(), type);
			case "prefix" -> toType(getPrefix(), type);
			case "wrapped" -> toType(getWrapped(), type);
			default -> super.get(property, type);
		};
	}

	/**
	 * Bean property getter:  <property>attribute</property>.
	 *
	 * <p>
	 * Declares whether the property definition translates to an attribute instead of an element.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Boolean getAttribute() { return attribute; }

	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * <p>
	 * The name of the element/attribute used for the described schema property.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getName() { return name; }

	/**
	 * Bean property getter:  <property>namespace</property>.
	 *
	 * <p>
	 * The URL of the namespace definition.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getNamespace() { return namespace; }

	/**
	 * Bean property getter:  <property>prefix</property>.
	 *
	 * <p>
	 * The prefix to be used for the name.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getPrefix() { return prefix; }

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
	public Boolean getWrapped() { return wrapped; }

	@Override /* Overridden from SwaggerElement */
	public Set<String> keySet() {
		// @formatter:off
		var s = setb(String.class)
			.addIf(nn(attribute), "attribute")
			.addIf(nn(name), "name")
			.addIf(nn(namespace), "namespace")
			.addIf(nn(prefix), "prefix")
			.addIf(nn(wrapped), "wrapped")
			.build();
		// @formatter:on
		return new MultiSet<>(s, super.keySet());
	}

	@Override /* Overridden from SwaggerElement */
	public Xml set(String property, Object value) {
		assertArgNotNull("property", property);
		return switch (property) {
			case "attribute" -> setAttribute(toBoolean(value));
			case "name" -> setName(s(value));
			case "namespace" -> setNamespace(s(value));
			case "prefix" -> setPrefix(s(value));
			case "wrapped" -> setWrapped(toBoolean(value));
			default -> {
				super.set(property, value);
				yield this;
			}
		};
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

	/**
	 * Sets strict mode on this bean.
	 *
	 * @return This object.
	 */
	@Override
	public Xml strict() {
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
	public Xml strict(Object value) {
		super.strict(value);
		return this;
	}
}