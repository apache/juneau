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

import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;

/**
 * Extended annotation for {@link RestOp#swagger() RestOp.swagger()}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Swagger</a>
 * </ul>
 */
@Retention(RUNTIME)
public @interface OpSwagger {

	/**
	 * Defines the swagger field <c>/paths/{path}/{method}/consumes</c>.
	 *
	 * <p>
	 * Use this value to override the supported <c>Content-Type</c> media types defined by the parsers defined via {@link RestOp#parsers()}.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is either a comma-delimited list of simple strings or a <a class="doclink" href="../../../../../index.html#jd.Swagger">Swagger</a> array.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		Values defined on this annotation override values defined for the method in the class swagger.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] consumes() default {};

	/**
	 * Defines the swagger field <c>/paths/{path}/{method}/deprecated</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@RestGet</ja>(
	 * 		swagger=<ja>@OpSwagger</ja>(
	 * 			deprecated=<jk>true</jk>
	 * 		)
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is boolean.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		Values defined on this annotation override values defined for the method in the class swagger.
	 * 	<li class='note'>
	 * 		If not specified, set to <js>"true"</js> if the method is annotated with {@link Deprecated @Deprecated}
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String deprecated() default "";

	/**
	 * Defines the swagger field <c>/paths/{path}/{method}/description</c>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is plain text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		Values defined on this annotation override values defined for the method in the class swagger.
	 * 	<li class='note'>
	 * 		If not specified, the value is pulled from {@link RestOp#description()}.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] description() default {};

	/**
	 * Defines the swagger field <c>/paths/{path}/{method}/externalDocs</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@RestGet</ja>(
	 * 		swagger=<ja>@OpSwagger</ja>(
	 * 			externalDocs=<ja>@ExternalDocs</ja>(url=<js>"http://juneau.apache.org"</js>)
	 * 		)
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		Values defined on this annotation override values defined for the method in the class swagger.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	ExternalDocs externalDocs() default @ExternalDocs;

	/**
	 * Defines the swagger field <c>/paths/{path}/{method}/operationId</c>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is plain text.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		Values defined on this annotation override values defined for the method in the class swagger.
	 * 	<li class='note'>
	 * 		If not specified, the value used is the Java method name.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String operationId() default "";

	/**
	 * Defines the swagger field <c>/paths/{path}/{method}/parameters</c>.
	 *
	 * <p>
	 * This annotation is provided for documentation purposes and is used to populate the method <js>"parameters"</js>
	 * column on the Swagger page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@RestPost</ja>(
	 * 		path=<js>"/{a}"</js>,
	 * 		description=<js>"This is my method."</js>,
	 * 		swagger=<ja>@OpSwagger</ja>(
	 * 			parameters={
	 * 				<js>"{in:'path', name:'a', description:'The \\'a\\' attribute'},"</js>,
	 * 				<js>"{in:'query', name:'b', description:'The \\'b\\' parameter', required:true},"</js>,
	 * 				<js>"{in:'body', description:'The HTTP content'},"</js>,
	 * 				<js>"{in:'header', name:'D', description:'The \\'D\\' header'}"</js>
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a <a class="doclink" href="../../../../../index.html#jd.Swagger">Swagger</a> array consisting of the concatenated individual strings.
	 * 		<br>The leading and trailing <js>'['</js> and <js>']'</js> characters are optional.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] parameters() default {};

	/**
	 * Defines the swagger field <c>/paths/{path}/{method}/consumes</c>.
	 *
	 * <p>
	 * Use this value to override the supported <c>Accept</c> media types defined by the serializers defined via {@link RestOp#serializers()}.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is either a comma-delimited list of simple strings or a <a class="doclink" href="../../../../../index.html#jd.Swagger">Swagger</a> array.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		Values defined on this annotation override values defined for the method in the class swagger.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] produces() default {};

	/**
	 * Defines the swagger field <c>/paths/{path}/{method}/responses</c>.
	 *
	 * <p>
	 * This annotation is provided for documentation purposes and is used to populate the method <js>"responses"</js>
	 * column on the Swagger page.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@RestGet</ja>(
	 * 		path=<js>"/"</js>,
	 * 		swagger=<ja>@OpSwagger</ja>(
	 * 			responses={
	 * 				<js>"200:{ description:'Okay' },"</js>,
	 * 				<js>"302:{ description:'Thing wasn't found here', headers={Location:{description:'The place to find the thing.'}}}"</js>
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a <a class="doclink" href="../../../../../index.html#jd.Swagger">Swagger</a> objc consisting of the concatenated individual strings.
	 * 		<br>The leading and trailing <js>'{'</js> and <js>'}'</js> characters are optional.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] responses() default {};

	/**
	 * Defines the swagger field <c>/paths/{path}/{method}/schemes</c>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is either a comma-delimited list of simple strings or a <a class="doclink" href="../../../../../index.html#jd.Swagger">Swagger</a> array.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		Values defined on this annotation override values defined for the method in the class swagger.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] schemes() default {};

	/**
	 * Defines the swagger field <c>/paths/{path}/{method}/summary</c>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is plain text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		Values defined on this annotation override values defined for the method in the class swagger.
	 * 	<li class='note'>
	 * 		If not specified, the value is pulled from {@link RestOp#summary()}.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] summary() default {};

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
	 * <p class='bjava'>
	 * 	<ja>@RestGet</ja>(
	 * 		swagger=<ja>@OpSwagger</ja>(
	 * 			tags=<js>"foo,bar"</js>
	 * 		)
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		Corresponds to the swagger field <c>/paths/{path}/{method}/tags</c>.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] tags() default {};

	/**
	 * Free-form value for the swagger of a resource method.
	 *
	 * <p>
	 * This is a <a class="doclink" href="../../../../../index.html#jd.Swagger">Swagger</a> object that makes up the swagger information for this resource method.
	 *
	 * <p>
	 * The following are completely equivalent ways of defining the swagger description of a resource method:
	 * <p class='bjava'>
	 * 	<jc>// Normal</jc>
	 * 	<ja>@RestPost</ja>(
	 * 		path=<js>"/pet"</js>,
	 * 		swagger=<ja>@OpSwagger</ja>(
	 * 			summary=<js>"Add pet"</js>,
	 * 			description=<js>"Adds a new pet to the store"</js>,
	 * 			tags=<js>"pet"</js>,
	 * 			externalDocs=<ja>@ExternalDocs</ja>(
	 * 				description=<js>"Home page"</js>,
	 * 				url=<js>"http://juneau.apache.org"</js>
	 * 			)
	 * 		)
	 * )
	 * </p>
	 * <p class='bjava'>
	 * 	<jc>// Free-form</jc>
	 * 	<ja>@RestPost</ja>(
	 * 		path=<js>"/pet"</js>,
	 * 		swagger=<ja>@OpSwagger</ja>({
	 * 			<js>"summary: 'Add pet',"</js>,
	 * 			<js>"description: 'Adds a new pet to the store',"</js>,
	 * 			<js>"tags: ['pet'],"</js>,
	 * 			<js>"externalDocs:{"</js>,
	 * 				<js>"description: 'Home page',"</js>,
	 * 				<js>"url: 'http://juneau.apache.org'"</js>,
	 * 			<js>"}"</js>
	 * 		})
	 * )
	 * </p>
	 * <p class='bjava'>
	 * 	<jc>// Free-form with variables</jc>
	 * 	<ja>@RestPost</ja>(
	 * 		path=<js>"/pet"</js>,
	 * 		swagger=<ja>@OpSwagger</ja>(<js>"$L{addPetSwagger}"</js>)
	 * )
	 * </p>
	 * <p class='bini'>
	 * 	<mc>// Contents of MyResource.properties</mc>
	 * 	<mk>addPetSwagger</mk> = <mv>{ summary: "Add pet", description: "Adds a new pet to the store", tags: ["pet"], externalDocs:{ description: "Home page", url: "http://juneau.apache.org" } }</mv>
	 * </p>
	 *
	 * <p>
	 * 	The reasons why you may want to use this field include:
	 * <ul>
	 * 	<li>You want to pull in the entire Swagger JSON definition for this content from an external source such as a properties file.
	 * 	<li>You want to add extra fields to the Swagger documentation that are not officially part of the Swagger specification.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a <a class="doclink" href="../../../../../index.html#jd.Swagger">Swagger</a> object.
	 * 	<li class='note'>
	 * 		The leading/trailing <c>{ }</c> characters are optional.
	 * 		<br>The following two example are considered equivalent:
	 * 		<p class='bjava'>
	 * 	<ja>@OpSwagger</ja>(<js>"{summary: 'Add pet'}"</js>)
	 * 		</p>
	 * 		<p class='bjava'>
	 * 	<ja>@OpSwagger</ja>(<js>"summary: 'Add pet'"</js>)
	 * 		</p>
	 * 	<li class='note'>
	 * 		Multiple lines are concatenated with newlines so that you can format the value to be readable.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li class='note'>
	 * 		Values defined in this field supersede values pulled from the Swagger JSON file and are superseded by individual values defined on this annotation.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] value() default {};
}
