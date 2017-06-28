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

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents an <code>atomLogo</code> construct in the RFC4287 specification.
 *
 * <h6 class='figure'>Schema</h6>
 * <p class='bcode'>
 * 	atomLogo = element atom:logo {
 * 		atomCommonAttributes,
 * 		(atomUri)
 * 	}
 * </p>
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'>
 * 		<a class='doclink' href='../../../../../overview-summary.html#DTOs'>Juneau Data Transfer Objects
 * 		(org.apache.juneau.dto)</a>
 * 		<ul>
 * 			<li class='sublink'>
 * 				<a class='doclink' href='../../../../../overview-summary.html#DTOs.Atom'>Atom</a>
 * 		</ul>
 * 	</li>
 * 	<li class='jp'>
 * 		<a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.dto.atom</a>
 * 	</li>
 * </ul>
 */
@Bean(typeName="logo")
@SuppressWarnings("hiding")
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
		uri(uri);
	}

	/** Bean constructor. */
	public Logo() {}


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Returns the URI of the logo.
	 *
	 * @return The URI of the logo.
	 */
	@Xml(format=ELEMENTS)
	public URI getUri() {
		return uri;
	}

	/**
	 * Sets the URI of the logo.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * <br>Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param uri The URI of the logo.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("uri")
	public Logo uri(Object uri) {
		this.uri = toURI(uri);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* Common */
	public Logo base(Object base) {
		super.base(base);
		return this;
	}

	@Override /* Common */
	public Logo lang(String lang) {
		super.lang(lang);
		return this;
	}
}
