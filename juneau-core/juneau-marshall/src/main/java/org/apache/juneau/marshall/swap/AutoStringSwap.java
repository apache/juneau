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
package org.apache.juneau.marshall.swap;

import org.apache.juneau.marshall.*;

/**
 * A dynamic string swap that serializes objects via {@link Object#toString()} and deserializes via
 * {@code fromString(String)}, {@code valueOf(String)}, or a single-{@code String}-argument constructor.
 *
 * <p>
 * Installed automatically when a class is annotated with
 * {@link org.apache.juneau.marshall.Marshalled @Marshalled}{@code (as=STRING)}.
 *
 * @param <T> The normal class type.
 */
public class AutoStringSwap<T> extends StringSwap<T> {

	private final ClassMeta<T> classMeta;

	/**
	 * Constructor.
	 *
	 * @param classMeta The class meta for the normal class.
	 */
	public AutoStringSwap(ClassMeta<T> classMeta) {
		super(classMeta.inner());
		this.classMeta = classMeta;
	}

	@Override /* Overridden from ObjectSwap */
	public String swap(MarshallingSession session, T o) {
		return o.toString();
	}

	@Override /* Overridden from ObjectSwap */
	public T unswap(MarshallingSession session, String s, ClassMeta<?> hint) throws Exception {
		return classMeta.newInstanceFromString(null, s);
	}
}
