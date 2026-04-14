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
package org.apache.juneau.hjson.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;

/**
 * Dynamically applies a {@link Hjson @Hjson} annotation to specified classes, methods, or fields.
 *
 * <p>
 * This annotation separates the <b>targeting</b> concern ({@link #on()}/{@link #onClass()}) from the
 * <b>content</b> concern ({@link #value()}), enabling {@link Hjson @Hjson} to be a pure data annotation
 * without marshall-specific application machinery.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
 * </ul>
 */
@Documented
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
@Repeatable(HjsonApply.Array.class)
@ContextApply(HjsonApplyAnnotation.Applier.class)
public @interface HjsonApply {

	/**
	 * The {@link Hjson @Hjson} annotation to apply.
	 *
	 * @return The annotation value.
	 */
	Hjson value();

	/**
	 * Dynamically apply this annotation to the specified classes/methods/fields.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] on() default {};

	/**
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] onClass() default {};

	/**
	 * A collection of {@link HjsonApply @HjsonApply annotations}.
	 */
	@Documented
	@Target({ TYPE, METHOD })
	@Retention(RUNTIME)
	public @interface Array {

		/**
		 * The child annotations.
		 *
		 * @return The annotation value.
		 */
		HjsonApply[] value();
	}
}
