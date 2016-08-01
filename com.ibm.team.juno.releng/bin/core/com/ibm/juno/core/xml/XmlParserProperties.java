/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.xml;

import javax.xml.stream.*;
import javax.xml.stream.util.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.parser.*;

/**
 * Configurable properties on the {@link XmlParser} class.
 * <p>
 * 	Use the {@link XmlParser#setProperty(String, Object)} method to set property values.
 * <p>
 * 	In addition to these properties, the following properties are also applicable for {@link XmlParser}.
 * <ul>
 * 	<li>{@link ParserProperties}
 * 	<li>{@link BeanContextProperties}
 * </ul>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class XmlParserProperties implements Cloneable {

	/**
	 * XMLSchema-instance namespace URI ({@link String}, default=<js>"http://www.w3.org/2001/XMLSchema-instance"</js>).
	 * <p>
	 * The XMLSchema namespace.
	 */
	public static final String XML_xsiNs = "XmlParser.xsiNs";

	/**
	 * Trim whitespace from text elements ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, whitespace in text elements will be automatically trimmed.
	 */
	public static final String XML_trimWhitespace = "XmlParser.trimWhitespace";

	/**
	 * Set validating mode ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, XML document will be validated.
	 * See {@link XMLInputFactory#IS_VALIDATING} for more info.
	 */
	public static final String XML_validating = "XmlParser.validating";

	/**
	 * Set coalescing mode ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, XML text elements will be coalesced.
	 * See {@link XMLInputFactory#IS_COALESCING} for more info.
	 */
	public static final String XML_coalescing = "XmlParser.coalescing";

	/**
	 * Replace entity references ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * If <jk>true</jk>, entity references will be replace during parsing.
	 * See {@link XMLInputFactory#IS_REPLACING_ENTITY_REFERENCES} for more info.
	 */
	public static final String XML_replaceEntityReferences = "XmlParser.replaceEntityReferences";

	/**
	 * XML reporter ({@link XMLReporter}, default=<jk>null</jk>).
	 * <p>
	 * Associates an {@link XMLReporter} with this parser.
	 * <p>
	 * Note:  Reporters are not copied to new parsers during a clone.
	 */
	public static final String XML_reporter = "XmlParser.reporter";

	/**
	 * XML resolver ({@link XMLResolver}, default=<jk>null</jk>).
	 * <p>
	 * Associates an {@link XMLResolver} with this parser.
	 */
	public static final String XML_resolver = "XmlParser.resolver";

	/**
	 * XML event allocator. ({@link XMLEventAllocator}, default=<jk>false</jk>).
	 * <p>
	 * Associates an {@link XMLEventAllocator} with this parser.
	 */
	public static final String XML_eventAllocator = "XmlParser.eventAllocator";

	/**
	 * Preserve root element during generalized parsing ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, when parsing into a generic {@link ObjectMap}, the map will
	 * 	contain a single entry whose key is the root element name.
	 *
	 * Example:
	 *	<table class='styled'>
	 *		<tr>
	 *			<td>XML</td>
	 *			<td>ObjectMap.toString(), preserveRootElement==false</td>
	 *			<td>ObjectMap.toString(), preserveRootElement==true</td>
	 *		</tr>
	 *		<tr>
	 *			<td><code><xt>&lt;root&gt;&lt;a&gt;</xt>foobar<xt>&lt;/a&gt;&lt;/root&gt;</xt><code></td>
	 *			<td><code>{ a:<js>'foobar'</js> }</code></td>
	 *			<td><code>{ root: { a:<js>'foobar'</js> }}</code></td>
	 *		</tr>
	 *	</table>
	 *
	 */
	public static final String XML_preserveRootElement = "XmlParser.preserveRootElement";

	private String xsiNs = "http://www.w3.org/2001/XMLSchema-instance";
	private boolean
		trimWhitespace = false,
		validating = false,
		coalescing = false,
		replaceEntityReferences = true,
		preserveRootElement = false;
	private XMLReporter reporter;
	private XMLResolver resolver;
	private XMLEventAllocator eventAllocator;

	/**
	 * Sets the specified property value.
	 * @param property The property name.
	 * @param value The property value.
	 * @return <jk>true</jk> if property name was valid and property was set.
	 * @throws LockedException If bean context is locked.
	 */
	protected boolean setProperty(String property, Object value) throws LockedException {
		BeanContext bc = BeanContext.DEFAULT;
		if (property.equals(XML_trimWhitespace))
			trimWhitespace = bc.convertToType(value, Boolean.class);
		else if (property.equals(XML_validating))
			validating = bc.convertToType(value, Boolean.class);
		else if (property.equals(XML_coalescing))
			coalescing = bc.convertToType(value, Boolean.class);
		else if (property.equals(XML_replaceEntityReferences))
			replaceEntityReferences = bc.convertToType(value, Boolean.class);
		else if (property.equals(XML_xsiNs))
			xsiNs = value.toString();
		else if (property.equals(XML_reporter) && value instanceof XMLReporter)
			reporter = (XMLReporter)value;
		else if (property.equals(XML_resolver) && value instanceof XMLResolver)
			resolver = (XMLResolver)value;
		else if (property.equals(XML_eventAllocator) && value instanceof XMLEventAllocator)
			eventAllocator = (XMLEventAllocator)value;
		else if (property.equals(XML_preserveRootElement))
			preserveRootElement = bc.convertToType(value, Boolean.class);
		else
			return false;
		return true;
	}

	/**
	 * Returns the current {@link #XML_xsiNs} value.
	 * @return The current {@link #XML_xsiNs} value.
	 */
	public String getXsiNs() {
		return xsiNs;
	}

	/**
	 * Returns the current {@link #XML_trimWhitespace} value.
	 * @return The current {@link #XML_trimWhitespace} value.
	 */
	public boolean isTrimWhitespace() {
		return trimWhitespace;
	}

	/**
	 * Returns the current {@link #XML_preserveRootElement} value.
	 * @return The current {@link #XML_preserveRootElement} value.
	 */
	public boolean isPreserveRootElement() {
		return preserveRootElement;
	}

	/**
	 * Returns the current {@link #XML_validating} value.
	 * @return The current {@link #XML_validating} value.
	 */
	public boolean isValidating() {
		return validating;
	}

	/**
	 * Returns the current {@link #XML_coalescing} value.
	 * @return The current {@link #XML_coalescing} value.
	 */
	public boolean isCoalescing() {
		return coalescing;
	}

	/**
	 * Returns the current {@link #XML_replaceEntityReferences} value.
	 * @return The current {@link #XML_replaceEntityReferences} value.
	 */
	public boolean isReplaceEntityReferences() {
		return replaceEntityReferences;
	}

	/**
	 * Returns the current {@link #XML_reporter} value.
	 * @return The current {@link #XML_reporter} value.
	 */
	public XMLReporter getReporter() {
		return reporter;
	}

	/**
	 * Returns the current {@link #XML_reporter} value.
	 * @return The current {@link #XML_reporter} value.
	 */
	public XMLResolver getResolver() {
		return resolver;
	}

	/**
	 * Returns the current {@link #XML_eventAllocator} value.
	 * @return The current {@link #XML_eventAllocator} value.
	 */
	public XMLEventAllocator getEventAllocator() {
		return eventAllocator;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Object */
	public XmlParserProperties clone() {
		try {
			return (XmlParserProperties)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen.
		}
	}
}
