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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.apache.juneau.rest.server.view.freemarker.console.DataSourceDef.*;
import static org.apache.juneau.test.bct.BctAssertions.assertSize;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

class DataSourceDef_Test extends TestBase {

	static Subscription range() {
		return Subscription.to(Topics.filter("range")).as("params").map("start", "start").map("end", "end");
	}

	static void e76(String reason, Supplier<Object> build) {
		var e = assertThrows(IllegalArgumentException.class, build::get);
		assertEquals("E-76: data source 'orders': " + reason, e.getMessage());
	}

	@Test void a01_defaults() {
		assertEquals("{\"id\":\"orders\",\"kind\":\"source\",\"url\":\"/rest/orders\",\"parser\":\"csv\","
			+ "\"builder\":\"columnar\",\"subscribes\":[],"
			+ "\"concurrency\":1,\"debounceMs\":250,\"cache\":\"memory\",\"mode\":\"raw\"}",
			DataSourceDef.create("orders").url("/rest/orders").toMap().toString());
	}

	@Test void a02_fullShape() {
		var m = DataSourceDef.create("orders")
			.url("/rest/orders/series").format(Format.NDJSON)
			.columns("start:time", "status:string", "ms:number", "ok:bool")
			.params(range(), Subscription.to(Topics.filter("search")).as("params").map("search", "search"))
			.chunk(Chunking.byDay("start", "end")).concurrency(3).debounceMs(100).cache(Cache.indexedDb())
			.epoch("/rest/session", "dataEpoch").mode(Mode.AUTO).maxRows(2_000_000)
			.aggregateUrl("/rest/orders/series/aggregate")
			.aggregate(Aggregate.bucket("auto").groupBy("status").measure("count", "sum(ms)"))
			.toMap();
		assertEquals("[{\"name\":\"start\",\"type\":\"time\"},{\"name\":\"status\",\"type\":\"string\"},"
			+ "{\"name\":\"ms\",\"type\":\"number\"},{\"name\":\"ok\",\"type\":\"bool\"}]",
			m.get("columns").toString().replace(" ", ""));
		assertEquals("ndjson", m.get("parser"));
		assertEquals("{\"unit\":\"day\",\"start\":\"start\",\"end\":\"end\"}", m.get("chunk").toString());
		assertEquals("indexedDb", m.get("cache"));
		assertEquals("{\"url\":\"/rest/session\",\"path\":\"dataEpoch\"}", m.get("epoch").toString());
		assertEquals("auto", m.get("mode"));
		assertEquals(2_000_000L, ((Number)m.get("maxRows")).longValue());
		assertEquals("{\"bucket\":\"auto\",\"groupBy\":[\"status\"],\"measures\":[\"count\",\"sum(ms)\"]}",
			m.get("aggregate").toString());
		assertSize(2, ((List<?>)m.get("subscribes")));
	}

	@Test void a03_appParserAndBuilder() {
		var m = DataSourceDef.create("orders").url("/x").parser("app.csv").builder("app.rows").toMap();
		assertEquals("app.csv", m.get("parser"));
		assertEquals("app.rows", m.get("builder"));
		var target = DataSourceDef.create("orders");
		var e = assertThrows(IllegalArgumentException.class, () -> target.parser("Bad"));
		assertEquals("E-74: 'Bad' is not a valid parser name", e.getMessage());
		var target2 = DataSourceDef.create("orders");
		e = assertThrows(IllegalArgumentException.class, () -> target2.builder("x..y"));
		assertEquals("E-74: 'x..y' is not a valid builder name", e.getMessage());
	}

	@Test void a04_e76_urls() {
		e76("url is required", () -> DataSourceDef.create("orders").toMap());
		e76("url 'https://evil/x' is not a same-origin path", () -> DataSourceDef.create("orders")
			.url("https://evil/x"));
		e76("aggregateUrl '//evil/x' is not a same-origin path", () -> DataSourceDef.create("orders")
			.aggregateUrl("//evil/x"));
		e76("epoch url '/a/../b' is not a same-origin path", () -> DataSourceDef.create("orders")
			.epoch("/a/../b", "e"));
		e76("epoch path 'a b' is not a dotted path", () -> DataSourceDef.create("orders").epoch("/s", "a b"));
		e76("url '/s?a=1#top' has a fragment", () -> DataSourceDef.create("orders").url("/s?a=1#top"));
	}

