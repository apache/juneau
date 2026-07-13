/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * REST request dynamic-URL annotation.
 *
 * <p>
 * Identifies a method parameter on a REST proxy interface whose value supplies the entire request URL (or a
 * root-relative URL) for that single call &mdash; the Retrofit <c>@Url</c> pattern.
 *
 * <h5 class='topic'>Arguments of client-side @Remote-annotated interfaces</h5>
 * <p>
 * When the bound argument is non-<jk>null</jk> and non-blank, its string value becomes the effective URL for that
 * invocation, overriding the interface/method-declared path resolution:
 * <ul class='spaced-list'>
 * 	<li>
 * 		If the value contains a scheme (e.g. <js>"http://other-host/path"</js>) it is treated as an <b>absolute</b> URL
 * 		that replaces the whole computed endpoint and bypasses the client root URL.
 * 	<li>
 * 		If the value has no scheme (e.g. <js>"/other/path"</js>) it is treated as a <b>relative</b> URL resolved against
 * 		the client root URL only &mdash; <i>not</i> against the interface {@link org.apache.juneau.http.remote.Remote#path()}
 * 		(matching Retrofit "endpoint replacement").
 * </ul>
 *
 * <p>
 * Any <c>{var}</c> tokens in the supplied value are still filled by the call's {@link Path @Path} parameters, so a
 * caller-supplied URL may contain path templates.  An <ja>@Url</ja> argument takes precedence over
 * {@link org.apache.juneau.http.remote.Remote#path()} + {@code @RemoteOp(path=...)} and over any declarative
 * {@code baseUrl} override.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Remote</ja>(path=<js>"/api"</js>)
 * 	<jk>public interface</jk> MyProxy {
 * 		<jc>// Caller chooses the endpoint at runtime.</jc>
 * 		<ja>@RemoteGet</ja>
 * 		String fetch(<ja>@Url</ja> String <jv>url</jv>);
 * 	}
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		At most one <ja>@Url</ja> parameter is allowed per method (validated at proxy-build time).
 * 	<li class='note'>
 * 		A <jk>null</jk> or blank <ja>@Url</ja> argument is rejected with an {@link IllegalArgumentException}.
 * 	<li class='note'>
 * 		Only <c>http</c>/<c>https</c> schemes are permitted; other schemes (e.g. <c>file:</c>, <c>gopher:</c>) are
 * 		rejected (SSRF guardrail).
 * 	<li class='note'>
 * 		Honored by the next-generation engine (<c>RestClient.remote(...)</c>) only; the classic engine currently
 * 		ignores this annotation.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestProxies">REST Proxy Basics</a>
 * </ul>
 *
 * @since 9.2.1
 */
@Documented
@Target({ PARAMETER })
@Retention(RUNTIME)
@Inherited
public @interface Url {
}
