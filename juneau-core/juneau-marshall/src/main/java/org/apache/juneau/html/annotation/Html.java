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

import org.apache.juneau.html.*;

/**
 * Annotation that can be applied to classes, fields, and methods to tweak how they are handled by {@link HtmlSerializer}.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-marshall.HtmlDetails.HtmlAnnotation}
 * </ul>
 */
@Documented
@Target({TYPE,FIELD,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface Html {

	/**
	 * Use the specified anchor text when serializing a URI.
	 *
	 * <p>
	 * The text can contain any bean property values resolved through variables of the form <js>"{property-name}"</js>.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Produces &lt;a href&#61;'...'&gt;drive&lt;/a&gt; when serialized to HTML.</jc>
	 * 	<ja>@Html</ja>(anchorText=<js>"drive"</js>)
	 * 	<ja>@URI</ja> <jc>// Treat property as a URL</jc>
	 * 	<jk>public</jk> String getDrive() {...}
	 * </p>
	 *
	 * This overrides the behavior specified by {@link HtmlSerializer#HTML_uriAnchorText}.
	 */
	String anchorText() default "";

	/**
	 * Specifies what format to use for the HTML element.
	 */
	HtmlFormat format() default HtmlFormat.HTML;

	/**
	 * Adds a hyperlink to a bean property when rendered as HTML.
	 *
	 * <p>
	 * The text can contain any bean property values resolved through variables of the form <js>"{property-name}"</js>.
	 *
	 * <p>
	 * The URLs can be any of the following forms:
	 * <ul>
	 * 	<li>Absolute - e.g. <js>"http://host:123/myContext/myServlet/myPath"</js>
	 * 	<li>Context-root-relative - e.g. <js>"/myContext/myServlet/myPath"</js>
	 * 	<li>Context-relative - e.g. <js>"context:/myServlet/myPath"</js>
	 * 	<li>Servlet-relative - e.g. <js>"servlet:/myPath"</js>
	 * 	<li>Path-info-relative - e.g. <js>"myPath"</js>
	 * </ul>
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jk>public class</jk> FileSpace {
	 *
	 * 		<ja>@Html</ja>(link=<js>"servlet:/drive/{drive}"</js>)
	 * 		<jk>public</jk> String getDrive() {
	 * 			...;
	 * 		}
	 * 	}
	 * </p>
	 */
	String link() default "";

	/**
	 * When <jk>true</jk>, don't add headers to tables.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link HtmlSerializer#HTML_addKeyValueTableHeaders}
	 * </ul>
	 */
	boolean noTableHeaders() default false;

	/**
	 * When <jk>true</jk>, collections of beans should be rendered as trees instead of tables.
	 *
	 * <p>
	 * Default is <jk>false</jk>.
	 */
	boolean noTables() default false;

	/**
	 * Associates an {@link HtmlRender} with a bean property for custom HTML rendering of the property.
	 *
	 * <p>
	 * This annotation applies to bean properties and classes.
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends HtmlRender> render() default HtmlRender.class;
}
