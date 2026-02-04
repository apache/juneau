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
package org.apache.juneau;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.svl.*;

/**
 * A list of {@link AnnotationWork} objects.
 *
 * @serial exclude
 */
@SuppressWarnings("java:S115")
public class AnnotationWorkList extends ArrayList<AnnotationWork> {

	// Argument name constants for assertArgNotNull
	private static final String ARG_vrs = "vrs";
	private static final long serialVersionUID = 1L;

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

	/**
	 * Static creator.
	 *
	 * @param annotations The annotations to create work from.
	 * @return A new list.
	 */
	public static AnnotationWorkList of(Stream<AnnotationInfo<? extends Annotation>> annotations) {
		return create().add(annotations);
	}

	/**
	 * Static creator.
	 *
	 * @param vrs The variable resolver.
	 * @param annotations The annotations to create work from.
	 * @return A new list.
	 */
	public static AnnotationWorkList of(VarResolverSession vrs, Stream<AnnotationInfo<? extends Annotation>> annotations) {
		return create(vrs).add(annotations);
	}

	private final VarResolverSession vrs;

	private AnnotationWorkList(VarResolverSession vrs) {
		this.vrs = assertArgNotNull(ARG_vrs, vrs);
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
	public AnnotationWorkList add(Stream<AnnotationInfo<? extends Annotation>> annotations) {
		annotations.sorted(Comparator.comparingInt(AnnotationInfo::getRank)).forEach(this::applyAnnotation);
		return this;
	}

	/**
	 * Helper method to extract and apply annotation appliers for a given annotation.
	 *
	 * @param ai The annotation info to process.
	 */
	@SuppressWarnings("unchecked")
	private void applyAnnotation(AnnotationInfo<?> ai) {
		var a = ai.inner();
		var cpa = assertNotNull(a.annotationType().getAnnotation(ContextApply.class), "Annotation found without @ContextApply: %s", cn(ai.annotationType()));
		Arrays.stream(cpa.value())
			.map(x -> safe(() -> (Constructor<? extends AnnotationApplier<?,?>>)x.getConstructor(VarResolverSession.class)))
			.forEach(applyConstructor -> {
				var applier = safe(() -> (AnnotationApplier<Annotation,Object>)applyConstructor.newInstance(vrs));
				add(ai, applier);
			});
	}
}