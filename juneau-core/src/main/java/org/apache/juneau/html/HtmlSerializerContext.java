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
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

/**
 * Configurable properties on the {@link HtmlSerializer} class.
 * <p>
 * Context properties are set by calling {@link PropertyStore#setProperty(String, Object)} on the property store
 * passed into the constructor.
 * <p>
 * See {@link PropertyStore} for more information about context properties.
 *
 * <h6 class='topic'>Inherited configurable properties</h6>
 * <ul class='doctree'>
 * 	<li class='jc'>
 * 		<a class="doclink" href="../BeanContext.html#ConfigProperties">BeanContext</a>
 * 		- Properties associated with handling beans on serializers and parsers.
 * 		<ul>
 * 			<li class='jc'>
 * 				<a class="doclink" href="../serializer/SerializerContext.html#ConfigProperties">SerializerContext</a>
 * 				- Configurable properties common to all serializers.
 * 		</ul>
 * 	</li>
 * </ul>
 */
public class HtmlSerializerContext extends XmlSerializerContext {

	/**
	 * <b>Configuration property:</b>  Anchor text source.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.uriAnchorText"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"toString"</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * When creating anchor tags (e.g. <code><xt>&lt;a</xt> <xa>href</xa>=<xs>'...'</xs>
	 * <xt>&gt;</xt>text<xt>&lt;/a&gt;</xt></code>) in HTML, this setting defines what to set the inner text to.
	 * <p>
	 * Possible values:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		{@link #TO_STRING} / <js>"toString"</js> - Set to whatever is returned by {@link #toString()} on the
	 * 		object.
	 * 	<li>
	 * 		{@link #URI} / <js>"uri"</js> - Set to the URI value.
	 * 	<li>
	 * 		{@link #LAST_TOKEN} / <js>"lastToken"</js> - Set to the last token of the URI value.
	 * 	<li>
	 * 		{@link #PROPERTY_NAME} / <js>"propertyName"</js> - Set to the bean property name.
	 * 	<li>
	 * 		{@link #URI_ANCHOR} / <js>"uriAnchor"</js> - Set to the anchor of the URL.
	 * 		(e.g. <js>"http://localhost:9080/foobar#anchorTextHere"</js>)
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
	 * <b>Configuration property:</b>  Look for URLs in {@link String Strings}.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.detectLinksInStrings"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If a string looks like a URL (e.g. starts with <js>"http://"</js> or <js>"https://"</js>, then treat it like a URL
	 * and make it into a hyperlink based on the rules specified by {@link #HTML_uriAnchorText}.
	 */
	public static final String HTML_detectLinksInStrings = "HtmlSerializer.detectLinksInStrings";

	/**
	 * <b>Configuration property:</b>  Look for link labels in the <js>"label"</js> parameter of the URL.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.lookForLabelParameters"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If the URL has a label parameter (e.g. <js>"?label=foobar"</js>), then use that as the anchor text of the link.
	 * <p>
	 * The parameter name can be changed via the {@link #HTML_labelParameter} property.
	 */
	public static final String HTML_lookForLabelParameters = "HtmlSerializer.lookForLabelParameters";

	/**
	 * <b>Configuration property:</b>  The parameter name to use when using {@link #HTML_lookForLabelParameters}.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.labelParameter"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"label"</js>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 */
	public static final String HTML_labelParameter = "HtmlSerializer.labelParameter";

	/**
	 * <b>Configuration property:</b>  Add key/value headers on bean/map tables.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.addKeyValueTableHeaders"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 */
	public static final String HTML_addKeyValueTableHeaders = "HtmlSerializer.addKeyValueTableHeaders";

	/**
	 * <b>Configuration property:</b>  Add <js>"_type"</js> properties when needed.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.addBeanTypeProperties"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 * This is used to recreate the correct objects during parsing if the object types cannot be inferred.
	 * For example, when serializing a {@code Map<String,Object>} field, where the bean class cannot be determined
	 * from the value type.
	 * <p>
	 * When present, this value overrides the {@link SerializerContext#SERIALIZER_addBeanTypeProperties} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerGroup}.
	 */
	public static final String HTML_addBeanTypeProperties = "HtmlSerializer.addBeanTypeProperties";


	final String uriAnchorText;
	final boolean
		lookForLabelParameters,
		detectLinksInStrings,
		addKeyValueTableHeaders,
		addBeanTypeProperties;
	final String labelParameter;

	/**
	 * Constructor.
	 * <p>
	 * Typically only called from {@link PropertyStore#getContext(Class)}.
	 *
	 * @param ps The property store that created this context.
	 */
	public HtmlSerializerContext(PropertyStore ps) {
		super(ps);
		uriAnchorText = ps.getProperty(HTML_uriAnchorText, String.class, TO_STRING);
		lookForLabelParameters = ps.getProperty(HTML_lookForLabelParameters, Boolean.class, true);
		detectLinksInStrings = ps.getProperty(HTML_detectLinksInStrings, Boolean.class, true);
		labelParameter = ps.getProperty(HTML_labelParameter, String.class, "label");
		addKeyValueTableHeaders = ps.getProperty(HTML_addKeyValueTableHeaders, Boolean.class, false);
		addBeanTypeProperties = ps.getProperty(HTML_addBeanTypeProperties, boolean.class,
			ps.getProperty(SERIALIZER_addBeanTypeProperties, boolean.class, true));
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("HtmlSerializerContext", new ObjectMap()
				.append("uriAnchorText", uriAnchorText)
				.append("lookForLabelParameters", lookForLabelParameters)
				.append("detectLinksInStrings", detectLinksInStrings)
				.append("labelParameter", labelParameter)
				.append("addKeyValueTableHeaders", addKeyValueTableHeaders)
				.append("addBeanTypeProperties", addBeanTypeProperties)
			);
	}
}
