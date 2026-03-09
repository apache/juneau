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
package org.apache.juneau.hocon.annotation;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.hocon.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link HoconConfig @HoconConfig} annotation.
 */
public class HoconConfigAnnotation {

	private HoconConfigAnnotation() {}

	/**
	 * Applies {@link HoconConfig} annotations to an {@link HoconParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<HoconConfig, HoconParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(HoconConfig.class, HoconParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<HoconConfig> ai, HoconParser.Builder b) {
			var a = ai.inner();
			bool(a.resolveSubstitutions()).ifPresent(b::resolveSubstitutions);
		}
	}

	/**
	 * Applies {@link HoconConfig} annotations to an {@link HoconSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<HoconConfig, HoconSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(HoconConfig.class, HoconSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<HoconConfig> ai, HoconSerializer.Builder b) {
			var a = ai.inner();
			bool(a.useEqualsSign()).ifPresent(b::useEqualsSign);
			bool(a.useUnquotedStrings()).ifPresent(b::useUnquotedStrings);
			bool(a.useUnquotedKeys()).ifPresent(b::useUnquotedKeys);
			bool(a.omitRootBraces()).ifPresent(b::omitRootBraces);
			bool(a.useNewlineSeparators()).ifPresent(b::useNewlineSeparators);
		}
	}
}
