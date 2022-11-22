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

import org.apache.juneau.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;

/**
 * Request bean annotation.
 *
 * <p>
 * Identifies an interface to use to interact with HTTP parts of an HTTP request through a bean.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Arguments and argument-types of server-side <ja>@RestOp</ja>-annotated methods.
 * 	<li>Arguments and argument-types of client-side <ja>@RemoteResource</ja>-annotated interfaces.
 * </ul>
 *
 * <h5 class='topic'>Arguments and argument-types of server-side @RestOp-annotated methods</h5>
 * <p>
 * Annotation that can be applied to a parameter of a <ja>@RestOp</ja>-annotated method to identify it as an interface for retrieving HTTP parts through a bean interface.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestGet</ja>(<js>"/mypath/{p1}/{p2}/*"</js>)
 * 	<jk>public void</jk> myMethod(<ja>@Request</ja> MyRequest <jv>requestBean</jv>) {...}
 *
 * 	<jk>public interface</jk> MyRequest {
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
 * <p class='bjava'>
 * 	<jc>// Same as above but annotation defined on interface.</jc>
 * 	<ja>@RestGet</ja>(path=<js>"/mypath/{p1}/{p2}/*"</js>)
 * 	<jk>public void</jk> myMethod(MyRequest <jv>requestBean</jv>) {...}
 *
 *	<ja>@Request</ja>
 * 	<jk>public interface</jk> MyRequest {...}
 *
 * <p>
 * The return types of the getters must be the supported parameter types for the HTTP-part annotation used.
 * <br>Schema-based serialization and parsing is allowed just as if used as individual parameter types.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * <h5 class='topic'>Arguments and argument-types of client-side @RemoteResource-annotated interfaces</h5>
 * <p>
 * Annotation applied to Java method arguments of interface proxies to denote a bean with remote resource annotations.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RemoteResource</ja>(path=<js>"/myproxy"</js>)
 * 	<jk>public interface</jk> MyProxy {
 *
 * 		<ja>@RemoteGet</ja>(<js>"/mymethod/{p1}/{p2}"</js>)
 * 		String myProxyMethod(<ja>@Request</ja> MyRequest <jv>requestBean</jv>);
 * 	}
 *
 * 	<jk>public class</jk> MyRequest {
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
 *		<jc>// Schema-based query parameter:  Pipe-delimited lists of comma-delimited lists of integers.</jc>
 * 		<ja>@Query</ja>(
 * 			schema=<ja>@Query</ja>(
 * 				collectionFormat=<js>"pipes"</js>
 * 				items=<ja>@Items</ja>(
 * 					items=<ja>@SubItems</ja>(
 * 						collectionFormat=<js>"csv"</js>
 * 						type=<js>"integer"</js>
 * 					)
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
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrc.Request">@Request</a>
 * </ul>
 * <p>
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@Documented
@Target({PARAMETER,TYPE,METHOD})
@Retention(RUNTIME)
@Inherited
@Repeatable(RequestAnnotation.Array.class)
@ContextApply(RequestAnnotation.Applier.class)
public @interface Request {

	/**
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] on() default {};

	/**
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <p>
	 * Identical to {@link #on()} except allows you to specify class objects instead of a strings.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] onClass() default {};

	/**
	 * Specifies the {@link HttpPartParser} class used for parsing strings to values.
	 *
	 * <p>
	 * Overrides for this part the part parser defined on the REST resource which by default is {@link OpenApiParser}.
	 *
	 * @return The annotation value.
	 */
	Class<? extends HttpPartParser> parser() default HttpPartParser.Void.class;

	/**
	 * Specifies the {@link HttpPartSerializer} class used for serializing values to strings.
	 *
	 * <p>
	 * Overrides for this part the part serializer defined on the REST client which by default is {@link OpenApiSerializer}.
	 *
	 * @return The annotation value.
	 */
	Class<? extends HttpPartSerializer> serializer() default HttpPartSerializer.Void.class;
}
