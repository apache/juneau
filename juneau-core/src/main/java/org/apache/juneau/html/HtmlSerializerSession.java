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
package org.apache.juneau.html;

import static org.apache.juneau.html.HtmlSerializerContext.*;
import static org.apache.juneau.msgpack.MsgPackSerializerContext.*;
import static org.apache.juneau.xml.XmlUtils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.json.*;
import org.apache.juneau.xml.*;

/**
 * Session object that lives for the duration of a single use of {@link HtmlSerializer}.
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 */
public class HtmlSerializerSession extends XmlSerializerSession {

	private final AnchorText anchorText;
	private final boolean
		detectLinksInStrings,
		lookForLabelParameters,
		addKeyValueTableHeaders,
		addBeanTypeProperties;
	private final Pattern urlPattern = Pattern.compile("http[s]?\\:\\/\\/.*");
	private final Pattern labelPattern;


	@SuppressWarnings("hiding")
	enum AnchorText {
		PROPERTY_NAME, TO_STRING, URI, LAST_TOKEN, URI_ANCHOR, CONTEXT_RELATIVE, SERVLET_RELATIVE, PATH_RELATIVE
	}

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param output The output object.  See {@link JsonSerializerSession#getWriter()} for valid class types.
	 * @param op The override properties.
	 * 	These override any context properties defined in the context.
	 * @param javaMethod The java method that called this serializer, usually the method in a REST servlet.
	 * @param locale The session locale.
	 * 	If <jk>null</jk>, then the locale defined on the context is used.
	 * @param timeZone The session timezone.
	 * 	If <jk>null</jk>, then the timezone defined on the context is used.
	 * @param mediaType The session media type (e.g. <js>"application/json"</js>).
	 * @param uriContext The URI context.
	 * 	Identifies the current request URI used for resolution of URIs to absolute or root-relative form.
	 */
	protected HtmlSerializerSession(HtmlSerializerContext ctx, ObjectMap op, Object output, Method javaMethod,
			Locale locale, TimeZone timeZone, MediaType mediaType, UriContext uriContext) {
		super(ctx, op, output, javaMethod, locale, timeZone, mediaType, uriContext);
		String labelParameter;
		if (op == null || op.isEmpty()) {
			anchorText = Enum.valueOf(AnchorText.class, ctx.uriAnchorText);
			detectLinksInStrings = ctx.detectLinksInStrings;
			lookForLabelParameters = ctx.lookForLabelParameters;
			labelParameter = ctx.labelParameter;
			addKeyValueTableHeaders = ctx.addKeyValueTableHeaders;
			addBeanTypeProperties = ctx.addBeanTypeProperties;
		} else {
			anchorText = Enum.valueOf(AnchorText.class, op.getString(HTML_uriAnchorText, ctx.uriAnchorText));
			detectLinksInStrings = op.getBoolean(HTML_detectLinksInStrings, ctx.detectLinksInStrings);
			lookForLabelParameters = op.getBoolean(HTML_lookForLabelParameters, ctx.lookForLabelParameters);
			labelParameter = op.getString(HTML_labelParameter, ctx.labelParameter);
			addKeyValueTableHeaders = op.getBoolean(HTML_addKeyValueTableHeaders, ctx.addKeyValueTableHeaders);
			addBeanTypeProperties = op.getBoolean(MSGPACK_addBeanTypeProperties, ctx.addBeanTypeProperties);
		}
		labelPattern = Pattern.compile("[\\?\\&]" + Pattern.quote(labelParameter) + "=([^\\&]*)");
	}

	@Override /* XmlSerializerSession */
	public HtmlWriter getWriter() throws Exception {
		Object output = getOutput();
		if (output instanceof HtmlWriter)
			return (HtmlWriter)output;
		return new HtmlWriter(super.getWriter(), isUseWhitespace(), getMaxIndent(), isTrimStrings(), getQuoteChar(),
			getUriResolver());
	}

	/**
	 * Returns <jk>true</jk> if the specified object is a URL.
	 *
	 * @param cm The ClassMeta of the object being serialized.
	 * @param pMeta The property metadata of the bean property of the object.
	 * Can be <jk>null</jk> if the object isn't from a bean property.
	 * @param o The object.
	 * @return <jk>true</jk> if the specified object is a URL.
	 */
	public boolean isUri(ClassMeta<?> cm, BeanPropertyMeta pMeta, Object o) {
		if (cm.isUri())
			return true;
		if (pMeta != null && pMeta.isUri())
			return true;
		if (detectLinksInStrings && o instanceof CharSequence && urlPattern.matcher(o.toString()).matches())
			return true;
		return false;
	}

	/**
	 * Returns the anchor text to use for the specified URL object.
	 *
	 * @param pMeta The property metadata of the bean property of the object.
	 * Can be <jk>null</jk> if the object isn't from a bean property.
	 * @param o The URL object.
	 * @return The anchor text to use for the specified URL object.
	 */
	public String getAnchorText(BeanPropertyMeta pMeta, Object o) {
		String s = o.toString();
		if (lookForLabelParameters) {
			Matcher m = labelPattern.matcher(s);
			if (m.find())
				return urlDecode(m.group(1));
		}
		switch (anchorText) {
			case LAST_TOKEN:
				s = resolveUri(s);
				if (s.indexOf('/') != -1)
					s = s.substring(s.lastIndexOf('/')+1);
				if (s.indexOf('?') != -1)
					s = s.substring(0, s.indexOf('?'));
				if (s.indexOf('#') != -1)
					s = s.substring(0, s.indexOf('#'));
				if (s.isEmpty())
					s = "/";
				return urlDecode(s);
			case URI_ANCHOR:
				if (s.indexOf('#') != -1)
					s = s.substring(s.lastIndexOf('#')+1);
				return urlDecode(s);
			case PROPERTY_NAME:
				return pMeta == null ? s : pMeta.getName();
			case URI:
				return resolveUri(s);
			case CONTEXT_RELATIVE:
				return relativizeUri("context:/", s);
			case SERVLET_RELATIVE:
				return relativizeUri("servlet:/", s);
			case PATH_RELATIVE:
				return relativizeUri("request:/", s);
			default /* TO_STRING */:
				return s;
		}
	}

	/**
	 * Returns the {@link HtmlSerializerContext#HTML_addKeyValueTableHeaders} setting value for this session.
	 *
	 * @return The {@link HtmlSerializerContext#HTML_addKeyValueTableHeaders} setting value for this session.
	 */
	public final boolean isAddKeyValueTableHeaders() {
		return addKeyValueTableHeaders;
	}

	/**
	 * Returns the {@link HtmlSerializerContext#HTML_addBeanTypeProperties} setting value for this session.
	 *
	 * @return The {@link HtmlSerializerContext#HTML_addBeanTypeProperties} setting value for this session.
	 */
	@Override /* SerializerSession */
	public final boolean isAddBeanTypeProperties() {
		return addBeanTypeProperties;
	}

	@Override /* XmlSerializer */
	public boolean isHtmlMode() {
		return true;
	}
}
