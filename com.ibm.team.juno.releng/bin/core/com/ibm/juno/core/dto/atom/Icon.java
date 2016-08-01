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
 * Represents an <code>atomIcon</code> construct in the RFC4287 specification.
 * <p>
 * <h6 class='figure'>Schema</h6>
 * <p class='bcode'>
 * 	atomIcon = element atom:icon {
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
@Xml(name="icon")
public class Icon extends Common {

	private URI uri;


	/**
	 * Normal constructor.
	 *
	 * @param uri The URI of the icon.
	 */
	public Icon(URI uri) {
		this.uri = uri;
	}

	/** Bean constructor. */
	public Icon() {}


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Returns the URI of this icon.
	 *
	 * @return The URI of this icon.
	 */
	@Xml(format=CONTENT)
	public URI getUri() {
		return uri;
	}

	/**
	 * Sets the URI of this icon.
	 *
	 * @param uri The URI of this icon.
	 * @return This object (for method chaining).
	 */
	public Icon setUri(URI uri) {
		this.uri = uri;
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* Common */
	public Icon setBase(URI base) {
		super.setBase(base);
		return this;
	}

	@Override /* Common */
	public Icon setLang(String lang) {
		super.setLang(lang);
		return this;
	}
}