	@Test void a05_e76_modes() {
		e76("mode aggregate needs aggregateUrl(...) and aggregate(...)", () -> DataSourceDef.create("orders").url("/x")
			.mode(Mode.AGGREGATE).toMap());
		e76("mode auto needs aggregateUrl(...) and aggregate(...)", () -> DataSourceDef.create("orders").url("/x")
			.mode(Mode.AUTO).aggregateUrl("/a").toMap());
		e76("mode auto needs maxRows(n) greater than 0", () -> DataSourceDef.create("orders").url("/x").mode(Mode.AUTO)
			.aggregateUrl("/a").aggregate(Aggregate.bucket("day")).toMap());
		e76("maxRows 0 is not greater than 0", () -> DataSourceDef.create("orders").maxRows(0));
		e76("aggregate bucket 'year' is not hour, day, week, month or auto", () -> DataSourceDef.create("orders")
			.aggregate(Aggregate.bucket("year")));
		e76("aggregate measure 'median(ms)' is not count, sum(f), avg(f), min(f) or max(f)",
			() -> DataSourceDef.create("orders").aggregate(Aggregate.bucket("day").measure("median(ms)")));
		e76("aggregate has 4 groupBy fields; at most 3", () -> DataSourceDef.create("orders")
			.aggregate(Aggregate.bucket("day").groupBy("a", "b", "c", "d")));
	}

	@Test void a06_e76_chunkWindowMapped() {
		e76("chunk window param 'end' is not mapped by any params subscription", () -> DataSourceDef.create("orders")
			.url("/x")
			.params(Subscription.to(Topics.filter("range")).as("params").map("start", "start"))
			.chunk(Chunking.byHour("start", "end")).toMap());
		assertNull(DataSourceDef.create("orders").url("/x").chunk(Chunking.none()).toMap().get("chunk"));
		assertEquals("{\"unit\":\"week\",\"start\":\"from\",\"end\":\"to\"}", DataSourceDef.create("orders").url("/x")
			.params(Subscription.to(Topics.filter("range")).as("params").map("from", "start").map("to", "end"))
			.chunk(Chunking.byWeek("from", "to")).toMap().get("chunk").toString());
	}

	@Test void a07_e76_columnsAndRoles() {
		e76("column 'when:date' type 'date' is not number, time, string or bool", () -> DataSourceDef.create("orders")
			.columns("when:date"));
		e76("column 'when' needs the form name:type", () -> DataSourceDef.create("orders").columns("when"));
		e76("subscription to 'filter:range' has role 'data'; a data source accepts params or refresh",
			() -> DataSourceDef.create("orders").params(Subscription.to(Topics.filter("range")).as("data")));
		assertEquals("refresh", ((Map<?,?>)((List<?>)DataSourceDef.create("orders").url("/x")
			.params(Subscription.to("app.tick").as("refresh")).toMap().get("subscribes")).get(0)).get("as"));
		e76("concurrency 7 is outside 1..6", () -> DataSourceDef.create("orders").concurrency(7));
		e76("debounceMs -1 is negative", () -> DataSourceDef.create("orders").debounceMs(-1));
	}

	@Test void a08_idIsATopicKey() {
		assertThrows(IllegalArgumentException.class, () -> DataSourceDef.create("bad id"));
		assertEquals("orders", DataSourceDef.create("orders").id());
	}

