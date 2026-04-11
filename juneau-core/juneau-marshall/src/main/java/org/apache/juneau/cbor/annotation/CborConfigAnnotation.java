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
package org.apache.juneau.cbor.annotation;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.cbor.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link CborConfig @CborConfig} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/CborBasics">CBOR Basics</a>
 * </ul>
 */
public class CborConfigAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private CborConfigAnnotation() {}

	/**
	 * Applies {@link CborConfig} annotations to a {@link org.apache.juneau.cbor.CborParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<CborConfig,CborParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(CborConfig.class, CborParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<CborConfig> ai, CborParser.Builder b) {
			// No-op: Annotation applier with no work to do
		}
	}

	/**
	 * Applies {@link CborConfig} annotations to a {@link org.apache.juneau.cbor.CborSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<CborConfig,CborSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(CborConfig.class, CborSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<CborConfig> ai, CborSerializer.Builder b) {
			CborConfig a = ai.inner();

			bool(a.addBeanTypes()).ifPresent(b::addBeanTypesCbor);
			bool(a.useTags()).ifPresent(b::useTags);
		}
	}
}
