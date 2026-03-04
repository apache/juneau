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
package org.apache.juneau.rest.config;

import org.apache.juneau.proto.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Basic configuration for a REST resource that supports Protobuf Text Format transport.
 *
 * <p>
 * 	Default settings defined:
 * </p>
 * <ul class='spaced-list'>
 * 	<li class='ja'>{@link Rest}:
 * 		<ul>
 * 			<li class='jma'>{@link Rest#serializers() serializers}: {@link ProtoSerializer}
 * 			<li class='jma'>{@link Rest#parsers() parsers}: {@link ProtoParser}
 * 			<li class='jma'>{@link Rest#defaultAccept() defaultAccept}:  <js>"text/protobuf"</js>
 *		</ul>
 *	</li>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestAnnotatedClassBasics">@Rest-Annotated Class Basics</a>
 * </ul>
 */
@Rest(
	serializers={ProtoSerializer.class},
	parsers={ProtoParser.class},
	defaultAccept="text/protobuf"
)
public interface BasicProtoConfig extends DefaultConfig {}
