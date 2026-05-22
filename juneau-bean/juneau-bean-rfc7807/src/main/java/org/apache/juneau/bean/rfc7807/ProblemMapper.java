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
package org.apache.juneau.bean.rfc7807;

/**
 * SPI for mapping a domain or framework {@link Throwable} into an
 * <a class="doclink" href="https://www.rfc-editor.org/rfc/rfc7807">RFC 7807 / 9457</a> {@link Problem} bean.
 *
 * <p>
 * Implementations contribute domain-specific error translation without subclassing the request-processor chain.
 * A {@code ProblemMapper} bean is registered via the resource bean store (e.g. a {@code @Bean ProblemMapper foo()}
 * factory method on a {@code @Rest}-annotated resource, or any other mechanism that contributes named beans to
 * the resource's bean store); the server-side processor picks it up automatically when an opted-in operation
 * throws or returns a matching exception.
 *
 * <h5 class='section'>Discovery model</h5>
 * <p>
 * The processor collects all {@code ProblemMapper} beans visible from the resource's bean store, then picks the
 * <b>most-specific</b> mapper for the exception class hierarchy (a mapper whose {@link #getExceptionType()} is the
 * closest superclass of the thrown exception wins; ties are broken by registration order, with the bean-store
 * walk order serving as the tiebreaker). Mappers that return {@code null} are skipped and the next-most-specific
 * mapper is tried, finally falling back to the default {@code ProblemAdapters.fromException(...)} adaptation in
 * {@code juneau-rest-common} for {@code BasicHttpException} subclasses.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>public class</jk> MyDomainProblemMapper <jk>implements</jk> ProblemMapper&lt;MyDomainException&gt; {
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> Class&lt;MyDomainException&gt; getExceptionType() {
 * 			<jk>return</jk> MyDomainException.<jk>class</jk>;
 * 		}
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> Problem map(MyDomainException <jv>e</jv>) {
 * 			<jk>return</jk> Problem.<jsm>fromStatus</jsm>(422, <js>"Domain rule violated"</js>, <jv>e</jv>.getMessage())
 * 				.setType(<jk>new</jk> URI(<js>"https://example.com/probs/domain"</js>))
 * 				.set(<js>"rule"</js>, <jv>e</jv>.getRuleId());
 * 		}
 * 	}
 *
 * 	<ja>@Rest</ja>(problemDetails=<js>"true"</js>)
 * 	<jk>public class</jk> MyResource {
 *
 * 		<jc>// Bean-store registration: factory method on the resource class.</jc>
 * 		<ja>@Bean</ja> <jk>public</jk> ProblemMapper&lt;MyDomainException&gt; myMapper() {
 * 			<jk>return new</jk> MyDomainProblemMapper();
 * 		}
 *
 * 		<ja>@RestGet</ja>(<js>"/buy"</js>)
 * 		<jk>public</jk> Receipt buy() {
 * 			<jk>throw new</jk> MyDomainException(...);  <jc>// translated by myMapper()</jc>
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Problem}
 * 	<li class='jc'>{@link ProblemException}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerProblemDetails">REST Server &mdash; RFC 7807 problem-details</a>
 * </ul>
 *
 * @param <T> The {@link Throwable} subtype this mapper handles.
 */
public interface ProblemMapper<T extends Throwable> {

	/**
	 * Returns the exception type this mapper handles.
	 *
	 * <p>
	 * The processor matches mappers by checking
	 * {@code getExceptionType().isAssignableFrom(thrown.getClass())} for each registered mapper, then picks the
	 * mapper whose {@code getExceptionType()} sits closest to the thrown class in the hierarchy. Implementations
	 * usually return the same class their {@code <T>} parameter binds; declaring it explicitly here avoids
	 * reflective inspection of the generic type at runtime.
	 *
	 * @return The exception class this mapper handles. Must not be <jk>null</jk>.
	 */
	Class<T> getExceptionType();

	/**
	 * Maps the supplied exception to a {@link Problem} bean.
	 *
	 * <p>
	 * Returning <jk>null</jk> signals "no opinion on this exception"; the processor then tries the next-most-specific
	 * registered mapper (and, ultimately, the built-in default fallback). Returning a populated {@link Problem}
	 * causes the processor to emit that bean as {@code application/problem+json}.
	 *
	 * @param exception The exception to translate. Never <jk>null</jk>.
	 * @return The {@link Problem} bean to emit, or <jk>null</jk> to defer to the next mapper.
	 */
	Problem map(T exception);
}
