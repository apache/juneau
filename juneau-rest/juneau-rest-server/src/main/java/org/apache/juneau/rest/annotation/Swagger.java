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
import org.apache.juneau.http.annotation.*;

/**
 * Extended annotation for {@link Rest#swagger() @Rest(swagger)}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Swagger</a>
 * </ul>
 */
@Retention(RUNTIME)
public @interface Swagger {

	/**
	 * Defines the swagger field <c>/info/contact</c>.
	 *
	 * <p>
	 * A <a class="doclink" href="../../../../../index.html#jd.Swagger">Swagger</a> string with the following fields:
	 * <p class='bschema'>
	 * 	{
	 * 		name: string,
	 * 		url: string,
	 * 		email: string
	 * 	}
	 * </p>
	 *
	 * <p>
	 * The default value pulls the description from the <c>contact</c> entry in the servlet resource bundle.
	 * (e.g. <js>"contact = {name:'John Smith',email:'john.smith@foo.bar'}"</js> or
	 * <js>"MyServlet.contact = {name:'John Smith',email:'john.smith@foo.bar'}"</js>).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Rest</ja>(
	 * 		swagger=<ja>@Swagger</ja>(
	 * 			contact=<js>"{name:'John Smith',email:'john.smith@foo.bar'}"</js>
	 * 		)
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a <a class="doclink" href="../../../../../index.html#jd.Swagger">Swagger</a> object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Contact contact() default @Contact;

	/**
	 * Defines the swagger field <c>/info/description</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Rest</ja>(
	 * 		swagger=<ja>@Swagger</ja>(
	 * 			description={
	 * 				<js>"This is a sample server Petstore server based on the Petstore sample at Swagger.io."</js>,
	 * 				<js>"You can find out more about Swagger at &lt;a class='link' href='http://swagger.io'&gt;http://swagger.io&lt;/a&gt;."</js>
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is plain text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li class='note'>
	 * 		The precedence of lookup for this field is:
	 * 		<ol>
	 * 			<li><c>{resource-class}.description</c> property in resource bundle.
	 * 			<li>{@link Swagger#description()} on this class, then any parent classes.
	 * 			<li>Value defined in Swagger JSON file.
	 * 			<li>{@link Rest#description()} on this class, then any parent classes.
	 * 		</ol>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] description() default {};

	/**
	 * Defines the swagger field <c>/externalDocs</c>.
	 *
	 * <p>
	 * It is used to populate the Swagger external documentation field and to display on HTML pages.
	 * 	 *
	 * <p>
	 * The default value pulls the description from the <c>externalDocs</c> entry in the servlet resource bundle.
	 * (e.g. <js>"externalDocs = {url:'http://juneau.apache.org'}"</js> or
	 * <js>"MyServlet.externalDocs = {url:'http://juneau.apache.org'}"</js>).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Rest</ja>(
	 * 		swagger=<ja>@Swagger</ja>(
	 * 			externalDocs=<ja>@ExternalDocs</ja>(url=<js>"http://juneau.apache.org"</js>)
	 * 		)
	 * 	)
	 * </p>
	 *
	 * @return The annotation value.
	 */
	ExternalDocs externalDocs() default @ExternalDocs;

