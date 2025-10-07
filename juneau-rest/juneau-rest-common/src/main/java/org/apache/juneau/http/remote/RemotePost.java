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
package org.apache.juneau.http.remote;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.io.*;
import java.lang.annotation.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.*;

/**
 * Annotation applied to Java methods on REST proxy interface classes.
 *
 * <p>
 * Note that this annotation is optional if you do not need to override any of the values.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestProxyBasics">REST Proxy Basics</a>
 * </ul>
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
@Inherited
@AnnotationGroup(RemoteOp.class)
public @interface RemotePost {

	/**
	 * REST service path.
	 *
	 * <p>
	 * If you do not specify a path, then the path is inferred from the Java method name.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// POST /pet</jc>
	 * 	<ja>@RemotePost</ja>
	 * 	<jk>public void</jk> postPet(...);
	 * </p>
	 *
	 * <p>
	 * Note that you can also use {@link #value()} to specify the path in shortened form.
	 *
	 * <ul class='values'>
	 * 	<li>An absolute URL.
	 * 	<li>A relative URL interpreted as relative to the root URL defined on the <c>RestClient</c> and/or {@link Remote#path()}.
	 * 	<li>No path.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String path() default "";

	/**
	 * The value the remote method returns.
	 *
	 * <ul class='values'>
	 * 	<li>
	 * 		{@link RemoteReturn#BODY} (default) - The body of the HTTP response converted to a POJO.
	 * 		<br>The return type on the Java method can be any of the following:
	 * 		<ul class='spaced-list'>
	 * 			<li>
	 * 				<jk>void</jk> - Don't parse any response.  Note that the method will still throw an exception if an
	 * 				error HTTP status is returned.
	 * 			<li>
	 * 				Any parsable POJO - The body of the response will be converted to the POJO using the parser defined
	 * 				on the <c>RestClient</c>.
	 * 			<li>
	 * 				Any POJO annotated with the {@link Response @Response} annotation.
	 * 				This allows for response beans to be used which also allows for OpenAPI-based parsing and validation.
	 * 			<li>
	 * 				<c>HttpResponse</c> - Returns the raw <c>HttpResponse</c> returned by the inner
	 * 				<c>HttpClient</c>.
	 * 			<li>
	 * 				{@link Reader} - Returns access to the raw reader of the response.
	 * 			<li>
	 * 				{@link InputStream} - Returns access to the raw input stream of the response.
	 * 		</ul>
	 * 	<li>
	 * 		{@link RemoteReturn#STATUS} - The HTTP status code on the response.
	 * 		<br>The return type on the Java method can be any of the following:
	 * 		<ul>
	 * 			<li><jk>int</jk>/<c>Integer</c> - The HTTP response code.
	 * 			<li><jk>boolean</jk>/<c>Boolean</c> - <jk>true</jk> if the response code is <c>&lt;400</c>
	 * 		</ul>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	RemoteReturn returns() default RemoteReturn.BODY;

	/**
	 * REST path.
	 *
	 * <p>
	 * Can be used to provide a shortened form for the {@link #path()} value.
	 *
	 * <p>
	 * The following examples are considered equivalent.
	 * <p class='bjava'>
	 * 	<jc>// Normal form</jc>
	 * 	<ja>@RemotePost</ja>(path=<js>"/{propertyName}"</js>)
	 *
	 * 	<jc>// Shortened form</jc>
	 * 	<ja>@RemotePost</ja>(<js>"/{propertyName}"</js>)
	 * </p>
	 *
	 * @return The annotation value.
	 */
	String value() default "";
}
