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
package org.apache.juneau.bean.mcp;

import org.apache.juneau.annotation.*;

/**
 * Polymorphic MCP resource body (text or base64 blob).
 *
 * <p>
 * Some MCP payloads omit a {@code type} discriminator and rely on the presence of {@code text} vs {@code blob}.
 * For Juneau dictionary parsing, include {@code type} with values {@code resourceText} or {@code resourceBlob}
 * when serializing mixed arrays; a transport layer may inject {@code type} when bridging from minimal wire forms.
 */
@Bean(
	typePropertyName = "type",
	dictionary = { TextResourceContents.class, BlobResourceContents.class }
)
public interface ResourceContents {}
