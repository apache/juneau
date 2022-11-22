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

import org.apache.juneau.reflect.*;

/**
 * A unit of work for applying an annotation to a context builder.
 *
 * <p>
 * Consists of a pair of objects:
 * <ul>
 * 	<li>{@link AnnotationInfo} - The annotation being applied.
 * 	<li>{@link AnnotationApplier} - The applier for that annotation.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@SuppressWarnings("rawtypes")
public class AnnotationWork {
	final AnnotationInfo annotation;
	final AnnotationApplier applier;

	/**
	 * Constructor.
	 *
	 * @param annotation The annotation being applied.
	 * @param applier The applier for that annotation.
	 */
	public AnnotationWork(AnnotationInfo annotation, AnnotationApplier applier) {
		this.annotation = annotation;
		this.applier = applier;
	}

	/**
	 * Returns <jk>true</jk> if the annotation in this work can be applied to the specified builder.
	 *
	 * @param builder The builder.
	 * @return <jk>true</jk> if the annotation in this work can be applied to the specified builder.
	 */
	public boolean canApply(Object builder) {
		return applier.canApply(builder);
	}

	/**
	 * Calls {@link AnnotationApplier#apply(AnnotationInfo, Object)} on the specified builder.
	 *
	 * <p>
	 * A no-op if {@link AnnotationApplier#canApply(Object)} returns <jk>false</jk>.
	 *
	 * @param builder The builder.
	 */
	@SuppressWarnings("unchecked")
	public void apply(Object builder) {
		if (canApply(builder))
			applier.apply(annotation, builder);
	}
}
