/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.xml.annotation;

/**
 * XML format to use when serializing a POJO.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public enum XmlFormat {

	/**
	 * Normal formatting (default)
	 */
	NORMAL,

	/**
	 * Render property as an attribute instead of an element.
	 * <p>
	 * 	Can only be applied to properties (methods/fields) of simple types (e.g. <code>String</code>, <code>Number</code>).
	 */
	ATTR,

	/**
	 * Render property as an element instead of an attribute.
	 * <p>
	 * 	Can be applied to URL and ID bean properties that would normally be rendered as attributes.
	 */
	ELEMENT,

	/**
	 * Prevents collections and arrays from being enclosed in <xt>&lt;array&gt;</xt> elements.
	 * <p>
	 * 	Can only be applied to properties (methods/fields) of type collection or array, or collection classes.
	 */
	COLLAPSED,

	/**
	 * Render property value directly as content of element.
	 * <p>
	 * 	By default, content is converted to plain text.
	 * <p>
	 * 	Can be used in combination with {@link Xml#contentHandler()} to produce something other
	 * 	than plain text, such as embedded XML.
	 */
	CONTENT
}