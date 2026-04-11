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
package org.apache.juneau.parquet.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.parquet.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes for the {@link ParquetConfig @ParquetConfig} annotation.
 */
public class ParquetConfigAnnotation {

	private ParquetConfigAnnotation() {}

	/**
	 * Applies {@link ParquetConfig} annotations to a {@link org.apache.juneau.parquet.ParquetSerializer.Builder}.
	 */
	public static class SerializerApply extends AnnotationApplier<ParquetConfig,ParquetSerializer.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public SerializerApply(VarResolverSession vr) {
			super(ParquetConfig.class, ParquetSerializer.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<ParquetConfig> ai, ParquetSerializer.Builder b) {
			var a = ai.inner();
			if (!a.compressionCodec().isEmpty()) {
				var cc = "GZIP".equalsIgnoreCase(a.compressionCodec())
					? CompressionCodec.GZIP : CompressionCodec.UNCOMPRESSED;
				b.compressionCodec(cc);
			}
			if (!a.rowGroupSize().isEmpty())
				b.rowGroupSize(Integer.parseInt(a.rowGroupSize()));
			if (!a.pageSize().isEmpty())
				b.pageSize(Integer.parseInt(a.pageSize()));
			if ("true".equalsIgnoreCase(a.addBeanTypes()))
				b.addBeanTypesParquet(true);
		}
	}

	/**
	 * Applies {@link ParquetConfig} annotations to a {@link org.apache.juneau.parquet.ParquetParser.Builder}.
	 */
	public static class ParserApply extends AnnotationApplier<ParquetConfig,ParquetParser.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public ParserApply(VarResolverSession vr) {
			super(ParquetConfig.class, ParquetParser.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<ParquetConfig> ai, ParquetParser.Builder b) {
			// No parser-specific config for now
		}
	}

	/**
	 * A collection of {@link ParquetConfig @ParquetConfig} annotations.
	 */
	@Documented
	@Target({ METHOD, TYPE })
	@Retention(RUNTIME)
	@Inherited
	public static @interface Array {
		/** The child annotations. */
		ParquetConfig[] value();
	}
}
