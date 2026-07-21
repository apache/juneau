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
package org.apache.juneau.http;

/**
 * Marker interface implemented by the nested {@code Unmodifiable} snapshot variants of the HTTP bean families.
 *
 * <p>
 * Immutability across the HTTP bean hierarchies is expressed with a "funnel + nested {@code Unmodifiable}
 * snapshot" paradigm: every state change on a bean is routed through a single protected {@code modify(Runnable)}
 * choke-point, and each concrete class gets a nested {@code X.Unmodifiable extends X} whose only behavioral
 * override is a throwing {@code modify(...)}.  Because there is no single common {@code Unmodifiable} supertype
 * across all leaves, this empty marker gives {@code isUnmodifiable()} a single type check that works uniformly
 * across every family and both the canonical and classic stacks:
 *
 * <p class='bjava'>
 * 	<jk>public boolean</jk> isUnmodifiable() { <jk>return this instanceof</jk> UnmodifiableBean; }
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommon">juneau-rest-common Basics</a>
 * </ul>
 */
public interface UnmodifiableBean {

	/**
	 * Returns the "logical" (declared) type of the specified object, unwrapping the internal
	 * {@code X.Unmodifiable} snapshot subclass when present.
	 *
	 * <p>
	 * The nested {@code X.Unmodifiable} snapshot classes are an internal implementation detail of the immutability
	 * paradigm.  Any code that reports or keys on a bean's runtime type over the wire (e.g. the {@code Thrown} response
	 * header) must use the logical type so that a frozen {@code BadRequest.Unmodifiable} still reports as
	 * {@code BadRequest}.
	 *
	 * @param o The object.  Must not be <jk>null</jk>.
	 * @return The declared type — the immediate superclass for an {@link UnmodifiableBean}, otherwise the runtime class.
	 */
	static Class<?> logicalType(Object o) {
		var c = o.getClass();
		return o instanceof UnmodifiableBean ? c.getSuperclass() : c;
	}
}
