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
package org.apache.juneau.commons.beanquery;

import java.util.*;

/**
 * The leaf predicate an application supplies for a <b>custom</b> search operator so the in-memory context can evaluate
 * it against a cell value (design §5.3).
 *
 * <p>
 * Built-in operators do <b>not</b> use this &mdash; their type-aware leaf semantics live inside the in-memory context.
 * The predicate never receives a parsed tree: only the already-split literal string arguments the parser produced
 * (for example, {@code $myOp(a,b)} yields {@code ["a","b"]}).  This keeps the raw-string boundary intact &mdash; no
 * parse tree crosses into caller code.
 *
 * @since 10.0.0
 */
@FunctionalInterface
public interface SearchPredicate {

	/**
	 * Tests whether a cell value matches this custom operator's arguments.
	 *
	 * @param cellValue The row's value for the searched column.  May be <jk>null</jk> when the row has no value.
	 * @param args The literal string arguments parsed from the operator invocation (never <jk>null</jk>; may be empty).
	 * @return <jk>true</jk> if the row matches.
	 */
	boolean test(Object cellValue, List<String> args);
}
