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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.httppart.*;

/**
 * Request bean annotation.
 *
 * <p>
 * Identifies an interface to use to interact with HTTP parts of an HTTP request through a bean.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Java method arguments and argument-types of client-side <ja>@Remoteable</ja>-annotated REST interface proxies.
 * 	<li>Java method arguments and argument-types of server-side <ja>@RestMethod</ja>-annotated REST Java methods.
 * </ul>
 *
 * <h5 class='topic'>Server-side REST</h5>
 *
 * Annotation that can be applied to a parameter of a <ja>@RestMethod</ja>-annotated method to identify it as an interface for retrieving HTTP parts through a bean interface.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<ja>@RestMethod</ja>(path=<js>"/mypath/{p1}/{p2}/*"</js>)
 * 	<jk>public void</jk> myMethod(<ja>@RequestBean</ja> MyRequestBean rb) {...}
 *
 * 	<jk>public interface</jk> MyRequestBean {
 *
 * 		<ja>@Path</ja> <jc>// Path variable name inferred from getter.</jc>
 * 		String getP1();
 *
 * 		<ja>@Path</ja>(<js>"p2"</js>)
 * 		String getX();
 *
 * 		<ja>@Path</ja>(<js>"/*"</js>)
 * 		String getRemainder();
 *
 * 		<ja>@Query</ja>
 * 		String getQ1();
 *
 * 		<ja>@Query</ja>
 * 		<ja>@BeanProperty</ja>(name=<js>"q2"</js>)
 * 		String getQuery2();
 *
 *		<jc>// Schema-based query parameter:  Pipe-delimited lists of comma-delimited lists of integers.</jc>
 * 		<ja>@Query</ja>(
 * 			collectionFormat=<js>"pipes"</js>
 * 			items=<ja>@Items</ja>(
 * 				items=<ja>@SubItems</ja>(
 * 					collectionFormat=<js>"csv"</js>
 * 					type=<js>"integer"</js>
 * 				)
 * 			)
 * 		)
 * 		<jk>int</jk>[][] getQ3();
 *
 * 		<ja>@Header</ja>(<js>"*"</js>)
 * 		Map&lt;String,Object&gt; getHeaders();
 * </p>
 * <p class='bcode w800'>
 * 	<jc>// Same as above but annotation defined on interface.</jc>
 * 	<ja>@RestMethod</ja>(path=<js>"/mypath/{p1}/{p2}/*"</js>)
 * 	<jk>public void</jk> myMethod(MyRequestBean rb) {...}
 *
 *	<ja>@RequestBean</ja>
 * 	<jk>public interface</jk> MyRequestBean {...}
 *
 * <p>
 * The return types of the getters must be the supported parameter types for the HTTP-part annotation used.
 * <br>Schema-based serialization and parsing is allowed just as if used as individual parameter types.
 *
 * <p>
 * TODO - Swagger-info in <ja>@RequestBean</ja>s is not currently reflected in generated Swagger documentation.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.HttpPartAnnotations.RequestBean">Overview &gt; juneau-rest-server &gt; @RequestBean</a>
 * </ul>
 *
 * <h5 class='topic'>Client-side REST</h5>
 *
 * Annotation applied to Java method arguments of interface proxies to denote a bean with remoteable annotations.

 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<ja>@Remoteable</ja>(path=<js>"/myproxy"</js>)
 * 	<jk>public interface</jk> MyProxy {
 *
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod/{p1}/{p2}"</js>)
 * 		String myProxyMethod(<ja>@RequestBean</ja> MyRequestBean bean);
 * 	}
 *
 * 	<jk>public class</jk> MyRequestBean {
 *
 * 		<ja>@Path</ja> <jc>// Path variable name inferred from getter.</jc>
 * 		<jk>public</jk> String getP1() {...}
 *
 * 		<ja>@Path</ja>(<js>"p2"</js>)
 * 		<jk>public</jk> String getX() {...}
 *
 * 		<ja>@Path</ja>(<js>"/*"</js>)
 * 		<jk>public</jk> String getRemainder() {...}
 *
 * 		<ja>@Query</ja>
 * 		<jk>public</jk> String getQ1() {...}
 *
 * 		<ja>@Query</ja>
 * 		<ja>@BeanProperty</ja>(name=<js>"q2"</js>)
 * 		<jk>public</jk> String getQuery2() {...}
 *
 *		<jc>// Schema-based query parameter:  Pipe-delimited lists of comma-delimited lists of integers.</jc>
 * 		<ja>@Query</ja>(
 * 			collectionFormat=<js>"pipes"</js>
 * 			items=<ja>@Items</ja>(
 * 				items=<ja>@SubItems</ja>(
 * 					collectionFormat=<js>"csv"</js>
 * 					type=<js>"integer"</js>
 * 				)
 * 			)
 * 		)
 * 		<jk>public</jk> <jk>int</jk>[][] getQ3() {...}
 *
 * 		<ja>@Header</ja>(<js>"*"</js>)
 * 		<jk>public</jk> Map&lt;String,Object&gt; getHeaders() {...}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../../overview-summary.html#juneau-rest-client.3rdPartyProxies'>Overview &gt; juneau-rest-client &gt; Interface Proxies Against 3rd-party REST Interfaces</a>
 * </ul>
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface RequestBean {

	/**
	 * Specifies the {@link HttpPartSerializer} class used for serializing values to strings.
	 *
	 * <p>
	 * Overrides for this part the part serializer defined on the REST client which by default is {@link OpenApiPartSerializer}.
	 */
	Class<? extends HttpPartSerializer> serializer() default HttpPartSerializer.Null.class;

	/**
	 * Specifies the {@link HttpPartParser} class used for parsing strings to values.
	 *
	 * <p>
	 * Overrides for this part the part parser defined on the REST resource which by default is {@link OpenApiPartParser}.
	 */
	Class<? extends HttpPartParser> parser() default HttpPartParser.Null.class;
}
