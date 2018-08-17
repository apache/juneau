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
import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Represents an <code>atomCommonAttributes</code> construct in the RFC4287 specification.
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bcode w800'>
 * 	atomCommonAttributes =
 * 		attribute xml:base { atomUri }?,
 * 		attribute xml:lang { atomLanguageTag }?,
 * 		undefinedAttribute*
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'>{@doc juneau-dto.Atom}
 * 	<li class='jp'><a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.dto.atom</a>
 * </ul>
 */
public abstract class Common {

	private URI base;
	private String lang;


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Returns the uri base of this object.
	 *
	 * @return The URI base of this object.
	 */
	@Xml(prefix="xml", format=ATTR)
	public URI getBase() {
		return base;
	}

	/**
	 * Sets the URI base of this object.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param base The URI base of this object.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("base")
	public Common base(Object base) {
		this.base = toURI(base);
		return this;
	}

	/**
	 * Returns the language of this object.
	 *
	 * @return The language of this object.
	 */
	@Xml(prefix="xml", format=ATTR)
	public String getLang() {
		return lang;
	}

	/**
	 * Sets the language of this object.
	 *
	 * @param lang The language of this object.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("lang")
	public Common lang(String lang) {
		this.lang = lang;
		return this;
	}

	@Override /* Object */
	public String toString() {
		return XmlSerializer.DEFAULT_SQ.toString(this);
	}
}
