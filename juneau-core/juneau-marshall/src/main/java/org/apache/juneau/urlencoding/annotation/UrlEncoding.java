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
package org.apache.juneau.urlencoding.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.urlencoding.*;

/**
 * Annotation that can be applied to classes, fields, and methods to tweak how they are handled by
 * {@link UrlEncodingSerializer} and {@link UrlEncodingParser}.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Marshalled classes/methods/fields.
 * 	<li><ja>@Rest</ja>-annotated classes and <ja>@RestOp</ja>-annotated methods when used with {@link UrlEncodingApply @UrlEncodingApply}.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/UrlEncodingBasics">URL-Encoding Basics</a>

 * </ul>
 */
@Documented
@Target({ TYPE, FIELD, METHOD })
@Retention(RUNTIME)
@Inherited
@Repeatable(UrlEncodingAnnotation.Array.class)
public @interface UrlEncoding {

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	String[] description() default {};

	/**
	 * When true, bean properties of type array or Collection will be expanded into multiple key/value pairings.
	 *
	 * <p>
	 * This annotation is identical in behavior to using the {@link org.apache.juneau.urlencoding.UrlEncodingSerializer.Builder#expandedParams()}
	 * and {@link org.apache.juneau.urlencoding.UrlEncodingParser.Builder#expandedParams()} properties, but applies to only instances of this bean.
	 *
	 * @return The annotation value.
	 */
	boolean expandedParams() default false;

}