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
import org.apache.juneau.urlencoding.*;

/**
 * Request bean annotation.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Java method arguments and argument-types of client-side <ja>@Remoteable</ja>-annotated REST interface proxies.
 * 	<li>Java method arguments and argument-types of server-side <ja>@RestMethod</ja>-annotated REST Java methods.
 * </ul>
 *
 * <h5 class='topic'>Server-side REST</h5>
 * TODO
 *
 * <h5 class='topic'>Client-side REST</h5>
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
 * 	<jk>public interface</jk> MyRequestBean {
 *
 * 		<ja>@Path</ja>
 * 		String getP1();
 *
 * 		<ja>@Path</ja>(<js>"p2"</js>)
 * 		String getX();
 *
 * 		<ja>@Query</ja>
 * 		String getQ1();
 *
 * 		<ja>@Query</ja>
 * 		<ja>@BeanProperty</ja>(name=<js>"q2"</js>)
 * 		String getQuery2();
 *
 * 		<ja>@Query</ja>(name=<js>"q3"</js>, skipIfEmpty=<jk>true</jk>)
 * 		String getQuery3();
 *
 * 		<ja>@Query</ja>(skipIfEmpty=<jk>true</jk>)
 * 		Map&lt;String,Object&gt; getExtraQueries();
 *
 * 		<ja>@FormData</ja>
 * 		String getF1();
 *
 * 		<ja>@FormData</ja>
 * 		<ja>@BeanProperty</ja>(name=<js>"f2"</js>)
 * 		String getFormData2();
 *
 * 		<ja>@FormData</ja>(name=<js>"f3"</js>,skipIfEmpty=<jk>true</jk>)
 * 		String getFormData3();
 *
 * 		<ja>@FormData</ja>(skipIfEmpty=<jk>true</jk>)
 * 		Map&lt;String,Object&gt; getExtraFormData();
 *
 * 		<ja>@Header</ja>
 * 		String getH1();
 *
 * 		<ja>@Header</ja>
 * 		<ja>@BeanProperty</ja>(name=<js>"H2"</js>)
 * 		String getHeader2();
 *
 * 		<ja>@Header</ja>(name=<js>"H3"</js>,skipIfEmpty=<jk>true</jk>)
 * 		String getHeader3();
 *
 * 		<ja>@Header</ja>(skipIfEmpty=<jk>true</jk>)
 * 		Map&lt;String,Object&gt; getExtraHeaders();
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../overview-summary.html#juneau-rest-client.3rdPartyProxies'>Overview &gt; juneau-rest-client &gt; Interface Proxies Against 3rd-party REST Interfaces</a>
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
	 * The default value defaults to the using the part serializer defined on the client which by default is
	 * {@link UrlEncodingSerializer}.
	 *
	 * <p>
	 * This annotation is provided to allow values to be custom serialized.
	 */
	Class<? extends HttpPartSerializer> serializer() default HttpPartSerializer.Null.class;

	/**
	 * TODO
	 */
	Class<? extends HttpPartParser> parser() default HttpPartParser.Null.class;
}
