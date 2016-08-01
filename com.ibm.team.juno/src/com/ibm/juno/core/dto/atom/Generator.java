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
 * Represents an <code>atomGenerator</code> construct in the RFC4287 specification.
 * <p>
 * <h6 class='figure'>Schema</h6>
 * <p class='bcode'>
 * 	atomGenerator = element atom:generator {
 * 		atomCommonAttributes,
 * 		attribute uri { atomUri }?,
 * 		attribute version { text }?,
 * 		text
 * 	}
 * </p>
 * <p>
 * 	Refer to {@link com.ibm.juno.core.dto.atom} for further information about ATOM support.
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Xml(name="generator")
public class Generator extends Common {

	private URI uri;
	private String version;
	private String text;


	/**
	 * Normal constructor.
	 *
	 * @param text The generator statement content.
	 */
	public Generator(String text) {
		this.text = text;
	}

	/** Bean constructor. */
	public Generator() {}


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Returns the URI of this generator statement.
	 *
	 * @return The URI of this generator statement.
	 */
	@Xml(format=ATTR)
	public URI getUri() {
		return uri;
	}

	/**
	 * Sets the URI of this generator statement.
	 *
	 * @param uri The URI of this generator statement.
	 * @return This object (for method chaining).
	 */
	public Generator setUri(URI uri) {
		this.uri = uri;
		return this;
	}

	/**
	 * Returns the version of this generator statement.
	 *
	 * @return The version of this generator statement.
	 */
	@Xml(format=ATTR)
	public String getVersion() {
		return version;
	}

	/**
	 * Sets the version of this generator statement.
	 *
	 * @param version The version of this generator statement.
	 * @return This object (for method chaining).
	 */
	public Generator setVersion(String version) {
		this.version = version;
		return this;
	}

	/**
	 * Returns the content of this generator statement.
	 *
	 * @return The content of this generator statement.
	 */
	@Xml(format=CONTENT)
	public String getText() {
		return text;
	}

	/**
	 * Sets the content of this generator statement.
	 *
	 * @param text The content of this generator statement.
	 * @return This object (for method chaining).
	 */
	public Generator setText(String text) {
		this.text = text;
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* Common */
	public Generator setBase(URI base) {
		super.setBase(base);
		return this;
	}

	@Override /* Common */
	public Generator setLang(String lang) {
		super.setLang(lang);
		return this;
	}
}
