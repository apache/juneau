/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.server.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Annotation used in conjunction with {@link RestMethod#responses()} to identify possible responses by the method.
 *
 * <h6 class='topic'>Example</h6>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(
 * 		name=<js>"*"</js>,
 * 		responses={
 * 			<ja>@Response</ja>(value=200,description=<js>"Everything was great."</js>),
 * 			<ja>@Response</ja>(value=404,description=<js>"File was not found."</js>)
 * 			<ja>@Response</ja>(500),
 * 		}
 * 	)
 * 	<jk>public void</jk> doAnything(RestRequest req, RestResponse res, <ja>@Method</ja> String method) {
 * 		...
 * 	}
 * </p>
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface Response {

	/**
	 * HTTP response code.
	 */
	int value();

	/**
	 * Optional description.
	 * <p>
	 * 	The default value pulls the description from the <code>description</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"myMethod.res.[code] = foo"</js> or <js>"MyServlet.myMethod.res.[code] = foo"</js>).
	 * <p>
	 * 	This field can contain variables (e.g. "$L{my.localized.variable}").
	 */
	String description() default "";

	/**
	 * Optional response variables.
	 * <p>
	 * 	Response variables can also be defined in the servlet resource bundle.
	 * 	(e.g. <js>"myMethod.res.[code].[category].[name] = foo"</js> or <js>"MyServlet.myMethod.res.[code].[category].[name] = foo"</js>).
	 */
	Var[] output() default {};
}
