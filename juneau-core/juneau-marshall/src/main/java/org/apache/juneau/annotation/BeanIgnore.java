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
package org.apache.juneau.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.commons.reflect.Visibility;

/**
 * Ignore classes, fields, and methods from being interpreted as bean or bean components.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Classes - Forces bean-like classes to be treated as non-beans.
 * 	<li>Methods - Forces getters/setters to be ignored.
 * 	<li>Fields - Forces bean fields to be ignored.
 * 	<li>
 * 		Fields — Use {@link #ignoreAccessors()} to also exclude matching JavaBean accessors from bean metadata (see
 * 		{@link #ignoreAccessors()}).
 * 	<li><ja>@Rest</ja>-annotated classes and <ja>@RestOp</ja>-annotated methods when used with {@link BeanIgnoreApply @BeanIgnoreApply}.
 * </ul>
 *
 * <h5 class='section'>Java Records:</h5>
 * <p>
 * Ignoring individual record components is not supported during parsing.
 * Because records are immutable, all components must be provided to the canonical constructor.
 * Applying this annotation to a record component's accessor method or field will exclude it from serialization
 * output, but the parser will be unable to instantiate the record if the component value is missing from the input.
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
	 * When <jk>true</jk> and this annotation is on a <jk>field</jk>, JavaBean accessors (<c>getX</c>/<c>setX</c>,
	 * <c>isX</c>) for the same logical property are also excluded from bean metadata.
	 *
	 * <p>
	 * Default is <jk>false</jk>: {@code @BeanIgnore} on a field only excludes the field from field-based discovery;
	 * accessors can still expose the property (for example when {@link org.apache.juneau.BeanContext.Builder#beanFieldVisibility(Visibility) beanFieldVisibility} is {@link Visibility#NONE NONE}). Set to <jk>true</jk> to
	 * omit the property from serialization and parsing while keeping accessors for other frameworks.
	 *
	 * @return The annotation value.
	 */
	boolean ignoreAccessors() default false;

}