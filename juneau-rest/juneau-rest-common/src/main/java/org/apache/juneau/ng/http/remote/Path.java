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

/**
 * Binds a remote proxy method parameter to a URL path variable.
 *
 * <p>
 * The value replaces the corresponding {@code {name}} template variable in the method's path.
 *
 * <p class='bjava'>
 * 	<ja>@Remote</ja>(path=<js>"/api/users"</js>)
 * 	<jk>public interface</jk> UserService {
 * 		<ja>@RemoteGet</ja>(<js>"/{id}"</js>)
 * 		String getUser(<ja>@Path</ja>(<js>"id"</js>) String id);
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
public @interface Path {

	/**
	 * The path variable name to replace.
	 *
	 * @return The path variable name. If empty, the parameter name is used (requires {@code -parameters} compiler flag).
	 */
	String value() default "";
}
