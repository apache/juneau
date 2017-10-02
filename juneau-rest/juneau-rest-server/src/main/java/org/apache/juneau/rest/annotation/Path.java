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

/**
 * Annotation that can be applied to a parameter of a {@link RestMethod} annotated method to identify it as a variable
 * in a URL path pattern converted to a POJO.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/myurl/{foo}/{bar}/{baz}/*"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res,
 * 			<ja>@Path</ja> String foo, <ja>@Path</ja> <jk>int</jk> bar, <ja>@Path</ja> UUID baz) {
 * 		...
 * 	}
 * </p>
 *
 * <p>
 * The <ja>@Path</ja> annotation is optional if the parameters are specified immediately following the
 * <code>RestRequest</code> and <code>RestResponse</code> parameters, and are specified in the same order as the
 * variables in the URL path pattern.
 * The following example is equivalent to the previous example.
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/myurl/{foo}/{bar}/{baz}/*"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res,
 * 			String foo, <jk>int</jk> bar, UUID baz) {
 * 		...
 * 	}
 * </p>
 *
 * <p>
 * If the order of parameters is not the default order shown above, the attribute names must be specified (since
 * parameter names are lost during compilation).
 * The following example is equivalent to the previous example, except the parameter order has been switched, requiring
 * the use of the <ja>@Path</ja> annotations.
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/myurl/{foo}/{bar}/{baz}/*"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res,
 * 			<ja>@Path</ja>(<js>"baz"</js>) UUID baz, <ja>@Path</ja>(<js>"foo"</js>) String foo, <ja>@Path</ja>(<js>"bar"</js>) <jk>int</jk> bar) {
 * 		...
 * 	}
 * </p>
 *
 * <p>
 * You can also use <code>{#}</code> notation to specify path parameters without specifying names.
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/myurl/{0}/{1}/{2}/*"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res,
 * 			<ja>@Path</ja> String foo, <ja>@Path</ja> <jk>int</jk> bar, <ja>@Path</ja> UUID baz) {
 * 		...
 * 	}
 * </p>
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface Path {

	/**
	 * URL path variable name.
	 *
	 * <p>
	 * Optional if the attributes are specified in the same order as in the URL path pattern.
	 */
	String name() default "";

	/**
	 * A synonym for {@link #name()}.
	 *
	 * <p>
	 * Allows you to use shortened notation if you're only specifying the name.
	 */
	String value() default "";
}
