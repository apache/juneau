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

import java.net.*;

/**
 * Represents an <code>atomPersonConstruct</code> construct in the RFC4287 specification.
 * <p>
 * <h6 class='figure'>Schema</h6>
 * <p class='bcode'>
 * 	atomPersonConstruct =
 * 		atomCommonAttributes,
 * 		(element atom:name { text }
 * 		& element atom:uri { atomUri }?
 * 		& element atom:email { atomEmailAddress }?
 * 		& extensionElement*)
 * </p>
 * <p>
 * 	Refer to {@link org.apache.juneau.dto.atom} for further information about ATOM support.
 * </p>
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
		this.name = name;
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
	public Person setName(String name) {
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
	 * @param uri The URI of the person.
	 * @return This object (for method chaining).
	 */
	public Person setUri(URI uri) {
		this.uri = uri;
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
	public Person setEmail(String email) {
		this.email = email;
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* Common */
	public Person setBase(URI base) {
		super.setBase(base);
		return this;
	}

	@Override /* Common */
	public Person setLang(String lang) {
		super.setLang(lang);
		return this;
	}
}
