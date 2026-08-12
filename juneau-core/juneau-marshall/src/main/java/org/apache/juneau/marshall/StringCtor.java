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
package org.apache.juneau.marshall;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Identifies the exact static factory method to use for creating an instance of a POJO from a string.
 *
 * <p>
 * By default, {@link ClassMeta#newInstanceFromString(Object,String)} looks for a static method matching one of a
 * fixed set of conventional names (<c>fromString</c>, <c>fromValue</c>, <c>valueOf</c>, <c>parse</c>,
 * <c>parseString</c>, <c>forName</c>, <c>forString</c>) that takes a single {@link String} argument and returns an
 * instance of the class, falling back to a single-{@link String}-arg constructor if none is found.
 *
 * <p>
 * This annotation lets you opt out of that name-based guessing and explicitly name the static factory method to use,
 * which is useful for classes whose factory method doesn't match any of the conventional names (e.g. <c>of</c> or
 * <c>create</c>).
 *
 * <p>
 * The annotated method must be <jk>static</jk>, return an instance of the class (or a subtype), and take exactly one
 * {@link String} argument.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@StringCtor</ja>(<js>"of"</js>)
 * 	<jk>public class</jk> MyClass {
 *
 * 		<jk>public static</jk> MyClass of(String <jv>value</jv>) {
 * 			<jk>return new</jk> MyClass(<jv>value</jv>);
 * 		}
 *
 * 		...
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jm'>{@link ClassMeta#newInstanceFromString(Object,String)}
 * </ul>
 *
 * @since 9.2.0
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Inherited
public @interface StringCtor {

	/**
	 * The name of the static factory method to use for string-convertibility.
	 *
	 * <p>
	 * The named method must be <jk>static</jk>, return an instance of the annotated class, and take exactly one
	 * {@link String} argument.
	 *
	 * @return The annotation value.
	 */
	String value();
}
