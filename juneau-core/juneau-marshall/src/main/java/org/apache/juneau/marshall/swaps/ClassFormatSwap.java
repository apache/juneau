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
package org.apache.juneau.marshall.swaps;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.swap.*;

/**
 * Thin delegating swap for {@link Class} values driven by the context-configured {@link ClassFormat}.
 *
 * <p>
 * Replaces the legacy {@code ClassSwap} (deleted in 9.6) — provides format-aware {@link Class}
 * serialization for root-level / map-key / map-value / collection-element positions where no
 * per-property {@link MarshalledProp#classFormat() @MarshalledProp(classFormat=…)} or
 * {@link Marshalled#classFormat() @Marshalled(classFormat=…)} swap is installed.
 *
 * <p>
 * Reads the context's {@link MarshallingContext#getClassFormat()} at swap/unswap time so changes to the
 * setting propagate without rebuilding {@link DefaultSwaps}. When the context is at
 * {@link ClassFormat#NOT_SET} the swap falls through to {@link ClassFormat#FQCN}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SwapBasics">Swap Basics</a>
 * </ul>
 */
public class ClassFormatSwap extends StringSwap<Class<?>> {

	@Override /* Overridden from ObjectSwap */
	public String swap(MarshallingSession session, Class<?> o) {
		if (o == null)
			return null;
		var fmt = resolveFormat(session);
		return ClassFormat.format(o, fmt);
	}

	@Override /* Overridden from ObjectSwap */
	public Class<?> unswap(MarshallingSession session, String o, ClassMeta<?> hint) {
		if (o == null)
			return null;
		var fmt = resolveFormat(session);
		ClassLoader cl = session != null ? session.getClassLoader() : null;
		if (cl == null)
			cl = Thread.currentThread().getContextClassLoader();
		return ClassFormat.parse(o, fmt, cl);
	}

	private static ClassFormat resolveFormat(MarshallingSession session) {
		if (session == null)
			return ClassFormat.FQCN;
		var fmt = session.getMarshallingContext().getClassFormat();
		return (fmt == null || fmt == ClassFormat.NOT_SET) ? ClassFormat.FQCN : fmt;
	}
}
