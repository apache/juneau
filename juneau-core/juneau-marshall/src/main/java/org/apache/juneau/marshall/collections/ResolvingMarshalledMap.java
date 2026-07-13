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
package org.apache.juneau.marshall.collections;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.svl.*;
import org.apache.juneau.marshall.*;

/**
 * Subclass of a {@link MarshalledMap} that automatically resolves any SVL variables in values.
 *
 * <p>
 * Resolves variables in the following values:
 * <ul>
 * 	<li>Values of type {@link CharSequence}.
 * 	<li>Arrays containing values of type {@link CharSequence}.
 * 	<li>Collections containing values of type {@link CharSequence}.
 * 	<li>Maps containing values of type {@link CharSequence}.
 * </ul>
 *
 * <p>
 * All other data types are left as-is.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MarshallSimpleVariableLanguage">Simple Variable Language Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"serial" // SerialVersionUID not needed for runtime-only map
})
public class ResolvingMarshalledMap extends MarshalledMap {

	private final transient VarResolverSession varResolver;

	/**
	 * Constructor.
	 *
	 * @param varResolver The var resolver session to use for resolving SVL variables.
	 */
	public ResolvingMarshalledMap(VarResolverSession varResolver) {
		this.varResolver = varResolver;
	}

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		return this == o || (o instanceof ResolvingMarshalledMap && super.equals(o));
	}

	@Override /* Overridden from Object */
	public int hashCode() {
		return super.hashCode();
	}

	@Override /* Overridden from MarshalledMap */
	public ResolvingMarshalledMap append(Map<String,Object> values) {
		super.append(values);
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	public ResolvingMarshalledMap append(String key, Object value) {
		super.append(key, value);
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	public ResolvingMarshalledMap appendIf(boolean flag, String key, Object value) {
		super.appendIf(flag, key, value);
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	public ResolvingMarshalledMap filtered(Predicate<Object> value) {
		super.filtered(value);
		return this;
	}

	@Override /* Overridden from Map */
	public Object get(Object key) {
		return varResolver.resolve(super.get(key));
	}

	@Override /* Overridden from MarshalledMap */
	public ResolvingMarshalledMap inner(Map<String,Object> inner) {
		super.inner(inner);
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	public ResolvingMarshalledMap keepAll(String...keys) {
		super.keepAll(keys);
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	public ResolvingMarshalledMap modifiable() {
		if (isUnmodifiable())
			return new ResolvingMarshalledMap(varResolver).inner(this);
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	public ResolvingMarshalledMap session(MarshallingSession session) {
		super.session(session);
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	public ResolvingMarshalledMap setBeanSession(MarshallingSession value) {
		super.setBeanSession(value);
		return this;
	}

	@Override /* Overridden from MarshalledMap */
	public ResolvingMarshalledMap unmodifiable() {
		return this;
	}
}
