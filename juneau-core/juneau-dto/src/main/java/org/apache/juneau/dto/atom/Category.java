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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.net.*;
import java.net.URI;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents an <c>atomCategory</c> construct in the RFC4287 specification.
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bcode w800'>
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
 * <ul class='seealso'>
 * 	<li class='link'>{@doc DtoAtom}
 * 	<li class='jp'>{@doc package-summary.html#TOC}
 * </ul>
 */
@Bean(typeName="category")
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
		term(term);
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
	 */
	@Xml(format=ATTR)
	public void setTerm(String value) {
		this.term = value;
	}

	/**
	 * Bean property fluent getter:  <property>term</property>.
	 *
	 * <p>
	 * The category term.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> term() {
		return Optional.ofNullable(term);
	}

	/**
	 * Bean property fluent setter:  <property>term</property>.
	 *
	 * <p>
	 * The category term.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Category term(String value) {
		setTerm(value);
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
	 */
	public void setScheme(Object value) {
		this.scheme = toURI(value);
	}

	/**
	 * Bean property fluent getter:  <property>scheme</property>.
	 *
	 * <p>
	 * The category scheme.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<URI> scheme() {
		return Optional.ofNullable(scheme);
	}

	/**
	 * Bean property fluent setter:  <property>scheme</property>.
	 *
	 * <p>
	 * The category scheme.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Category scheme(Object value) {
		setScheme(value);
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
	 */
	public void setLabel(String value) {
		this.label = value;
	}

	/**
	 * Bean property fluent getter:  <property>label</property>.
	 *
	 * <p>
	 * The category label.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> label() {
		return Optional.ofNullable(label);
	}

	/**
	 * Bean property fluent setter:  <property>label</property>.
	 *
	 * <p>
	 * The category label.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Category label(String value) {
		setLabel(value);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Common */
	public Category base(Object base) {
		super.base(base);
		return this;
	}

	@Override /* Common */
	public Category lang(String lang) {
		super.lang(lang);
		return this;
	}
}
