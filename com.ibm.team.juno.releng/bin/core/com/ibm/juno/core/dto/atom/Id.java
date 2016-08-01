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
 * Represents an <code>atomId</code> construct in the RFC4287 specification.
 * <p>
 * <h6 class='figure'>Schema</h6>
 * <p class='bcode'>
 * 	atomId = element atom:id {
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
@Xml(name="id")
public class Id extends Common {

	private String text;

	/**
	 * Normal constructor.
	 *
	 * @param text The id element contents.
	 */
	public Id(String text) {
		this.text = text;
	}

	/** Bean constructor. */
	public Id() {}


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Returns the content of this identifier.
	 *
	 * @return The content of this identifier.
	 */
	@Xml(format=CONTENT)
	public String getText() {
		return text;
	}

	/**
	 * Sets the content of this identifier.
	 *
	 * @param text The content of this identifier.
	 * @return This object (for method chaining).
	 */
	public Id setText(String text) {
		this.text = text;
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* Common */
	public Id setBase(URI base) {
		super.setBase(base);
		return this;
	}

	@Override /* Common */
	public Id setLang(String lang) {
		super.setLang(lang);
		return this;
	}
}
