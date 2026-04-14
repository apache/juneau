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
package org.apache.juneau.markdown.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Annotation for customizing Markdown serialization and parsing behavior on classes, methods, and fields.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Marshalled classes/methods/fields.
 * </ul>
 *
 */
@Documented
@Target({ TYPE, FIELD, METHOD })
@Retention(RUNTIME)
@Inherited
public @interface Markdown {

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 */
	String[] description() default {};

	/**
	 * Rendering format override.
	 *
	 * @return The annotation value.
	 */
	MarkdownFormat format() default MarkdownFormat.DEFAULT;

	/**
	 * Custom heading text for document-mode rendering.
	 *
	 * <p>
	 * Overrides the property name used as the heading in {@link org.apache.juneau.markdown.MarkdownDocSerializer} output.
	 *
	 * @return The annotation value.
	 */
	String heading() default "";

	/**
	 * Suppress table rendering; force list rendering for collections.
	 *
	 * <p>
	 * Mirrors {@link org.apache.juneau.html.annotation.Html#noTables()}.
	 *
	 * @return The annotation value.
	 */
	boolean noTables() default false;

	/**
	 * Suppress table header row rendering.
	 *
	 * @return The annotation value.
	 */
	boolean noHeaders() default false;

	/**
	 * Render property value as inline backtick-quoted code.
	 *
	 * @return The annotation value.
	 */
	boolean code() default false;

	/**
	 * Render property value as a Markdown link using the specified URL template.
	 *
	 * <p>
	 * The template may use <js>"{value}"</js> as a placeholder for the actual property value.
	 *
	 * @return The annotation value.
	 */
	String link() default "";
}
