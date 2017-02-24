// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.xml;

import static org.apache.juneau.msgpack.MsgPackSerializerContext.*;
import static org.apache.juneau.xml.NamespaceFactory.*;
import static org.apache.juneau.xml.XmlSerializerContext.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;

/**
 * Session object that lives for the duration of a single use of {@link XmlSerializer}.
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 */
@SuppressWarnings("hiding")
public class XmlSerializerSession extends SerializerSession {

	private final boolean
		autoDetectNamespaces,
		enableNamespaces,
		addNamespaceUrlsToRoot,
		addBeanTypeProperties;

	private Namespace
		defaultNamespace;
	private final Namespace
		xsNamespace;

	private Namespace[] namespaces = new Namespace[0];

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx The context creating this session object.
	 * The context contains all the configuration settings for this object.
	 * @param output The output object.  See {@link JsonSerializerSession#getWriter()} for valid class types.
	 * @param op The override properties.
	 * These override any context properties defined in the context.
	 * @param javaMethod The java method that called this parser, usually the method in a REST servlet.
	 * @param locale The session locale.
	 * If <jk>null</jk>, then the locale defined on the context is used.
	 * @param timeZone The session timezone.
	 * If <jk>null</jk>, then the timezone defined on the context is used.
	 * @param mediaType The session media type (e.g. <js>"application/json"</js>).
	 */
	public XmlSerializerSession(XmlSerializerContext ctx, ObjectMap op, Object output, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType) {
		super(ctx, op, output, javaMethod, locale, timeZone, mediaType);
		if (op == null || op.isEmpty()) {
			enableNamespaces = ctx.enableNamespaces;
			autoDetectNamespaces = ctx.autoDetectNamespaces;
			addNamespaceUrlsToRoot = ctx.addNamespaceUrlsToRoot;
			addNamespaces(ctx.namespaces);
			defaultNamespace = findDefaultNamespace(ctx.defaultNamespace);
			xsNamespace = ctx.xsNamespace;
			addBeanTypeProperties = ctx.addBeanTypeProperties;
		} else {
			enableNamespaces = op.getBoolean(XML_enableNamespaces, ctx.enableNamespaces);
			autoDetectNamespaces = op.getBoolean(XML_autoDetectNamespaces, ctx.autoDetectNamespaces);
			addNamespaceUrlsToRoot = op.getBoolean(XML_addNamespaceUrisToRoot, ctx.addNamespaceUrlsToRoot);
			namespaces = (op.containsKey(XML_namespaces) ? parseNamespaces(op.get(XML_namespaces)) : ctx.namespaces);
			defaultNamespace = findDefaultNamespace(op.containsKey(XML_defaultNamespace) ? op.getString(XML_defaultNamespace) : ctx.defaultNamespace);
			xsNamespace = (op.containsKey(XML_xsNamespace) ? parseNamespace(op.get(XML_xsNamespace)) : ctx.xsNamespace);
			addBeanTypeProperties = op.getBoolean(MSGPACK_addBeanTypeProperties, ctx.addBeanTypeProperties);
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
	 * Returns the {@link XmlSerializerContext#XML_addBeanTypeProperties} setting value for this session.
	 *
	 * @return The {@link XmlSerializerContext#XML_addBeanTypeProperties} setting value for this session.
	 */
	@Override /* SerializerSession */
	public boolean isAddBeanTypeProperties() {
		return addBeanTypeProperties;
	}

	/**
	 * Returns the {@link XmlSerializerContext#XML_defaultNamespace} setting value in this context.
	 *
	 * @return The {@link XmlSerializerContext#XML_defaultNamespace} setting value in this context.
	 */
	public final Namespace getDefaultNamespace() {
		return defaultNamespace;
	}

	/**
	 * Returns the {@link XmlSerializerContext#XML_xsNamespace} setting value in this context.
	 *
	 * @return The {@link XmlSerializerContext#XML_xsNamespace} setting value in this context.
	 */
	public final Namespace getXsNamespace() {
		return xsNamespace;
	}

	/**
	 * Returns <jk>true</jk> if we're serializing HTML.
	 * <p>
	 * The difference in behavior is how empty non-void elements are handled.
	 * The XML serializer will produce a collapsed tag, whereas the HTML serializer
	 * will produce a start and end tag.
	 *
	 * @return <jk>true</jk> if we're generating HTML.
	 */
	public boolean isHtmlMode() {
		return false;
	}

	@Override /* SerializerSession */
	public XmlWriter getWriter() throws Exception {
		Object output = getOutput();
		if (output instanceof XmlWriter)
			return (XmlWriter)output;
		return new XmlWriter(super.getWriter(), isUseWhitespace(), isTrimStrings(), getQuoteChar(), getRelativeUriBase(), getAbsolutePathUriBase(), isEnableNamespaces(), getDefaultNamespace());
	}
}
