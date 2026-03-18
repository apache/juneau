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

/**
 * Apache Parquet serialization and parsing support.
 *
 * <p>
 * Parquet is a columnar binary format for analytics and data pipelines. This package provides
 * {@link org.apache.juneau.parquet.ParquetSerializer} and {@link org.apache.juneau.parquet.ParquetParser}
 * for reading and writing Parquet files, and {@link org.apache.juneau.marshaller.Parquet} as a
 * convenience marshaller.
 *
 * <p>
 * Parquet is collection-oriented: the serializer accepts {@link java.util.Collection}&lt;T&gt; or
 * T[] at the root; a single bean is wrapped in a one-element list. The parser always returns
 * {@code List<T>}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ParquetBasics">Parquet Basics</a>
 * 	<li class='link'><a class="doclink" href="https://parquet.apache.org/docs/file-format/">Parquet File Format</a>
 * </ul>
 */
package org.apache.juneau.parquet;
