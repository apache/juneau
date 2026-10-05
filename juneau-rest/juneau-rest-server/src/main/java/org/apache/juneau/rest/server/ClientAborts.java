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
package org.apache.juneau.rest.server;

import java.io.*;
import java.nio.channels.*;
import java.util.*;

/**
 * Internal utility for recognizing failures caused by the client disconnecting before the response completed.
 *
 * <p>
 * A client that closes its connection mid-response (closed browser tab, cancelled request, timed-out proxy) is routine,
 * not a server error.  Servlet containers surface it as an {@link IOException} from the response output stream or
 * writer, but the concrete type is container-specific.  This class detects it without a compile-time dependency on any
 * container.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>try</jk> {
 * 		<jv>res</jv>.flushBuffer();
 * 	} <jk>catch</jk> (Exception <jv>e</jv>) {
 * 		<jk>if</jk> (ClientAborts.<jsm>isClientAbort</jsm>(<jv>e</jv>))
 * 			<jc>// Routine: the client went away.  Don't record it as an error.</jc>
 * 			<jk>return</jk>;
 * 		<jk>throw</jk> <jv>e</jv>;
 * 	}
 * </p>
 *
 * <p>
 * Not part of the public REST API; subject to change without notice.
 *
 * @since 10.0.0
 */
public final class ClientAborts {

	private static final int MAX_DEPTH = 32;

	private ClientAborts() {}

	/**
	 * Returns <jk>true</jk> if the specified throwable, or anything in its cause chain, indicates that the client
	 * disconnected.
	 *
	 * <p>
	 * The cause chain is walked up to a bounded depth and guarded against cycles.  A throwable matches if it is:
	 * <ul>
	 * 	<li>A class whose simple name is <c>ClientAbortException</c> (Tomcat; matched by name so there is no Tomcat dependency).
	 * 	<li>An {@link EOFException} (Jetty's <c>EofException</c> extends it).
	 * 	<li>A {@link ClosedChannelException}.
	 * 	<li>An {@link IOException} whose message contains <js>"broken pipe"</js> or <js>"connection reset"</js> (case-insensitive).
	 * </ul>
	 *
	 * @param t The throwable to test.  Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the throwable represents a client abort.
	 */
	public static boolean isClientAbort(Throwable t) {
		return isClientAbort(t, false);
	}

	/**
	 * Returns <jk>true</jk> only if the throwable, or anything in its cause chain, is a servlet-container client-abort type.
	 *
	 * <p>
	 * Matches only classes whose simple name is <c>ClientAbortException</c> (Tomcat, which wraps every response write/flush
	 * failure in it) or <c>EofException</c> (Jetty).  Unlike {@link #isClientAbort(Throwable)}, plain {@link EOFException},
	 * {@link ClosedChannelException} and "broken pipe"/"connection reset" {@link IOException}s do <b>not</b> match.
	 *
	 * <p>
	 * Use this where the failure could have come from somewhere other than the client socket (e.g. an exception escaping
	 * operation execution or response rendering, where a backend call might also have reset).  Use the lenient
	 * {@link #isClientAbort(Throwable)} only for response flush paths, where the failure is necessarily on the client socket.
	 *
	 * @param t The throwable to test.  Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the throwable represents a container-reported client abort.
	 */
	public static boolean isContainerClientAbort(Throwable t) {
		return isClientAbort(t, true);
	}

	private static boolean isClientAbort(Throwable t, boolean strict) {
		var seen = Collections.newSetFromMap(new IdentityHashMap<Throwable,Boolean>());
		for (var i = 0; t != null && i < MAX_DEPTH && seen.add(t); i++, t = t.getCause()) {
			var n = t.getClass().getSimpleName();
			if ("ClientAbortException".equals(n) || (strict && "EofException".equals(n)))
				return true;
			if (strict)
				continue;
			if (t instanceof EOFException || t instanceof ClosedChannelException)
				return true;
			if (t instanceof IOException && isAbortMessage(t.getMessage()))
				return true;
		}
		return false;
	}

	private static boolean isAbortMessage(String m) {
		if (m == null)
			return false;
		var lc = m.toLowerCase(Locale.ROOT);
		return lc.contains("broken pipe") || lc.contains("connection reset");
	}
}