	@Test void a09_edgeCases() {
		e76("column 'a' is declared twice", () -> DataSourceDef.create("orders").columns("a:number", "a:string"));
		e76("url 'null' is not a same-origin path", () -> DataSourceDef.create("orders").url(null));
		e76("mode is null", () -> DataSourceDef.create("orders").mode(null));
		e76("aggregate is null", () -> DataSourceDef.create("orders").aggregate(null));
		e76("aggregate groupBy field ' ' is not a field name", () -> DataSourceDef.create("orders")
			.aggregate(Aggregate.bucket("day").groupBy(" ")));
		e76("chunk window params 's' and 's' must be non-blank and different", () -> DataSourceDef.create("orders")
			.chunk(Chunking.byDay("s", "s")));
		e76("chunk window params '' and 'e' must be non-blank and different", () -> DataSourceDef.create("orders")
			.chunk(Chunking.byDay("", "e")));
	}

	@Test void a10_aggregateSnapshotAndFields() {
		var agg = Aggregate.bucket("day").groupBy("status").measure("sum(ms)");
		var d = DataSourceDef.create("orders").url("/x").aggregateUrl("/a").aggregate(agg);
		agg.groupBy("a", "b", "c", "d").measure("bogus");
		assertEquals("{\"bucket\":\"day\",\"groupBy\":[\"status\"],\"measures\":[\"sum(ms)\"]}",
			d.toMap().get("aggregate").toString());
		for (var g : new String[]{"a,b", "a b", " ", "", "sum(a)"})
			e76("aggregate groupBy field '" + g + "' is not a field name", () -> DataSourceDef.create("orders")
				.aggregate(Aggregate.bucket("day").groupBy(g)));
		for (var m : new String[]{"sum(a,b)", "sum( )", "sum(a b)", "count(a)"})
			e76("aggregate measure '" + m + "' is not count, sum(f), avg(f), min(f) or max(f)",
				() -> DataSourceDef.create("orders").aggregate(Aggregate.bucket("day").measure(m)));
		assertNotNull(DataSourceDef.create("orders")
			.aggregate(Aggregate.bucket("day").groupBy("a.b", "c_d").measure("avg(x-y)")));
	}

	@Test void a11_nullsAndBlanks() {
		e76("aggregate bucket 'null' is not hour, day, week, month or auto", () -> DataSourceDef.create("orders")
			.aggregate(Aggregate.bucket(null)));
		e76("aggregate groupBy field 'null' is not a field name", () -> DataSourceDef.create("orders")
			.aggregate(Aggregate.bucket("day").groupBy((String)null)));
		e76("aggregate measure 'null' is not count, sum(f), avg(f), min(f) or max(f)",
			() -> DataSourceDef.create("orders").aggregate(Aggregate.bucket("day").measure((String)null)));
		e76("format is null", () -> DataSourceDef.create("orders").format(null));
		e76("chunk is null", () -> DataSourceDef.create("orders").chunk(null));
		e76("cache is null", () -> DataSourceDef.create("orders").cache(null));
		e76("subscription is null", () -> DataSourceDef.create("orders").params((Subscription)null));
		e76("column is null", () -> DataSourceDef.create("orders").columns((String)null));
		e76("column ' :number' needs the form name:type", () -> DataSourceDef.create("orders").columns(" :number"));
	}

	@Test void a12_epochIsAtomic() {
		var d = DataSourceDef.create("orders").url("/x").epoch("/s", "e");
		e76("epoch path 'a b' is not a dotted path", () -> d.epoch("/t", "a b"));
		assertEquals("{\"url\":\"/s\",\"path\":\"e\"}", d.toMap().get("epoch").toString());
	}

	@Test void a99_idMustMatchCardIdGrammar() {
		var msg = "E-76: data source '%s': id must match ^[A-Za-z][A-Za-z0-9_-]{0,63}$";
		for (var bad : new String[]{"1st", "_x", "a.b", "a".repeat(65)})
			assertEquals(msg.formatted(bad),
				assertThrows(IllegalArgumentException.class, () -> DataSourceDef.create(bad)).getMessage());
		assertEquals("a".repeat(64), DataSourceDef.create("a".repeat(64)).id());
	}
}
