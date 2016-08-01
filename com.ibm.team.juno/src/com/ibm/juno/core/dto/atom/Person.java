/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.dto.atom;

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
 * 	Refer to {@link com.ibm.juno.core.dto.atom} for further information about ATOM support.
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
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
