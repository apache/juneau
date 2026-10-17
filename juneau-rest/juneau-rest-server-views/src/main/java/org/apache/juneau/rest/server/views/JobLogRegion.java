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
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

/**
 * The author-facing declaration of a job-log row-detail region, compiled to a {@link RegionDef}.
 *
 * <p>
 * The console's {@code job-log} card, hosted in a row's detail: a form whose submit runs a row-bound job through
 * {@code JuneauViews.jobs}, streams its log, and optionally writes its result into a field.  {@link #toRegion()}
 * produces a {@link RegionDef#TYPE_ROW_DETAIL} region with {@code populate("job-log")}, opts that populator in, and
 * carries the whole declaration as {@link RegionDef#config}.  The populator ships in the console's {@code joblog}
 * pack.
 *
 * <p>
 * The submit always uses the action envelope {@code {action, targetId, idempotencyKey, fields}}: {@code targetId} is
 * the row id, and the key comes from {@code submit.keyUrl}, an endpoint that answers
 * {@link JobKeyLedger#response(IdempotencyKey)}.  The shape rules live in {@link JobLogChecks}, shared with the page
 * card.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	RegionDef <jv>region</jv> = JobLogRegion.<jsm>create</jsm>(<js>"restock-species"</js>)
 * 		.submit(JobLogRegion.Submit.<jsm>create</jsm>(<js>"/jobs/species/{id}/restock"</js>, <js>"restock-species"</js>, <js>"/jobs/species/{id}/key"</js>)
 * 			.submitLabel(<js>"Restock"</js>)
 * 			.field(JobLogRegion.Field.<jsm>of</jsm>(<js>"count"</js>).label(<js>"How many"</js>).type(<js>"number"</js>).defaultValue(1))
 * 			.field(JobLogRegion.Field.<jsm>of</jsm>(<js>"species"</js>).type(<js>"hidden"</js>).source(<js>"row.species"</js>)))
 * 		.output(JobLogRegion.Output.<jsm>field</jsm>(<js>"available"</js>).from(<js>"row.available"</js>))
 * 		.toRegion();
 * </p>
 *
 * @since 10.0.0
 */
public final class JobLogRegion {

	/** The populator name. */
	public static final String POPULATOR = "job-log";

	/** The smallest accepted {@code height}, in pixels; matches the page card's floor. */
	public static final int MIN_HEIGHT = 80;

	/** The top-level config keys, in emission order. */
	public static final List<String> KEYS = List.of("submit", "detail", "height", "clearOnSubmit", "output");

	/** Region id. */
	public final String id;

	private final Map<String,Object> config = new LinkedHashMap<>();

	private JobLogRegion(String id) {
		this.id = id;
	}

	/**
	 * Creates a declaration.
	 *
	 * @param id The region id.
	 * @return A new declaration.
	 */
	public static JobLogRegion create(String id) {
		return new JobLogRegion(id);
	}

	/**
	 * Creates a declaration from a parsed map (a datatables card's {@code detail.region.config}).
	 *
	 * @param id The region id.
	 * @param config The {@code config} object; keys must be a subset of {@link #KEYS}.
	 * @return A new declaration (not yet validated).
	 * @throws IllegalArgumentException If {@code config} is not an object or has an unknown key.
	 */
	public static JobLogRegion fromMap(String id, Map<String,?> config) {
		var d = create(id);
		if (config == null)
			return d;
		for (var e : config.entrySet()) {
			var k = e.getKey();
			if ("urlParam".equals(k))
				throw iaex("JobLogRegion '%s' urlParam is not allowed in a row-detail region; a page reload does not reattach.", id);
			if (! KEYS.contains(k))
				throw iaex("JobLogRegion '%s' unknown key '%s'; allowed: %s.", id, k, KEYS);
			d.config.put(k, e.getValue());
		}
		return d;
	}

