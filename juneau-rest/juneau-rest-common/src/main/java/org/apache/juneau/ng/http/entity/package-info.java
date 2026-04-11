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
 * Next-generation HTTP body (entity) implementations.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This package is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * Binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release (and possibly earlier).
 *
 * <p>
 * Concrete body types:
 * <ul>
 * 	<li>{@link org.apache.juneau.ng.http.entity.StringBody} — UTF-8 string content
 * 	<li>{@link org.apache.juneau.ng.http.entity.ByteArrayBody} — raw byte array content
 * 	<li>{@link org.apache.juneau.ng.http.entity.StreamBody} — wraps a one-shot {@link java.io.InputStream}
 * 	<li>{@link org.apache.juneau.ng.http.entity.FileBody} — streams a {@link java.io.File}
 * 	<li>{@link org.apache.juneau.ng.http.entity.MultipartBody} — {@code multipart/form-data} with file uploads
 * 	<li>{@link org.apache.juneau.ng.http.entity.HttpBodyBean} — wraps an existing body with an overridden content type
 * </ul>
 */
package org.apache.juneau.ng.http.entity;
