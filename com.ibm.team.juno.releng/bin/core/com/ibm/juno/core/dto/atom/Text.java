/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.dto.atom;

import static com.ibm.juno.core.xml.XmlUtils.*;
import static com.ibm.juno.core.xml.annotation.XmlFormat.*;

import java.net.*;

import javax.xml.stream.*;

import com.ibm.juno.core.xml.*;
import com.ibm.juno.core.xml.annotation.*;

/**
 * Represents an <code>atomTextConstruct</code> construct in the RFC4287 specification.
 * <p>
 * <h6 class='figure'>Schema</h6>
 * <p class='bcode'>
 * 	atomTextConstruct = atomPlainTextConstruct | atomXHTMLTextConstruct
 *
 * 	atomPlainTextConstruct =
 * 		atomCommonAttributes,
 * 		attribute type { "text" | "html" }?,
 * 		text
 *
 * 	atomXHTMLTextConstruct =
 * 		atomCommonAttributes,
 * 		attribute type { "xhtml" },
 * 		xhtmlDiv
 *
 * 	xhtmlDiv = element xhtml:div {
 * 		(attribute * { text }
 * 		| text
 * 		| anyXHTML)*
 * 	}
 * </p>
 * <p>
 * 	Refer to {@link com.ibm.juno.core.dto.atom} for further information about ATOM support.
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class Text extends Common {

	private String type;
	String text;


	/**
	 * Normal content.
	 *
	 * @param type The content type of this content.
	 * @param text The text of this content.
	 */
	public Text(String type, String text) {
		this.type = type;
		this.text = text;
	}

	/**
	 * Normal content.
	 *
	 * @param text The text of this content.
	 */
	public Text(String text) {
		this.text = text;
	}

	/** Bean constructor. */
	public Text() {}


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Returns the content type of this content.
	 *
	 * @return The content type of this content.
	 */
	@Xml(format=ATTR)
	public String getType() {
		return type;
	}

	/**
	 * Sets the content type of this content.
	 * <p>
	 * 	Must be one of the following:
	 * <ul>
	 * 	<li><js>"text"</js>
	 * 	<li><js>"html"</js>
	 * 	<li><js>"xhtml"</js>
	 * 	<li><jk>null</jk> (defaults to <js>"text"</js>)
	 * </ul>
	 *
	 * @param type The content type of this content.
	 * @return This object (for method chaining).
	 */
	public Text setType(String type) {
		this.type = type;
		return this;
	}

	/**
	 * Returns the content of this content.
	 *
	 * @return The content of this content.
	 */
	@Xml(format=CONTENT, contentHandler=TextContentHandler.class)
	public String getText() {
		return text;
	}

	/**
	 * Sets the content of this content.
	 *
	 * @param text The content of this content.
	 * @return This object (for method chaining).
	 */
	public Text setText(String text) {
		this.text = text;
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* Common */
	public Text setBase(URI base) {
		super.setBase(base);
		return this;
	}

	@Override /* Common */
	public Text setLang(String lang) {
		super.setLang(lang);
		return this;
	}

	/**
	 * Specialized content handler for correctly handling XML element content based
	 * 	on the <code>type</code> attribute of the element.
	 * <p>
	 * 	If the <code>type</code> attribute is <js>"xhtml"</js> the content is treated
	 * 	as XML.  Otherwise, it's treated as plain text.
	 */
	public static class TextContentHandler implements XmlContentHandler<Text> {

		@Override /* XmlContentHandler */
		public void parse(XMLStreamReader r, Text text) throws Exception {
			String type = text.type;
			if (type != null && type.equals("xhtml"))
				text.text = decode(readXmlContents(r).trim());
			else
				text.text = decode(r.getElementText().trim());
		}

		@Override /* XmlContentHandler */
		public void serialize(XmlSerializerWriter w, Text text) throws Exception {
			String type = text.type;
			String content = text.text;
			if (type != null && type.equals("xhtml"))
				w.encodeTextInvalidChars(content);
			else
				w.encodeText(content);
		}
	}
}
