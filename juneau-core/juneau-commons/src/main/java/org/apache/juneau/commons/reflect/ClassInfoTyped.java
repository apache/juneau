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
package org.apache.juneau.commons.reflect;

import java.lang.reflect.Type;

/**
 * Generic, typed variant of {@link ClassInfo}.
 *
 * @param <T> The raw class type this instance represents.
 */
public class ClassInfoTyped<T> extends ClassInfo {

	/**
	 * Constructor.
	 *
	 * @param inner The class type.
	 */
	protected ClassInfoTyped(Class<T> inner) {
		super(inner, inner);
	}

	/**
	 * Constructor.
	 *
	 * @param inner The class type.
	 * @param innerType The generic type (if parameterized type).
	 */
	protected ClassInfoTyped(Class<T> inner, Type innerType) {
		super(inner, innerType);
	}

	@Override
	public T getPrimitiveDefault() { return (T)super.getPrimitiveDefault(); }
}
