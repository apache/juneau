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
package org.apache.juneau.rest.view;

import org.apache.juneau.rest.processor.*;

/**
 * Marker interface for response processors that handle {@link View}-typed return values.
 *
 * <p>
 * During {@link ResponseProcessorList} construction, any processor implementing this interface
 * is automatically repositioned to run before any {@link CatchAllResponseProcessor} in the chain.
 * This ensures view renderers are consulted before the catch-all serializer for
 * {@code View}-returning {@code @RestOp} methods.
 *
 * <p>
 * The four built-in renderers automatically implement this interface:
 * <ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.view.jsp.JspViewRenderer}
 * 	<li class='jc'>{@link org.apache.juneau.rest.view.thymeleaf.ThymeleafViewRenderer}
 * 	<li class='jc'>{@link org.apache.juneau.rest.view.mustache.MustacheViewRenderer}
 * 	<li class='jc'>{@link org.apache.juneau.rest.view.freemarker.FreemarkerViewRenderer}
 * </ul>
 *
 * <p>
 * Third-party view renderers should implement this interface to inherit the same ordering
 * guarantee.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link CatchAllResponseProcessor}
 * 	<li class='jc'>{@link ResponseProcessorList}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ResponseProcessors">Response Processors</a>
 * </ul>
 *
 * @since 9.5.0
 */
public interface ViewRenderer extends ResponseProcessor {}
