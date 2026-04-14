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

/**
 * Used to identify a class or bean property as a URI.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Classes that should be treated as URIs when serialized.
 * 	<li>Methods/fields whose values should be treated as URIs when serialized.
 * 	<li><ja>@Rest</ja>-annotated classes and <ja>@RestOp</ja>-annotated methods when used with {@link UriApply @UriApply}.
 * </ul>
 *
 * <p>
 * This annotation allows you to identify other classes that return URIs via <c>toString()</c> as URI objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MarshallingUris">URIs</a>

 * </ul>
 */
@Documented
@Target({ TYPE, FIELD, METHOD })
@Retention(RUNTIME)
@Inherited
@Repeatable(UriAnnotation.Array.class)
public @interface Uri {

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	String[] description() default {};

}