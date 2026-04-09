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
 * Next-generation HTTP header implementations.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This package is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * Binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release (and possibly earlier).
 *
 * <p>
 * {@link org.apache.juneau.ng.http.header.HttpHeaderBean} is the root concrete class for all header types.
 * Typed sub-classes (e.g. {@code HttpStringHeader}, {@code HttpDateHeader}) extend it to provide
 * strongly-typed accessors.
 */
package org.apache.juneau.ng.http.header;
