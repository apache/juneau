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
package org.apache.juneau.commons.inject;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Bean injection annotation.
 *
 * <p>
 * Used on methods and fields of {@link BeanStore}-managed objects to denote methods and fields that override and
 * customize beans used by the framework.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	<jc>// Rest resource that uses a customized call logger.</jc>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyRest <jk>extends</jk> BasicRestServlet {
 *
 * 		<jc>// Option #1:  As a field.</jc>
 * 		<ja>@Bean</ja>
 * 		CallLogger <jf>myCallLogger</jf> = CallLogger.<jsm>create</jsm>().logger(<js>"mylogger"</js>).build();
 *
 * 		<jc>// Option #2:  As a method.</jc>
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> CallLogger myCallLogger() {
 * 			<jk>return</jk> CallLogger.<jsm>create</jsm>().logger(<js>"mylogger"</js>).build();
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * 	The {@link Bean#name()}/{@link Bean#value()} attributes are used to differentiate between named beans.
 * </p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	<jc>// Customized default request headers.</jc>
 * 	<ja>@Bean</ja>(<js>"defaultRequestHeaders"</js>)
 * 	HeaderList <jf>defaultRequestHeaders</jf> = HeaderList.<jsm>create</jsm>().set(ContentType.<jsf>TEXT_PLAIN</jsf>).build();
 *
 * 	<jc>// Customized default response headers.</jc>
 * 	<ja>@Bean</ja>(<js>"defaultResponseHeaders"</js>)
 * 	HeaderList <jf>defaultResponseHeaders</jf> = HeaderList.<jsm>create</jsm>().set(ContentType.<jsf>TEXT_PLAIN</jsf>).build();
 * </p>
 *
 * <p>
 * 	The {@link Bean#methodScope()} attribute is used to define beans in the scope of specific {@code @RestOp}-annotated methods.
 * </p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	<jc>// Set a default header on a specific REST method.</jc>
 * 	<jc>// Input parameter is the default header list builder with all annotations applied.</jc>
 * 	<ja>@Bean</ja>(name=<js>"defaultRequestHeaders"</js>, methodScope=<js>"myRestMethod"</js>)
 * 	<jk>public</jk> HeaderList.Builder myRequestHeaders(HeaderList.Builder <jv>builder</jv>) {
 * 		<jk>return</jk> <jv>builder</jv>.set(ContentType.<jsf>TEXT_PLAIN</jsf>);
 * 	}
 *
 * 	<jc>// Method that picks up default header defined above.</jc>
 * 	<ja>@RestGet</ja>
 * 	<jk>public</jk> Object myRestMethod(ContentType <jv>contentType</jv>) { ... }
 * </p>
 *
 * <p>
 * 	This annotation can also be used to inject arbitrary beans into the bean store which allows them to be
 * 	passed as resolved parameters on {@code @RestOp}-annotated methods.
 * </p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	<jc>// Custom beans injected into the bean store.</jc>
 * 	<ja>@Bean</ja> MyBean <jv>myBean1</jv> = <jk>new</jk> MyBean();
 * 	<ja>@Bean</ja>(<js>"myBean2"</js>) MyBean <jv>myBean2</jv> = <jk>new</jk> MyBean();
 *
 * 	<jc>// Method that uses injected beans.</jc>
 * 	<ja>@RestGet</ja>
 * 	<jk>public</jk> Object doGet(MyBean <jv>myBean1</jv>, <ja>@Name</ja>(<js>"myBean2"</js>) MyBean <jv>myBean2</jv>) { ... }
 * </p>
 *
 * <p>
 * 	This annotation can also be used on uninitialized fields.  When fields are uninitialized, they will
 * 	be set during initialization based on beans found in the bean store.
 * </p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	<jc>// Fields that get set during initialization based on beans found in the bean store.</jc>
 * 	<ja>@Bean</ja> CallLogger <jf>callLogger</jf>;
 * 	<ja>@Bean</ja> BeanStore <jf>beanStore</jf>;  <jc>// Note that the BeanStore itself can be accessed this way.</jc>
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>Methods and fields can be static or non-static.
 * 	<li class='note'>Any injectable beans (including spring beans) can be passed as arguments into methods.
 * 	<li class='note'>Bean names are required when multiple beans of the same type exist in the bean store.
 * 	<li class='note'>By default, the injected bean scope is class-level (applies to the entire class).  The
 * 		{@link Bean#methodScope()} annotation can be used to apply to method-level only (when applicable).
 * </ul>
 *
 * <h5 class='section'>Precedence (since 9.5.0):</h5>
 * <p>
 * 	{@code @Bean} acts as a <i>programmable default</i>, analogous to Spring's
 * 	<c>&#64;ConditionalOnMissingBean</c>.  When a REST context resolves a framework-managed bean
 * 	(<c>CallLogger</c>, <c>EncoderSet</c>, <c>SerializerSet</c>, <c>ParserSet</c>, <c>ThrownStore</c>,
 * 	<c>Config</c>, <c>VarResolver</c>, <c>HttpPartSerializer</c>, <c>HttpPartParser</c>, etc.), the lookup
 * 	walks the following tiers in order, returning the first hit:
 * </p>
 * <ol>
 * 	<li><b>Overriding-parent bean store</b> — Spring beans (in <c>juneau-rest-server-springboot</c> deployments,
 * 		via <c>SpringBeanStore</c>), or any bean reachable through the configured overriding-parent
 * 		bean-store chain.</li>
 * 	<li><b>{@code @Bean} method/field on the resource class</b> — registered as a regular bean-store
 * 		entry, beating the framework default.</li>
 * 	<li><b>Memoizer-backed framework default</b> — built into the context as a default supplier.</li>
 * </ol>
 * <p>
 * 	In other words: a Spring <c>&#64;Bean</c> of type <c>CallLogger</c> wins over a {@code @Bean CallLogger}
 * 	method on the same servlet, which in turn wins over the framework's built-in <c>BasicCallLogger</c>.  Non-Spring
 * 	deployments have an empty overriding-parent layer, so the chain naturally collapses to
 * 	{@code @Bean > default}.
 * </p>
 * <p>
 * 	Prior to 9.5 the order was {@code @Bean > Spring > default}.  See the 9.5.0 release notes for migration
 * 	guidance if you need to keep that legacy behavior (typically by removing the Spring <c>&#64;Bean</c> or marking the
 * 	{@code @Bean} method's type with a Spring-native override such as <c>&#64;Primary</c>).
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BeanStore}
 * 	<li class='jc'>{@link WritableBeanStore}
 * </ul>
 */
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
@Inherited
public @interface Bean {

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	String[] description() default {};

	/**
	 * The short names of the methods that this annotation applies to.
	 *
	 * <p>
	 * Can use <js>"*"</js> to apply to all methods.
	 *
	 * <p>
	 * Ignored for class-level scope.
	 *
	 * @return The short names of the methods that this annotation applies to, or empty if class-scope.
	 */
	String[] methodScope() default {};

	/**
	 * The bean name to use to distinguish beans of the same type for different purposes.
	 *
	 * <p>
	 * For example, there are two {@code HeaderList} beans:  <js>"defaultRequestHeaders"</js> and <js>"defaultResponseHeaders"</js>.  This annotation
	 * would be used to differentiate between them.
	 *
	 * @return The bean name to use to distinguish beans of the same type for different purposes, or blank if bean type is unique.
	 */
	String name() default "";

	/**
	 * Same as {@link #name()}.
	 *
	 * @return The bean name to use to distinguish beans of the same type for different purposes, or blank if bean type is unique.
	 */
	String value() default "";

	/**
	 * Optional ordering value used when exporting ordered bean collections.
	 *
	 * <p>
	 * Lower values are higher precedence.  If both {@link Order} and this value are present on the same
	 * contribution source, {@link Order} wins.
	 *
	 * @return The priority value.
	 */
	int priority() default Integer.MAX_VALUE / 2;
}
