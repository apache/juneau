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
 * Client elicitation capability (SEP-2322): presence on {@link ClientCapabilities#getElicitation()} signals the
 * client understands {@code input_required} elicitation-flavored requests.
 *
 * <p>
 * Deliberately empty today &mdash; the pinned schema defines no sub-fields for this capability yet. A future
 * MCP revision (C6 elicitation territory, not covered by this class) may add fields here; this bean exists now so
 * the capability-gate check (see the {@code 2026-07-28} adapter) has a typed, presence-checkable field to test
 * rather than a bare boolean, matching {@link SamplingCapability}'s existing shape.
 */
@Marshalled
public class ElicitationCapability {
}
