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
package org.apache.juneau.marshall.httppart;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Specifies the {@link HttpPartSerializer} and {@link HttpPartParser} to use for serializing/parsing HTTP parts.
 *
 * <p>
 * Can be applied alongside the HTTP-part parameter annotations <ja>@Query</ja>, <ja>@Header</ja>, <ja>@FormData</ja>,
 * and <ja>@Path</ja> to override the default part serializer for that specific value.  It may also be declared at the
 * method or interface level to supply a default for all parts of that scope.
 *
 * <p>
 * The next-generation {@code org.apache.juneau.rest.client.RestClient} remote-proxy engine
 * ({@code RestClient.remote(Class)}) honors the {@link #serializer()} attribute for outgoing HTTP <i>parts</i>
 * (query / header / path / form-data), resolving the most-specific declaration: parameter-level overrides
 * method-level, which overrides interface-level.  When absent, the client's default part serializer is used
 * (behavior is unchanged).
 *
 * <p>
 * <b>Not currently consumed:</b> the {@link #parser()} attribute (the next-gen engine has no response-part parsing
 * path; response bodies are handled by the client {@code Parser}s, not part parsers), the request body (<ja>@Content</ja>,
 * which uses a full {@code Serializer}/{@code Parser} rather than a part serializer/parser), and the classic
 * {@code RestClient} remote-proxy path (which does not discover this annotation).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RemoteGet</ja>(<js>"/mymethod"</js>)
 * 	String myMethod(<ja>@Query</ja>(<js>"foo"</js>) <ja>@HttpPartMarshalling</ja>(serializer=MySerializer.<jk>class</jk>) String <jv>foo</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HttpPartSerializersParsers">HTTP Part Serializers and Parsers</a>
 * </ul>
 */
@Documented
@Target({ PARAMETER, METHOD, TYPE, FIELD })
@Retention(RUNTIME)
public @interface HttpPartMarshalling {

	/**
	 * Specifies the {@link HttpPartParser} class used for parsing strings to values.
	 *
	 * @return The annotation value.
	 */
	Class<? extends HttpPartParser> parser() default HttpPartParser.Void.class;

	/**
	 * Specifies the {@link HttpPartSerializer} class used for serializing values to strings.
	 *
	 * @return The annotation value.
	 */
	Class<? extends HttpPartSerializer> serializer() default HttpPartSerializer.Void.class;
}
