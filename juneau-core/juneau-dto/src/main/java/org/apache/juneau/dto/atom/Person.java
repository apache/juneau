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

import java.net.*;
import java.net.URI;

import org.apache.juneau.*;

import org.apache.juneau.annotation.*;

/**
 * Represents an <code>atomPersonConstruct</code> construct in the RFC4287 specification.
 *
 * <h5 class='figure'>Schema</h5>
 * <p class='bcode w800'>
 * 	atomPersonConstruct =
 * 		atomCommonAttributes,
 * 		(element atom:name { text }
 * 		&amp; element atom:uri { atomUri }?
 * 		&amp; element atom:email { atomEmailAddress }?
 * 		&amp; extensionElement*)
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../../overview-summary.html#juneau-dto.Atom'>Overview &gt; juneau-dto &gt; Atom</a>
 * 	<li class='jp'><a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.dto.atom</a>
 * </ul>
 */
public class Person extends Common {

	private String name;
	private URI uri;
	private String email;


	/**
	 * Normal constructor.
	 *
	 * @param name The name of the person.
	 */
	public Person(String name) {
		name(name);
	}

	/** Bean constructor. */
	public Person() {}


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Returns the name of the person.
	 *
	 * @return The name of the person.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the person.
	 *
	 * @param name The name of the person.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("name")
	public Person name(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Returns the URI of the person.
	 *
	 * @return The URI of the person.
	 */
	public URI getUri() {
		return uri;
	}

	/**
	 * Sets the URI of the person.
	 *
	 * <p>
	 * The value can be of any of the following types: {@link URI}, {@link URL}, {@link String}.
	 * Strings must be valid URIs.
	 *
	 * <p>
	 * URIs defined by {@link UriResolver} can be used for values.
	 *
	 * @param uri The URI of the person.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("uri")
	public Person uri(Object uri) {
		this.uri = toURI(uri);
		return this;
	}

	/**
	 * Returns the email address of the person.
	 *
	 * @return The email address of the person.
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Sets the email address of the person.
	 *
	 * @param email The email address of the person.
	 * @return This object (for method chaining).
	 */
	@BeanProperty("email")
	public Person email(String email) {
		this.email = email;
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* Common */
	public Person base(Object base) {
		super.base(base);
		return this;
	}

	@Override /* Common */
	public Person lang(String lang) {
		super.lang(lang);
		return this;
	}
}
