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
package org.apache.juneau.commons.logging;

import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Identity-keyed side table that carries a {@link LogContext} snapshot alongside a {@link java.util.logging.LogRecord}.
 *
 * <p>
 * The context snapshot is attached in a side table (not a field on any {@link java.util.logging.LogRecord} type) so it is
 * visible regardless of whether the record is a base JUL {@link java.util.logging.LogRecord} (as both debug pipelines
 * construct) or the commons {@link LogRecord} subclass.
 *
 * <h5 class='section'>Identity-key invariant (load-bearing):</h5>
 * <p>
 * The backing {@link WeakHashMap} behaves as an identity map <b>only because</b> neither base JUL
 * {@link java.util.logging.LogRecord} nor the commons {@link LogRecord} subclass overrides {@code equals}/{@code hashCode}
 * (both use {@link Object} identity).  If a future change gives either value-equality, the side table silently corrupts
 * (two distinct records comparing equal would share/overwrite one entry).  A unit test guards this by asserting both
 * record types remain on {@link Object} equality.
 *
 * <h5 class='section'>Framework attach points:</h5>
 * <p>
 * The two {@code attachIfAbsent} overloads are called by {@link RichLogger#log(java.util.logging.LogRecord)} and by the
 * two REST debug pipelines to pre-seed a record's correlation context.  They are <b>not</b> intended as a general
 * application-facing mutator.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link LogContext}
 * 	<li class='jc'>{@link RichLogger#log(java.util.logging.LogRecord)}
 * </ul>
 */
public final class LogRecordContext {

	private static final Map<java.util.logging.LogRecord,Map<String,Object>> TABLE =
		Collections.synchronizedMap(new WeakHashMap<>());

	/** White-box counter of side-table put attempts; incremented only when a non-empty map reaches the table. */
	private static final AtomicLong PUT_COUNT = new AtomicLong();

	private LogRecordContext() {}

	/**
	 * Returns the context snapshot attached to the specified record.
	 *
	 * @param record The record.  Must not be <jk>null</jk>.
	 * @return The attached snapshot, or the shared empty {@link Map#of()} singleton if none is attached.  Never
	 * 	<jk>null</jk>.
	 */
	public static Map<String,Object> of(java.util.logging.LogRecord record) {
		var m = TABLE.get(record);
		return m == null ? Map.of() : m;
	}

	/**
	 * Attaches the current thread's live {@link LogContext} snapshot to the specified record, if no snapshot is already
	 * attached.
	 *
	 * <p>
	 * Framework attach point (called as the first statement of {@link RichLogger#log(java.util.logging.LogRecord)}).  When
	 * the live context is empty this returns immediately, before touching the synchronized side table, so callers who
	 * never use {@link LogContext} pay only a thread-local read and an {@code isEmpty()} check &mdash; never a lock.
	 *
	 * @param record The record to attach to.  Must not be <jk>null</jk>.
	 */
	public static void attachIfAbsent(java.util.logging.LogRecord record) {
		attachIfAbsent(record, LogContext.INSTANCE.snapshot());
	}

	/**
	 * Attaches the supplied context snapshot to the specified record, if no snapshot is already attached.
	 *
	 * <p>
	 * Framework attach point used by the debug pipelines to pre-seed a record from a snapshot carried across an async
	 * boundary.  When the supplied map is empty this returns immediately, before touching the synchronized side table
	 * &mdash; symmetric with the one-arg overload &mdash; so a later one-arg call from an empty emitting thread cannot
	 * clobber a previously pre-seeded snapshot.
	 *
	 * @param record The record to attach to.  Must not be <jk>null</jk>.
	 * @param ctx The context snapshot to attach.  Must not be <jk>null</jk>.
	 */
	public static void attachIfAbsent(java.util.logging.LogRecord record, Map<String,Object> ctx) {
		if (ctx.isEmpty())
			return;
		PUT_COUNT.incrementAndGet();
		TABLE.putIfAbsent(record, ctx);
	}

	/**
	 * White-box accessor: the number of live entries in the side table.  For tests only.
	 *
	 * @return The current side-table size.
	 */
	static int tableSize() {
		return TABLE.size();
	}

	/**
	 * White-box accessor: the number of times a non-empty context reached the synchronized side table.  For tests only.
	 *
	 * @return The cumulative put-attempt count.
	 */
	static long putCount() {
		return PUT_COUNT.get();
	}
}
