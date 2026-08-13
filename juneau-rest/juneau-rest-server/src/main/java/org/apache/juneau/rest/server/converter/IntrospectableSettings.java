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
package org.apache.juneau.rest.server.converter;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.reflect.*;

/**
 * Deny-by-default allow-list settings for the {@link Introspectable} converter.
 *
 * <p>
 * 	Reflective method dispatch via {@link Introspectable} is <b>off by default</b> on security grounds &mdash;
 * 	unless explicitly allow-listed, invoking arbitrary public methods on a response object via a URL query
 * 	parameter is a dangerous capability to expose over the wire.  A consumer opts in <i>explicitly</i> by
 * 	registering an {@code IntrospectableSettings} bean (built with {@link Builder#allow(Class, String...)
 * 	allow(...)} or {@link Builder#allowAll()}) in the resource's bean store.  When no such bean is present,
 * 	{@link Introspectable} resolves the default (deny-all) and every <c>invokeMethod</c> request is refused
 * 	with an HTTP 500 ({@link org.apache.juneau.marshall.objecttools.MethodNotAllowlistedException}).
 * </p>
 *
 * <h5 class='section'>Example - allow-listing specific methods:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(converters=Introspectable.<jk>class</jk>)
 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
 *
 * 		<ja>@Bean</ja>
 * 		<jk>public</jk> IntrospectableSettings introspectableSettings() {
 * 			<jk>return</jk> IntrospectableSettings.<jsm>create</jsm>()
 * 				.allow(MyBean.<jk>class</jk>, <js>"getName"</js>, <js>"getAge"</js>)
 * 				.build();
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Example - trusted resource, pre-10.0 behavior:</h5>
 * <p class='bjava'>
 * 	<ja>@Bean</ja>
 * 	<jk>public</jk> IntrospectableSettings introspectableSettings() {
 * 		<jk>return</jk> IntrospectableSettings.<jsm>create</jsm>().allowAll().build();
 * 	}
 * </p>
 * <p>
 * 	<b>Never</b> call {@link Builder#allowAll() allowAll()} on a resource whose response objects expose
 * 	methods you wouldn't want an arbitrary caller to invoke &mdash; allow-list the specific methods instead.
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Introspectable}
 * 	<li class='jc'>{@link org.apache.juneau.marshall.objecttools.ObjectIntrospector}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ObjectTools">Object Tools</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class IntrospectableSettings {

	/** The default (deny-all) settings used when no bean is registered. */
	public static final IntrospectableSettings DEFAULT = create().build();

	private final Predicate<Method> allowed;

	private IntrospectableSettings(Builder b) {
		this.allowed = b.allowed;
	}

	/**
	 * Builder creator.
	 *
	 * @return A new builder (deny-all).
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Returns the effective allow-list filter for these settings.
	 *
	 * @return
	 * 	A filter that returns <jk>true</jk> for methods that may be invoked.  Never <jk>null</jk> &mdash;
	 * 	returns <jk>false</jk> for every method when nothing has been allow-listed (the default).
	 */
	public Predicate<Method> asFilter() {
		return allowed == null ? m -> false : allowed;
	}

	/**
	 * Builder for {@link IntrospectableSettings}.
	 */
	public static class Builder {
		private Predicate<Method> allowed;

		/**
		 * Allow-lists methods matching the specified filter.
		 *
		 * <p>
		 * Can be called multiple times; the filters are OR'ed together, so a method is allowed if it matches
		 * <b>any</b> filter that was added.
		 *
		 * @param filter Filter that returns <jk>true</jk> for methods that may be invoked.
		 * @return This object.
		 */
		public Builder allow(Predicate<Method> filter) {
			if (filter != null)
				allowed = (allowed == null) ? filter : allowed.or(filter);
			return this;
		}

		/**
		 * Allow-lists specific method signatures declared on (or inherited by) the specified class.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	IntrospectableSettings.<jsm>create</jsm>().allow(MyBean.<jk>class</jk>, <js>"getName"</js>, <js>"getAge"</js>);
		 * </p>
		 *
		 * @param declaringClass The class the allow-listed methods must be declared on (or a subtype thereof).
		 * @param signatures
		 * 	One or more method signatures as returned by {@link MethodInfo#getSignature()} (e.g. <js>"getName"</js>,
		 * 	<js>"substring(int,int)"</js>).
		 * @return This object.
		 */
		public Builder allow(Class<?> declaringClass, String...signatures) {
			var sigs = Set.of(signatures);
			return allow(m -> declaringClass.isAssignableFrom(m.getDeclaringClass()) && sigs.contains(MethodInfo.of(m).getSignature()));
		}

		/**
		 * Allow-lists any public, non-deprecated method &mdash; the pre-10.0 behavior.
		 *
		 * <p>
		 * <b>Use only for trusted resources</b> whose response objects' full public API surface is one you're
		 * comfortable exposing to any caller who can reach this endpoint, since the method name and arguments
		 * for {@link Introspectable} always come from untrusted request query parameters.
		 *
		 * @return This object.
		 */
		public Builder allowAll() {
			allowed = m -> true;
			return this;
		}

		/**
		 * Builds the settings.
		 *
		 * @return A new {@link IntrospectableSettings}.
		 */
		public IntrospectableSettings build() {
			return new IntrospectableSettings(this);
		}
	}
}
