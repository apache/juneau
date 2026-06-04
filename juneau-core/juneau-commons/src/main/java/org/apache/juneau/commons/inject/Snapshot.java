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
package org.apache.juneau.commons.inject;

/**
 * Opaque handle for a single pushed overlay frame on a {@link WritableBeanStore}.
 *
 * <p>
 * A {@code Snapshot} is returned by {@link WritableBeanStore#pushOverlay(BeanStore)} and must be passed back to
 * {@link WritableBeanStore#popOverlay(Snapshot)} to remove the corresponding frame.  The type intentionally exposes
 * no public state: tests treat it as a token, the implementation uses it to enforce LIFO discipline and reject
 * cross-store pops.
 *
 * <p>
 * Instances cannot be constructed externally &mdash; only {@code pushOverlay(...)} produces them.  Equality is
 * identity-based; two distinct push operations always produce distinct snapshots, even when the overlay and the
 * owning store are the same.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	WritableBeanStore <jv>store</jv> = ...;
 * 	BeanStore <jv>overlay</jv> = <jk>new</jk> BasicBeanStore().addBean(MyService.<jk>class</jk>, <jv>mock</jv>);
 *
 * 	Snapshot <jv>snap</jv> = <jv>store</jv>.pushOverlay(<jv>overlay</jv>);
 * 	<jk>try</jk> {
 * 		<jc>// overlay active here</jc>
 * 	} <jk>finally</jk> {
 * 		<jv>store</jv>.popOverlay(<jv>snap</jv>);
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link WritableBeanStore} - The interface that produces and consumes {@code Snapshot} instances.
 * 	<li class='jc'>{@link StackOverlay} - The underlying stack-overlay primitive {@code BasicBeanStore} composes with.
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // owner and frame are not owned by Snapshot; their lifecycle is managed externally.
})
public final class Snapshot {

	private final WritableBeanStore owner;
	private final BeanStore frame;

	/**
	 * Package-private constructor.
	 *
	 * <p>
	 * Only {@link BasicBeanStore#pushOverlay(BeanStore)} (and other {@code WritableBeanStore} implementations sharing
	 * the same package) may construct {@code Snapshot} instances.  External code receives them as opaque handles.
	 *
	 * @param owner The store that produced this snapshot.  Must not be <jk>null</jk>.
	 * @param frame The overlay frame this snapshot identifies.  Must not be <jk>null</jk>.
	 */
	Snapshot(WritableBeanStore owner, BeanStore frame) {
		this.owner = owner;
		this.frame = frame;
	}

	/**
	 * Returns the {@link WritableBeanStore} that produced this snapshot.
	 *
	 * <p>
	 * Used by the producing store to reject {@link WritableBeanStore#popOverlay(Snapshot)} calls invoked on a
	 * different store instance (i.e. foreign-snapshot pops).
	 *
	 * @return The owning store.  Never <jk>null</jk>.
	 */
	WritableBeanStore owner() {
		return owner;
	}

	/**
	 * Returns the overlay frame this snapshot identifies.
	 *
	 * <p>
	 * Used by the producing store to verify the snapshot matches the current top-of-stack frame before popping.
	 *
	 * @return The overlay frame.  Never <jk>null</jk>.
	 */
	BeanStore frame() {
		return frame;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return "Snapshot[owner=" + Integer.toHexString(System.identityHashCode(owner))
			+ ", frame=" + Integer.toHexString(System.identityHashCode(frame)) + "]";
	}
}
