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
 * Represents an <code>atomCommonAttributes</code> construct in the RFC4287 specification.
 * <p>
 * <h6 class='figure'>Schema</h6>
 * <p class='bcode'>
 * 	atomCommonAttributes =
 * 		attribute xml:base { atomUri }?,
 * 		attribute xml:lang { atomLanguageTag }?,
 * 		undefinedAttribute*
 * </p>
 * <p>
 * 	Refer to {@link com.ibm.juno.core.dto.atom} for further information about ATOM support.
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
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
	 * @param base The URI base of this object.
	 * @return This object (for method chaining).
	 */
	public Common setBase(URI base) {
		this.base = base;
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
	public Common setLang(String lang) {
		this.lang = lang;
		return this;
	}
}
