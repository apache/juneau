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

import java.io.*;
import java.lang.annotation.*;

import org.apache.juneau.serializer.*;

/**
 * Annotation applied to Java method arguments of interface proxies to denote that they are the HTTP body of the request.
 * 
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<ja>@Remoteable</ja>(path=<js>"/myproxy"</js>)
 * 	<jk>public interface</jk> MyProxy {
 * 
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod"</js>)
 * 		String myProxyMethod(<ja>@Body</ja> MyPojo pojo);
 * 	}
 * </p>
 * 
 * <p>
 * The argument can be any of the following types:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Any serializable POJO - Converted to text using the {@link Serializer} registered with the
 * 		<code>RestClient</code>.
 * 	<li>
 * 		{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
 * 	<li>
 * 		{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
 * 	<li>
 * 		<code>HttpEntity</code> - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
 * 	<li>
 * 		<code>NameValuePairs</code> - Converted to a URL-encoded FORM post.
 * </ul>
 * 
 * <p>
 * The annotation can also be applied to a bean property field or getter when the argument is annotated with
 * {@link RequestBean @RequestBean}:
 * 
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
 * 		<ja>@Body</ja>
 * 		MyPojo getMyPojo();
 * 	}
 * </p>
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../overview-summary.html#juneau-rest-client.3rdPartyProxies'>Overview &gt; juneau-rest-client &gt; Interface Proxies Against 3rd-party REST Interfaces</a>
 * </ul>
 */
@Documented
@Target({PARAMETER,FIELD,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface Body {}
