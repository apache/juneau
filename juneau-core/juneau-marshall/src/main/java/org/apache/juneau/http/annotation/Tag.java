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
package org.apache.juneau.http.annotation;

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Swagger tag annotation.
 * {@review}
 *
 * <p>
 * Allows adding meta data to a single tag that is used by the {@doc ext.SwaggerOperationObject}.
 * It is not mandatory to have a Tag Object per tag used there.
 *
 * <p>
 * Used to populate the auto-generated Swagger documentation and UI for server-side <ja>@Rest</ja>-annotated classes.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// A response object thats a hex-encoded string</jc>
 * 	<ja>@Rest</ja>(
 * 		swagger=<ja>@Swagger</ja>{
 * 			tags={
 * 				<ja>@Tag</ja>(
 * 					name=<js>"utility"</js>,
 * 					description=<js>"Utility methods"</js>
 * 				)
 * 			}
 * 		}
 * 	)
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jrs.Swagger}
 * 	<li class='extlink'>{@doc ext.SwaggerTagObject}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Documented
@Retention(RUNTIME)
public @interface Tag {

	/**
	 * <mk>description</mk> field of the {@doc ext.SwaggerTagObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc jd.Swagger} object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc jrs.SvlVariables} (e.g. <js>"$L{my.localized.variable}"</js>) for the swagger generator.
	 * </ul>
	 */
	String[] description() default {};

	/**
	 * <mk>externalDocs</mk> field of the {@doc ext.SwaggerTagObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc jd.Swagger} object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * </ul>
	 */
	ExternalDocs externalDocs() default @ExternalDocs;

	/**
	 * <mk>name</mk> field of the {@doc ext.SwaggerTagObject}.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is plain text.
	 * </ul>
	 */
	String name() default "";
}
