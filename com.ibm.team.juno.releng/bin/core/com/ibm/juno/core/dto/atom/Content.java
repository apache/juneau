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
 * Represents an <code>atomContent</code> construct in the RFC4287 specification.
 * <p>
 *
 * <h6 class='figure'>Schema</h6>
 * <p class='bcode'>
 * 	atomContent = atomInlineTextContent
 * 		| atomInlineXHTMLContent
 * 		| atomInlineOtherContent
 * 		| atomOutOfLineContent
 *
 * 	atomInlineTextContent =
 * 		element atom:content {
 * 			atomCommonAttributes,
 * 			attribute type { "text" | "html" }?,
 * 			(text)*
 * 		}
 *
 * 	atomInlineXHTMLContent =
 * 		element atom:content {
 * 			atomCommonAttributes,
 * 			attribute type { "xhtml" },
 * 			xhtmlDiv
 * 		}
 *
 * 	atomInlineOtherContent =
 * 		element atom:content {
 * 			atomCommonAttributes,
 * 			attribute type { atomMediaType }?,
 * 			(text|anyElement)*
 * 	}
 *
 * 	atomOutOfLineContent =
 * 		element atom:content {
 * 			atomCommonAttributes,
 * 			attribute type { atomMediaType }?,
 * 			attribute src { atomUri },
 * 			empty
 * 	}
 * </p>
 * <p>
 * 	Refer to {@link com.ibm.juno.core.dto.atom} for further information about ATOM support.
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class Content extends Text {

	private URI src;


	/**
	 * Normal content.
	 *
	 * @param type The content type of this content.
	 * @param content The content of this content.
	 */
	public Content(String type, String content) {
		super(type, content);
	}

	/**
	 * Normal content.
	 *
	 * @param content The content of this content.
	 */
	public Content(String content) {
		super(content);
	}

	/** Bean constructor. */
	public Content() {}


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Returns the source URI.
	 *
	 * @return the source URI.
	 */
	@Xml(format=ATTR)
	public URI getSrc() {
		return src;
	}

	/**
	 * Sets the source URI.
	 *
	 * @param src The source URI.
	 * @return This object (for method chaining).
	 */
	public Content setSrc(URI src) {
		this.src = src;
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* Text */
	public Content setText(String text) {
		super.setText(text);
		return this;
	}

	@Override /* Text */
	public Content setType(String type) {
		super.setType(type);
		return this;
	}

	@Override /* Common */
	public Content setBase(URI base) {
		super.setBase(base);
		return this;
	}

	@Override /* Common */
	public Content setLang(String lang) {
		super.setLang(lang);
		return this;
	}
}