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
package org.apache.juneau;

import java.lang.annotation.*;
import java.util.*;

import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * A list of {@link AnnotationWork} objects.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 *
 * @serial exclude
 */
public class AnnotationWorkList extends ArrayList<AnnotationWork> {
	private static final long serialVersionUID = 1L;

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param annotations The annotations to create work from.
	 * @param vrs The variable resolver.
	 * @return A new list.
	 */
	public static AnnotationWorkList of(VarResolverSession vrs, AnnotationList annotations) {
		return create(vrs).add(annotations);
	}

	/**
	 * Static creator.
	 *
	 * @param annotations The annotations to create work from.
	 * @return A new list.
	 */
	public static AnnotationWorkList of(AnnotationList annotations) {
		return create().add(annotations);
	}

	/**
	 * Static creator.
	 *
	 * @return A new list.
	 */
	public static AnnotationWorkList create() {
		return new AnnotationWorkList(VarResolver.DEFAULT.createSession());
	}

	/**
	 * Static creator.
	 *
	 * @param vrs The variable resolver.
	 * @return A new list.
	 */
	public static AnnotationWorkList create(VarResolverSession vrs) {
		return new AnnotationWorkList(vrs);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final VarResolverSession vrs;

	private AnnotationWorkList(VarResolverSession vrs) {
		this.vrs = vrs;
	}

	/**
	 * Adds an entry to this list.
	 *
	 * @param ai The annotation being applied.
	 * @param aa The applier for the annotation.
	 * @return This object.
	 */
	public AnnotationWorkList add(AnnotationInfo<?> ai, AnnotationApplier<Annotation,Object> aa) {
		add(new AnnotationWork(ai, aa));
		return this;
	}

	/**
	 * Adds entries for the specified annotations to this work list.
	 *
	 * @param annotations The annotations to create work from.
	 * @return This object.
	 */
	public AnnotationWorkList add(AnnotationList annotations) {
		annotations.sort().forEach(x ->  x.getApplies(vrs, y -> add(x, y)));
		return this;
	}
}
