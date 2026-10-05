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
package org.apache.juneau.petstore.service;

/**
 * One staged inline edit of a pet field, waiting on the P10 Pending changes page to be applied or discarded.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	PendingChange <jv>c</jv> = <jv>store</jv>.pendingChanges().stage(1, <js>"name"</js>, <js>"Sir Frisky"</js>, <js>"alice"</js>);
 * 	<jc>// c.getState() == State.PENDING; the pet is unchanged until apply()</jc>
 * 	<jv>store</jv>.pendingChanges().apply(<jv>c</jv>.getId(), <js>"console:alice"</js>);
 * </p>
 */
public class PendingChange {

	/** Lifecycle of a staged change. */
	public enum State {
		/** Staged; may be applied or discarded. */
		PENDING,
		/** Written to the pet. */
		APPLIED,
		/** Apply failed; may be retried. */
		FAILED
	}

	private long id;
	private long petId;
	private String field;
	private String oldValue;
	private String newValue;
	private String author;
	private String summary;
	private String message;
	private State state = State.PENDING;

	/** @return The change id, increasing in staging order. */
	public long getId() { return id; }

	/**
	 * @param value The change id, increasing in staging order.
	 * @return This object.
	 */
	public PendingChange setId(long value) { id = value; return this; }

	/** @return The id of the pet the change targets. */
	public long getPetId() { return petId; }

	/**
	 * @param value The id of the pet the change targets.
	 * @return This object.
	 */
	public PendingChange setPetId(long value) { petId = value; return this; }

	/** @return The staged field, {@code "name"} or {@code "price"}. */
	public String getField() { return field; }

	/**
	 * @param value The staged field, {@code "name"} or {@code "price"}.
	 * @return This object.
	 */
	public PendingChange setField(String value) { field = value; return this; }

	/** @return The field value when the change was staged. */
	public String getOldValue() { return oldValue; }

	/**
	 * @param value The field value when the change was staged.
	 * @return This object.
	 */
	public PendingChange setOldValue(String value) { oldValue = value; return this; }

	/** @return The staged new value. */
	public String getNewValue() { return newValue; }

	/**
	 * @param value The staged new value.
	 * @return This object.
	 */
	public PendingChange setNewValue(String value) { newValue = value; return this; }

	/** @return The lifecycle state. */
	public State getState() { return state; }

	/**
	 * @param value The lifecycle state.
	 * @return This object.
	 */
	public PendingChange setState(State value) { state = value; return this; }

	/** @return Who staged the change. */
	public String getAuthor() { return author; }

	/**
	 * @param value Who staged the change.
	 * @return This object.
	 */
	public PendingChange setAuthor(String value) { author = value; return this; }

	/** @return A short human-readable summary of the change. */
	public String getSummary() { return summary; }

	/**
	 * @param value A short human-readable summary of the change.
	 * @return This object.
	 */
	public PendingChange setSummary(String value) { summary = value; return this; }

	/** @return The failure text when the state is {@code FAILED}, otherwise <jk>null</jk>. */
	public String getMessage() { return message; }

	/**
	 * @param value The failure text when the state is {@code FAILED}, otherwise <jk>null</jk>.
	 * @return This object.
	 */
	public PendingChange setMessage(String value) { message = value; return this; }
}
