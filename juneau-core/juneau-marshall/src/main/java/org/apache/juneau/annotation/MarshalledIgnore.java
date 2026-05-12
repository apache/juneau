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

import org.apache.juneau.commons.bean.*;

/**
 * Skip a type entirely during marshalling.
 *
 * <p>
 * Serializers output {@code null} for instances of the annotated type and parsers return {@code null}
 * when asked to instantiate it. This is a marshalling-layer concern: the wire format renders the
 * value as {@code null} regardless of what (if anything) the bean-modeling layer would do with the
 * type.
 *
 * <p>
 * Bean-modeling decisions are controlled by the sibling annotation
 * {@link BeanIgnore @BeanIgnore} (in <c>juneau-commons</c>), which is responsible for excluding
 * classes from bean detection ("this isn't a bean — fall through to swap/{@code @Marshalled(as=STRING)}/etc.")
 * and for excluding fields, methods, and constructors from bean property/constructor detection.
 *
 * <p>
 * The two annotations are independent and may be combined:
 * <ul>
 * 	<li>{@code @MarshalledIgnore} alone — the class still participates in bean detection (it may be
 * 		treated as a bean by the modeling layer) but the marshaller emits {@code null}.
 * 	<li>{@code @BeanIgnore} alone — the class is not a bean for modeling purposes, but the marshaller
 * 		still tries to render it (typically via {@code toString()} or an installed swap).
 * 	<li>Both — the class is not a bean and the marshaller emits {@code null}.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link BeanIgnore}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MarshalledIgnoreAnnotation">@MarshalledIgnore Annotation</a>
 * </ul>
 */
@Documented
@Target({ TYPE })
@Retention(RUNTIME)
@Inherited
public @interface MarshalledIgnore {

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	String[] description() default {};

}
