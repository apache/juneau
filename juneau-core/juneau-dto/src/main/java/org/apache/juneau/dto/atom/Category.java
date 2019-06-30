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
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'>{@doc juneau-dto.Atom}
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
	 * @return The category term.
	 */
	@Xml(format=ATTR)
	public String getTerm() {
		return term;
	}

	/**
	 * Sets the category term.
	 *
	 * @param term The category term.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("term")
	public Category term(String term) {
		this.term = term;
		return this;
	}

	/**
	 * Returns the category scheme.
	 *
	 * @return The category scheme.
	 */
	@Xml(format=ATTR)
	public URI getScheme() {
		return scheme;
	}

	/**
	 * Sets the category scheme.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param scheme The category scheme.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("scheme")
	public Category scheme(Object scheme) {
		this.scheme = toURI(scheme);
		return this;
	}

	/**
	 * Returns the category label.
	 *
	 * @return The category label.
	 */
	@Xml(format=ATTR)
	public String getLabel() {
		return label;
	}

	/**
	 * Sets the category label.
	 *
	 * @param label The category label.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("label")
	public Category label(String label) {
		this.label = label;
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
