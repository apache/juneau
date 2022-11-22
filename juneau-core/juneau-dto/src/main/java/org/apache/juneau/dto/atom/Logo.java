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

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents an <c>atomLogo</c> construct in the RFC4287 specification.
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bschema'>
 * 	atomLogo = element atom:logo {
 * 		atomCommonAttributes,
 * 		(atomUri)
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jd.Atom">Overview &gt; juneau-dto &gt; Atom</a>
 * </ul>
 */
@Bean(typeName="logo")
@FluentSetters
public class Logo extends Common {

	private URI uri;


	/**
	 * Normal constructor.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * <br>Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param uri The URI of the logo.
	 */
	public Logo(Object uri) {
		setUri(uri);
	}

	/** Bean constructor. */
	public Logo() {}


	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>uri</property>.
	 *
	 * <p>
	 * The URI of the logo.
	 *
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	@Xml(format=ELEMENTS)
	public URI getUri() {
		return uri;
	}

	/**
	 * Bean property setter:  <property>uri</property>.
	 *
	 * <p>
	 * The URI of the logo.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * <br>Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object
	 */
	public Logo setUri(Object value) {
		this.uri = toURI(value);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.dto.atom.Common */
	public Logo setBase(Object value) {
		super.setBase(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.dto.atom.Common */
	public Logo setLang(String value) {
		super.setLang(value);
		return this;
	}

	// </FluentSetters>
}
