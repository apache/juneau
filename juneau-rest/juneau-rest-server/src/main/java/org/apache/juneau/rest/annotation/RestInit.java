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
package org.apache.juneau.rest.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.rest.*;

import jakarta.servlet.*;

/**
 * Identifies a method that gets called during servlet initialization.
 *
 * <p>
 * This method is called from within the {@link Servlet#init(ServletConfig)} method after the
 * resource's annotation-driven configuration has been applied to the in-flight {@link RestContext}, but before
 * any HTTP request is dispatched. It's the supported hook for one-time setup work such as loading data files
 * or precomputing caches.
 *
 * <p>
 * Method parameters are resolved from the
 * {@link org.apache.juneau.cp.BasicBeanStore bean store} the same way as any other Juneau-injected
 * method. {@link jakarta.servlet.ServletConfig}, {@link jakarta.servlet.ServletContext}, the resource instance
 * itself, and any bean registered via {@link org.apache.juneau.rest.annotation.RestInject @RestInject} or the
 * configured bean-store hooks are all resolvable. Zero-argument variants are also supported.
 *
 * <p>
 * <b>Note (9.5):</b> two related Builder-injection protocols have been removed in this release. They had zero
 * real-world callers across the codebase before deletion (TODO-16 Phase C-3):
 * <ul>
 * 	<li><b>Per-operation:</b> {@code @RestInit public void init(RestOpContext.Builder b)} (invoked once per
 * 		<code>@RestOp</code>-annotated method) — replaced by declarative <code>@RestOp(...)</code> attributes,
 * 		<code>@RestInject(name=, methodScope=)</code>-named beans, or class-level <code>@RestInit</code> hooks.
 * 	<li><b>Class-level Builder injection:</b> {@code @RestInit public void init(RestContext.Builder b)} (which
 * 		injected the in-flight resource-level builder so the hook could imperatively mutate it) — replaced by the
 * 		same declarative surfaces. Migrate by moving each <code>builder.xxx(...)</code> call to the equivalent
 * 		<code>@Rest(xxx=...)</code> annotation attribute or {@code @RestInject}-named bean.
 * </ul>
 *
 * <p>
 * The remaining supported {@code @RestInit} hook shape is one whose parameters are bean-store-resolvable
 * (no {@code RestContext.Builder} or {@code RestOpContext.Builder}). The example below uses the resource
 * instance itself, but {@code @RestInject}-supplied beans, {@code ServletConfig}, etc. work the same way.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(...)
 * 	<jk>public class</jk> PetStoreResource <jk>extends</jk> BasicRestServlet <jk>implements</jk> BasicUniversalJenaConfig {
 *
 * 		<jc>// Our database.</jc>
 * 		<jk>private</jk> Map&lt;Integer,Pet&gt; <jf>petDB</jf>;
 *
 * 		<ja>@RestInit</ja>
 * 		<jk>public void</jk> onInit() <jk>throws</jk> Exception {
 * 			<jc>// Load our database from a local JSON file.</jc>
 * 			<jf>petDB</jf> = JsonParser.<jsf>DEFAULT</jsf>.parse(getClass().getResourceAsStream(<js>"PetStore.json"</js>), LinkedHashMap.<jk>class</jk>, Integer.<jk>class</jk>, Pet.<jk>class</jk>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		The method should return <jk>void</jk> although if it does return any value, the value will be ignored.
 * 	<li class='note'>
 * 		The method should be <jk>public</jk> although other visibilities are valid if the security manager allows it.
 * 	<li class='note'>
 * 		Static methods can be used.
 * 	<li class='note'>
 * 		Multiple init methods can be defined on a class.
 * 		<br>Init methods on parent classes are invoked before init methods on child classes.
 * 		<br>The order of init method invocations within a class is alphabetical, then by parameter count, then by parameter types.
 * 	<li class='note'>
 * 		The method can throw any exception causing initialization of the servlet to fail.
 * 	<li class='note'>
 * 		Note that if you override a parent method, you probably need to call <code><jk>super</jk>.parentMethod(...)</code>.
 * 		<br>The method is still considered part of the parent class for ordering purposes even though it's
 * 		overridden by the child class.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/LifecycleHooks">Lifecycle Hooks</a>
 * </ul>
 */
@Target({ METHOD })
@Retention(RUNTIME)
@Inherited
public @interface RestInit {

	/**
	 * Optional description for the exposed API.
	 *
	 * @return The annotation value.
	 * @since 9.2.0
	 */
	String[] description() default {};
}