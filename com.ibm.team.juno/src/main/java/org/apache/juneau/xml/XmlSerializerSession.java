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

import static org.apache.juneau.xml.NamespaceFactory.*;
import static org.apache.juneau.xml.XmlSerializerContext.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * Session object that lives for the duration of a single use of {@link XmlSerializer}.
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@SuppressWarnings("hiding")
public class XmlSerializerSession extends SerializerSession {

	private final boolean
		addJsonTypeAttrs,
		addJsonStringTypeAttrs,
		autoDetectNamespaces,
		enableNamespaces,
		addNamespaceUrlsToRoot;

	private Namespace
		defaultNamespace;
	private final Namespace
		xsiNamespace,
		xsNamespace;

	private Namespace[] namespaces = new Namespace[0];

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param beanContext The bean context being used.
	 * @param output The output object.  See {@link JsonSerializerSession#getWriter()} for valid class types.
	 * @param op The override properties.
	 * 	These override any context properties defined in the context.
	 * @param javaMethod The java method that called this parser, usually the method in a REST servlet.
	 */
	public XmlSerializerSession(XmlSerializerContext ctx, BeanContext beanContext, Object output, ObjectMap op, Method javaMethod) {
		super(ctx, beanContext, output, op, javaMethod);
		if (op == null || op.isEmpty()) {
			addJsonTypeAttrs = ctx.addJsonTypeAttrs;
			addJsonStringTypeAttrs = ctx.addJsonStringTypeAttrs;
			enableNamespaces = ctx.enableNamespaces;
			autoDetectNamespaces = ctx.autoDetectNamespaces;
			addNamespaceUrlsToRoot = ctx.addNamespaceUrlsToRoot;
			addNamespaces(ctx.namespaces);
			defaultNamespace = findDefaultNamespace(ctx.defaultNamespace);
			xsiNamespace = ctx.xsiNamespace;
			xsNamespace = ctx.xsNamespace;
		} else {
			addJsonTypeAttrs = op.getBoolean(XML_addJsonTypeAttrs, ctx.addJsonTypeAttrs);
			addJsonStringTypeAttrs = op.getBoolean(XML_addJsonStringTypeAttrs, ctx.addJsonStringTypeAttrs);
			enableNamespaces = op.getBoolean(XML_enableNamespaces, ctx.enableNamespaces);
			autoDetectNamespaces = op.getBoolean(XML_autoDetectNamespaces, ctx.autoDetectNamespaces);
			addNamespaceUrlsToRoot = op.getBoolean(XML_addNamespaceUrisToRoot, ctx.addNamespaceUrlsToRoot);
			namespaces = (op.containsKey(XML_namespaces) ? parseNamespaces(op.get(XML_namespaces)) : ctx.namespaces);
			defaultNamespace = findDefaultNamespace(op.containsKey(XML_defaultNamespaceUri) ? op.getString(XML_defaultNamespaceUri) : ctx.defaultNamespace);
			xsiNamespace = (op.containsKey(XML_xsiNamespace) ? parseNamespace(op.get(XML_xsiNamespace)) : ctx.xsiNamespace);
			xsNamespace = (op.containsKey(XML_xsNamespace) ? parseNamespace(op.get(XML_xsNamespace)) : ctx.xsNamespace);
		}
	}

	private Namespace findDefaultNamespace(String s) {
		if (s == null)
			return null;
		if (StringUtils.startsWith(s, '{'))
			return parseNamespace(s);
		if (! s.startsWith("http://"))
			return get(s, "http://unknown");
		return get(null, s);
	}

	private void addNamespaces(Namespace...namespaces) {
		for (Namespace ns : namespaces)
			addNamespace(ns);
	}

	/**
	 * Add a namespace to this session.
	 *
	 * @param ns The namespace being added.
	 */
	public void addNamespace(Namespace ns) {
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
	 * Returns the {@link XmlSerializerContext#XML_addJsonTypeAttrs} setting value in this context.
	 *
	 * @return The {@link XmlSerializerContext#XML_addJsonTypeAttrs} setting value in this context.
	 */
	public final boolean isAddJsonTypeAttrs() {
		return addJsonTypeAttrs;
	}

	/**
	 * Returns the {@link XmlSerializerContext#XML_addJsonStringTypeAttrs} setting value in this context.
	 *
	 * @return The {@link XmlSerializerContext#XML_addJsonStringTypeAttrs} setting value in this context.
	 */
	public final boolean isAddJsonStringTypeAttrs() {
		return addJsonStringTypeAttrs;
	}

	/**
	 * Returns the {@link XmlSerializerContext#XML_autoDetectNamespaces} setting value in this context.
	 *
	 * @return The {@link XmlSerializerContext#XML_autoDetectNamespaces} setting value in this context.
	 */
	public final boolean isAutoDetectNamespaces() {
		return enableNamespaces && autoDetectNamespaces;
	}

	/**
	 * Returns the {@link XmlSerializerContext#XML_enableNamespaces} setting value in this context.
	 *
	 * @return The {@link XmlSerializerContext#XML_enableNamespaces} setting value in this context.
	 */
	public final boolean isEnableNamespaces() {
		return enableNamespaces;
	}

	/**
	 * Returns the {@link XmlSerializerContext#XML_addNamespaceUrisToRoot} setting value in this context.
	 *
	 * @return The {@link XmlSerializerContext#XML_addNamespaceUrisToRoot} setting value in this context.
	 */
	public final boolean isAddNamespaceUrlsToRoot() {
		return addNamespaceUrlsToRoot;
	}

	/**
	 * Returns the {@link XmlSerializerContext#XML_defaultNamespaceUri} setting value in this context.
	 *
	 * @return The {@link XmlSerializerContext#XML_defaultNamespaceUri} setting value in this context.
	 */
	public final Namespace getDefaultNamespace() {
		return defaultNamespace;
	}

	/**
	 * Returns the {@link XmlSerializerContext#XML_xsiNamespace} setting value in this context.
	 *
	 * @return The {@link XmlSerializerContext#XML_xsiNamespace} setting value in this context.
	 */
	public final Namespace getXsiNamespace() {
		return xsiNamespace;
	}

	/**
	 * Returns the {@link XmlSerializerContext#XML_xsNamespace} setting value in this context.
	 *
	 * @return The {@link XmlSerializerContext#XML_xsNamespace} setting value in this context.
	 */
	public final Namespace getXsNamespace() {
		return xsNamespace;
	}

	@Override /* SerializerSession */
	public XmlWriter getWriter() throws Exception {
		Object output = getOutput();
		if (output instanceof XmlWriter)
			return (XmlWriter)output;
		return new XmlWriter(super.getWriter(), isUseIndentation(), isTrimStrings(), getQuoteChar(), getRelativeUriBase(), getAbsolutePathUriBase(), isEnableNamespaces(), getDefaultNamespace());
	}
}
