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

/**
 * Extended annotation for {@link RestMethod#swagger() RestMethod.swagger()}.
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.OptionsPages">Overview &gt; juneau-rest-server &gt; OPTIONS Pages</a>
 * </ul>
 */
public @interface MethodSwagger {
	

	/**
	 * Swagger JSON.
	 * 
	 * <p>
	 * Used for free-form Swagger documentation of a REST resource.
	 * 
	 * 
		swagger={
			"tags:[ 'store' ],",
			"responses:{",
				"200:{ 'x-example':{AVAILABLE:123} }",
			"},",
			"security:[ { api_key:[] } ]"
		}
	)
		swagger={
			"tags:[ 'store' ],",
			"responses:{",
				"200:{ 'x-example':{AVAILABLE:123} }",
			"},",
			"security:[ { api_key:[] } ]"
		}
			swagger= {
				"parameters:[",
					"{name:'a',in:'path',type:'string',description:'Test1.d'},",
					"{name:'b',in:'query',type:'string',description:'Test1.e'},",
					"{in:'body',type:'string',description:'Test1.f'},",
					"{name:'D',in:'header',type:'string',description:'Test1.g'},",
					"{name:'a2',in:'path',type:'string',description:'Test1.h'},",
					"{name:'b2',in:'query',type:'string',description:'Test1.i'},",
					"{name:'D2',in:'header',type:'string',description:'Test1.j'}",
				"],",
				"responses:{",
					"200: {description:'OK'},",
					"201: {description:'Test1.l',headers:{bar:{description:'Test1.m',type:'string'}}}",
				"}"
			}
			swagger=@MethodSwagger(
				parameters={
					"{name:'a',in:'path',type:'string',description:'Test1.d'},",
					"{name:'b',in:'query',type:'string',description:'Test1.e'},",
					"{in:'body',type:'string',description:'Test1.f'},",
					"{name:'D',in:'header',type:'string',description:'Test1.g'},",
					"{name:'a2',in:'path',type:'string',description:'Test1.h'},",
					"{name:'b2',in:'query',type:'string',description:'Test1.i'},",
					"{name:'D2',in:'header',type:'string',description:'Test1.j'}",
				},
				responses={
					"200: {description:'OK'},",
					"201: {description:'Test1.l',headers:{bar:{description:'Test1.m',type:'string'}}}",
				}
			)
	 * 
	 * 
	 */
	String[] value() default {};
	
	String[] summary() default {};
	
	String[] description() default {};

	/**
	 * Optional deprecated flag for the exposed API.
	 * 
	 * <p>
	 * Used to populate the Swagger deprecated field.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			deprecated=<jk>true</jk>
	 * 		)
	 * 	)
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Corresponds to the swagger field <code>/paths/{path}/{method}/deprecated</code>.
	 * </ul>
	 */
	String deprecated() default "";

	/**
	 * Optional external documentation information for the exposed API.
	 * 
	 * <p>
	 * Used to populate the Swagger external documentation field.
	 * 
	 * <p>
	 * A simplified JSON string with the following fields:
	 * <p class='bcode'>
	 * 	{
	 * 		description: string,
	 * 		url: string
	 * 	}
	 * </p>
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			<js>"{url:'http://juneau.apache.org'}"</js>
	 * 		)
	 * 	)
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Corresponds to the swagger field <code>/paths/{path}/{method}/externalDocs</code>.
	 * </ul>
	 */
	String[] externalDocs() default {};

	/**
	 * Optional parameter descriptions.
	 * 
	 * <p>
	 * This annotation is provided for documentation purposes and is used to populate the method <js>"parameters"</js>
	 * column on the Swagger page.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		name=<jsf>POST</jsf>, path=<js>"/{a}"</js>,
	 * 		description=<js>"This is my method."</js>,
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			parameters={
	 * 				<ja>@Parameter</ja>(in=<js>"path"</js>, name=<js>"a"</js>, description=<js>"The 'a' attribute"</js>),
	 * 				<ja>@Parameter</ja>(in=<js>"query"</js>, name=<js>"b"</js>, description=<js>"The 'b' parameter"</js>, required=<jk>true</jk>),
	 * 				<ja>@Parameter</ja>(in=<js>"body"</js>, description=<js>"The HTTP content"</js>),
	 * 				<ja>@Parameter</ja>(in=<js>"header"</js>, name=<js>"D"</js>, description=<js>"The 'D' header"</js>),
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Corresponds to the swagger field <code>/paths/{path}/{method}/parameters</code>.
	 * </ul>
	 */
	String[] parameters() default {};

	/**
	 * Optional output description.
	 * 
	 * <p>
	 * This annotation is provided for documentation purposes and is used to populate the method <js>"responses"</js>
	 * column on the Swagger page.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		name=<jsf>GET</jsf>, path=<js>"/"</js>,
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			responses={
	 * 				<ja>@Response</ja>(200),
	 * 				<ja>@Response</ja>(
	 * 					value=302,
	 * 					description=<js>"Thing wasn't found here"</js>,
	 * 					headers={
	 * 						<ja>@Parameter</ja>(name=<js>"Location"</js>, description=<js>"The place to find the thing"</js>)
	 * 					}
	 * 				)
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Corresponds to the swagger field <code>/paths/{path}/{method}/responses</code>.
	 * </ul>
	 */
	String[] responses() default {};

	/**
	 * Optional tagging information for the exposed API.
	 * 
	 * <p>
	 * Used to populate the Swagger tags field.
	 * 
	 * <p>
	 * A comma-delimited list of tags for API documentation control.
	 * <br>Tags can be used for logical grouping of operations by resources or any other qualifier.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			tags=<js>"foo,bar"</js>
	 * 		)
	 * 	)
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Corresponds to the swagger field <code>/paths/{path}/{method}/tags</code>.
	 * </ul>
	 */
	String[] tags() default "";
}
