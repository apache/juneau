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
package org.apache.juneau.rest.server.view.freemarker.console;

import java.util.*;

/**
 * Per-render navigation context shared between {@code <@navigation>} and the recursive {@code <@node>}
 * directives via {@link freemarker.core.Environment#setCustomState(Object, Object)}.
 *
 * <p>
 * {@code <@navigation>} installs a fresh instance before rendering its body; each {@code <@node>} pushes
 * its own {@code id} onto {@link #ancestors} on enter and pops it on exit, so a node can compute its
 * full slash-joined path (for {@code aria-current} matching against the page's {@code tab=}) without
 * threading state through directive parameters.
 *
 * @since 10.0.0
 */
final class NavContext {

	/** Identity key for {@code Environment} custom-state storage. */
	static final Object KEY = new Object();

	/** The live ancestor-id stack (head..tail = outermost..innermost enclosing node). */
	final Deque<String> ancestors = new ArrayDeque<>();

	NavContext() {}
}
