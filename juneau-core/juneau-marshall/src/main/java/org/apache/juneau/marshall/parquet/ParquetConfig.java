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
package org.apache.juneau.marshall.parquet;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.marshall.*;

/**
 * Annotation for configuring Parquet serializer/parser on classes and methods.
 *
 * @see ParquetSerializer
 * @see ParquetParser
 */
@Documented
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
@Inherited
@Repeatable(ParquetConfigAnnotation.Array.class)
@ContextApply({ ParquetConfigAnnotation.SerializerApply.class, ParquetConfigAnnotation.ParserApply.class })
public @interface ParquetConfig {

	/** Compression codec (UNCOMPRESSED, GZIP). */
	String compressionCodec() default "";

	/** Row group size in bytes. */
	String rowGroupSize() default "";

	/** Page size in bytes. */
	String pageSize() default "";

	/** Add bean types column. */
	String addBeanTypes() default "";

	/**
	 * Maximum allowed wire-declared length (in bytes) for a single Parquet page (compressed or
	 * uncompressed) when parsing.
	 *
	 * <p>
	 * Guards against malformed or adversarial input where a small file declares a huge per-page byte
	 * size. Default is <js>"268435456"</js> (256 MiB). A value of <js>"0"</js> or less disables the cap.
	 *
	 * @return The annotation value.
	 */
	String maxLength() default "";

	/**
	 * Maximum allowed wire-declared element count (file/row-group row counts, column-chunk value counts)
	 * when parsing.
	 *
	 * <p>
	 * Guards against malformed or adversarial input where a small footer declares a huge row or value
	 * count. Default is <js>"10000000"</js> (10 million). A value of <js>"0"</js> or less disables the cap.
	 *
	 * @return The annotation value.
	 */
	String maxCount() default "";

	/**
	 * Maximum allowed size (in bytes) of the whole Parquet input buffered into memory before parsing.
	 *
	 * <p>
	 * Guards against an oversized input being buffered unbounded into memory. Default is
	 * <js>"268435456"</js> (256 MiB). A value of <js>"0"</js> or less disables the cap.
	 *
	 * @return The annotation value.
	 */
	String maxInputLength() default "";

	/** Rank for application order. */
	int rank() default 0;
}
