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

import org.apache.juneau.swap.*;

/**
 * Associates {@link ObjectSwap} and {@link Surrogate} classes with POJOs and bean properties.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Classes.
 * 	<li>Bean getters/setters/fields.
 * 	<li><ja>@Rest</ja>-annotated classes and <ja>@RestOp</ja>-annotated methods when used with {@link SwapApply @SwapApply}.
 * </ul>

 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SwapAnnotation">@Swap Annotation</a>

 * </ul>
 */
@Documented
@Target({ TYPE, ANNOTATION_TYPE, FIELD, METHOD })
@Retention(RUNTIME)
@Inherited
@Repeatable(SwapAnnotation.Array.class)
public @interface Swap {

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	String[] description() default {};

	/**
	 * The {@link ObjectSwap} and {@link Surrogate} class.
	 *
	 * <p>
	 * A synonym for {@link #value()}.
	 *
	 * @return The annotation value.
	 */
	Class<?> impl() default void.class;

	/**
	 * Identifies the media types that this swap is applicable for.
	 *
	 * <p>
	 * In the following example, the swap is only invoked by the JSON serializer:
	 *
	 * <p class='bjava'>
	 * 	<ja>@Swap</ja>(impl=ToStringSwap.<jk>class</jk>, mediaTypes=<js>"&#42;/json"</js>)
	 * 	<jk>public class</jk> MyBean { ... }
	 *
	 * 	<jk>public class</jk> ToStringSwap <jk>extends</jk> ObjectSwap&lt;Object,String&gt; {
	 * 			<jk>public</jk> String swap(BeanSession <jv>session</jv>, Object <jv>value</jv>) <jk>throws</jk> Exception {
	 * 				<jk>return</jk> <jv>value</jv>.toString();
	 * 			}
	 * 		}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/PerMediaTypeSwaps">Per-media-type Swaps</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] mediaTypes() default {};

	/**
	 * Identifies a template string along with this swap.
	 *
	 * <p>
	 * Template strings are arbitrary strings associated with swaps that help provide additional context information
	 * for the swap class.
	 * They're called 'templates' because their primary purpose is for providing template names, such as Apache FreeMarker
	 * template names.
	 *
	 * <p>
	 * The following is an example of a templated swap class used to serialize POJOs to HTML using FreeMarker:
	 *
	 * <p class='bjava'>
	 * 	<jc>// Our templated swap class.</jc>
	 * 	<jk>public class</jk> FreeMarkerSwap <jk>extends</jk> ObjectSwap&lt;Object,Reader&gt; {
	 *
	 * 		<jk>public</jk> MediaType[] forMediaTypes() {
	 * 			<jk>return</jk> MediaType.<jsm>forStrings</jsm>(<js>"&#42;/html"</js>);
	 * 		}
	 *
	 * 		<jk>public</jk> Reader swap(BeanSession <jv>session</jv>, Object <jv>value</jv>, String <jv>template</jv>) <jk>throws</jk> Exception {
	 * 			<jk>return</jk> <jsm>getFreeMarkerReader</jsm>(<jv>template</jv>, <jv>value</jv>);  <jc>// Some method that creates raw HTML.</jc>
	 * 		}
	 * 	}
	 * </p>
	 * <p class='bjava'>
	 * 	<ja>@Swap</ja>(impl=FreeMarkerSwap.<jk>class</jk>, template=<js>"MyPojo.div.ftl"</js>)
	 * 	<jk>public class</jk> MyPojo {}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/TemplatedSwaps">Templated Swaps</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String template() default "";

	/**
	 * The {@link ObjectSwap} and {@link Surrogate} class.
	 *
	 * <p>
	 * A synonym for {@link #impl()}.
	 *
	 * @return The annotation value.
	 */
	Class<?> value() default void.class;
}