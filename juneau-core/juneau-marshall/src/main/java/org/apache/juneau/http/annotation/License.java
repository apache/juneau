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
 * Swagger license annotation.
 *
 * <p>
 * License information for the exposed API.
 *
 * <p>
 * Used to populate the auto-generated Swagger documentation and UI for server-side <ja>@Rest</ja>-annotated classes.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Swagger</ja>(
 * 		license=<ja>@License</ja>(
 * 			name=<js>"Apache 2.0"</js>,
 * 			url=<js>"http://www.apache.org/licenses/LICENSE-2.0.html"</js>
 * 		)
 * 	)
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Swagger</a>
 * 	<li class='extlink'><a class="doclink" href="https://swagger.io/specification/v2#licenseObject">Swagger License Object</a>
 * </ul>
 */
@Documented
@Retention(RUNTIME)
public @interface License {

	/**
	 * <mk>name</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#licenseObject">Swagger License Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a plain-text string.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>) for the swagger generator.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String name() default "";

	/**
	 * <mk>url</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#licenseObject">Swagger License Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a plain-text string.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>) for the swagger generator.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String url() default "";
}
