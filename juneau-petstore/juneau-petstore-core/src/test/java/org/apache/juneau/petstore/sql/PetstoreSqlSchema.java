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
package org.apache.juneau.petstore.sql;

import static org.apache.juneau.commons.beanquery.SearchType.*;

import java.sql.*;
import java.time.*;

import org.apache.juneau.beanquery.postgres.*;
import org.apache.juneau.beanquery.sql.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.petstore.service.*;

/**
 * H2 tables holding a copy of a seeded {@link PetStore}, and the SQL query contexts over them.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>var</jk> <jv>schema</jv> = PetstoreSqlSchema.<jsm>load</jsm>(<js>"jdbc:h2:mem:petstore;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"</js>, <jv>store</jv>);
 * 	<jk>try</jk> (<jk>var</jk> <jv>s</jv> = <jv>schema</jv>.pets().getSession()) {
 * 		<jk>var</jk> <jv>page</jv> = DataTablesQuery.<jsm>run</jsm>(<jv>request</jv>, <jv>s</jv>);
 * 	}
 * </p>
 */
@SuppressWarnings({
	"resource" // connect() returns a Connection owned by the caller (closed via try-with-resources); Eclipse JDT @Owning warning is by design.
})
public final class PetstoreSqlSchema {

	private final String url;

	private PetstoreSqlSchema(String url) {
		this.url = url;
	}

	/**
	 * Creates the tables and copies every pet and audit row from the store.
	 *
	 * @param url The H2 URL; must keep the database open between connections ({@code DB_CLOSE_DELAY=-1}).
	 * @param store The seeded store.
	 * @return The schema.
	 * @throws SQLException On a DDL or insert failure.
	 */
	public static PetstoreSqlSchema load(String url, PetStore store) throws SQLException {
		try (var c = DriverManager.getConnection(url); var s = c.createStatement()) {
			s.execute("CREATE TABLE \"pet\" (\"id\" BIGINT PRIMARY KEY, \"name\" VARCHAR(200), \"species\" VARCHAR(20), \"price\" REAL, \"status\" VARCHAR(20))");
			s.execute("CREATE TABLE \"audit\" (\"id\" BIGINT PRIMARY KEY, \"at\" TIMESTAMP WITH TIME ZONE, \"actor\" VARCHAR(200), \"action\" VARCHAR(50), \"entity\" VARCHAR(50), \"entityId\" VARCHAR(50), \"detail\" VARCHAR(2000))");
			try (var p = c.prepareStatement("INSERT INTO \"pet\" VALUES (?, ?, ?, ?, ?)")) {
				for (var x : store.getPets()) {
					p.setLong(1, x.getId());
					p.setString(2, x.getName());
					p.setString(3, x.getSpecies() == null ? null : x.getSpecies().name());
					p.setFloat(4, x.getPrice());
					p.setString(5, x.getStatus() == null ? null : x.getStatus().name());
					p.addBatch();
				}
				p.executeBatch();
			}
			try (var p = c.prepareStatement("INSERT INTO \"audit\" VALUES (?, ?, ?, ?, ?, ?, ?)")) {
				for (var x : store.getAudit()) {
					p.setLong(1, x.getId());
					p.setObject(2, x.getAt().atOffset(ZoneOffset.UTC));
					p.setString(3, x.getActor());
					p.setString(4, x.getAction());
					p.setString(5, x.getEntity());
					p.setString(6, x.getEntityId());
					p.setString(7, x.getDetail());
					p.addBatch();
				}
				p.executeBatch();
			}
		}
		return new PetstoreSqlSchema(url);
	}

	private Connection connect() {
		try {
			return DriverManager.getConnection(url);
		} catch (SQLException e) {
			throw new IllegalStateException("Cannot connect to '" + url + "'", e);
		}
	}

	/** @return A query context over the {@code pet} table, with the same columns as {@code PetStore.PET_QUERY}. */
	public SqlBeanQueryContext<Pet> pets() {
		return SqlBeanQueryContext.create(Pet.class)
			.dialect(PostgresDialect.INSTANCE)
			.table("pet")
			.column("id", NUMERIC)
			.column("name", TEXT)
			.column("species", ENUM)
			.column("price", NUMERIC)
			.column("status", ENUM)
			.rowMapper((rs, cols) -> new Pet()
				.setId(rs.getLong("id"))
				.setName(rs.getString("name"))
				.setSpecies(rs.getString("species") == null ? null : Species.valueOf(rs.getString("species")))
				.setPrice(rs.getFloat("price"))
				.setStatus(rs.getString("status") == null ? null : PetStatus.valueOf(rs.getString("status"))))
			.connectionSupplier(this::connect)
			.build();
	}

	/** @return A query context over the {@code audit} table, with the same columns as {@code PetStore.AUDIT_QUERY}. */
	public SqlBeanQueryContext<AuditEntry> audit() {
		return SqlBeanQueryContext.create(AuditEntry.class)
			.dialect(PostgresDialect.INSTANCE)
			.table("audit")
			.column("id", NUMERIC)
			.column("at", TIMESTAMP)
			.column("actor", TEXT)
			.column("action", TEXT)
			.column("entity", TEXT)
			.column("entityId", TEXT)
			.column("detail", TEXT)
			.rowMapper((rs, cols) -> new AuditEntry()
				.setId(rs.getLong("id"))
				.setAt(rs.getObject("at", OffsetDateTime.class).toInstant())
				.setActor(rs.getString("actor"))
				.setAction(rs.getString("action"))
				.setEntity(rs.getString("entity"))
				.setEntityId(rs.getString("entityId"))
				.setDetail(rs.getString("detail")))
			.connectionSupplier(this::connect)
			.build();
	}
}