	/** @param value The submit declaration. @return This object. */
	public JobLogRegion submit(Submit value) { config.put("submit", value.map); return this; }
	/** @param value The optional field grid painted above the form. @return This object. */
	public JobLogRegion detail(Detail value) { config.put("detail", value.map); return this; }
	/** @param px The log pane height, in pixels. @return This object. */
	public JobLogRegion height(int px) { config.put("height", px); return this; }
	/** @param value Whether a submit clears the log first. @return This object. */
	public JobLogRegion clearOnSubmit(boolean value) { config.put("clearOnSubmit", value); return this; }
	/** @param value Where a terminal result is written. @return This object. */
	public JobLogRegion output(Output value) { config.put("output", value.map); return this; }

	/**
	 * Validates this declaration (fail-closed).
	 *
	 * @return This object.
	 * @throws IllegalArgumentException If a rule is broken.
	 */
	public JobLogRegion validate() {
		if (id == null || id.isBlank())
			throw iaex("JobLogRegion id must not be blank.");
		try {
			if (! (config.get("submit") instanceof Map<?,?> submit))
				throw iaex("requires a 'submit' object.");
			JobLogChecks.submit(submit, JobLogChecks.Mode.REGION);
			if (config.containsKey("detail"))
				JobLogChecks.detail(config.get("detail"));
			JobLogChecks.height(config.get("height"), MIN_HEIGHT);
			JobLogChecks.clearOnSubmit(config.get("clearOnSubmit"));
			if (config.containsKey("output"))
				JobLogChecks.output(config.get("output"));
		} catch (IllegalArgumentException e) {
			throw iaex("JobLogRegion '%s' %s", id, e.getMessage());
		}
		return this;
	}

	/**
	 * Returns the config in canonical key order, so the builder and the console-card path emit the same sidecar.
	 *
	 * @return A new ordered map.
	 */
	public Map<String,Object> toMap() {
		var m = order(config, KEYS);
		if (m.get("submit") instanceof Map<?,?> s) {
			// A region always uses the action envelope; stamp it so an authored config that left it implicit emits the
			// same sidecar as the builder.
			var withEnvelope = new LinkedHashMap<Object,Object>(s);
			withEnvelope.putIfAbsent("envelope", "action");
			var submit = order(withEnvelope, JobLogChecks.REGION_SUBMIT_KEYS);
			if (submit.get("fields") instanceof List<?> l)
				submit.put("fields", l.stream().map(f -> f instanceof Map<?,?> fm ? order(fm, JobLogChecks.FIELD_KEYS) : f).toList());
			m.put("submit", submit);
		}
		if (m.get("detail") instanceof Map<?,?> d)
			m.put("detail", order(d, JobLogChecks.DETAIL_KEYS));
		if (m.get("output") instanceof Map<?,?> o) {
			var output = order(o, JobLogChecks.OUTPUT_KEYS);
			if (output.get("target") instanceof Map<?,?> t)
				output.put("target", order(t, List.of("field", "selector")));
			m.put("output", output);
		}
		return m;
	}

	private static Map<String,Object> order(Map<?,?> in, List<String> keys) {
		var m = new LinkedHashMap<String,Object>();
		for (var k : keys)
			if (in.containsKey(k))
				m.put(k, in.get(k));
		for (var e : in.entrySet())
			if (! m.containsKey(String.valueOf(e.getKey())))
				m.put(String.valueOf(e.getKey()), e.getValue());
		return m;
	}

	/**
	 * Validates and compiles this declaration.
	 *
	 * @return A validated row-detail region.
	 */
	public RegionDef toRegion() {
		validate();
		var r = RegionDef.create(id).type(RegionDef.TYPE_ROW_DETAIL).populate(POPULATOR).allowPopulators(POPULATOR).config(toMap());
		r.validate();
		return r;
	}

	/** The {@code submit} declaration. */
	public static final class Submit {
		final Map<String,Object> map = new LinkedHashMap<>();

		private Submit() {}

