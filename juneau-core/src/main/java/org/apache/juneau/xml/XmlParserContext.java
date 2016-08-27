/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.xml;

import javax.xml.stream.*;
import javax.xml.stream.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;

/**
 * Configurable properties on the {@link XmlParser} class.
 * <p>
 * Context properties are set by calling {@link ContextFactory#setProperty(String, Object)} on the context factory
 * returned {@link CoreApi#getContextFactory()}.
 * <p>
 * The following convenience methods are also provided for setting context properties:
 * <ul>
 * 	<li>{@link XmlParser#setProperty(String,Object)}
 * 	<li>{@link XmlParser#setProperties(ObjectMap)}
 * 	<li>{@link XmlParser#addNotBeanClasses(Class[])}
 * 	<li>{@link XmlParser#addBeanFilters(Class[])}
 * 	<li>{@link XmlParser#addPojoSwaps(Class[])}
 * 	<li>{@link XmlParser#addImplClass(Class,Class)}
 * </ul>
 * <p>
 * See {@link ContextFactory} for more information about context properties.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class XmlParserContext extends ParserContext {

	/**
	 * XMLSchema-instance namespace URI ({@link String}, default=<js>"http://www.w3.org/2001/XMLSchema-instance"</js>).
	 * <p>
	 * The XMLSchema namespace.
	 */
	public static final String XML_xsiNs = "XmlParser.xsiNs";

	/**
	 * Trim whitespace from text elements ({@link Boolean}, default=<jk>true</jk>).
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

	final String xsiNs;
	final boolean
		trimWhitespace,
		validating,
		coalescing,
		replaceEntityReferences,
		preserveRootElement;
	final XMLReporter reporter;
	final XMLResolver resolver;
	final XMLEventAllocator eventAllocator;

	/**
	 * Constructor.
	 * <p>
	 * Typically only called from {@link ContextFactory#getContext(Class)}.
	 *
	 * @param cf The factory that created this context.
	 */
	public XmlParserContext(ContextFactory cf) {
		super(cf);
		xsiNs = cf.getProperty(XML_xsiNs, String.class, "http://www.w3.org/2001/XMLSchema-instance");
		trimWhitespace = cf.getProperty(XML_trimWhitespace, boolean.class, true);
		validating = cf.getProperty(XML_validating, boolean.class, false);
		coalescing = cf.getProperty(XML_coalescing, boolean.class, false);
		replaceEntityReferences = cf.getProperty(XML_replaceEntityReferences, boolean.class, true);
		preserveRootElement = cf.getProperty(XML_preserveRootElement, boolean.class, false);
		reporter = cf.getProperty(XML_reporter, XMLReporter.class, null);
		resolver = cf.getProperty(XML_resolver, XMLResolver.class, null);
		eventAllocator = cf.getProperty(XML_eventAllocator, XMLEventAllocator.class, null);
	}
}
