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
package org.apache.juneau.rest.server.management;

import java.io.*;
import java.lang.management.*;
import java.nio.file.*;

import org.apache.juneau.rest.server.*;

/**
 * Shared worker for the diagnostic dump endpoints ({@code /threaddump}, {@code /heapdump}).
 *
 * <p>
 * Both the {@code *Mixin} and {@code *Resource} flavors delegate here so the two forms cannot drift &mdash;
 * the same shared-worker pattern the health and metrics surfaces use.
 *
 * <p>
 * <b>Thread dump</b> is rendered from the JDK-standard {@link ThreadMXBean} (always available).
 * <b>Heap dump</b> is produced via the HotSpot {@code HotSpotDiagnosticMXBean#dumpHeap(String,boolean)}
 * method, invoked <b>reflectively</b> so this module carries no compile-time dependency on
 * {@code com.sun.management} &mdash; on a non-HotSpot JVM the heap dump degrades cleanly (the endpoints
 * surface that as HTTP 501).
 *
 * <p>
 * Exposure is governed by {@link DumpsSettings} resolved from the bean store; both dumps are
 * <b>deny-by-default</b> &mdash; see {@link DumpsSettings}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link DumpsSettings}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ManagementSurface">Management Surface</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class DumpsManager {

	/**
	 * Resolves the {@link DumpsSettings} from the host context's bean store, falling back to the deny-all default.
	 *
	 * @param context The REST context whose bean store is searched.  May be <jk>null</jk>.
	 * @return The registered settings, or {@link DumpsSettings#DEFAULT} when none is registered.
	 */
	@SuppressWarnings({
		"resource" // The bean store is owned by the RestContext; this adapter only borrows beans and must not close it.
	})
	public DumpsSettings resolveSettings(RestContext context) {
		if (context == null)
			return DumpsSettings.DEFAULT;
		return context.getBeanStore().getBean(DumpsSettings.class).orElse(DumpsSettings.DEFAULT);
	}

	/**
	 * Renders a full thread dump from the {@link ThreadMXBean}.
	 *
	 * @return A human-readable thread dump (never <jk>null</jk> or empty).
	 */
	public String threadDump() {
		var tmx = ManagementFactory.getThreadMXBean();
		var infos = tmx.dumpAllThreads(true, true);
		var sb = new StringBuilder();
		for (var info : infos)
			sb.append(info.toString());
		return sb.toString();
	}

	/**
	 * Writes a heap dump (hprof) to the supplied file via the HotSpot {@code HotSpotDiagnosticMXBean}.
	 *
	 * <p>
	 * Invoked reflectively to avoid a compile-time dependency on {@code com.sun.management}.  The target file
	 * <b>must not already exist</b> (a {@code dumpHeap} requirement).
	 *
	 * @param target The hprof output file.  Must not be <jk>null</jk> (and must not pre-exist).
	 * @param live If <jk>true</jk>, dump only live (reachable) objects.
	 * @return <jk>true</jk> if the dump was written; <jk>false</jk> if this JVM has no HotSpot heap-dump support
	 * 	(the endpoints surface that as HTTP 501).
	 */
	public boolean heapDump(File target, boolean live) {
		try {
			var diagClass = Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
			@SuppressWarnings("unchecked")
			var bean = ManagementFactory.getPlatformMXBean((Class<? extends PlatformManagedObject>) diagClass);
			if (bean == null)
				return false;  // HTT: only on a JVM exposing the class but no platform bean — not reproducible on HotSpot CI.
			var m = diagClass.getMethod("dumpHeap", String.class, boolean.class);
			m.invoke(bean, target.getAbsolutePath(), live);
			return true;
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			return false;  // HTT: non-HotSpot JVM with no heap-dump support — not reproducible on HotSpot CI (caller degrades to 501).
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to write heap dump to " + target.getAbsolutePath(), e);
		}
	}

	/**
	 * Produces a heap dump and returns it as a self-deleting input stream.
	 *
	 * <p>
	 * Writes the hprof to a fresh temp file (the {@code dumpHeap} no-pre-existing-file requirement is satisfied
	 * by deleting the just-created temp file before the dump), then returns a stream over it that deletes the
	 * backing file when closed.
	 *
	 * @param live If <jk>true</jk>, dump only live (reachable) objects.
	 * @return A stream over the hprof file, or <jk>null</jk> if this JVM has no HotSpot heap-dump support.
	 * @throws IOException If the temp file could not be created or opened.
	 */
	@SuppressWarnings({
		"java:S5443" // Heap dump is an explicitly opt-in, admin-gated diagnostic; the random-named temp file is created then immediately reclaimed for dumpHeap.
	})
	public InputStream heapDumpStream(boolean live) throws IOException {
		var f = File.createTempFile("juneau-heapdump-", ".hprof");
		Files.delete(f.toPath());  // dumpHeap requires the target file to not already exist.
		if (! heapDump(f, live)) {  // HTT: heapDump only returns false on a non-HotSpot JVM — not reproducible on HotSpot CI.
			Files.deleteIfExists(f.toPath());
			return null;
		}
		return new FileInputStream(f) {
			@Override
			public void close() throws IOException {
				try {
					super.close();
				} finally {
					Files.deleteIfExists(f.toPath());
				}
			}
		};
	}
}