		/**
		 * Creates a submit declaration.
		 *
		 * @param endpoint The same-origin submit path template; {@code {id}} is the only token.
		 * @param action The action id the key is bound to.
		 * @param keyUrl The same-origin key-endpoint path template; {@code {id}} is the only token.
		 * @return A new declaration.
		 */
		public static Submit create(String endpoint, String action, String keyUrl) {
			var s = new Submit();
			s.map.put("endpoint", endpoint);
			s.map.put("envelope", "action");
			s.map.put("action", action);
			s.map.put("keyUrl", keyUrl);
			return s;
		}

		/** @param value {@code POST} (the default) or {@code PUT}. @return This object. */
		public Submit method(String value) { map.put("method", value); return this; }
		/** @param value The submit button's label. @return This object. */
		public Submit submitLabel(String value) { map.put("submitLabel", value); return this; }

		/**
		 * Adds a field.
		 *
		 * @param value The field.
		 * @return This object.
		 */
		@SuppressWarnings("unchecked")
		public Submit field(Field value) {
			((List<Object>)map.computeIfAbsent("fields", k -> new ArrayList<>())).add(value.map);
			return this;
		}
	}

	/** One {@code submit.fields} entry. */
	public static final class Field {
		final Map<String,Object> map = new LinkedHashMap<>();

		private Field() {}

		/**
		 * @param name The field name, sent under {@code fields}.
		 * @return A new field.
		 */
		public static Field of(String name) {
			var f = new Field();
			f.map.put("name", name);
			return f;
		}

		/** @param value The label. @return This object. */
		public Field label(String value) { map.put("label", value); return this; }
		/** @param value {@code text}, {@code textarea}, {@code number} or {@code hidden}. @return This object. */
		public Field type(String value) { map.put("type", value); return this; }
		/** @param value The placeholder. @return This object. */
		public Field placeholder(String value) { map.put("placeholder", value); return this; }
		/** @param value Whether the field must be filled in. @return This object. */
		public Field required(boolean value) { map.put("required", value); return this; }
		/** @param value The literal default, or the fallback when {@link #source(String)} is absent. @return This object. */
		public Field defaultValue(Object value) { map.put("default", value); return this; }
		/** @param value {@code row.<field>}: the parent row's own property. @return This object. */
		public Field source(String value) { map.put("source", value); return this; }
	}

	/** The optional field grid painted above the form, whose slots {@code output.target.field} can address. */
	public static final class Detail {
		final Map<String,Object> map = new LinkedHashMap<>();

		private Detail() {}

		/**
		 * @param renderer The region helper that paints the grid, for example {@code fieldGrid}.
		 * @param fields The field catalog.
		 * @return A new grid declaration.
		 */
		public static Detail of(String renderer, RegionDef.Field...fields) {
			var d = new Detail();
			d.map.put("renderer", renderer);
			d.map.put("fields", Arrays.stream(fields).map(RegionDef.Field::toContractMap).toList());
			return d;
		}
	}

	/** Where a terminal result is written. */
	public static final class Output {
		final Map<String,Object> map = new LinkedHashMap<>();

		private Output() {}

		/**
		 * @param name The {@code data-juneau-field} name to write, within the region.
		 * @return A new output.
		 */
		public static Output field(String name) {
			var o = new Output();
			o.map.put("target", new LinkedHashMap<>(Map.of("field", name)));
			return o;
		}

		/**
		 * @param css A selector resolved within the region.
		 * @return A new output.
		 */
		public static Output selector(String css) {
			var o = new Output();
			o.map.put("target", new LinkedHashMap<>(Map.of("selector", css)));
			return o;
		}

		/** @param value {@code replace} (the default) or {@code append}. @return This object. */
		public Output mode(String value) { map.put("mode", value); return this; }
		/** @param value {@code message} (the default) or {@code row.<key>}. @return This object. */
		public Output from(String value) { map.put("from", value); return this; }
		/** @param value {@code success} (the default) or {@code any}. @return This object. */
		public Output when(String value) { map.put("when", value); return this; }
	}
}
