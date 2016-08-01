/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.xml;

import static com.ibm.juno.core.xml.NamespaceFactory.*;
import static com.ibm.juno.core.xml.XmlSerializerProperties.*;

import java.io.*;
import java.lang.reflect.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;

/**
 * Context object that lives for the duration of a single serialization of the {@link XmlSerializer}.
 * <p>
 * 	See {@link SerializerContext} for details.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@SuppressWarnings("hiding")
public class XmlSerializerContext extends SerializerContext {

	private final boolean
		addJsonTypeAttrs,
		addJsonStringTypeAttrs,
		autoDetectNamespaces,
		enableNamespaces,
		addNamespaceUrlsToRoot;

	private Namespace
		defaultNamespace,
		xsiNamespace,
		xsNamespace;

	private Namespace[] namespaces = new Namespace[0];

	/**
	 * Constructor.
	 * @param beanContext The bean context being used by the serializer.
	 * @param sp Default general serializer properties.
	 * @param xsp Default XML serializer properties.
	 * @param op Override properties.
	 * @param javaMethod Java method that invoked this serializer.
	 * 	When using the REST API, this is the Java method invoked by the REST call.
	 * 	Can be used to access annotations defined on the method or class.
	 */
	public XmlSerializerContext(BeanContext beanContext, SerializerProperties sp, XmlSerializerProperties xsp, ObjectMap op, Method javaMethod) {
		super(beanContext, sp, op, javaMethod);
		if (op == null || op.isEmpty()) {
			addJsonTypeAttrs = xsp.addJsonTypeAttrs;
			addJsonStringTypeAttrs = xsp.addJsonStringTypeAttrs;
			enableNamespaces = xsp.enableNamespaces;
			autoDetectNamespaces = xsp.autoDetectNamespaces;
			addNamespaceUrlsToRoot = xsp.addNamespaceUrlsToRoot;
			addNamespaces(xsp.namespaces);
			setDefaultNamespace(xsp.defaultNamespace);
			xsiNamespace = xsp.xsiNamespace;
			xsNamespace = xsp.xsNamespace;
		} else {
			addJsonTypeAttrs = op.getBoolean(XML_addJsonTypeAttrs, xsp.addJsonTypeAttrs);
			addJsonStringTypeAttrs = op.getBoolean(XML_addJsonStringTypeAttrs, xsp.addJsonStringTypeAttrs);
			enableNamespaces = op.getBoolean(XML_enableNamespaces, xsp.enableNamespaces);
			autoDetectNamespaces = op.getBoolean(XML_autoDetectNamespaces, xsp.autoDetectNamespaces);
			addNamespaceUrlsToRoot = op.getBoolean(XML_addNamespaceUrisToRoot, xsp.addNamespaceUrlsToRoot);
			namespaces = (op.containsKey(XML_namespaces) ? parseNamespaces(op.get(XML_namespaces)) : xsp.namespaces);
			setDefaultNamespace(op.containsKey(XML_defaultNamespaceUri) ? op.getString(XML_defaultNamespaceUri) : xsp.defaultNamespace);
			xsiNamespace = (op.containsKey(XML_xsiNamespace) ? parseNamespace(op.get(XML_xsiNamespace)) : xsp.xsiNamespace);
			xsNamespace = (op.containsKey(XML_xsNamespace) ? parseNamespace(op.get(XML_xsNamespace)) : xsp.xsNamespace);
		}
	}

	private void setDefaultNamespace(String s) {
		if (s == null)
			return;
		if (StringUtils.startsWith(s, '{'))
			defaultNamespace = parseNamespace(s);
		else if (! s.startsWith("http://"))
			defaultNamespace = get(s, "http://unknown");
		else
			defaultNamespace = get(null, s);
	}

	private void addNamespaces(Namespace...namespaces) {
		for (Namespace ns : namespaces)
			addNamespace(ns);
	}

	void addNamespace(Namespace ns) {
		if (ns == defaultNamespace)
			return;

		for (Namespace n : namespaces)
			if (n == ns)
				return;

		if (defaultNamespace != null && (ns.uri.equals(defaultNamespace.uri) || ns.name.equals(defaultNamespace.name)))
			defaultNamespace = ns;
		else
			namespaces = ArrayUtils.append(namespaces, ns);
	}

	/**
	 * Returns the list of namespaces being used in the current XML serialization.
	 *
	 * @return The list of namespaces being used in the current XML serialization.
	 */
	public Namespace[] getNamespaces() {
		return namespaces;
	}

	/**
	 * Returns the {@link XmlSerializerProperties#XML_addJsonTypeAttrs} setting value in this context.
	 *
	 * @return The {@link XmlSerializerProperties#XML_addJsonTypeAttrs} setting value in this context.
	 */
	public final boolean isAddJsonTypeAttrs() {
		return addJsonTypeAttrs;
	}

	/**
	 * Returns the {@link XmlSerializerProperties#XML_addJsonStringTypeAttrs} setting value in this context.
	 *
	 * @return The {@link XmlSerializerProperties#XML_addJsonStringTypeAttrs} setting value in this context.
	 */
	public final boolean isAddJsonStringTypeAttrs() {
		return addJsonStringTypeAttrs;
	}

	/**
	 * Returns the {@link XmlSerializerProperties#XML_autoDetectNamespaces} setting value in this context.
	 *
	 * @return The {@link XmlSerializerProperties#XML_autoDetectNamespaces} setting value in this context.
	 */
	public final boolean isAutoDetectNamespaces() {
		return enableNamespaces && autoDetectNamespaces;
	}

	/**
	 * Returns the {@link XmlSerializerProperties#XML_enableNamespaces} setting value in this context.
	 *
	 * @return The {@link XmlSerializerProperties#XML_enableNamespaces} setting value in this context.
	 */
	public final boolean isEnableNamespaces() {
		return enableNamespaces;
	}

	/**
	 * Returns the {@link XmlSerializerProperties#XML_addNamespaceUrisToRoot} setting value in this context.
	 *
	 * @return The {@link XmlSerializerProperties#XML_addNamespaceUrisToRoot} setting value in this context.
	 */
	public final boolean isAddNamespaceUrlsToRoot() {
		return addNamespaceUrlsToRoot;
	}

	/**
	 * Returns the {@link XmlSerializerProperties#XML_defaultNamespaceUri} setting value in this context.
	 *
	 * @return The {@link XmlSerializerProperties#XML_defaultNamespaceUri} setting value in this context.
	 */
	public final Namespace getDefaultNamespace() {
		return defaultNamespace;
	}

	/**
	 * Returns the {@link XmlSerializerProperties#XML_xsiNamespace} setting value in this context.
	 *
	 * @return The {@link XmlSerializerProperties#XML_xsiNamespace} setting value in this context.
	 */
	public final Namespace getXsiNamespace() {
		return xsiNamespace;
	}

	/**
	 * Returns the {@link XmlSerializerProperties#XML_xsNamespace} setting value in this context.
	 *
	 * @return The {@link XmlSerializerProperties#XML_xsNamespace} setting value in this context.
	 */
	public final Namespace getXsNamespace() {
		return xsNamespace;
	}

	/**
	 * Wraps the specified writer in a {@link XmlSerializerWriter} if it is not already an instance of that class.
	 *
	 * @param out The writer being wrapped.
	 * @return The wrapped writer.
	 */
	public XmlSerializerWriter getWriter(Writer out) {
		if (out instanceof XmlSerializerWriter)
			return (XmlSerializerWriter)out;
		return new XmlSerializerWriter(out, isUseIndentation(), getQuoteChar(), getRelativeUriBase(), getAbsolutePathUriBase(), isEnableNamespaces(), getDefaultNamespace());
	}
}
