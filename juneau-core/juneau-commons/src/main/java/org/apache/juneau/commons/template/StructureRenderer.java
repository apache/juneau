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
package org.apache.juneau.commons.template;

import java.util.function.*;

import org.apache.juneau.commons.svl.*;

/**
 * Renders a {@link Structure} (a list, block or table produced by <c>ul</c>, <c>ol</c>, <c>block</c> or <c>table</c>) for
 * one destination.
 *
 * <p>
 * The renderer owns the encoding of its content: it must encode every item and cell it writes and must not encode the
 * markup it adds.  The value encoder of the render is <em>not</em> applied to a {@link Structure}.  A nested
 * {@link Structure} item is rendered by calling this renderer again.
 */
@FunctionalInterface
public interface StructureRenderer {

	/**
	 * Renders a structure.
	 *
	 * @param structure The structure.  Never <jk>null</jk>.
	 * @param toText Converts one item or cell value to text using the resolver's value formatter (before encoding).
	 * @return The rendered text.  Never <jk>null</jk>.
	 */
	String render(Structure structure, Function<Object,String> toText);
}
