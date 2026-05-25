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
package org.apache.juneau.rest.auth;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;
import java.security.*;

/**
 * Marks a {@code @RestOp}-method parameter as the authenticated {@link Principal} for the current request.
 *
 * <p>
 * When a request reaches an op handler annotated with {@code @Auth}, the framework's
 * {@link AuthArg} resolver pulls the {@link Principal} previously stashed by an upstream AuthN guard
 * (such as {@link BearerTokenGuard} or {@link ApiKeyGuard}) on the request attributes under the
 * {@link org.apache.juneau.rest.RestServerConstants#PRINCIPAL_ATTR PRINCIPAL_ATTR} key.
 *
 * <p>
 * If an annotated parameter is reached without a principal being stashed (i.e. no guard ran, or the
 * request was made on an unguarded path) the resolver returns <jk>null</jk> &mdash; the guard chain is
 * the contract that guarantees a non-null value; this annotation only requests the lookup.
 *
 * <h5 class='section'>Type-driven equivalent:</h5>
 *
 * <p>
 * For convenience, the framework also resolves a bare {@link Principal} (or {@link ClaimsPrincipal})
 * parameter <i>without</i> any annotation &mdash; the type alone is enough to trigger the same lookup.
 * Use {@code @Auth} when you want the intent to be self-documenting on the parameter list or you want
 * to disambiguate against another arg resolver that might claim the parameter first.
 *
 * <h5 class='section'>Example:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@RestGet</ja>(path=<js>"/me"</js>)
 * 	<jk>public</jk> Profile me(<ja>@Auth</ja> Principal <jv>p</jv>) {
 * 		<jk>return</jk> <jf>profileService</jf>.lookup(<jv>p</jv>.getName());
 * 	}
 *
 * 	<ja>@RestGet</ja>(path=<js>"/claims"</js>)
 * 	<jk>public</jk> String scope(<ja>@Auth</ja> ClaimsPrincipal <jv>p</jv>) {
 * 		<jk>return</jk> <jv>p</jv>.getClaim(<js>"scope"</js>, String.<jk>class</jk>).orElse(<js>""</js>);
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link AuthArg}
 * 	<li class='jc'>{@link BearerTokenGuard}
 * 	<li class='jc'>{@link ApiKeyGuard}
 * 	<li class='jc'>{@link ClaimsPrincipal}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/AuthGuards">AuthN Guards</a>
 * </ul>
 *
 * @since 9.5.0
 */
@Target(PARAMETER)
@Retention(RUNTIME)
@Documented
public @interface Auth {
}
