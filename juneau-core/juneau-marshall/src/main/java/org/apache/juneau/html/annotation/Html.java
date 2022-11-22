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

/**
 * Annotation that can be applied to classes, fields, and methods to tweak how they are handled by {@link HtmlSerializer}.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Marshalled classes/methods/fields.
 * 	<li><ja>@Rest</ja>-annotated classes and <ja>@RestOp</ja>-annotated methods when an {@link #on()} value is specified.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.HtmlAnnotation">@Html Annotation</a>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.HtmlDetails">HTML Details</a> * </ul>
 */
@Documented
@Target({TYPE,FIELD,METHOD})
@Retention(RUNTIME)
@Inherited
@Repeatable(HtmlAnnotation.Array.class)
@ContextApply(HtmlAnnotation.Apply.class)
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
	 * Dynamically apply this annotation to the specified classes/methods/fields.
	 *
	 * <p>
	 * Used in conjunction with {@link org.apache.juneau.BeanContext.Builder#applyAnnotations(Class...)} to dynamically apply an annotation to an existing class/method/field.
	 * It is ignored when the annotation is applied directly to classes/methods/fields.
	 *
	 * <h5 class='section'>Valid patterns:</h5>
	 * <ul class='spaced-list'>
	 *  <li>Classes:
	 * 		<ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass"</js>
	 * 				</ul>
	 * 			<li>Fully qualified inner class:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass$Inner1$Inner2"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass"</js>
	 * 				</ul>
	 * 			<li>Simple inner:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2"</js>
	 * 					<li><js>"Inner1$Inner2"</js>
	 * 					<li><js>"Inner2"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>Fully qualified with args:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myMethod(String,int)"</js>
	 * 					<li><js>"com.foo.MyClass.myMethod(java.lang.String,int)"</js>
	 * 					<li><js>"com.foo.MyClass.myMethod()"</js>
	 * 				</ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myMethod"</js>
	 * 				</ul>
	 * 			<li>Simple with args:
	 * 				<ul>
	 * 					<li><js>"MyClass.myMethod(String,int)"</js>
	 * 					<li><js>"MyClass.myMethod(java.lang.String,int)"</js>
	 * 					<li><js>"MyClass.myMethod()"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass.myMethod"</js>
	 * 				</ul>
	 * 			<li>Simple inner class:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2.myMethod"</js>
	 * 					<li><js>"Inner1$Inner2.myMethod"</js>
	 * 					<li><js>"Inner2.myMethod"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>Fields:
	 * 		<ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myField"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass.myField"</js>
	 * 				</ul>
	 * 			<li>Simple inner class:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2.myField"</js>
	 * 					<li><js>"Inner1$Inner2.myField"</js>
	 * 					<li><js>"Inner2.myField"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>A comma-delimited list of anything on this list.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] on() default {};

	/**
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <p>
	 * Identical to {@link #on()} except allows you to specify class objects instead of a strings.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] onClass() default {};

	/**
	 * Associates an {@link HtmlRender} with a bean property for custom HTML rendering of the property.
	 *
	 * <p>
	 * This annotation applies to bean properties and classes.
	 *
	 * @return The annotation value.
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends HtmlRender> render() default HtmlRender.class;
}
