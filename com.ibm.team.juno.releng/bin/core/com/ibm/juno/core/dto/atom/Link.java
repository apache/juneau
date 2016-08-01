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
 * Represents an <code>atomLink</code> construct in the RFC4287 specification.
 * <p>
 * <h6 class='figure'>Schema</h6>
 * <p class='bcode'>
 * 	atomLink =
 * 		element atom:link {
 * 			atomCommonAttributes,
 * 			attribute href { atomUri },
 * 			attribute rel { atomNCName | atomUri }?,
 * 			attribute type { atomMediaType }?,
 * 			attribute hreflang { atomLanguageTag }?,
 * 			attribute title { text }?,
 * 			attribute length { text }?,
 * 			undefinedContent
 * 		}
 * </p>
 * <p>
 * 	Refer to {@link com.ibm.juno.core.dto.atom} for further information about ATOM support.
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Xml(name="link")
public class Link extends Common {

	private String href;
	private String rel;
	private String type;
	private String hreflang;
	private String title;
	private Integer length;


	/**
	 * Normal constructor.
	 *
	 * @param rel The rel of the link.
	 * @param type The type of the link.
	 * @param href The URI of the link.
	 */
	public Link(String rel, String type, String href) {
		this.rel = rel;
		this.type = type;
		this.href = href;
	}

	/** Bean constructor. */
	public Link() {}


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Returns the href of the target of this link.
	 *
	 * @return The href of the target of this link.
	 */
	@Xml(format=ATTR)
	public String getHref() {
		return href;
	}

	/**
	 * Sets the href of the target of this link.
	 *
	 * @param href The href of the target of this link.
	 * @return This object (for method chaining).
	 */
	public Link setHref(String href) {
		this.href = href;
		return this;
	}

	/**
	 * Returns the rel of this link.
	 *
	 * @return The rel of this link.
	 */
	@Xml(format=ATTR)
	public String getRel() {
		return rel;
	}

	/**
	 * Sets the rel of this link.
	 *
	 * @param rel The rell of this link.
	 * @return This object (for method chaining).
	 */
	public Link setRel(String rel) {
		this.rel = rel;
		return this;
	}

	/**
	 * Returns the content type of the target of this link.
	 *
	 * @return The content type of the target of this link.
	 */
	@Xml(format=ATTR)
	public String getType() {
		return type;
	}

	/**
	 * Sets the content type of the target of this link.
	 * <p>
	 * 	Must be one of the following:
	 * <ul>
	 * 	<li><js>"text"</js>
	 * 	<li><js>"html"</js>
	 * 	<li><js>"xhtml"</js>
	 * 	<li><jk>null</jk> (defaults to <js>"text"</js>)
	 * </ul>
	 *
	 * @param type The content type of the target of this link.
	 * @return This object (for method chaining).
	 */
	public Link setType(String type) {
		this.type = type;
		return this;
	}

	/**
	 * Returns the language of the target of this link.
	 *
	 * @return The language of the target of this link.
	 */
	@Xml(format=ATTR)
	public String getHreflang() {
		return hreflang;
	}

	/**
	 * Sets the language of the target of this link.
	 *
	 * @param hreflang The language of the target of this link.
	 * @return This object (for method chaining).
	 */
	public Link setHreflang(String hreflang) {
		this.hreflang = hreflang;
		return this;
	}

	/**
	 * Returns the title of the target of this link.
	 *
	 * @return The title of the target of this link.
	 */
	@Xml(format=ATTR)
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the title of the target of this link.
	 *
	 * @param title The title of the target of this link.
	 * @return This object (for method chaining).
	 */
	public Link setTitle(String title) {
		this.title = title;
		return this;
	}

	/**
	 * Returns the length of the contents of the target of this link.
	 *
	 * @return The length of the contents of the target of this link.
	 */
	@Xml(format=ATTR)
	public Integer getLength() {
		return length;
	}

	/**
	 * Sets the length of the contents of the target of this link.
	 *
	 * @param length The length of the contents of the target of this link.
	 * @return This object (for method chaining).
	 */
	public Link setLength(Integer length) {
		this.length = length;
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* Common */
	public Link setBase(URI base) {
		super.setBase(base);
		return this;
	}

	@Override /* Common */
	public Link setLang(String lang) {
		super.setLang(lang);
		return this;
	}
}
