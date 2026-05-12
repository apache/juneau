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
package org.apache.juneau.commons.bean;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Excludes a class, field, method, or constructor from bean introspection.
 *
 * <p>
 * The bean-modeling sibling of {@code @MarshalledIgnore}: where {@code @MarshalledIgnore} (in
 * <c>juneau-marshall</c>) controls whether the marshaller skips a type entirely at the wire-format
 * layer, this annotation describes whether something participates in <i>bean detection</i>. It lives
 * in <c>juneau-commons</c> so the bean-modeling layer can describe non-bean types and excluded bean
 * members without depending on the marshalling layer.
 *
 * <p>
 * Behavior depends on the target:
 * <ul>
 * 	<li><b>TYPE (class)</b> — marks the class as <i>not a bean</i>. The marshaller falls through to
 * 		its other type-detection logic (object swaps, {@link org.apache.juneau.commons.bean.BeanType @BeanType}-style
 * 		hints, {@code @Marshalled(as=STRING)}, etc.) instead of treating the class as a bean. This is
 * 		the original pre-TODO-21 {@code @BeanIgnore} semantic.
 * 	<li><b>FIELD</b> — excludes the field from bean property discovery. Use {@link #ignoreAccessors()}
 * 		to also suppress matching JavaBean accessors.
 * 	<li><b>METHOD</b> — excludes the getter/setter from bean property discovery.
 * 	<li><b>CONSTRUCTOR</b> — excludes the constructor from constructor detection.
 * </ul>
 *
 * <h5 class='section'>Java Records:</h5>
 * <p>
 * Ignoring individual record components is not supported during parsing. Because records are
 * immutable, all components must be provided to the canonical constructor. Applying this
 * annotation to a record component's accessor method or field will exclude it from serialization
 * output, but the parser will be unable to instantiate the record if the component value is missing
 * from the input.
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
	 * accessors can still expose the property (for example when bean field visibility is set to {@code NONE}).
	 * Set to <jk>true</jk> to omit the property from serialization and parsing while keeping accessors for other
	 * frameworks.
	 *
	 * @return The annotation value.
	 */
	boolean ignoreAccessors() default false;

}
