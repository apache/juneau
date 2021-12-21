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
package org.apache.juneau.rest.springboot.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import org.apache.juneau.rest.RestServlet;
import org.springframework.context.annotation.*;

import java.lang.annotation.*;

/**
 * Added to Spring application classes to denote Juneau REST resource classes to deploy as servlets.
 *
 * <p>
 * The annotation can be used in two places:
 * <ul class='spaced-list'>
 * 	<li>
 * 		On the source class of a Spring Boot application.
 * 	<li>
 * 		On {@link Bean}-annotated methods on configuration beans.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server-springboot}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Documented
@Inherited
public @interface JuneauRestRoot {

	/**
	 * Specifies one or more implementations of {@link RestServlet} to deploy as servlets.
	 * <p>
	 * This method is only applicable when used on the source class of a Spring Boot application.
	 */
	Class<? extends RestServlet>[] servlets() default {};
}
