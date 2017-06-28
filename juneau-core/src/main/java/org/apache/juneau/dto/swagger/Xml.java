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

import org.apache.juneau.annotation.*;

/**
 * A metadata object that allows for more fine-tuned XML model definitions.
 *
 * <p>
 * When using arrays, XML element names are not inferred (for singular/plural forms) and the name property should be
 * used to add that information.
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'>
 * 		<a class='doclink' href='../../../../../overview-summary.html#DTOs'>Juneau Data Transfer Objects
 * 		(org.apache.juneau.dto)</a>
 * 		<ul>
 * 			<li class='sublink'>
 * 				<a class='doclink' href='../../../../../overview-summary.html#DTOs.Swagger'>Swagger</a>
 * 		</ul>
 * 	</li>
 * 	<li class='jp'>
 * 		<a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.dto.swagger</a>
 * 	</li>
 * </ul>
 */
@Bean(properties="name,namespace,prefix,attribute,wrapped")
@SuppressWarnings("hiding")
public class Xml extends SwaggerElement {

	private String name;
	private String namespace;
	private String prefix;
	private Boolean attribute;
	private Boolean wrapped;

	/**
	 * Bean property getter:  <property>name</property>.
	 *
	 * <p>
	 * Replaces the name of the element/attribute used for the described schema property.
	 *
	 * <p>
	 * When defined within the Items Object (<code>items</code>), it will affect the name of the individual XML elements
	 * within the list.
	 * When defined alongside <code>type</code> being array (outside the <code>items</code>), it will affect the
	 * wrapping element and only if wrapped is <jk>true</jk>.
	 * If wrapped is <jk>false</jk>, it will be ignored.
	 *
	 * @return The value of the <property>name</property> property on this bean, or <jk>null</jk> if it is not set.
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
	 * When defined alongside <code>type</code> being array (outside the <code>items</code>), it will affect the
	 * wrapping element and only if wrapped is <jk>true</jk>.
	 * If wrapped is <jk>false</jk>, it will be ignored.
	 *
	 * @param name The new value for the <property>name</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Xml setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Synonym for {@link #setName(String)}.
	 *
	 * @param name The new value for the <property>name</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Xml name(String name) {
		return setName(name);
	}

	/**
	 * Bean property getter:  <property>namespace</property>.
	 *
	 * <p>
	 * The URL of the namespace definition. Value SHOULD be in the form of a URL.
	 *
	 * @return The value of the <property>namespace</property> property on this bean, or <jk>null</jk> if it is not set.
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
	 * @param namespace The new value for the <property>namespace</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Xml setNamespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	/**
	 * Synonym for {@link #setNamespace(String)}.
	 *
	 * @param namespace The new value for the <property>namespace</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Xml namespace(String namespace) {
		return setNamespace(namespace);
	}

	/**
	 * Bean property getter:  <property>prefix</property>.
	 *
	 * <p>
	 * The prefix to be used for the name.
	 *
	 * @return The value of the <property>prefix</property> property on this bean, or <jk>null</jk> if it is not set.
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
	 * @param prefix The new value for the <property>prefix</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Xml setPrefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	/**
	 * Synonym for {@link #setPrefix(String)}.
	 *
	 * @param prefix The new value for the <property>prefix</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Xml prefix(String prefix) {
		return setPrefix(prefix);
	}

	/**
	 * Bean property getter:  <property>attribute</property>.
	 *
	 * <p>
	 * Declares whether the property definition translates to an attribute instead of an element.
	 *
	 * <p>
	 * Default value is <jk>false</jk>.
	 *
	 * @return The value of the <property>attribute</property> property on this bean, or <jk>null</jk> if it is not set.
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
	 * <p>
	 * Default value is <jk>false</jk>.
	 *
	 * @param attribute The new value for the <property>attribute</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Xml setAttribute(Boolean attribute) {
		this.attribute = attribute;
		return this;
	}

	/**
	 * Synonym for {@link #setAttribute(Boolean)}.
	 *
	 * @param attribute The new value for the <property>attribute</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Xml attribute(Boolean attribute) {
		return setAttribute(attribute);
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
	 * Default value is <jk>false</jk>.
	 * The definition takes effect only when defined alongside <code>type</code> being <code>array</code>
	 * (outside the <code>items</code>).
	 *
	 * @return The value of the <property>wrapped</property> property on this bean, or <jk>null</jk> if it is not set.
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
	 * Default value is <jk>false</jk>.
	 * The definition takes effect only when defined alongside <code>type</code> being <code>array</code>
	 * (outside the <code>items</code>).
	 *
	 * @param wrapped The new value for the <property>wrapped</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Xml setWrapped(Boolean wrapped) {
		this.wrapped = wrapped;
		return this;
	}

	/**
	 * Synonym for {@link #setWrapped(Boolean)}.
	 *
	 * @param wrapped The new value for the <property>wrapped</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public Xml wrapped(Boolean wrapped) {
		return setWrapped(wrapped);
	}
}
