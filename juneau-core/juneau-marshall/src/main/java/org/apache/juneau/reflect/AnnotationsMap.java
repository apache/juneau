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
package org.apache.juneau.reflect;

import java.lang.annotation.*;
import java.util.*;

/**
 * A mapping of annotation classes to lists of annotations.
 */
public class AnnotationsMap extends HashMap<Class<? extends Annotation>,List<Annotation>> {

	private static final long serialVersionUID = 1L;

	/**
	 * Adds an annotation to this map.
	 *
	 * @param a The annotation to add.
	 * @return This object (for method chaining).
	 */
	public AnnotationsMap add(Annotation a) {
		if (a == null || ! accept(a))
			return this;
		Class<? extends Annotation> c = a.annotationType();
		List<Annotation> l = super.get(c);
		if (l == null) {
			l = new ArrayList<>();
			put(c, l);
		}
		l.add(a);
		return this;
	}


	/**
	 * Convenience method for adding an array of annotations.
	 *
	 * @param annotations The annotations to add to this map.
	 * @return This object (for method chaining).
	 */
	public AnnotationsMap addAll(Annotation[] annotations) {
		if (annotations != null)
			for (Annotation a : annotations)
				add(a);
		return this;
	}

	/**
	 * Overridable method for filtering annotations added to this map.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if annotation should be added to this map.
	 */
	public boolean accept(Annotation a) {
		return true;
	}

	/**
	 * Returns the list of annotations of the specified type.
	 *
	 * @param <T> The annotation type.
	 * @param c The annotation type.
	 * @return The list of annotations, <jk>null</jk> if not found.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Annotation> List<T> get(Class<T> c) {
		return (List<T>)super.get(c);
	}
}
