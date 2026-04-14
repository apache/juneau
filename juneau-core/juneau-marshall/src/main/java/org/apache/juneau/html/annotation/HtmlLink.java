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
 * Used in conjunction with the {@link HtmlSerializer} class to define hyperlinks.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Classes.
 * 	<li><ja>@Rest</ja>-annotated classes and <ja>@RestOp</ja>-annotated methods when used with {@link HtmlLinkApply @HtmlLinkApply}.
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
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HtmlBasics">HTML Basics</a>

 * </ul>
 */
@Documented
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
@Inherited
@Repeatable(HtmlLinkAnnotation.Array.class)
public @interface HtmlLink {

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	String[] description() default {};

	/**
	 * The bean property whose value becomes the name in the hyperlink.
	 *
	 * @return The annotation value.
	 */
	String nameProperty() default "name";

	/**
	 * The bean property whose value becomes the url in the hyperlink.
	 *
	 * @return The annotation value.
	 */
	String uriProperty() default "uri";
}