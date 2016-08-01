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
 * Represents an <code>atomCategory</code> construct in the RFC4287 specification.
 * <p>
 * <h6 class='figure'>Schema</h6>
 * <p class='bcode'>
 * 	atomCategory =
 * 		element atom:category {
 * 			atomCommonAttributes,
 * 			attribute term { text },
 * 			attribute scheme { atomUri }?,
 * 			attribute label { text }?,
 * 			undefinedContent
 * 		}
 * </p>
 * <p>
 * 	Refer to {@link com.ibm.juno.core.dto.atom} for further information about ATOM support.
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Xml(name="category")
public class Category extends Common {

	private String term;
	private URI scheme;
	private String label;

	/**
	 * Normal constructor.
	 * @param term The category term.
	 */
	public Category(String term) {
		this.term = term;
	}

	/** Bean constructor. */
	public Category() {}


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

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
	public Category setTerm(String term) {
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
	 * @param scheme The category scheme.
	 * @return This object (for method chaining).
	 */
	public Category setScheme(URI scheme) {
		this.scheme = scheme;
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
	public Category setLabel(String label) {
		this.label = label;
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* Common */
	public Category setBase(URI base) {
		super.setBase(base);
		return this;
	}

	@Override /* Common */
	public Category setLang(String lang) {
		super.setLang(lang);
		return this;
	}
}
