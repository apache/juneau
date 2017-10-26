// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.csv;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;

/**
 * Session object that lives for the duration of a single use of {@link CsvSerializer}.
 *
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused within the same thread.
 */
public final class CsvSerializerSession extends WriterSerializerSession {

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime arguments.
	 * 	These specify session-level information such as locale and URI context.
	 * 	It also include session-level properties that override the properties defined on the bean and
	 * 	serializer contexts.
	 */
	protected CsvSerializerSession(CsvSerializerContext ctx, SerializerSessionArgs args) {
		super(ctx, args);
	}

	@Override /* SerializerSession */
	protected final void doSerialize(SerializerPipe pipe, Object o) throws Exception {
		try (Writer w = pipe.getWriter()) {
			ClassMeta<?> cm = getClassMetaForObject(o);
			Collection<?> l = null;
			if (cm.isArray()) {
				l = Arrays.asList((Object[])o);
			} else {
				l = (Collection<?>)o;
			}
			// TODO - Doesn't support DynaBeans.
			if (l.size() > 0) {
				ClassMeta<?> entryType = getClassMetaForObject(l.iterator().next());
				if (entryType.isBean()) {
					BeanMeta<?> bm = entryType.getBeanMeta();
					int i = 0;
					for (BeanPropertyMeta pm : bm.getPropertyMetas()) {
						if (i++ > 0)
							w.append(',');
						append(w, pm.getName());
					}
					w.append('\n');
					for (Object o2 : l) {
						i = 0;
						BeanMap<?> bean = toBeanMap(o2);
						for (BeanPropertyMeta pm : bm.getPropertyMetas()) {
							if (i++ > 0)
								w.append(',');
							append(w, pm.get(bean, pm.getName()));
						}
						w.append('\n');
					}
				}
			}
		}
	}

	private static void append(Writer w, Object o) throws IOException {
		if (o == null)
			w.append("null");
		else {
			String s = o.toString();
			boolean mustQuote = false;
			for (int i = 0; i < s.length() && ! mustQuote; i++) {
				char c = s.charAt(i);
				if (Character.isWhitespace(c) || c == ',')
					mustQuote = true;
			}
			if (mustQuote)
				w.append('"').append(s).append('"');
			else
				w.append(s);
		}
	}
}
