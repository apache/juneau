// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.urlencoding.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.urlencoding.*;

/**
 * Annotation that can be applied to classes, fields, and methods to tweak how they are handled by
 * {@link UrlEncodingSerializer} and {@link UrlEncodingParser}.
 */
@Documented
@Target({TYPE})
@Retention(RUNTIME)
@Inherited
public @interface UrlEncoding {

	/**
	 * When true, bean properties of type array or Collection will be expanded into multiple key/value pairings.
	 * <p>
	 * This annotation is identical in behavior to using the {@link UrlEncodingContext#URLENC_expandedParams}
	 * property, but applies to only instances of this bean.
	 */
	boolean expandedParams() default false;
}
