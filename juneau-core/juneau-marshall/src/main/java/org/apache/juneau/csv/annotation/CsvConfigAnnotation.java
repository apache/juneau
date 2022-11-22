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
package org.apache.juneau.csv.annotation;

import org.apache.juneau.*;
import org.apache.juneau.csv.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link CsvConfig @CsvConfig} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class CsvConfigAnnotation {

	/**
	 * Applies {@link CsvConfig} annotations to a {@link org.apache.juneau.csv.CsvSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<CsvConfig,CsvSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(CsvConfig.class, CsvSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<CsvConfig> ai, CsvSerializer.Builder b) {
		}
	}

	/**
	 * Applies {@link CsvConfig} annotations to a {@link org.apache.juneau.csv.CsvParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<CsvConfig,CsvParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(CsvConfig.class, CsvParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<CsvConfig> ai, CsvParser.Builder b) {
		}
	}
}