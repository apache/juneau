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
 * 	<li>{@link HtmlSerializer#addToDictionary(Class[])}
 * 	<li>{@link HtmlSerializer#addImplClass(Class,Class)}
 * </ul>
 * <p>
 * See {@link ContextFactory} for more information about context properties.
 *
 *
 * <h6 class='topic' id='ConfigProperties'>Configurable properties on the HTML serializer</h6>
 * <table class='styled' style='border-collapse: collapse;'>
 * 	<tr><th>Setting name</th><th>Description</th><th>Data type</th><th>Default value</th></tr>
 * 	<tr>
 * 		<td>{@link #HTML_uriAnchorText}</td>
 * 		<td>Anchor text source.</td>
 * 		<td><code>String</code></td>
 * 		<td><js>"TO_STRING"</js></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #HTML_detectLinksInStrings}</td>
 * 		<td>Look for URLs in {@link String Strings}.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #HTML_lookForLabelParameters}</td>
 * 		<td>Look for link labels in the <js>"label"</js> parameter of the URL.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #HTML_labelParameter}</td>
 * 		<td>The parameter name to use when using {@link #HTML_lookForLabelParameters}.</td>
 * 		<td><code>String</code></td>
 * 		<td><js>"label"</js></td>
 * 	</tr>
 * </table>
 *
 * <h6 class='topic'>Configurable properties inherited from parent classes</h6>
 * <ul class='javahierarchy'>
 * 	<li class='c'><a class='doclink' href='../BeanContext.html#ConfigProperties'>BeanContext</a> - Properties associated with handling beans on serializers and parsers.
 * 	<ul>
 * 		<li class='c'><a class='doclink' href='../serializer/SerializerContext.html#ConfigProperties'>SerializerContext</a> - Configurable properties common to all serializers.
 * 	</ul>
 * </ul>
 *
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class HtmlSerializerContext extends XmlSerializerContext {

	/**
	 * <b>Configuration property:</b>  Anchor text source.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.uriAnchorText"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"toString"</js>
	 * </ul>
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
	 * <b>Configuration property:</b>  Look for URLs in {@link String Strings}.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.detectLinksInStrings"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * If a string looks like a URL (e.g. starts with <js>"http://"</js> or <js>"https://"</js>, then treat it like a URL
	 * 	and make it into a hyperlink based on the rules specified by {@link #HTML_uriAnchorText}.
	 */
	public static final String HTML_detectLinksInStrings = "HtmlSerializer.detectLinksInStrings";

	/**
	 * <b>Configuration property:</b>  Look for link labels in the <js>"label"</js> parameter of the URL.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"HtmlSerializer.lookForLabelParameters"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
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
	 * </ul>
	 * <p>
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
