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
package org.apache.juneau.rest.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.rest.*;

/**
 * @deprecated Use {@link org.apache.juneau.http.annotation.Response}
 */
@Deprecated
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface Response {

	/**
	 * Optional description.
	 *
	 * <p>
	 * The default value pulls the description from the <code>description</code> entry in the servlet resource bundle.
	 * (e.g. <js>"myMethod.res.[code].description = foo"</js> or
	 * <js>"MyServlet.myMethod.res.[code].description = foo"</js>).
	 *
	 * <p>
	 * This field can contain variables (e.g. "$L{my.localized.variable}").
	 * <br>See {@link RestContext#getVarResolver()} for the list of supported variables.
	 *
	 * <p>
	 * Corresponds to the swagger field <code>/paths/{path}/{method}/responses/{code}/description</code>.
	 */
	String description() default "";

	/**
	 * Optional response headers.
	 *
	 * <p>
	 * Response variables can also be defined in the servlet resource bundle.
	 * (e.g. <js>"myMethod.res.[code].[category].[name] = foo"</js> or
	 * <js>"MyServlet.myMethod.res.[code].[category].[name] = foo"</js>).
	 */
	Parameter[] headers() default {};

	/**
	 * A definition of the response structure.
	 *
	 * <p>
	 * It can be a primitive, an array or an object.
	 * If this field does not exist, it means no content is returned as part of the response.
	 * As an extension to the <a class="doclink" href="http://swagger.io/specification/#schemaObject">Schema Object</a>,
	 * its root type value may also be <js>"file"</js>.
	 * This SHOULD be accompanied by a relevant produces mime-type.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		name=<js>"*"</js>,
	 * 		swagger=@MethodSwagger(
	 * 			responses={
	 * 				<ja>@Response</ja>(value=200,schema=<js>"{type:'string',description:'A serialized Person bean.'}"</js>),
	 * 			}
	 * 		)
	 * </p>
	 */
	String schema() default "";

	/**
	 * HTTP response code.
	 */
	int value();
}