	/**
	 * Defines the swagger field <c>/info/license</c>.
	 *
	 * <p>
	 * It is used to populate the Swagger license field and to display on HTML pages.
	 *
	 * <p>
	 * A <a class="doclink" href="../../../../../index.html#jd.Swagger">Swagger</a> string with the following fields:
	 * <p class='bschema'>
	 * 	{
	 * 		name: string,
	 * 		url: string
	 * 	}
	 * </p>
	 *
	 * <p>
	 * The default value pulls the description from the <c>license</c> entry in the servlet resource bundle.
	 * (e.g. <js>"license = {name:'Apache 2.0',url:'http://www.apache.org/licenses/LICENSE-2.0.html'}"</js> or
	 * <js>"MyServlet.license = {name:'Apache 2.0',url:'http://www.apache.org/licenses/LICENSE-2.0.html'}"</js>).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Rest</ja>(
	 * 		swagger=<ja>@Swagger</ja>(
	 * 			license=<js>"{name:'Apache 2.0',url:'http://www.apache.org/licenses/LICENSE-2.0.html'}"</js>
	 * 		)
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a <a class="doclink" href="../../../../../index.html#jd.Swagger">Swagger</a> object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	License license() default @License;

	/**
	 * Defines the swagger field <c>/tags</c>.
	 *
	 *
	 * Optional tagging information for the exposed API.
	 *
	 * <p>
	 * It is used to populate the Swagger tags field and to display on HTML pages.
	 *
	 * <p>
	 * A <a class="doclink" href="../../../../../index.html#jd.Swagger">Swagger</a> string with the following fields:
	 * <p class='bschema'>
	 * 	[
	 * 		{
	 * 			name: string,
	 * 			description: string,
	 * 			externalDocs: {
	 * 				description: string,
	 * 				url: string
	 * 			}
	 * 		}
	 * 	]
	 * </p>
	 *
	 * <p>
	 * The default value pulls the description from the <c>tags</c> entry in the servlet resource bundle.
	 * (e.g. <js>"tags = [{name:'Foo',description:'Foobar'}]"</js> or
	 * <js>"MyServlet.tags = [{name:'Foo',description:'Foobar'}]"</js>).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Rest</ja>(
	 * 		swagger=<ja>@Swagger</ja>(
	 * 			tags=<js>"[{name:'Foo',description:'Foobar'}]"</js>
	 * 		)
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a <a class="doclink" href="../../../../../index.html#jd.Swagger">Swagger</a> array.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Tag[] tags() default {};

	/**
	 * Defines the swagger field <c>/info/termsOfService</c>.
	 *
	 *
	 * Optional servlet terms-of-service for this API.
	 *
	 * <p>
	 * It is used to populate the Swagger terms-of-service field.
	 *
	 * <p>
	 * The default value pulls the description from the <c>termsOfService</c> entry in the servlet resource bundle.
	 * (e.g. <js>"termsOfService = foo"</js> or <js>"MyServlet.termsOfService = foo"</js>).
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is plain text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] termsOfService() default {};

	/**
	 * Defines the swagger field <c>/info/title</c>.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@Rest</ja>(
	 * 		swagger=<ja>@Swagger</ja>(
	 * 			title=<js>"Petstore application"</js>
	 * 		)
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is plain-text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li class='note'>
	 * 		The precedence of lookup for this field is:
	 * 		<ol>
	 * 			<li><c>{resource-class}.title</c> property in resource bundle.
	 * 			<li>{@link Swagger#title()} on this class, then any parent classes.
	 * 			<li>Value defined in Swagger JSON file.
	 * 			<li>{@link Rest#title()} on this class, then any parent classes.
	 * 		</ol>
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] title() default {};

	/**
	 * Free-form value for the swagger of a resource.
	 *
	 * <p>
	 * This is a <a class="doclink" href="../../../../../index.html#jd.Swagger">Swagger</a> object that makes up the swagger information for this resource.
	 *
	 * <p>
	 * The following are completely equivalent ways of defining the swagger description of a resource:
	 * <p class='bjava'>
	 * 	<jc>// Normal</jc>
	 * 	<ja>@Rest</ja>(
	 * 		swagger=<ja>@Swagger</ja>(
	 * 			title=<js>"Petstore application"</js>,
	 * 			description={
	 * 				<js>"This is a sample server Petstore server based on the Petstore sample at Swagger.io."</js>,
	 * 				<js>"You can find out more about Swagger at &lt;a class='link' href='http://swagger.io'&gt;http://swagger.io&lt;/a&gt;."</js>
	 * 			},
	 * 			contact=<ja>@Contact</ja>(
	 * 				name=<js>"John Smith"</js>,
	 * 				email=<js>"john@smith.com"</js>
	 * 			),
	 * 			license=<ja>@License</ja>(
	 * 				name=<js>"Apache 2.0"</js>,
	 * 				url=<js>"http://www.apache.org/licenses/LICENSE-2.0.html"</js>
	 * 			),
	 * 			version=<js>"2.0"</js>,
	 * 			termsOfService=<js>"You are on your own."</js>,
	 * 			tags={
	 * 				<ja>@Tag</ja>(
	 * 					name=<js>"Java"</js>,
	 * 					description=<js>"Java utility"</js>,
	 * 					externalDocs=<ja>@ExternalDocs</ja>(
	 * 						description=<js>"Home page"</js>,
	 * 						url=<js>"http://juneau.apache.org"</js>
	 * 					)
	 * 				}
	 * 			},
	 * 			externalDocs=<ja>@ExternalDocs</ja>(
	 * 				description=<js>"Home page"</js>,
	 * 				url=<js>"http://juneau.apache.org"</js>
	 * 			)
	 * 		)
	 * 	)
	 * </p>
	 * <p class='bjava'>
	 * 	<jc>// Free-form</jc>
	 * 	<ja>@Rest</ja>(
	 * 		swagger=@Swagger({
	 * 			<js>"title: 'Petstore application',"</js>,
	 * 			<js>"description: 'This is a sample server Petstore server based on the Petstore sample at Swagger.io.\nYou can find out more about Swagger at &lt;a class='link' href='http://swagger.io'&gt;http://swagger.io&lt;/a&gt;.',"</js>,
	 * 			<js>"contact:{"</js>,
	 * 				<js>"name: 'John Smith',"</js>,
	 * 				<js>"email: 'john@smith.com'"</js>,
	 * 			<js>"},"</js>,
	 * 			<js>"license:{"</js>,
	 * 				<js>"name: 'Apache 2.0',"</js>,
	 * 				<js>"url: 'http://www.apache.org/licenses/LICENSE-2.0.html'"</js>,
	 * 			<js>"},"</js>,
	 * 			<js>"version: '2.0',"</js>,
	 * 			<js>"termsOfService: 'You are on your own.',"</js>,
	 * 			<js>"tags:["</js>,
	 * 				<js>"{"</js>,
	 * 					<js>"name: 'Java',"</js>,
	 * 					<js>"description: 'Java utility',"</js>,
	 * 					<js>"externalDocs:{"</js>,
	 * 						<js>"description: 'Home page',"</js>,
	 * 						<js>"url: 'http://juneau.apache.org'"</js>,
	 * 					<js>"}"</js>,
	 * 				<js>"}"</js>,
	 * 			<js>"],"</js>,
	 * 			<js>"externalDocs:{"</js>,
	 * 				<js>"description: 'Home page',"</js>,
	 * 				<js>"url: 'http://juneau.apache.org'"</js>,
	 * 			<js>"}"</js>
	 * 		})
	 * 	)
	 * </p>
	 * <p class='bjava'>
	 * 	<jc>// Free-form with variables</jc>
	 * 	<ja>@Rest</ja>(
	 * 		swagger=@Swagger(<js>"$F{MyResourceSwagger.json}"</js>)
	 * 	)
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
	 * 	<ja>@Swagger</ja>(<js>"{title:'Petstore application'}"</js>)
	 * 		</p>
	 * 		<p class='bjava'>
	 * 	<ja>@Swagger</ja>(<js>"title:'Petstore application'"</js>)
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

	/**
	 * Defines the swagger field <c>/info/version</c>.
	 *
	 *
	 *
	 * Provides the version of the application API (not to be confused with the specification version).
	 *
	 * <p>
	 * It is used to populate the Swagger version field and to display on HTML pages.
	 *
	 * <p>
	 * The default value pulls the description from the <c>version</c> entry in the servlet resource bundle.
	 * (e.g. <js>"version = 2.0"</js> or <js>"MyServlet.version = 2.0"</js>).
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is plain text.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String version() default "";
}
