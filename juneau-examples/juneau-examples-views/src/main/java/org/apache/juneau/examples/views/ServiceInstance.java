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
package org.apache.juneau.examples.views;

/**
 * A row bean for the ten-tab region-hosted detail example (design §11.1a).
 *
 * <p>
 * The point of this example is that the <b>server knows about none of the ten tabs</b>: the view declares one
 * {@code RowDetailDef.region(...)} and the author's populate draws the whole panel. So this bean carries only what
 * the <i>table</i> shows plus what the eager Details tab reads from the shared expand GET &mdash; there is
 * deliberately no per-tab field here, and adding one would be a sign the server had started to know.
 *
 * @since 10.0.0
 */
public class ServiceInstance {

	/** Stable id; the row identity the detail region receives as {@code ctx.ids.rowId}. */
	public String id;

	/** Display name. */
	public String name;

	/** Instance kind (e.g. {@code "primary"}, {@code "replica"}). */
	public String type;

	/** Running application version. */
	public String version;

	/** Deployment zone. */
	public String zone;

	/** One of {@code "healthy"}, {@code "degraded"}, {@code "offline"}; rendered as a pill. */
	public String status;

	/** ISO-8601 timestamp of the last heartbeat. */
	public String lastSeen;

	/** Expand-GET-only: the environment this instance belongs to.  Never a table column. */
	public String environment;

	/** Expand-GET-only: database vendor. */
	public String dbVendor;

	/** Expand-GET-only: release cadence. */
	public String releaseCycle;

	/** Expand-GET-only: last modification timestamp. */
	public String modified;

	/**
	 * Creates a table row (the seven visible columns only).
	 *
	 * @param id The stable id.
	 * @param name The display name.
	 * @param type The instance kind.
	 * @param version The application version.
	 * @param zone The deployment zone.
	 * @param status The status token.
	 * @param lastSeen An ISO-8601 timestamp.
	 */
	public ServiceInstance(String id, String name, String type, String version, String zone, String status,
			String lastSeen) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.version = version;
		this.zone = zone;
		this.status = status;
		this.lastSeen = lastSeen;
	}

	/**
	 * Adds the fields only the expand GET returns &mdash; what the eager Details tab reads through
	 * {@code ctx.data ?? ctx.fetchDeclared()}.
	 *
	 * @param environment The environment name.
	 * @param dbVendor The database vendor.
	 * @param releaseCycle The release cadence.
	 * @param modified An ISO-8601 timestamp.
	 * @return This object.
	 */
	public ServiceInstance detail(String environment, String dbVendor, String releaseCycle, String modified) {
		this.environment = environment;
		this.dbVendor = dbVendor;
		this.releaseCycle = releaseCycle;
		this.modified = modified;
		return this;
	}
}
