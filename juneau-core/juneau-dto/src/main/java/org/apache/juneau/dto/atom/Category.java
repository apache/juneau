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
package org.apache.juneau.dto.atom;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.net.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents an <c>atomCategory</c> construct in the RFC4287 specification.
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bschema'>
 * 	atomCategory =
 * 		element atom:category {
 * 			atomCommonAttributes,
 * 			attribute term { text },
 * 			attribute scheme { atomUri }?,
 * 			attribute label { text }?,
 * 			undefinedContent
 * 		}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jd.Atom">Overview &gt; juneau-dto &gt; Atom</a>
 * </ul>
 */
@Bean(typeName="category")
@FluentSetters
public class Category extends Common {

	private String term;
	private URI scheme;
	private String label;

	/**
	 * Normal constructor.
	 *
	 * @param term The category term.
	 */
	public Category(String term) {
		setTerm(term);
	}

	/** Bean constructor. */
	public Category() {}


	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>term</property>.
	 *
	 * <p>
	 * The category term.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=ATTR)
	public String getTerm() {
		return term;
	}

	/**
	 * Bean property setter:  <property>term</property>.
	 *
	 * <p>
	 * The category term.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	@Xml(format=ATTR)
	public Category setTerm(String value) {
		this.term = value;
		return this;
	}

	/**
	 * Bean property getter:  <property>scheme</property>.
	 *
	 * <p>
	 * The category scheme.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=ATTR)
	public URI getScheme() {
		return scheme;
	}

	/**
	 * Bean property setter:  <property>scheme</property>.
	 *
	 * <p>
	 * The category scheme.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Category setScheme(Object value) {
		this.scheme = toURI(value);
		return this;
	}

	/**
	 * Bean property getter:  <property>label</property>.
	 *
	 * <p>
	 * The category label.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=ATTR)
	public String getLabel() {
		return label;
	}

	/**
	 * Bean property setter:  <property>scheme</property>.
	 *
	 * <p>
	 * The category label.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Category setLabel(String value) {
		this.label = value;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.dto.atom.Common */
	public Category setBase(Object value) {
		super.setBase(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.Common */
	public Category setLang(String value) {
		super.setLang(value);
		return this;
	}

	// </FluentSetters>
}