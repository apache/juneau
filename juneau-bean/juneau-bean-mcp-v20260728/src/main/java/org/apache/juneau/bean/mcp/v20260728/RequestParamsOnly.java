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
package org.apache.juneau.bean.mcp.v20260728;

import org.apache.juneau.marshall.*;

/**
 * Metadata-only request params carrier for v2 methods whose only common params member is {@code _meta}.
 *
 * <p>
 * {@code tools/list}, {@code prompts/list}, {@code resources/list}, {@code resources/templates/list}, {@code ping},
 * and {@code server/discover} send an object params payload containing nothing but negotiation/trace metadata (list
 * pagination cursors, where present, remain ordinary sibling params fields handled by the adapter and are not
 * modeled here). Declares no members of its own; {@code _meta} is inherited from {@link RequestParams}.
 */
@Marshalled
public class RequestParamsOnly extends RequestParams<RequestParamsOnly> {
}
