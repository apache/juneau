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
package org.apache.juneau.httppart;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Specifies the {@link HttpPartSerializer} and {@link HttpPartParser} to use for serializing/parsing HTTP parts.
 *
 * <p>
 * Can be applied alongside parameter annotations such as <ja>@Query</ja>, <ja>@Header</ja>, <ja>@FormData</ja>,
 * <ja>@Path</ja>, <ja>@Request</ja>, and <ja>@Response</ja> to override the default part serializer/parser
 * for that specific parameter.
 *
 * <p>
 * This annotation is used by the classic {@code RestClient} remote-proxy path.
 * The next-generation {@code NgRestClient} does not use this annotation.
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
