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
package org.apache.juneau.ng.http.remote;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.ng.http.HttpBody;

/**
 * Marks a remote proxy method parameter as the HTTP request body.
 *
 * <p>
 * The parameter value is serialized as the request body. If the value is an
 * {@link HttpBody}, it is used directly; otherwise,
 * it is converted via {@code String.valueOf()} with content type {@code text/plain}.
 *
 * <p class='bjava'>
 * 	<ja>@Remote</ja>(path=<js>"/api/users"</js>)
 * 	<jk>public interface</jk> UserService {
 * 		<ja>@RemotePost</ja>
 * 		String createUser(<ja>@Body</ja> String json);
 *
 * 		<ja>@RemotePut</ja>(<js>"/{id}"</js>)
 * 		void updateUser(<ja>@Path</ja>(<js>"id"</js>) String id, <ja>@Body</ja> HttpBody body);
 * 	}
 * </p>
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 *
 * @since 9.2.1
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Body {
}
