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

import org.apache.juneau.httppart.*;
import org.apache.juneau.urlencoding.*;

/**
 * @deprecated Use {@link org.apache.juneau.http.annotation.Header}
 */
@Deprecated
@Documented
@Target({PARAMETER,FIELD,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface Header {

	/**
	 * The HTTP header name.
	 *
	 * <p>
	 * A blank value (the default) indicates to reuse the bean property name when used on a request bean property.
	 *
	 * <p>
	 * The value should be either <js>"*"</js> to represent multiple name/value pairs, or a label that defines the
	 * header name.
	 *
	 * <p>
	 * A blank value (the default) has the following behavior:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		If the data type is <code>NameValuePairs</code>, <code>Map</code>, or a bean,
	 * 		then it's the equivalent to <js>"*"</js> which will cause the value to be serialized as name/value pairs.
	 *
	 * 		<h5 class='figure'>Example:</h5>
	 * 		<p class='bcode'>
	 * 	<jc>// When used on a remote method parameter</jc>
	 * 	<ja>@Remoteable</ja>(path=<js>"/myproxy"</js>)
	 * 	<jk>public interface</jk> MyProxy {
	 *
	 * 		<jc>// Equivalent to @Header("*")</jc>
	 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod"</js>)
	 * 		String myProxyMethod1(<ja>@Header</ja> Map&lt;String,Object&gt; headers);
	 * 	}
	 *
	 * 	<jc>// When used on a request bean method</jc>
	 * 	<jk>public interface</jk> MyRequestBean {
	 *
	 * 		<jc>// Equivalent to @Header("*")</jc>
	 * 		<ja>@Header</ja>
	 * 		Map&lt;String,Object&gt; getFoo();
	 * 	}
	 * 		</p>
	 * 	</li>
	 * 	<li>
	 * 		If used on a request bean method, uses the bean property name.
	 *
	 * 		<h5 class='figure'>Example:</h5>
	 * 		<p class='bcode'>
	 * 	<jk>public interface</jk> MyRequestBean {
	 *
	 * 		<jc>// Equivalent to @Header("Foo")</jc>
	 * 		<ja>@Header</ja>
	 * 		<ja>@BeanProperty</ja>(<js>"Foo"</js>)
	 * 		String getFoo();
	 * 	}
	 * 		</p>
	 * 	</li>
	 * </ul>
	 */
	String name() default "";

	/**
	 * A synonym for {@link #name()}.
	 *
	 * <p>
	 * Allows you to use shortened notation if you're only specifying the name.
	 */
	String value() default "";

	/**
	 * Skips this value if it's an empty string or empty collection/array.
	 *
	 * <p>
	 * Note that <jk>null</jk> values are already ignored.
	 */
	boolean skipIfEmpty() default false;

	/**
	 * Specifies the {@link HttpPartSerializer} class used for serializing values to strings.
	 *
	 * <p>
	 * The default value defaults to the using the part serializer defined on the {@link RequestBean @RequestBean} annotation,
	 * then on the client which by default is {@link UrlEncodingSerializer}.
	 *
	 * <p>
	 * This annotation is provided to allow values to be custom serialized.
	 */
	Class<? extends HttpPartSerializer> serializer() default HttpPartSerializer.Null.class;
}
