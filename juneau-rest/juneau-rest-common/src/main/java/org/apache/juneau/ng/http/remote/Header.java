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
 * Binds a remote proxy method parameter to a request header.
 *
 * <p class='bjava'>
 * 	<ja>@Remote</ja>(path=<js>"/api/items"</js>)
 * 	<jk>public interface</jk> ItemService {
 * 		<ja>@RemoteGet</ja>
 * 		String getItems(<ja>@Header</ja>(<js>"Authorization"</js>) String token);
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
public @interface Header {

	/**
	 * The header name.
	 *
	 * @return The header name. If empty, the parameter name is used (requires {@code -parameters} compiler flag).
	 */
	String value() default "";
}
