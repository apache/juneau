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

import java.lang.annotation.*;

import org.apache.juneau.http.header.*;

/**
 * Identifies a proxy against a REST interface.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestcProxies}
 * </ul>
 */
@Documented
@Target({TYPE})
@Retention(RUNTIME)
@Inherited
public @interface Remote {

	/**
	 * REST service path.
	 *
	 * <p>
	 * The possible values are:
	 * <ul class='spaced-list'>
	 * 	<li>An absolute URL.
	 * 	<li>A relative URL interpreted as relative to the root URL defined on the <c>RestClient</c>
	 * 	<li>No path interpreted as the class name (e.g. <js>"http://localhost/root-url/org.foo.MyInterface"</js>)
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc DefaultVarResolver}
	 * 		(e.g. <js>"$P{mySystemProperty}"</js>).
	 * </ul>
	 */
	String path() default "";

	/**
	 * Default request headers.
	 *
	 * <p>
	 * Specifies headers to set on all requests.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc DefaultVarResolver}
	 * 		(e.g. <js>"$P{mySystemProperty}"</js>).
	 * </ul>
	 */
	String[] headers() default {};

	/**
	 * Default request header list.
	 *
	 * <p>
	 * Specifies a supplier of headers to set on all requests.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supplier class must provide a public no-arg constructor.
	 * </ul>
	 */
	Class<? extends HeaderList> headerList() default HeaderList.Null.class;

	/**
	 * Specifies the client version of this interface.
	 *
	 * <p>
	 * Used to populate the <js>"X-Client-Version"</js> header that identifies what version of client this is
	 * so that the server side can handle older versions accordingly.
	 *
	 * <p>
	 * The format of this is a string of the format <c>#[.#[.#[...]]</c> (e.g. <js>"1.2.3"</js>).
	 *
	 * <p>
	 * The server side then uses an OSGi-version matching pattern to identify which methods to call:
	 * <p class='bcode w800'>
	 * 	<jc>// Call this method if X-Client-Version is at least 2.0.
	 * 	// Note that this also matches 2.0.1.</jc>
	 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> Object method1()  {...}
	 *
	 * 	<jc>// Call this method if X-Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>)
	 * 	<jk>public</jk> Object method2()  {...}
	 *
	 * 	<jc>// Call this method if X-Client-Version is less than 1.1.</jc>
	 * 	<ja>@RestGet</ja>(path=<js>"/foobar"</js>, clientVersion=<js>"[0,1.1)"</js>)
	 * 	<jk>public</jk> Object method3()  {...}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc DefaultVarResolver}
	 * 		(e.g. <js>"$P{mySystemProperty}"</js>).
	 * </ul>
	 */
	String version() default "";

	/**
	 * Specifies the client version header name.
	 *
	 * <p>
	 * The default value is <js>"X-Client-Version"</js>.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc DefaultVarResolver}
	 * 		(e.g. <js>"$P{mySystemProperty}"</js>).
	 * </ul>
	 */
	String versionHeader() default "";
}
