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
package org.apache.juneau.commons.function;

/**
 * A universal factory interface used with <ja>@Bean</ja><c>(factory=X.class)</c> for any bean type the
 * framework needs to instantiate.
 *
 * <p>
 * This interface provides a single DI hook for all framework-managed classes including
 * {@link BeanConsumer}, {@link BeanSupplier}, {@link BeanChannel}, {@code ObjectSwap} subclasses,
 * and any ordinary bean class parsed from JSON/XML/etc.
 *
 * <p>
 * When a class annotated with <ja>@Bean</ja><c>(factory=MyFactory.class)</c> needs to be instantiated,
 * the framework resolves the factory as follows:
 * <ol>
 * 	<li>Look up the factory class in the {@code BeanStore} (e.g. Spring {@code ApplicationContext})
 * 	<li>If not found in the store, attempt to instantiate the factory directly via no-arg constructor
 * 	    or {@code getInstance()} static method
 * 	<li>If both fail, throw {@link IllegalArgumentException}
 * </ol>
 *
 * <h5 class='section'>Example (Spring integration):</h5>
 * <p class='bjava'>
 * 	<jc>// Spring singleton factory that creates per-request ItemChannel instances</jc>
 * 	<ja>@Component</ja>
 * 	<jk>public class</jk> ItemChannelFactory <jk>implements</jk> BeanFactory&lt;ItemChannel&gt; {
 * 		<ja>@Autowired</ja> DataSource <jv>ds</jv>;
 *
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> ItemChannel create() {
 * 			<jk>return new</jk> ItemChannel(<jv>ds</jv>);
 * 		}
 * 	}
 *
 * 	<jc>// The target class declares which factory creates it</jc>
 * 	<ja>@Bean</ja>(factory=ItemChannelFactory.<jk>class</jk>)
 * 	<jk>public class</jk> ItemChannel <jk>implements</jk> BeanChannel&lt;Item&gt; { ... }
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BeanConsumer} - Parse-only lifecycle interface
 * 	<li class='jc'>{@link BeanSupplier} - Serialize-only lifecycle interface
 * 	<li class='jc'>{@link BeanChannel} - Round-trip lifecycle interface
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Marshalling">Marshalling</a>
 * </ul>
 *
 * @param <T> The type of bean this factory creates.
 */
@FunctionalInterface
public interface BeanFactory<T> {

	/**
	 * Creates a new instance of the bean.
	 *
	 * @return A new bean instance.
	 * @throws Exception If the bean cannot be created.
	 */
	T create() throws Exception;

	/**
	 * Sentinel class used as the default value for {@code @Bean(factory=...)} and
	 * {@code @Beanp(factory=...)} when no factory is specified.
	 */
	@SuppressWarnings({
		"rawtypes" // Raw type required for use as annotation sentinel
	})
	final class Void implements BeanFactory {
		private Void() {}

		@Override
		public Object create() {
			throw new UnsupportedOperationException("BeanFactory.Void is a sentinel and cannot be instantiated.");
		}
	}
}
