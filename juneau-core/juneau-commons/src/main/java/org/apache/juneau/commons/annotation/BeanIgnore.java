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
package org.apache.juneau.commons.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Marks types in <c>juneau-commons</c> that must not be interpreted as beans (for example HTTP header value types
 * serialized as strings).
 *
 * <p>
 * Marshall's {@code org.apache.juneau.annotation.BeanIgnore} is the full-featured annotation (repeatable, dynamically
 * applied, and so on). This annotation is recognized alongside it when classes are analyzed in juneau-marshall.
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/BeanIgnoreAnnotation">@BeanIgnore Annotation</a>
 * </ul>
 */
@Documented
@Target({ FIELD, METHOD, TYPE, CONSTRUCTOR })
@Retention(RUNTIME)
@Inherited
public @interface BeanIgnore {

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	String[] description() default {};

	/**
	 * When <jk>true</jk> and this annotation is on a <jk>field</jk>, JavaBean accessors for the same logical property
	 * are also excluded from bean metadata.
	 *
	 * @return The annotation value.
	 */
	boolean ignoreAccessors() default false;
}
