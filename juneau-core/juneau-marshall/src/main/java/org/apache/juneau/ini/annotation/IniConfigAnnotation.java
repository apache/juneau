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
package org.apache.juneau.ini.annotation;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link IniConfig @IniConfig} annotation.
 */
public class IniConfigAnnotation {

	private IniConfigAnnotation() {}

	/**
	 * Applies {@link IniConfig} annotations to an {@link org.apache.juneau.ini.IniParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<IniConfig, IniParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(IniConfig.class, IniParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<IniConfig> ai, IniParser.Builder b) {
			// No-op: Parser accepts both = and :; no format-specific settings needed.
		}
	}

	/**
	 * Applies {@link IniConfig} annotations to an {@link org.apache.juneau.ini.IniSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<IniConfig, IniSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(IniConfig.class, IniSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<IniConfig> ai, IniSerializer.Builder b) {
			IniConfig a = ai.inner();
			string(a.kvSeparator()).filter(s -> !s.isEmpty()).ifPresent(s -> b.kvSeparator(s.charAt(0)));
			bool(a.spacedSeparator()).ifPresent(b::spacedSeparator);
			bool(a.useComments()).ifPresent(b::useComments);
		}
	}
}
