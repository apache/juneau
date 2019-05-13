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
package org.apache.juneau.html.annotation;

import static org.apache.juneau.html.HtmlSerializer.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.html.*;
import org.apache.juneau.serializer.*;

/**
 * Annotation for specifying config properties defined in {@link HtmlSerializer}, {@link HtmlParser}, and {@link HtmlDocSerializer}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 */
@Documented
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@PropertyStoreApply(HtmlConfigApply.class)
public @interface HtmlConfig {

	//-------------------------------------------------------------------------------------------------------------------
	// HtmlSerializer
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 *
	 * <p>
	 * When present, this value overrides the {@link #SERIALIZER_addBeanTypes} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerGroup}.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"HtmlSerializer.addBeanTypes.b"</js>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlSerializer#HTML_addBeanTypes}
	 * </ul>
	 */
	String addBeanTypes() default "";

	/**
	 * Configuration property:  Add key/value headers on bean/map tables.
	 *
	 * <p>
	 * When enabled, <code><b>key</b></code> and <code><b>value</b></code> column headers are added to tables.
	 *
	 * <p>
	 * The following shows the difference between the two generated outputs:
	 *
	 * <table class='styled'>
	 * 	<tr>
	 * 		<th><code>withoutHeaders</code></th>
	 * 		<th><code>withHeaders</code></th>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			<table class='unstyled'>
	 * 				<tr><td>f1</td><td>foo</td></tr>
	 * 				<tr><td>f2</td><td>bar</td></tr>
	 * 			</table>
	 * 		</td>
	 * 		<td>
	 * 			<table class='unstyled'>
	 * 				<tr><th>key</th><th>value</th></tr>
	 * 				<tr><td>f1</td><td>foo</td></tr>
	 * 				<tr><td>f2</td><td>bar</td></tr>
	 * 			</table>
	 * 		</td>
	 * 	</tr>
	 * </table>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js>
	 * 			<li><js>"false"</js> (default)
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"HtmlSerializer.addKeyValueTableHeaders.b"</js>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlSerializer#HTML_addKeyValueTableHeaders}
	 * </ul>
	 */
	String addKeyValueTableHeaders() default "";

	/**
	 * Configuration property:  Look for URLs in {@link String Strings}.
	 *
	 * <p>
	 * If a string looks like a URL (i.e. starts with <js>"http://"</js> or <js>"https://"</js>, then treat it like a URL
	 * and make it into a hyperlink based on the rules specified by {@link #HTML_uriAnchorText}.
	 *
	 * <p>
	 * The following shows the difference between the two generated outputs:
	 *
	 * <table class='styled'>
	 * 	<tr>
	 * 		<th><code>withLinks</code></th>
	 * 		<th><code>withoutLinks</code></th>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			<table class='unstyled'>
	 * 				<tr><th>key</th><th>value</th></tr>
	 * 				<tr><td>f1</td><td><a href='http://www.apache.org'>http://www.apache.org</a></td></tr>
	 * 			</table>
	 * 		</td>
	 * 		<td>
	 * 			<table class='unstyled'>
	 * 				<tr><th>key</th><th>value</th></tr>
	 * 				<tr><td>f1</td><td>http://www.apache.org</td></tr>
	 * 			</table>
	 * 		</td>
	 * 	</tr>
	 * </table>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js> (default)
	 * 			<li><js>"false"</js>
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"HtmlSerializer.detectLinksInStrings.b"</js>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlSerializer#HTML_detectLinksInStrings}
	 * </ul>
	 */
	String detectLinksInStrings() default "";

	/**
	 * Configuration property:  Link label parameter name.
	 *
	 * <p>
	 * The parameter name to look for when resolving link labels via {@link #HTML_detectLabelParameters}.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Default value: <js>"label"</js>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"HtmlSerializer.labelParameter.s"</js>.
	 * </ul>
	 *
	 * <h5 class=''>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link #HTML_labelParameter}
	 * </ul>
	 */
	String labelParameter() default "";

	/**
	 * Configuration property:  Look for link labels in URIs.
	 *
	 * <p>
	 * If the URL has a label parameter (e.g. <js>"?label=foobar"</js>), then use that as the anchor text of the link.
	 *
	 * <p>
	 * The parameter name can be changed via the {@link #HTML_labelParameter} property.
	 *
	 * <p>
	 * The following shows the difference between the two generated outputs.
	 * <br>Note that they're both hyperlinks, but the anchor text differs:
	 *
	 * <table class='styled'>
	 * 	<tr>
	 * 		<th><code>withLabels</code></th>
	 * 		<th><code>withoutLabels</code></th>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>
	 * 			<table class='unstyled'>
	 * 				<tr><th>key</th><th>value</th></tr>
	 * 				<tr><td>f1</td><td><a href='http://www.apache.org?label=Apache%20Foundation'>Apache Foundation</a></td></tr>
	 * 			</table>
	 * 		</td>
	 * 		<td>
	 * 			<table class='unstyled'>
	 * 				<tr><th>key</th><th>value</th></tr>
	 * 				<tr><td>f1</td><td><a href='http://www.apache.org?label=Apache%20Foundation'>http://www.apache.org?label=Apache%20Foundation</a></td></tr>
	 * 			</table>
	 * 		</td>
	 * 	</tr>
	 * </table>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"true"</js> (default)
	 * 			<li><js>"false"</js>
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"HtmlSerializer.detectLabelParameters.b"</js>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlSerializer#HTML_detectLabelParameters}
	 * </ul>
	 */
	String detectLabelParameters() default "";

	/**
	 * Configuration property:  Anchor text source.
	 *
	 * <p>
	 * When creating anchor tags (e.g. <code><xt>&lt;a</xt> <xa>href</xa>=<xs>'...'</xs>
	 * <xt>&gt;</xt>text<xt>&lt;/a&gt;</xt></code>) in HTML, this setting defines what to set the inner text to.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Possible values:
	 * 		<ul>
	 * 			<li><js>"TO_STRING"</js> - Set to whatever is returned by {@link #toString()} on the object.
	 * 			<li><js>"PROPERTY_NAME"</js> - Set to the bean property name.
	 * 			<li><js>"URI"</js> - Set to the URI value.
	 * 			<li><js>"LAST_TOKEN"</js> - Set to the last token of the URI value.
	 * 			<li><js>"URI_ANCHOR"</js> - Set to the anchor of the URL.
	 * 			<li><js>"CONTEXT_RELATIVE"</js> - Same as <js>"TO_STRING"</js> but assumes it's a context-relative path.
	 * 			<li><js>"SERVLET_RELATIVE"</js> - Same as <js>"TO_STRING"</js> but assumes it's a servlet-relative path.
	 * 			<li><js>"PATH_RELATIVE"</js> - Same as <js>"TO_STRING"</js> but assumes it's a path-relative path.
	 * 		</ul>
	 * 	<li>
	 * 		Supports {@doc DefaultSvlVariables} (e.g. <js>"$C{myConfigVar}"</js>).
	 * 	<li>
	 * 		A default global value can be set via the system property <js>"HtmlSerializer.uriAnchorText.s"</js>.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link HtmlSerializer#HTML_uriAnchorText}
	 * </ul>
	 */
	String uriAnchorText() default "";
}
