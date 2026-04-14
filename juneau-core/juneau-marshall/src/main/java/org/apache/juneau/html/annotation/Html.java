/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.html.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.html.*;

/**
 * Annotation that can be applied to classes, fields, and methods to tweak how they are handled by {@link HtmlSerializer}.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Marshalled classes/methods/fields.
 * 	<li><ja>@Rest</ja>-annotated classes and <ja>@RestOp</ja>-annotated methods when used with {@link HtmlApply @HtmlApply}.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HtmlAnnotation">@Html Annotation</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HtmlBasics">HTML Basics</a>
 * </ul>
 */
@Documented
@Target({ TYPE, FIELD, METHOD })
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
	 * <p class='bjava'>
	 * 	<jc>// Produces &lt;a href&#61;'...'&gt;drive&lt;/a&gt; when serialized to HTML.</jc>
	 * 	<ja>@Html</ja>(anchorText=<js>"drive"</js>)
	 * 	<ja>@URI</ja> <jc>// Treat property as a URL</jc>
	 * 	<jk>public</jk> String getDrive() {...}
	 * </p>
	 *
	 * <p>
	 * This overrides the behavior specified by {@link org.apache.juneau.html.HtmlSerializer.Builder#uriAnchorText(AnchorText)}.
	 *
	 * @return The annotation value.
	 */
	String anchorText() default "";

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	String[] description() default {};

	/**
	 * Specifies what format to use for the HTML element.
	 *
	 * @return The annotation value.
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
	 * <p class='bjava'>
	 * 	<jk>public class</jk> FileSpace {
	 *
	 * 		<ja>@Html</ja>(link=<js>"servlet:/drive/{drive}"</js>)
	 * 		<jk>public</jk> String getDrive() {
	 * 			...;
	 * 		}
	 * 	}
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String link() default "";

	/**
	 * When <jk>true</jk>, don't add headers to tables.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.html.HtmlSerializer.Builder#addKeyValueTableHeaders()}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	boolean noTableHeaders() default false;

	/**
	 * When <jk>true</jk>, collections of beans should be rendered as trees instead of tables.
	 *
	 * <p>
	 * Default is <jk>false</jk>.
	 *
	 * @return The annotation value.
	 */
	boolean noTables() default false;

	/**
	 * Associates an {@link HtmlRender} with a bean property for custom HTML rendering of the property.
	 *
	 * <p>
	 * This annotation applies to bean properties and classes.
	 *
	 * @return The annotation value.
	 */
	@SuppressWarnings({
		"rawtypes" // Raw types necessary for HtmlRender class parameter
	})
	Class<? extends HtmlRender> render() default HtmlRender.class;

	/**
	 * Specifies the CSS style to apply to the HTML element containing the bean property value.
	 *
	 * <p>
	 * This is a simpler alternative to using {@link #render()} when you only need to apply CSS styling
	 * without custom content transformation.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Apply styling to a bean property</jc>
	 * 	<ja>@Html</ja>(style=<js>"white-space:normal;min-width:200px"</js>)
	 * 	<jk>public</jk> String getField() {...}
	 * </p>
	 *
	 * <p>
	 * If both {@link #style()} and {@link #render()} are specified, the render takes precedence.
	 *
	 * @return The annotation value.
	 */
	String style() default "";
}