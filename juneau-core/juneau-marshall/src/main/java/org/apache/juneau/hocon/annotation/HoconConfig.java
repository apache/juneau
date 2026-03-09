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
package org.apache.juneau.hocon.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.hocon.*;

/**
 * Annotation for specifying config properties defined in {@link HoconSerializer} and {@link HoconParser}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://github.com/lightbend/config/blob/main/HOCON.md">HOCON Specification</a>
 * </ul>
 */
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
@Inherited
@ContextApply({ HoconConfigAnnotation.SerializerApply.class, HoconConfigAnnotation.ParserApply.class })
public @interface HoconConfig {

	/**
	 * Use = for key-value separator.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js> (default)
	 * 	<li><js>"false"</js>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String useEqualsSign() default "";

	/**
	 * Omit quotes for simple string values.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js> (default)
	 * 	<li><js>"false"</js>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String useUnquotedStrings() default "";

	/**
	 * Omit quotes for simple keys.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js> (default)
	 * 	<li><js>"false"</js>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String useUnquotedKeys() default "";

	/**
	 * Omit root object braces.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js> (default)
	 * 	<li><js>"false"</js>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String omitRootBraces() default "";

	/**
	 * Use newlines instead of commas between members.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js> (default)
	 * 	<li><js>"false"</js>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String useNewlineSeparators() default "";

	/**
	 * Resolve ${var} substitutions during parsing.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js> (default)
	 * 	<li><js>"false"</js>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String resolveSubstitutions() default "";

	/**
	 * Optional rank for this config.
	 *
	 * @return The annotation value.
	 */
	int rank() default 0;
}
