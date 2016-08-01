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
package org.apache.juneau.html;

import static org.apache.juneau.html.HtmlSerializerContext.*;

import java.lang.reflect.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.xml.*;

/**
 * Session object that lives for the duration of a single use of {@link HtmlSerializer}.
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class HtmlSerializerSession extends XmlSerializerSession {

	private final AnchorText anchorText;
	private final boolean detectLinksInStrings, lookForLabelParameters;
	private final Pattern urlPattern = Pattern.compile("http[s]?\\:\\/\\/.*");
	private final Pattern labelPattern;
	private final String absolutePathUriBase, relativeUriBase;


	@SuppressWarnings("hiding")
	enum AnchorText {
		PROPERTY_NAME, TO_STRING, URI, LAST_TOKEN, URI_ANCHOR
	}

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
	protected HtmlSerializerSession(HtmlSerializerContext ctx, BeanContext beanContext, Object output, ObjectMap op, Method javaMethod) {
		super(ctx, beanContext, output, op, javaMethod);
		String labelParameter;
		if (op == null || op.isEmpty()) {
			anchorText = Enum.valueOf(AnchorText.class, ctx.uriAnchorText);
			detectLinksInStrings = ctx.detectLinksInStrings;
			lookForLabelParameters = ctx.lookForLabelParameters;
			labelParameter = ctx.labelParameter;
		} else {
			anchorText = Enum.valueOf(AnchorText.class, op.getString(HTML_uriAnchorText, ctx.uriAnchorText));
			detectLinksInStrings = op.getBoolean(HTML_detectLinksInStrings, ctx.detectLinksInStrings);
			lookForLabelParameters = op.getBoolean(HTML_lookForLabelParameters, ctx.lookForLabelParameters);
			labelParameter = op.getString(HTML_labelParameter, ctx.labelParameter);
		}
		labelPattern = Pattern.compile("[\\?\\&]" + Pattern.quote(labelParameter) + "=([^\\&]*)");
		this.absolutePathUriBase = getAbsolutePathUriBase();
		this.relativeUriBase = getRelativeUriBase();
	}

	@Override /* XmlSerializerSession */
	public HtmlWriter getWriter() throws Exception {
		Object output = getOutput();
		if (output instanceof HtmlWriter)
			return (HtmlWriter)output;
		return new HtmlWriter(super.getWriter(), isUseIndentation(), isTrimStrings(), getQuoteChar(), getRelativeUriBase(), getAbsolutePathUriBase());
	}

	/**
	 * Returns <jk>true</jk> if the specified object is a URL.
	 *
	 * @param cm The ClassMeta of the object being serialized.
	 * @param pMeta The property metadata of the bean property of the object.  Can be <jk>null</jk> if the object isn't from a bean property.
	 * @param o The object.
	 * @return <jk>true</jk> if the specified object is a URL.
	 */
	public boolean isUri(ClassMeta<?> cm, BeanPropertyMeta<?> pMeta, Object o) {
		if (cm.isUri())
			return true;
		if (pMeta != null && (pMeta.isUri() || pMeta.isBeanUri()))
			return true;
		if (detectLinksInStrings && o instanceof CharSequence && urlPattern.matcher(o.toString()).matches())
			return true;
		return false;
	}

	/**
	 * Returns the anchor text to use for the specified URL object.
	 *
	 * @param pMeta The property metadata of the bean property of the object.  Can be <jk>null</jk> if the object isn't from a bean property.
	 * @param o The URL object.
	 * @return The anchor text to use for the specified URL object.
	 */
	public String getAnchorText(BeanPropertyMeta<?> pMeta, Object o) {
		String s;
		if (lookForLabelParameters) {
			s = o.toString();
			Matcher m = labelPattern.matcher(s);
			if (m.find())
				return m.group(1);
		}
		switch (anchorText) {
			case LAST_TOKEN:
				s = o.toString();
				if (s.indexOf('/') != -1)
					s = s.substring(s.lastIndexOf('/')+1);
				if (s.indexOf('?') != -1)
					s = s.substring(0, s.indexOf('?'));
				if (s.indexOf('#') != -1)
					s = s.substring(0, s.indexOf('#'));
				return s;
			case URI_ANCHOR:
				s = o.toString();
				if (s.indexOf('#') != -1)
					s = s.substring(s.lastIndexOf('#')+1);
				return s;
			case PROPERTY_NAME:
				return pMeta == null ? o.toString() : pMeta.getName();
			case URI:
				s = o.toString();
				if (s.indexOf("://") == -1) {
					if (StringUtils.startsWith(s, '/')) {
						s = absolutePathUriBase + s;
					} else {
						if (relativeUriBase != null) {
							if (! relativeUriBase.equals("/"))
								s = relativeUriBase + "/" + s;
							else
								s = "/" + s;
						}
					}
				}
				return s;
			default:
				return o.toString();
		}
	}
}
