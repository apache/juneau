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
import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes for the {@link Parquet @Parquet} annotation.
 */
public class ParquetAnnotation {

	private ParquetAnnotation() {}

	/**
	 * Applies targeted {@link Parquet} annotations to a {@link org.apache.juneau.Context.Builder}.
	 */
	public static class Apply extends AnnotationApplier<Parquet,Context.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(VarResolverSession vr) {
			super(Parquet.class, Context.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Parquet> ai, Context.Builder b) {
			Parquet a = ai.inner();
			if (isEmptyArray(a.on()) && isEmptyArray(a.onClass()))
				return;
			b.annotations(copy(a, vr()));
		}
	}

	/**
	 * A collection of {@link Parquet @Parquet} annotations.
	 */
	@Documented
	@Target({ METHOD, TYPE })
	@Retention(RUNTIME)
	@Inherited
	public static @interface Array {

		/** The child annotations. */
		Parquet[] value();
	}

	/**
	 * Creates a copy of the specified annotation.
	 *
	 * @param a The annotation to copy.
	 * @param r The var resolver.
	 * @return A copy of the annotation.
	 */
	public static Parquet copy(Parquet a, VarResolverSession r) {
		return new Object(a.parquetType(), a.logicalType(), r.resolve(a.on()), a.onClass());
	}

	private static class Object implements Parquet {

		private final String parquetType;
		private final String logicalType;
		private final String[] on;
		private final Class<?>[] onClass;

		Object(String parquetType, String logicalType, String[] on, Class<?>[] onClass) {
			this.parquetType = parquetType == null ? "" : parquetType;
			this.logicalType = logicalType == null ? "" : logicalType;
			this.on = on == null ? new String[0] : on;
			this.onClass = onClass == null ? new Class[0] : onClass;
		}

		@Override
		public String parquetType() { return parquetType; }

		@Override
		public String logicalType() { return logicalType; }

		@Override
		public String[] on() { return on; }

		@Override
		public Class<?>[] onClass() { return onClass; }

		@Override
		public Class<? extends Annotation> annotationType() { return Parquet.class; }
	}
}
