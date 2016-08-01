/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.html;

import static com.ibm.juno.core.html.HtmlDocSerializerProperties.*;
import static com.ibm.juno.core.html.HtmlSerializerProperties.*;

import java.io.*;
import java.lang.reflect.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.core.xml.*;

/**
 * Context object that lives for the duration of a single serialization of {@link HtmlSerializer} and its subclasses.
 * <p>
 * 	See {@link SerializerContext} for details.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class HtmlSerializerContext extends XmlSerializerContext {

	private final String uriAnchorText, title, description, cssUrl;
	private final String[] cssImports;
	private final ObjectMap links;
	private final boolean nowrap;

	/**
	 * Constructor.
	 *
	 * @param beanContext The bean context being used by the serializer.
	 * @param sp Default general serializer properties.
	 * @param xsp Default XML serializer properties.
	 * @param hsp Default HTML serializer properties.
	 * @param op Override properties.
	 * @param javaMethod Java method that invoked this serializer.
	 * 	When using the REST API, this is the Java method invoked by the REST call.
	 * 	Can be used to access annotations defined on the method or class.
	 */
	protected HtmlSerializerContext(BeanContext beanContext, SerializerProperties sp, XmlSerializerProperties xsp, HtmlSerializerProperties hsp, ObjectMap op, Method javaMethod) {
		super(beanContext, sp, xsp, op, javaMethod);
		if (op == null || op.isEmpty()) {
			uriAnchorText = hsp.uriAnchorText;
			title = hsp.title;
			description = hsp.description;
			links = hsp.links;
			cssUrl = hsp.cssUrl;
			cssImports = hsp.cssImports;
			nowrap = hsp.nowrap;
		} else {
			uriAnchorText = op.getString(HTML_uriAnchorText, hsp.uriAnchorText);
			title = op.getString(HTMLDOC_title, hsp.title);
			description = op.getString(HTMLDOC_description, hsp.description);
			ObjectMap m = op.getObjectMap(HTMLDOC_links, hsp.links);
			if (op.containsKey(HTMLDOC_addLinks))
				if (m == null)
					m = op.getObjectMap(HTMLDOC_addLinks, null);
				else
					m.putAll(op.getObjectMap(HTMLDOC_addLinks, null));
			links = m;
			cssUrl = op.getString(HTMLDOC_cssUrl, hsp.cssUrl);
			cssImports = StringUtils.split(op.getString(HTMLDOC_cssImports, null), ',');
			nowrap = op.getBoolean(HTMLDOC_cssUrl, hsp.nowrap);
		}
	}

	/**
	 * Returns the {@link HtmlSerializerProperties#HTML_uriAnchorText} setting value in this context.
	 *
	 * @return The {@link HtmlSerializerProperties#HTML_uriAnchorText} setting value in this context.
	 */
	public final String getUriAnchorText() {
		return uriAnchorText;
	}

	/**
	 * Returns the {@link HtmlDocSerializerProperties#HTMLDOC_title} setting value in this context.
	 *
	 * @return The {@link HtmlDocSerializerProperties#HTMLDOC_title} setting value in this context.
	 */
	public final String getTitle() {
		return title;
	}

	/**
	 * Returns the {@link HtmlDocSerializerProperties#HTMLDOC_description} setting value in this context.
	 *
	 * @return The {@link HtmlDocSerializerProperties#HTMLDOC_description} setting value in this context.
	 */
	public final String getDescription() {
		return description;
	}

	/**
	 * Returns the {@link HtmlDocSerializerProperties#HTMLDOC_links} setting value in this context.
	 *
	 * @return The {@link HtmlDocSerializerProperties#HTMLDOC_links} setting value in this context.
	 */
	public final ObjectMap getLinks() {
		return links;
	}

	/**
	 * Returns the {@link HtmlDocSerializerProperties#HTMLDOC_cssUrl} setting value in this context.
	 *
	 * @return The {@link HtmlDocSerializerProperties#HTMLDOC_cssUrl} setting value in this context.
	 */
	public final String getCssUrl() {
		return cssUrl;
	}

	/**
	 * Returns the {@link HtmlDocSerializerProperties#HTMLDOC_cssImports} setting value in this context.
	 *
	 * @return The {@link HtmlDocSerializerProperties#HTMLDOC_cssImports} setting value in this context.
	 */
	public final String[] getCssImports() {
		return cssImports;
	}

	/**
	 * Returns the {@link HtmlDocSerializerProperties#HTMLDOC_nowrap} setting value in this context.
	 *
	 * @return The {@link HtmlDocSerializerProperties#HTMLDOC_nowrap} setting value in this context.
	 */
	public final boolean isNoWrap() {
		return nowrap;
	}

	/**
	 * Wraps the specified writer in a {@link HtmlSerializerWriter}.
	 */
	@Override /* XmlSerializerContext */
	public HtmlSerializerWriter getWriter(Writer w) {
		if (w instanceof HtmlSerializerWriter)
			return (HtmlSerializerWriter)w;
		return new HtmlSerializerWriter(w, isUseIndentation(), getQuoteChar(), getRelativeUriBase(), getAbsolutePathUriBase());
	}
}
