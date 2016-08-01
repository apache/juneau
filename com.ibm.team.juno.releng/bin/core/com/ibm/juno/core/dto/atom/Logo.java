/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.dto.atom;

import static com.ibm.juno.core.xml.annotation.XmlFormat.*;

import java.net.*;

import com.ibm.juno.core.xml.annotation.*;

/**
 * Represents an <code>atomLogo</code> construct in the RFC4287 specification.
 * <p>
 * <h6 class='figure'>Schema</h6>
 * <p class='bcode'>
 * 	atomLogo = element atom:logo {
 * 		atomCommonAttributes,
 * 		(atomUri)
 * 	}
 * </p>
 * <p>
 * 	Refer to {@link com.ibm.juno.core.dto.atom} for further information about ATOM support.
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Xml(name="logo")
public class Logo extends Common {

	private URI uri;


	/**
	 * Normal constructor.
	 *
	 * @param uri The URI of the logo.
	 */
	public Logo(URI uri) {
		this.uri = uri;
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
	@Xml(format=CONTENT)
	public URI getUri() {
		return uri;
	}

	/**
	 * Sets the URI of the logo.
	 *
	 * @param uri The URI of the logo.
	 * @return This object (for method chaining).
	 */
	public Logo setUri(URI uri) {
		this.uri = uri;
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* Common */
	public Logo setBase(URI base) {
		super.setBase(base);
		return this;
	}

	@Override /* Common */
	public Logo setLang(String lang) {
		super.setLang(lang);
		return this;
	}
}
