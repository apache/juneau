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
package org.apache.juneau.marshall.toml;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.marshall.*;

/**
 * Annotation for specifying config properties defined in {@link TomlSerializer} and {@link TomlParser}.
 *
 * <p>
 * Used primarily for specifying bean configuration properties on REST classes and methods.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@TomlConfig</ja>(sortKeys=<js>"true"</js>, inlineTableThreshold=<js>"5"</js>, nullValue=<js>"~"</js>)
 * 	<jk>public class</jk> MyRestClass {}
 * </p>
 */
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
@Inherited
@ContextApply({ TomlConfigAnnotation.SerializerApply.class, TomlConfigAnnotation.ParserApply.class })
public @interface TomlConfig {

	/**
	 * String written for null values; the parser treats values matching this string as null.
	 *
	 * <p>
	 * An empty value (the default) leaves the builder default of <js>"&lt;NULL&gt;"</js> in place.
	 * Applied to both {@link TomlSerializer} and {@link TomlParser}.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link TomlSerializer.Builder#nullValue(String)}
	 * 	<li class='jm'>{@link TomlParser.Builder#nullValue(String)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String nullValue() default "";

	/**
	 * Maximum number of properties for a table to be written in inline-table format.
	 *
	 * <p>
	 * An empty value (the default) leaves the builder default in place.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link TomlSerializer.Builder#inlineTableThreshold(int)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String inlineTableThreshold() default "";

	/**
	 * Use inline tables when possible.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js>
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link TomlSerializer.Builder#useInlineTables(boolean)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String useInlineTables() default "";

	/**
	 * Sort keys alphabetically in output.
	 *
	 * <ul class='values'>
	 * 	<li><js>"true"</js>
	 * 	<li><js>"false"</js>
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="https://juneau.apache.org/docs/topics/DefaultVarResolver">VarResolver.DEFAULT</a> (e.g. <js>"$C{myConfigVar}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link TomlSerializer.Builder#sortKeys(boolean)}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String sortKeys() default "";

	/**
	 * Optional rank for this config.
	 *
	 * <p>
	 * Can be used to override default ordering and application of config annotations.
	 *
	 * @return The annotation value.
	 */
	int rank() default 0;
}
