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
 * Polymorphic MCP content block (tool results, prompt messages).
 *
 * <p>
 * Wire objects carry a {@code type} discriminator with values {@code text}, {@code image}, or {@code resource}.
 */
@Bean(
	typePropertyName = "type",
	dictionary = { TextContent.class, ImageContent.class, EmbeddedResourceContent.class }
)
public interface Content {}
