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
 */
public @interface MethodSwagger {

	/**
	 * Optional external documentation information for the exposed API.
	 * <p>
	 * Used to populate the Swagger external documentation field.
	 * <p>
	 * A simplified JSON string with the following fields:
	 * <p class='bcode'>
	 * 	{
	 * 		description: string,
	 * 		url: string
	 * 	}
	 * </p>
	 * <p>
	 * The default value pulls the description from the <code>(className.?)[javaMethodName].externalDocs</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"MyClass.myMethod.externalDocs = {url:'http://juneau.apache.org'}"</js> or <js>"myMethod.externalDocs = {url:'http://juneau.apache.org'}"</js>).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			<js>"{url:'http://juneau.apache.org'}"</js>
	 * 		)
	 * 	)
	 * </p>
	 * <p>
	 * This field can contain variables (e.g. "$L{my.localized.variable}").
	 * <p>
	 * Corresponds to the swagger field <code>/paths/{path}/{method}/externalDocs</code>.
	 */
	String externalDocs() default "";

	/**
	 * Optional tagging information for the exposed API.
	 * <p>
	 * Used to populate the Swagger tags field.
	 * <p>
	 * A comma-delimited list of tags for API documentation control.
	 * Tags can be used for logical grouping of operations by resources or any other qualifier.
	 * <p>
	 * The default value pulls the description from the <code>(className.?)[javaMethodName].tags</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"MyClass.myMethod.tags = foo,bar"</js> or <js>"myMethod.tags = foo,bar"</js>).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			tags=<js>"foo,bar"</js>
	 * 		)
	 * 	)
	 * </p>
	 * <p>
	 * This field can contain variables (e.g. "$L{my.localized.variable}").
	 * <p>
	 * Corresponds to the swagger field <code>/paths/{path}/{method}/tags</code>.
	 */
	String tags() default "";

	/**
	 * Optional deprecated flag for the exposed API.
	 * <p>
	 * Used to populate the Swagger deprecated field.
	 * <p>
	 * The default value pulls the description from the <code>(className.?)[javaMethodName].deprecated</code> entry in the servlet resource bundle.
	 * 	(e.g. <js>"MyClass.myMethod.deprecated = true"</js> or <js>"myMethod.deprecated = foo,bar"</js>).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		swagger=<ja>@MethodSwagger</ja>(
	 * 			deprecated=<jk>true</jk>
	 * 		)
	 * 	)
	 * </p>
	 * <p>
	 * This field can contain variables (e.g. "$L{my.localized.variable}").
	 * <p>
	 * Corresponds to the swagger field <code>/paths/{path}/{method}/deprecated</code>.
	 */
	boolean deprecated() default false;

	/**
	 * Optional parameter descriptions.
	 * <p>
	 * This annotation is provided for documentation purposes and is used to populate the method <js>"parameters"</js> column
	 * 	on the Swagger page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		name=<js>"POST"</js>, path=<js>"/{a}"</js>,
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
	 * This is functionally equivalent to specifying the following keys in the resource bundle for the class, except in this case
	 * 	the strings are internationalized.
	 * <p class='bcode'>
	 * 	<jk>MyClass.myMethod.description</jk> = <js>This is my method.</js>
	 * 	<jk>MyClass.myMethod.req.path.a.description</jk> = <js>The 'a' attribute</js>
	 * 	<jk>MyClass.myMethod.req.query.b.description</jk> = <js>The 'b' parameter</js>
	 * 	<jk>MyClass.myMethod.req.body.description</jk> = <js>The HTTP content</js>
	 * 	<jk>MyClass.myMethod.req.header.d.description</jk> = <js>The 'D' header</js>
	 * <p>
	 * As a general rule, use annotations when you don't care about internationalization (i.e. you only want to support English),
	 * 	and use resource bundles if you need to support localization.
	 * <p>
	 * These annotations can contain variables (e.g. "$L{my.localized.variable}").
	 * <p>
	 * Corresponds to the swagger field <code>/paths/{path}/{method}/parameters</code>.
	 */
	Parameter[] parameters() default {};

	/**
	 * Optional output description.
	 * <p>
	 * This annotation is provided for documentation purposes and is used to populate the method <js>"responses"</js> column
	 * 	on the Swagger page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		name=<js>"GET"</js>, path=<js>"/"</js>,
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
	 * This is functionally equivalent to specifying the following keys in the resource bundle for the class, except in this case
	 * 	the strings are internationalized.
	 * <p class='bcode'>
	 * 	<jk>MyClass.myMethod.res.200.description</jk> = <js>OK</js>
	 * 	<jk>MyClass.myMethod.res.302.description</jk> = <js>Thing wasn't found here</js>
	 * 	<jk>MyClass.myMethod.res.302.header.Location.description</jk> = <js>The place to find the thing</js>
	 * <p>
	 * As a general rule, use annotations when you don't care about internationalization (i.e. you only want to support English),
	 * 	and use resource bundles if you need to support localization.
	 * <p>
	 * These annotations can contain variables (e.g. "$L{my.localized.variable}").
	 */
	Response[] responses() default {};

}
