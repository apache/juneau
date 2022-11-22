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
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.HtmlDetails">HTML Details</a>
 * </ul>
 */
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@ContextApply({HtmlConfigAnnotation.SerializerApply.class,HtmlConfigAnnotation.ParserApply.class})
public @interface HtmlConfig {

	/**
	 * Optional rank for this config.
	 *
	 * <p>
	 * Can be used to override default ordering and application of config annotations.
	 *
	 * @return The annotation value.
	 */
	int rank() default 0;

	//-------------------------------------------------------------------------------------------------------------------
	// HtmlCommon
	//-------------------------------------------------------------------------------------------------------------------

	//-------------------------------------------------------------------------------------------------------------------
	// HtmlSerializer
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 *
	 * <p>
	 * When present, this value overrides the {@link org.apache.juneau.serializer.Serializer.Builder#addBeanTypes()} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerSet}.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.html.HtmlSerializer.Builder#addBeanTypesHtml()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String addBeanTypes() default "";

	/**
	 * Add key/value headers on bean/map tables.
	 *
	 * <p>
	 * When enabled, <bc>key</bc> and <bc>value</bc> column headers are added to tables.
	 *
	 * <p>
	 * The following shows the difference between the two generated outputs:
	 *
	 * <table class='styled'>
	 * 	<tr>
	 * 		<th><c>withoutHeaders</c></th>
	 * 		<th><c>withHeaders</c></th>
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
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.html.HtmlSerializer.Builder#addKeyValueTableHeaders()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String addKeyValueTableHeaders() default "";

	/**
	 * Don't look for URLs in {@link String Strings}.
	 *
	 * <p>
	 * Disables the feature where if a string looks like a URL (i.e. starts with <js>"http://"</js> or <js>"https://"</js>, then treat it like a URL
	 * and make it into a hyperlink based on the rules specified by {@link org.apache.juneau.html.HtmlSerializer.Builder#uriAnchorText(AnchorText)}.
	 *
	 * <p>
	 * The following shows the difference between the two generated outputs:
	 *
	 * <table class='styled'>
	 * 	<tr>
	 * 		<th><c>withLinks</c></th>
	 * 		<th><c>withoutLinks</c></th>
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
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.html.HtmlSerializer.Builder#disableDetectLinksInStrings()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String disableDetectLinksInStrings() default "";

	/**
	 * Link label parameter name.
	 *
	 * <p>
	 * The parameter name to look for when resolving link labels.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Default value: <js>"label"</js>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.html.HtmlSerializer.Builder#labelParameter(String)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String labelParameter() default "";

	/**
	 * Don't look for link labels in URIs.
	 *
	 * <p>
	 * Disables the feature where if the URL has a label parameter (e.g. <js>"?label=foobar"</js>), then use that as the anchor text of the link.
	 *
	 * <p>
	 * The parameter name can be changed via the {@link org.apache.juneau.html.HtmlSerializer.Builder#labelParameter(String)} property.
	 *
	 * <p>
	 * The following shows the difference between the two generated outputs.
	 * <br>Note that they're both hyperlinks, but the anchor text differs:
	 *
	 * <table class='styled'>
	 * 	<tr>
	 * 		<th><c>withLabels</c></th>
	 * 		<th><c>withoutLabels</c></th>
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
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.html.HtmlSerializer.Builder#disableDetectLabelParameters()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String disableDetectLabelParameters() default "";

	/**
	 * Anchor text source.
	 *
	 * <p>
	 * When creating anchor tags (e.g. <code><xt>&lt;a</xt> <xa>href</xa>=<xs>'...'</xs>
	 * <xt>&gt;</xt>text<xt>&lt;/a&gt;</xt></code>) in HTML, this setting defines what to set the inner text to.
	 *
	 * <ul class='values'>
	 * 	<li><js>"TO_STRING"</js> - Set to whatever is returned by {@link #toString()} on the object.
	 * 	<li><js>"PROPERTY_NAME"</js> - Set to the bean property name.
	 * 	<li><js>"URI"</js> - Set to the URI value.
	 * 	<li><js>"LAST_TOKEN"</js> - Set to the last token of the URI value.
	 * 	<li><js>"URI_ANCHOR"</js> - Set to the anchor of the URL.
	 * 	<li><js>"CONTEXT_RELATIVE"</js> - Same as <js>"TO_STRING"</js> but assumes it's a context-relative path.
	 * 	<li><js>"SERVLET_RELATIVE"</js> - Same as <js>"TO_STRING"</js> but assumes it's a servlet-relative path.
	 * 	<li><js>"PATH_RELATIVE"</js> - Same as <js>"TO_STRING"</js> but assumes it's a path-relative path.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jm.DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.html.HtmlSerializer.Builder#uriAnchorText(AnchorText)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String uriAnchorText() default "";
}
