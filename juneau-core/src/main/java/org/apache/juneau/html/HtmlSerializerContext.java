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

import org.apache.juneau.*;
import org.apache.juneau.xml.*;

/**
 * Configurable properties on the {@link HtmlSerializer} class.
 * <p>
 * Context properties are set by calling {@link ContextFactory#setProperty(String, Object)} on the context factory
 * returned {@link CoreApi#getContextFactory()}.
 * <p>
 * The following convenience methods are also provided for setting context properties:
 * <ul>
 * 	<li>{@link HtmlSerializer#setProperty(String,Object)}
 * 	<li>{@link HtmlSerializer#setProperties(ObjectMap)}
 * 	<li>{@link HtmlSerializer#addNotBeanClasses(Class[])}
 * 	<li>{@link HtmlSerializer#addBeanFilters(Class[])}
 * 	<li>{@link HtmlSerializer#addPojoSwaps(Class[])}
 * 	<li>{@link HtmlSerializer#addImplClass(Class,Class)}
 * </ul>
 * <p>
 * See {@link ContextFactory} for more information about context properties.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class HtmlSerializerContext extends XmlSerializerContext {

	/**
	 * Anchor text source ({@link String}, default={@link #TO_STRING}).
	 * <p>
	 * When creating anchor tags (e.g. <code><xt>&lt;a</xt> <xa>href</xa>=<xs>'...'</xs><xt>&gt;</xt>text<xt>&lt;/a&gt;</xt></code>)
	 * 	in HTML, this setting defines what to set the inner text to.
	 * <p>
	 * Possible values:
	 * <ul class='spaced-list'>
	 * 	<li>{@link #TO_STRING} / <js>"toString"</js> - Set to whatever is returned by {@link #toString()} on the object.
	 * 	<li>{@link #URI} / <js>"uri"</js> - Set to the URI value.
	 * 	<li>{@link #LAST_TOKEN} / <js>"lastToken"</js> - Set to the last token of the URI value.
	 * 	<li>{@link #PROPERTY_NAME} / <js>"propertyName"</js> - Set to the bean property name.
	 * 	<li>{@link #URI_ANCHOR} / <js>"uriAnchor"</js> - Set to the anchor of the URL.  (e.g. <js>"http://localhost:9080/foobar#anchorTextHere"</js>)
	 * </ul>
	 */
	public static final String HTML_uriAnchorText = "HtmlSerializer.uriAnchorText";

	/** Constant for {@link HtmlSerializerContext#HTML_uriAnchorText} property. */
	public static final String PROPERTY_NAME = "PROPERTY_NAME";
	/** Constant for {@link HtmlSerializerContext#HTML_uriAnchorText} property. */
	public static final String TO_STRING = "TO_STRING";
	/** Constant for {@link HtmlSerializerContext#HTML_uriAnchorText} property. */
	public static final String URI = "URI";
	/** Constant for {@link HtmlSerializerContext#HTML_uriAnchorText} property. */
	public static final String LAST_TOKEN = "LAST_TOKEN";
	/** Constant for {@link HtmlSerializerContext#HTML_uriAnchorText} property. */
	public static final String URI_ANCHOR = "URI_ANCHOR";


	/**
	 * Look for URLs in {@link String Strings} ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * If a string looks like a URL (e.g. starts with <js>"http://"</js> or <js>"https://"</js>, then treat it like a URL
	 * 	and make it into a hyperlink based on the rules specified by {@link #HTML_uriAnchorText}.
	 */
	public static final String HTML_detectLinksInStrings = "HtmlSerializer.detectLinksInStrings";

	/**
	 * Look for link labels in the <js>"label"</js> parameter of the URL ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * If the URL has a label parameter (e.g. <js>"?label=foobar"</js>), then use that as the anchor text of the link.
	 * <p>
	 * The parameter name can be changed via the {@link #HTML_labelParameter} property.
	 */
	public static final String HTML_lookForLabelParameters = "HtmlSerializer.lookForLabelParameters";

	/**
	 * The parameter name to use when using {@link #HTML_lookForLabelParameters} ({@link String}, default=<js>"label"</js>).
	 */
	public static final String HTML_labelParameter = "HtmlSerializer.labelParameter";

	final String uriAnchorText;
	final boolean lookForLabelParameters, detectLinksInStrings;
	final String labelParameter;

	/**
	 * Constructor.
	 * <p>
	 * Typically only called from {@link ContextFactory#getContext(Class)}.
	 *
	 * @param cf The factory that created this context.
	 */
	public HtmlSerializerContext(ContextFactory cf) {
		super(cf);
		uriAnchorText = cf.getProperty(HTML_uriAnchorText, String.class, TO_STRING);
		lookForLabelParameters = cf.getProperty(HTML_lookForLabelParameters, Boolean.class, true);
		detectLinksInStrings = cf.getProperty(HTML_detectLinksInStrings, Boolean.class, true);
		labelParameter = cf.getProperty(HTML_labelParameter, String.class, "label");
	}
}
