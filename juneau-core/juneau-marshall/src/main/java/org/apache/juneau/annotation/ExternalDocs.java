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
package org.apache.juneau.annotation;

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Swagger external documentation annotation.
 *
 * <p>
 * Allows referencing an external resource for extended documentation.
 *
 * <p>
 * Used to populate the auto-generated Swagger documentation and UI for server-side <ja>@Rest</ja>-annotated classes.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Swagger</ja>(
 * 		externalDocs=<ja>@ExternalDocs</ja>(
 * 			description=<js>"Apache Juneau"</js>,
 * 			url=<js>"http://juneau.apache.org"</js>
 * 		)
 * 	)
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jrs.Swagger}
 * 	<li class='extlink'>{@doc ext.SwaggerExternalDocumentationObject}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Documented
@Retention(RUNTIME)
public @interface ExternalDocs {

	/**
	 * <mk>description</mk> field of the {@doc ext.SwaggerExternalDocumentationObject}.
	 *
	 * <p>
	 * A short description of the target documentation.
	 *
	 * <ul class='notes'>
	 * 	<li class='note'>
	 * 		The format is a plain-text string.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li class='note'>
	 * 		Supports {@doc jrs.SvlVariables} (e.g. <js>"$L{my.localized.variable}"</js>) for the swagger generator.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] description() default {};

	/**
	 * <mk>url</mk> field of the {@doc ext.SwaggerExternalDocumentationObject}.
	 *
	 * <p>
	 * The URL for the target documentation. Value MUST be in the format of a URL.
	 *
	 * <ul class='notes'>
	 * 	<li class='note'>
	 * 		The value is required.
	 * 	<li class='note'>
	 * 		The format is a URL string.
	 * 	<li class='note'>
	 * 		Supports {@doc jrs.SvlVariables} (e.g. <js>"$L{my.localized.variable}"</js>) for the swagger generator.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String url() default "";
}
