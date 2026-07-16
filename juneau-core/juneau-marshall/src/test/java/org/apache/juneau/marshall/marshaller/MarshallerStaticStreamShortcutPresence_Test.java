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
package org.apache.juneau.marshall.marshaller;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Presence guard for the Feature-A stream-based static shortcuts.
 *
 * <p>
 * Because the Feature-A stream statics are added <i>uniformly</i> to every facade (they are not
 * capability-gated), the guard is a simple presence/absence check rather than a role-driven drift
 * test:
 *
 * <ul>
 * 	<li>Every char facade (subclass of {@link CharMarshaller}) must declare the Reader/Writer trio
 * 		({@code to(Reader,Class)}, {@code to(Reader,Type,Type...)}, {@code of(Object,Writer)}) and
 * 		must <b>not</b> declare the InputStream/OutputStream trio.
 * 	<li>Every stream facade (subclass of {@link StreamMarshaller}) must declare the
 * 		InputStream/OutputStream trio ({@code to(InputStream,Class)}, {@code to(InputStream,Type,Type...)},
 * 		{@code of(Object,OutputStream)}) and must <b>not</b> declare the Reader/Writer trio.
 * </ul>
 *
 * <p>
 * The facade list is shared with {@link MarshallerStreamShortcutDrift_Test#marshallerClasses()} and
 * partitioned by {@link CharMarshaller}/{@link StreamMarshaller} assignability via reflection so the
 * two families cannot silently drift into each other.
 */
class MarshallerStaticStreamShortcutPresence_Test extends TestBase {

	@ParameterizedTest
	@MethodSource("org.apache.juneau.marshall.marshaller.MarshallerStreamShortcutDrift_Test#marshallerClasses")
	void a01_streamStaticsPresentForFamily(Class<? extends Marshaller> c) {
		var isChar = CharMarshaller.class.isAssignableFrom(c);
		var isStream = StreamMarshaller.class.isAssignableFrom(c);
		assertTrue(isChar ^ isStream, () -> c.getSimpleName() + ": must be exactly one of CharMarshaller/StreamMarshaller");

		if (isChar) {
			assertDeclared(c, "to", Reader.class, Class.class);
			assertDeclared(c, "to", Reader.class, Type.class, Type[].class);
			assertDeclared(c, "of", Object.class, Writer.class);

			assertAbsent(c, "to", InputStream.class, Class.class);
			assertAbsent(c, "to", InputStream.class, Type.class, Type[].class);
			assertAbsent(c, "of", Object.class, OutputStream.class);
		} else {
			assertDeclared(c, "to", InputStream.class, Class.class);
			assertDeclared(c, "to", InputStream.class, Type.class, Type[].class);
			assertDeclared(c, "of", Object.class, OutputStream.class);

			assertAbsent(c, "to", Reader.class, Class.class);
			assertAbsent(c, "to", Reader.class, Type.class, Type[].class);
			assertAbsent(c, "of", Object.class, Writer.class);
		}
	}

	private static void assertDeclared(Class<?> c, String name, Class<?>... params) {
		assertTrue(isDeclared(c, name, params),
			() -> c.getSimpleName() + ": expected to declare static " + signature(name, params));
	}

	private static void assertAbsent(Class<?> c, String name, Class<?>... params) {
		assertFalse(isDeclared(c, name, params),
			() -> c.getSimpleName() + ": expected NOT to declare static " + signature(name, params) + " (wrong-family trio)");
	}

	private static boolean isDeclared(Class<?> c, String name, Class<?>... params) {
		try {
			var m = c.getDeclaredMethod(name, params);
			return Modifier.isStatic(m.getModifiers());
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

	private static String signature(String name, Class<?>... params) {
		var sb = new StringBuilder(name).append('(');
		for (var i = 0; i < params.length; i++) {
			if (i > 0)
				sb.append(',');
			sb.append(params[i].getSimpleName());
		}
		return sb.append(')').toString();
	}
}
