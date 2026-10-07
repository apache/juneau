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

import java.time.*;

/**
 * One audit-trail record: who changed which petstore entity, how, and when.
 *
 * <p>
 * {@link PetStore} appends one entry for every successful create, update or delete, whether it came from the
 * {@code /petstore} API (actor {@code "api"}) or from the console (actor {@code "console:<user>"}).  Failed mutations
 * append nothing.  The Audit console page lists these entries.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	PetStore <jv>store</jv> = <jk>new</jk> PetStore();
 * 	<jv>store</jv>.createPet(<jk>new</jk> Pet().setName(<js>"Rex"</js>), <js>"console:alice"</js>);
 * 	AuditEntry <jv>e</jv> = <jv>store</jv>.getAudit().get(0);
 * 	<jc>// e.getEntity() = "Pet", e.getAction() = "CREATE", e.getDetail() = "Rex"</jc>
 * </p>
 */
public class AuditEntry {

	private long id;
	private Instant at;
	private String actor;
	private String entity;
	private String entityId;
	private String action;
	private String detail;

	/** @return The audit id, increasing in insertion order. */
	public long getId() { return id; }

	/**
	 * @param value The audit id.
	 * @return This object.
	 */
	public AuditEntry setId(long value) { id = value; return this; }

	/** @return When the mutation happened, from the store's {@link Clock}. */
	public Instant getAt() { return at; }

	/**
	 * @param value The timestamp.
	 * @return This object.
	 */
	public AuditEntry setAt(Instant value) { at = value; return this; }

	/** @return Who made the change, e.g. {@code "api"} or {@code "console:alice"}. */
	public String getActor() { return actor; }

	/**
	 * @param value The actor.
	 * @return This object.
	 */
	public AuditEntry setActor(String value) { actor = value; return this; }

	/** @return The entity kind: {@code "Pet"}, {@code "Order"}, {@code "User"} or {@code "Change"}. */
	public String getEntity() { return entity; }

	/**
	 * @param value The entity kind.
	 * @return This object.
	 */
	public AuditEntry setEntity(String value) { entity = value; return this; }

	/** @return The entity id (pet/order id, or username). */
	public String getEntityId() { return entityId; }

	/**
	 * @param value The entity id.
	 * @return This object.
	 */
	public AuditEntry setEntityId(String value) { entityId = value; return this; }

	/** @return The action, e.g. {@code "CREATE"}, {@code "UPDATE"}, {@code "DELETE"}. */
	public String getAction() { return action; }

	/**
	 * @param value The action.
	 * @return This object.
	 */
	public AuditEntry setAction(String value) { action = value; return this; }

	/** @return A short human-readable detail (pet name, username, ...), or <jk>null</jk>. */
	public String getDetail() { return detail; }

	/**
	 * @param value The detail.
	 * @return This object.
	 */
	public AuditEntry setDetail(String value) { detail = value; return this; }
}
