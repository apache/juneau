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
package org.apache.juneau.hjson.annotation;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.hjson.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link HjsonConfig @HjsonConfig} annotation.
 */
public class HjsonConfigAnnotation {

	private HjsonConfigAnnotation() {}

	/**
	 * Applies {@link HjsonConfig} annotations to an {@link HjsonParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<HjsonConfig, HjsonParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(HjsonConfig.class, HjsonParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<HjsonConfig> ai, HjsonParser.Builder b) {
			// No format-specific parser settings from config.
		}
	}

	/**
	 * Applies {@link HjsonConfig} annotations to an {@link HjsonSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<HjsonConfig, HjsonSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(HjsonConfig.class, HjsonSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<HjsonConfig> ai, HjsonSerializer.Builder b) {
			var a = ai.inner();
			bool(a.useMultilineStrings()).ifPresent(b::useMultilineStrings);
			bool(a.useQuotelessStrings()).ifPresent(b::useQuotelessStrings);
			bool(a.useQuotelessKeys()).ifPresent(b::useQuotelessKeys);
			bool(a.omitRootBraces()).ifPresent(b::omitRootBraces);
			bool(a.useNewlineSeparators()).ifPresent(b::useNewlineSeparators);
		}
	}
}
