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
package org.apache.juneau.yaml.annotation;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.yaml.*;

/**
 * Utility classes and methods for the {@link YamlConfig @YamlConfig} annotation.
 */
public class YamlConfigAnnotation {

	private YamlConfigAnnotation() {}

	/**
	 * Applies {@link YamlConfig} annotations to a {@link org.apache.juneau.yaml.YamlParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<YamlConfig,YamlParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(YamlConfig.class, YamlParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<YamlConfig> ai, YamlParser.Builder b) {
			// No parser-specific properties yet.
		}
	}

	/**
	 * Applies {@link YamlConfig} annotations to a {@link org.apache.juneau.yaml.YamlSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<YamlConfig,YamlSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(YamlConfig.class, YamlSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<YamlConfig> ai, YamlSerializer.Builder b) {
			YamlConfig a = ai.inner();

			bool(a.addBeanTypes()).ifPresent(b::addBeanTypesYaml);
		}
	}
}
