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
package org.apache.juneau.ini.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.ini.*;

/**
 * Annotation for specifying config properties defined in {@link IniSerializer} and {@link IniParser}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 */
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
@Inherited
@ContextApply({ IniConfigAnnotation.SerializerApply.class, IniConfigAnnotation.ParserApply.class })
public @interface IniConfig {

	/**
	 * Key-value separator character.
	 *
	 * <ul class='values'>
	 * 	<li><js>"="</js> (default)
	 * 	<li><js>":"</js>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String kvSeparator() default "";

	/**
	 * Whether to add spaces around the separator.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js> (default) — <c>key = value</c>
	 * 	<li><js>"false"</js> — <c>key=value</c>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String spacedSeparator() default "";

	/**
	 * Whether to emit property descriptions as <c>#</c> comments.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js> (default)
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String useComments() default "";

	/**
	 * Optional rank for this config.
	 *
	 * @return The annotation value.
	 */
	int rank() default 0;
}
