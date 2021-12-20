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

import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents an <c>atomCommonAttributes</c> construct in the RFC4287 specification.
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bcode w800'>
 * 	atomCommonAttributes =
 * 		attribute xml:base { atomUri }?,
 * 		attribute xml:lang { atomLanguageTag }?,
 * 		undefinedAttribute*
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jd.Atom}
 * 	<li class='jp'>{@doc package-summary.html#TOC}
 * </ul>
 */
public abstract class Common {

	private URI base;
	private String lang;


	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>base</property>.
	 *
	 * <p>
	 * The URI base of this object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(prefix="xml", format=ATTR)
	public URI getBase() {
		return base;
	}

	/**
	 * Bean property setter:  <property>term</property>.
	 *
	 * <p>
	 * The URI base of this object.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void setBase(Object value) {
		this.base = toURI(value);
	}

	/**
	 * Bean property fluent getter:  <property>base</property>.
	 *
	 * <p>
	 * The URI base of this object.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<URI> base() {
		return Optional.ofNullable(base);
	}

	/**
	 * Bean property fluent setter:  <property>base</property>.
	 *
	 * <p>
	 * The URI base of this object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Common base(Object value) {
		setBase(value);
		return this;
	}

	/**
	 * Bean property getter:  <property>lang</property>.
	 *
	 * <p>
	 * The language of this object.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(prefix="xml", format=ATTR)
	public String getLang() {
		return lang;
	}

	/**
	 * Bean property setter:  <property>lang</property>.
	 *
	 * <p>
	 * The language of this object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 */
	public void setLang(String value) {
		this.lang = value;
	}

	/**
	 * Bean property fluent getter:  <property>lang</property>.
	 *
	 * <p>
	 * The language of this object.
	 *
	 * @return The property value as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<String> lang() {
		return Optional.ofNullable(lang);
	}

	/**
	 * Bean property fluent setter:  <property>lang</property>.
	 *
	 * <p>
	 * The language of this object.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public Common lang(String value) {
		setLang(value);
		return this;
	}

	@Override /* Object */
	public String toString() {
		return XmlSerializer.DEFAULT_SQ.toString(this);
	}
}
