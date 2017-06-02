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
package org.apache.juneau.remoteable;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.serializer.*;
import org.apache.juneau.urlencoding.*;

/**
 * Annotation applied to Java method arguments of interface proxies to denote that they are QUERY parameters on the request.
 * <p>
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<ja>@Remoteable</ja>(path=<js>"/myproxy"</js>)
 * 	<jk>public interface</jk> MyProxy {
 *
 * 		<jc>// Explicit names specified for query parameters.</jc>
 * 		<jc>// pojo will be converted to UON notation (unless plain-text parts enabled).</jc>
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod1"</js>)
 * 		String myProxyMethod1(<ja>@Query</ja>(<js>"foo"</js>)</ja> String foo, <ja>@Query</ja>(<js>"bar"</js>)</ja> MyPojo pojo);
 *
 * 		<jc>// Multiple values pulled from a NameValuePairs object.</jc>
 * 		<jc>// Same as @Query("*").</jc>
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod2"</js>)
 * 		String myProxyMethod2(<ja>@Query</ja> NameValuePairs nameValuePairs);
 *
 * 		<jc>// Multiple values pulled from a Map.</jc>
 * 		<jc>// Same as @Query("*").</jc>
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod3"</js>)
 * 		String myProxyMethod3(<ja>@Query</ja> Map&lt;String,Object&gt; map);
 *
 * 		<jc>// Multiple values pulled from a bean.</jc>
 * 		<jc>// Same as @Query("*").</jc>
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod4"</js>)
 * 		String myProxyMethod4(<ja>@Query</ja> MyBean myBean);
 *
 * 		<jc>// An entire query string as a String.</jc>
 * 		<jc>// Same as @FQuery("*").</jc>
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod5"</js>)
 * 		String myProxyMethod5(<ja>@Query</ja> String string);
 *
 * 		<jc>// An entire query string as a Reader.</jc>
 * 		<jc>// Same as @Query("*").</jc>
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod6"</js>)
 * 		String myProxyMethod6(<ja>@Query</ja> Reader reader);
 * 	}
 * </p>
 * <p>
 * The annotation can also be applied to a bean property field or getter when the argument is annotated with
 *  {@link RequestBean @RequestBean}:
 * <p>
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<ja>@Remoteable</ja>(path=<js>"/myproxy"</js>)
 * 	<jk>public interface</jk> MyProxy {
 *
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod"</js>)
 * 		String myProxyMethod(<ja>@RequestBean</ja> MyRequestBean bean);
 * 	}
 *
 * 	<jk>public interface</jk> MyRequestBean {
 *
 * 		<jc>// Name explicitly specified.</jc>
 * 		<ja>@Query</ja>(<js>"foo"</js>)
 * 		String getX();
 *
 * 		<jc>// Name inherited from bean property.</jc>
 * 		<jc>// Same as @Query("bar")</jc>
 * 		<ja>@Query</ja>
 * 		String getBar();
 *
 * 		<jc>// Name inherited from bean property.</jc>
 * 		<jc>// Same as @Query("baz")</jc>
 * 		<ja>@Query</ja>
 * 		<ja>@BeanProperty</ja>(<js>"baz"</js>)
 * 		String getY();
 *
 * 		<jc>// Multiple values pulled from NameValuePairs object.</jc>
 * 		<jc>// Same as @Query("*")</jc>
 * 		<ja>@Query</ja>
 * 		NameValuePairs getNameValuePairs();
 *
 * 		<jc>// Multiple values pulled from Map.</jc>
 * 		<jc>// Same as @Query("*")</jc>
 * 		<ja>@Query</ja>
 * 	 	Map&lt;String,Object&gt; getMap();
 *
 * 		<jc>// Multiple values pulled from bean.</jc>
 * 		<jc>// Same as @Query("*")</jc>
 * 		<ja>@Query</ja>
 * 	 	MyBean getMyBean();
 *
 * 		<jc>// An entire query string as a Reader.</jc>
 * 		<jc>// Same as @Query("*")</jc>
 * 		<ja>@Query</ja>
 * 		Reader getReader();
 * 	}
 * </p>
 * <p>
 * The {@link #name()} and {@link #value()} elements are synonyms for specifying the parameter name.  Only one should be used.
 * <br>The following annotations are fully equivalent:
 * <p>
 * <p class='bcode'>
 * 	<ja>@Query</ja>(name=<js>"foo"</js>)
 *
 * 	<ja>@Query</ja>(<js>"foo"</js>)
 * </p>
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../overview-summary.html#Remoteable.3rdParty'>Interface proxies against 3rd-party REST interfaces</a>
 * 	<li class='jp'><a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.remoteable</a>
 * </ul>
 */
@Documented
@Target({PARAMETER,FIELD,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface Query {

	/**
	 * The query parameter name.
	 * <p>
	 * Note that {@link #name()} and {@link #value()} are synonyms.
	 * <p>
	 * The value should be either <js>"*"</js> to represent multiple name/value pairs, or a label that defines the
	 * 	query parameter name.
	 * <p>
	 * A blank value (the default) has the following behavior:
	 * <ul class='spaced-list'>
	 * 	<li>If the data type is <code>NameValuePairs</code>, <code>Map</code>, or a bean,
	 * 		then it's the equivalent to <js>"*"</js> which will cause the value to be serialized as name/value pairs.
	 * 		<h6 class='figure'>Example:</h6>
	 * 		<p class='bcode'>
	 * 	<jc>// When used on a remote method parameter</jc>
	 * 	<ja>@Remoteable</ja>(path=<js>"/myproxy"</js>)
	 * 	<jk>public interface</jk> MyProxy {
	 *
	 * 		<jc>// Equivalent to @Query("*")</jc>
	 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod"</js>)
	 * 		String myProxyMethod1(<ja>@Query</ja> Map&lt;String,Object&gt; formData);
	 * 	}
	 *
	 * 	<jc>// When used on a request bean method</jc>
	 * 	<jk>public interface</jk> MyRequestBean {
	 *
	 * 		<jc>// Equivalent to @Query("*")</jc>
	 * 		<ja>@Query</ja>
	 * 		Map&lt;String,Object&gt; getFoo();
	 * 	}
	 * 		</p>
	 *			<br>
	 * 	<li>If used on a request bean method, uses the bean property name.
	 * 		<h6 class='figure'>Example:</h6>
	 * 		<p class='bcode'>
	 * 	<jk>public interface</jk> MyRequestBean {
	 *
	 * 		<jc>// Equivalent to @Query("foo")</jc>
	 * 		<ja>@Query</ja>
	 * 		String getFoo();
	 * 	}
	 * 		</p>
	 * 	</ul>
	 * </ul>
	 */
	String name() default "";

	/**
	 * A synonym for {@link #name()}.
	 * <p>
	 * Allows you to use shortened notation if you're only specifying the name.
	 */
	String value() default "";

	/**
	 * Skips this value if it's an empty string or empty collection/array.
	 * <p>
	 * Note that <jk>null</jk> values are already ignored.
	 */
	boolean skipIfEmpty() default false;

	/**
	 * Specifies the {@link PartSerializer} class used for serializing values to strings.
	 * <p>
	 * The default value defaults to the using the part serializer defined on the {@link RequestBean} annotation,
	 * 	then on the client which by default is {@link UrlEncodingSerializer}.
	 * <p>
	 * This annotation is provided to allow values to be custom serialized.
	 */
	Class<? extends PartSerializer> serializer() default PartSerializer.class;
}
