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
 * Used in conjunction with the {@link HtmlSerializer} class to define hyperlinks.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Classes.
 * 	<li><ja>@Rest</ja>-annotated classes and <ja>@RestOp</ja>-annotated methods when an {@link #on()} value is specified.
 * </ul>
 *
 * <p>
 * Annotation that can be used to specify that a class has a URL associated with it.
 *
 * <p>
 * When rendered using the {@link org.apache.juneau.html.HtmlSerializer HtmlSerializer} class, this class will get
 * rendered as a hyperlink like so...
 * <p class='code'>
 * 	<xt>&lt;a</xt> <xa>href</xa>=<xs>'hrefProperty'</xs><xt>&gt;</xt>nameProperty<xt>&lt;/a&gt;</xt>
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.HtmlDetails">HTML Details</a>
 * </ul>
 */
@Documented
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@Repeatable(HtmlLinkAnnotation.Array.class)
@ContextApply(HtmlLinkAnnotation.Apply.class)
public @interface HtmlLink {

	/**
	 * The bean property whose value becomes the name in the hyperlink.
	 *
	 * @return The annotation value.
	 */
	String nameProperty() default "name";

	/**
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <p>
	 * Used in conjunction with {@link org.apache.juneau.BeanContext.Builder#applyAnnotations(Class...)} to dynamically apply an annotation to an existing class.
	 * It is ignored when the annotation is applied directly to classes.
	 *
	 * <h5 class='section'>Valid patterns:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Classes:
	 * 		<ul>
	 * 			<li>Fully qualified: <js>"com.foo.MyClass"</js>
	 * 			<li>Fully qualified inner class: <js>"com.foo.MyClass$Inner1$Inner2"</js>
	 * 			<li>Simple: <js>"MyClass"</js>
	 * 			<li>Simple inner: <js>"MyClass$Inner1$Inner2"</js> or <js>"Inner1$Inner2"</js> or <js>"Inner2"</js>
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
	 * The bean property whose value becomes the url in the hyperlink.
	 *
	 * @return The annotation value.
	 */
	String uriProperty() default "uri";
}
