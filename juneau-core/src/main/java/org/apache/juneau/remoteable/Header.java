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

import org.apache.juneau.urlencoding.*;

/**
 * Annotation applied to Java method arguments of interface proxies to denote that they are serialized as an HTTP header value.
 * <p>
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<ja>@Remoteable</ja>(path=<js>"/myproxy"</js>)
 * 	<jk>public interface</jk> MyProxy {
 *
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod1"</js>)
 * 		String myProxyMethod1(<ja>@Header</ja>(<js>"Foo"</js>)</ja> String foo, <ja>@Header</ja>(<js>"Bar"</js>)</ja> MyPojo pojo);
 *
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod2"</js>)
 * 		String myProxyMethod2(<ja>@Header</ja> Map&lt;String,Object&gt; headers);
 * 	}
 * </p>
 * <p>
 * The argument can be any of the following types:
 * <ul class='spaced-list'>
 * 	<li><code>NameValuePairs</code> - Individual name-value pairs.
 * 	<li>Any serializable POJO - Converted to text using {@link UrlEncodingSerializer#serializePart(Object, Boolean, Boolean)}.
 * 	<li><code>Map&lt;String,Object&gt;</code> - Individual name-value pairs.
 * 		Values are converted to text using {@link UrlEncodingSerializer#serializePart(Object, Boolean, Boolean)}.
 * 	<li>A bean - Individual name-value pairs.
 * 		Values are converted to text using {@link UrlEncodingSerializer#serializePart(Object, Boolean, Boolean)}.
 * </ul>
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
@Inherited
public @interface Header {

	/**
	 * The HTTP header name.
	 * <p>
	 * A value of <js>"*"</js> indicates the value should be serialized as name/value pairs and is applicable
	 * for the following data types:
	 * <ul>
	 * 	<li><code>NameValuePairs</code>
	 * 	<li><code>Map&lt;String,Object&gt;</code>
	 * 	<li>A bean
	 * </ul>
	 */
	String value() default "*";
}
