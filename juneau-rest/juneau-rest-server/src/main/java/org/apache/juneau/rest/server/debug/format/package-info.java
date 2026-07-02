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
 * Output formatters for the REST server request/response debug-logging feature.
 *
 * <p>
 * Provides pluggable renderings of captured request/response traffic, including
 * {@link org.apache.juneau.rest.server.debug.format.BasicTextFormat basic text},
 * {@link org.apache.juneau.rest.server.debug.format.OneLineFormat one-line},
 * {@link org.apache.juneau.rest.server.debug.format.JsonFormat JSON}, and a
 * {@link org.apache.juneau.rest.server.debug.format.CapturingFormat capturing} format for test assertions.
 */
package org.apache.juneau.rest.server.debug.format;
