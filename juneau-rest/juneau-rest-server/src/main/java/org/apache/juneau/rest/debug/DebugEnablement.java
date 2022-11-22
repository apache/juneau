// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.debug;

import static org.apache.juneau.Enablement.*;
import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import java.lang.reflect.Method;
import java.util.function.*;

import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.utils.*;

/**
 * Interface used for selectively turning on debug per request.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.LoggingAndDebugging">Logging / Debugging</a>
 * </ul>
 */
public abstract class DebugEnablement {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Represents no DebugEnablement.
	 */
	public abstract class Void extends DebugEnablement {
		Void(BeanStore beanStore) {
			super(beanStore);
		}
	};

	/**
	 * Static creator.
	 *
	 * @param beanStore The bean store to use for creating beans.
	 * @return A new builder for this object.
	 */
	public static Builder create(BeanStore beanStore) {
		return new Builder(beanStore);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	public static class Builder {

		ReflectionMap.Builder<Enablement> mapBuilder;
		Enablement defaultEnablement = NEVER;
		Predicate<HttpServletRequest> conditional;
		BeanCreator<DebugEnablement> creator;

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BeanStore beanStore) {
			mapBuilder = ReflectionMap.create(Enablement.class);
			defaultEnablement = NEVER;
			conditional = x -> "true".equalsIgnoreCase(x.getHeader("Debug"));
			creator = beanStore.createBean(DebugEnablement.class).type(BasicDebugEnablement.class).builder(Builder.class, this);
		}

		/**
		 * Creates a new {@link DebugEnablement} object from this builder.
		 *s
		 * <p>
		 * Instantiates an instance of the {@link #type(Class) implementation class} or
		 * else {@link BasicDebugEnablement} if implementation class was not specified.
		 *
		 * @return A new {@link DebugEnablement} object.
		 */
		public DebugEnablement build() {
			try {
				return creator.run();
			} catch (Exception e) {
				throw new InternalServerError(e);
			}
		}

		/**
		 * Specifies a subclass of {@link DebugEnablement} to create when the {@link #build()} method is called.
		 *
		 * @param value The new value for this setting.
		 * @return  This object.
		 */
		public Builder type(Class<? extends DebugEnablement> value) {
			creator.type(value == null ? BasicDebugEnablement.class : value);
			return this;
		}

		/**
		 * Specifies an already-instantiated bean for the {@link #build()} method to return.
		 *
		 * @param value The setting value.
		 * @return This object.
		 */
		public Builder impl(DebugEnablement value) {
			creator.impl(value);
			return this;
		}

		/**
		 * Enables or disables debug on the specified classes and/or methods.
		 *
		 * <p>
		 * Allows you to target specified debug enablement on specified classes and/or methods.
		 *
		 * @param enablement
		 * 	The debug enablement setting to set on the specified classes/methods.
		 * 	<br>Can be any of the following:
		 * 	<ul>
		 * 		<li>{@link Enablement#ALWAYS ALWAYS} - Debug is always enabled.
		 * 		<li>{@link Enablement#NEVER NEVER} - Debug is always disabled.
		 * 		<li>{@link Enablement#CONDITIONAL CONDITIONAL} - Debug is enabled when the {@link #conditional(Predicate)} conditional predicate test} passes.
		 * 	</ul>
		 * @param keys
		 * 	The mapping keys.
		 * 	<br>Can be any of the following:
		 * 	<ul>
		 * 		<li>Full class name (e.g. <js>"com.foo.MyClass"</js>).
		 * 		<li>Simple class name (e.g. <js>"MyClass"</js>).
		 * 		<li>All classes (e.g. <js>"*"</js>).
		 * 		<li>Full method name (e.g. <js>"com.foo.MyClass.myMethod"</js>).
		 * 		<li>Simple method name (e.g. <js>"MyClass.myMethod"</js>).
		 * 		<li>A comma-delimited list of anything on this list.
		 * 	</ul>
		 * @return This object.
		 */
		public Builder enable(Enablement enablement, String...keys) {
			for (String k : keys)
				mapBuilder.append(k, enablement);
			return this;
		}

		/**
		 * Enables or disables debug on the specified classes.
		 *
		 * <p>
		 * Identical to {@link #enable(Enablement, String...)} but allows you to specify specific classes.
		 *
		 * @param enablement
		 * 	The debug enablement setting to set on the specified classes/methods.
		 * 	<br>Can be any of the following:
		 * 	<ul>
		 * 		<li>{@link Enablement#ALWAYS ALWAYS} - Debug is always enabled.
		 * 		<li>{@link Enablement#NEVER NEVER} - Debug is always disabled.
		 * 		<li>{@link Enablement#CONDITIONAL CONDITIONAL} - Debug is enabled when the {@link #conditional(Predicate)} conditional predicate test} passes.
		 * 	</ul>
		 * @param classes
		 * 	The classes to set the debug enablement setting on.
		 * @return This object.
		 */
		public Builder enable(Enablement enablement, Class<?>...classes) {
			for (Class<?> c : classes)
				mapBuilder.append(c.getName(), enablement);
			return this;
		}

		/**
		 * Specifies the default debug enablement setting if not overridden per class/method.
		 *
		 * <p>
		 * The default value for this setting is {@link Enablement#NEVER NEVER}.
		 *
		 * @param value The default debug enablement setting if not overridden per class/method.
		 * @return This object.
		 */
		public Builder defaultEnable(Enablement value) {
			defaultEnablement = value;
			return this;
		}

		/**
		 * Specifies the predicate to use for conditional debug enablement.
		 *
		 * <p>
		 * Specifies the predicate to use to determine whether debug is enabled when the resolved enablement value
		 * is {@link Enablement#CONDITIONAL CONDITIONAL}.
		 *
		 * <p>
		 * The default value for this setting is <c>(<jv>x</jv>)-&gt;<js>"true"</js>.equalsIgnoreCase(<jv>x</jv>.getHeader(<js>"Debug"</js>))</c>.
		 *
		 * @param value The predicate.
		 * @return This object.
		 */
		public Builder conditional(Predicate<HttpServletRequest> value) {
			conditional = value;
			return this;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Enablement defaultEnablement;
	private final ReflectionMap<Enablement> enablementMap;
	private final Predicate<HttpServletRequest> conditionalPredicate;

	/**
	 * Constructor.
	 * <p>
	 * Subclasses typically override the {@link #init(BeanStore)} method when using this constructor.
	 *
	 * @param beanStore The bean store containing injectable beans for this enablement.
	 */
	public DebugEnablement(BeanStore beanStore) {
		Builder builder = init(beanStore);
		this.defaultEnablement = firstNonNull(builder.defaultEnablement, NEVER);
		this.enablementMap = builder.mapBuilder.build();
		this.conditionalPredicate = firstNonNull(builder.conditional, x -> "true".equalsIgnoreCase(x.getHeader("Debug")));
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this enablement.
	 */
	public DebugEnablement(Builder builder) {
		this.defaultEnablement = firstNonNull(builder.defaultEnablement, NEVER);
		this.enablementMap = builder.mapBuilder.build();
		this.conditionalPredicate = firstNonNull(builder.conditional, x -> "true".equalsIgnoreCase(x.getHeader("Debug")));

	}

	/**
	 * Initializer.
	 * <p>
	 * Subclasses should override this method to make modifications to the builder used to create this logger.
	 *
	 * @param beanStore The bean store containing injectable beans for this logger.
	 * @return A new builder object.
	 */
	protected Builder init(BeanStore beanStore) {
		return new Builder(beanStore);
	}

	/**
	 * Returns <jk>true</jk> if debug is enabled on the specified class and request.
	 *
	 * <p>
	 * This enables debug mode on requests once the matched class is found and before the
	 * Java method is found.
	 *
	 * @param context The context of the {@link Rest}-annotated class.
	 * @param req The HTTP request.
	 * @return <jk>true</jk> if debug is enabled on the specified method and request.
	 */
	public boolean isDebug(RestContext context, HttpServletRequest req) {
		Class<?> c = context.getResourceClass();
		Enablement e = enablementMap.find(c).orElse(defaultEnablement);
		return e == ALWAYS || (e == CONDITIONAL && isConditionallyEnabled(req));
	}

	/**
	 * Returns <jk>true</jk> if debug is enabled on the specified method and request.
	 *
	 * <p>
	 * This enables debug mode after the Java method is found and allows you to enable
	 * debug on individual Java methods instead of the entire class.
	 *
	 * @param context The context of the {@link RestOp}-annotated method.
	 * @param req The HTTP request.
	 * @return <jk>true</jk> if debug is enabled on the specified method and request.
	 */
	public boolean isDebug(RestOpContext context, HttpServletRequest req) {
		Method m = context.getJavaMethod();
		Enablement e = enablementMap.find(m).orElse(enablementMap.find(m.getDeclaringClass()).orElse(defaultEnablement));
		return e == ALWAYS || (e == CONDITIONAL && isConditionallyEnabled(req));
	}

	/**
	 * Returns <jk>true</jk> if debugging is conditionally enabled on the specified request.
	 *
	 * <p>
	 * This method only gets called when the enablement value resolves to {@link Enablement#CONDITIONAL CONDITIONAL}.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own implementation.
	 * The default implementation is provided by {@link DebugEnablement.Builder#conditional(Predicate)}
	 * which has a default predicate of <c><jv>x</jv> -&gt; <js>"true"</js>.equalsIgnoreCase(<jv>x</jv>.getHeader(<js>"Debug"</js>)</c>.
	 *
	 * @param req The incoming HTTP request.
	 * @return <jk>true</jk> if debugging is conditionally enabled on the specified request.
	 */
	protected boolean isConditionallyEnabled(HttpServletRequest req) {
		return conditionalPredicate.test(req);
	}

	@Override /* Object */
	public String toString() {
		return filteredMap()
			.append("defaultEnablement", defaultEnablement)
			.append("enablementMap", enablementMap)
			.append("conditionalPredicate", conditionalPredicate)
			.asString();
	}
}
