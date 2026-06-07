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
package org.apache.juneau.rest.server.processor;

import org.apache.juneau.rest.server.view.*;

/**
 * Marker interface for response processors that act as catch-alls — i.e. processors that
 * accept any non-null return value when no more-specific processor matches.
 *
 * <p>
 * During {@link ResponseProcessorList} construction, any processor implementing
 * {@link ViewRenderer} is automatically repositioned to run before the first
 * {@code CatchAllResponseProcessor} in the chain.  This ensures that
 * {@code View}-returning {@code @RestOp} methods reach the appropriate renderer rather than
 * falling through to the catch-all serializer.
 *
 * <p>
 * {@link SerializedPojoProcessor} is the canonical catch-all and implements this interface.
 * Future catch-all processors should also implement this interface to preserve the ordering
 * invariant for any registered {@link ViewRenderer}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ViewRenderer}
 * 	<li class='jc'>{@link ResponseProcessorList}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ResponseProcessors">Response Processors</a>
 * </ul>
 *
 * @since 10.0.0
 */
public interface CatchAllResponseProcessor extends ResponseProcessor {}
