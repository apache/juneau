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
package org.apache.juneau.rest.server.openapi;

import static org.apache.juneau.commons.utils.ObjectUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.bean.openapi3.OpenApi;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.marshall.cp.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.jsonschema.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.swagger.*;

/**
 * A single session of generating an OpenAPI 3.1 document.
 *
 * <p>
 * Reuses the {@link BasicSwaggerProviderSession} machinery to produce a Swagger 2.0 document, then
 * applies the well-known Swagger 2.0 → OpenAPI 3.1 transformation (servers from host/basePath/schemes,
 * requestBody from in:body and in:formData parameters, content negotiation moved to per-response
 * blocks, definitions lifted to components.schemas, $ref rewritten under
 * {@code "#/components/schemas/"}, etc) and parses the resulting JSON into an {@link OpenApi} bean.
 *
 * <p>
 * The transformation runs at the JSON level on the Json5 representation of the Swagger document,
 * which keeps the conversion rules co-located and makes them straightforward to extend without
 * touching the underlying annotation-walker.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ApiDocsMixins">OpenAPI 3.1 Server Emission</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S115", // Field/constant identifiers mirror OpenAPI/Swagger wire-format keys (camelCase, dollar-prefixed)
	"java:S1192", // Duplicate string literals are OpenAPI wire-format keys used in JSON map construction; intentional
	"java:S3776" // transform(), transformOperation() and visitOperation() walk every Swagger 2 section to OpenAPI 3.1 in one pass; splitting them would obscure the mapping
})
public class BasicOpenApiProviderSession {

	private static final String OPENAPI_VERSION = "3.1.0";

	private static final Set<String> PARAMETER_SCHEMA_KEYS = Set.of(
		"type", "format", "enum", "items", "default", "pattern",
		"minLength", "maxLength", "minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum",
		"minItems", "maxItems", "uniqueItems", "multipleOf", "collectionFormat"
	);

	private final BasicSwaggerProviderSession swaggerSession;
	private final JsonParser jp = Json5Parser.create().ignoreUnknownBeanProperties().build();

	/**
	 * Constructor.
	 *
	 * @param context The context of the REST object we're generating an OpenAPI doc for.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param locale The language of the document we're asking for.
	 * @param ff The file finder to use for finding JSON files.
	 * @param messages The messages to use for finding localized strings.
	 * @param vr The variable resolver to use for resolving variables.
	 * @param js The JSON-schema generator to use for stuff like examples.
	 */
	public BasicOpenApiProviderSession(RestContext context, Locale locale, FileFinder ff, Messages messages, VarResolverSession vr, JsonSchemaGeneratorSession js) {
		this.swaggerSession = new BasicSwaggerProviderSession(context, locale, ff, messages, vr, js);
	}

	/**
	 * Generates the OpenAPI 3.1 document.
	 *
	 * @return A new {@link OpenApi} object.
	 * @throws Exception If an error occurred producing the document.
	 */
	@SuppressWarnings({
		"java:S112" // throws Exception intentional - callback/lifecycle method
	})
	public OpenApi getOpenApi() throws Exception {
		var swagger = swaggerSession.getSwagger();
		// Round-trip via Json5 so we work with a plain Json5Map and can apply the spec mapping
		// without touching the typed Swagger / OpenApi beans.
		var swaggerMap = Json5.to(Json5.of(swagger), Json5Map.class);
		if (swaggerMap == null)
			swaggerMap = new Json5Map();
		var openApiMap = transform(swaggerMap);
		var openApiJson = Json5R.of(openApiMap);
		return jp.read(openApiJson, OpenApi.class);
	}

	/**
	 * Apply the Swagger 2.0 → OpenAPI 3.1 mapping to a Json5Map produced by the Swagger session.
	 *
	 * @param swagger The Swagger 2.0 representation as a Json5Map.
	 * @return A fresh {@link Json5Map} representing the OpenAPI 3.1 document.
	 */
	static Json5Map transform(Json5Map swagger) {
		var out = new Json5Map();
		out.put("openapi", OPENAPI_VERSION);

		copyIfPresent(swagger, out, "info");
		copyIfPresent(swagger, out, "tags");
		copyIfPresent(swagger, out, "externalDocs");

		// Build servers from host / basePath / schemes.
		var servers = buildServers(swagger);
		if (! servers.isEmpty())
			out.put("servers", servers);

		var topConsumes = listOfStrings(swagger.get("consumes"));
		var topProduces = listOfStrings(swagger.get("produces"));

		// Paths: rewrite each operation.
		var paths = swagger.get("paths");
		if (paths instanceof Map<?,?> paths2) {
			var newPaths = new Json5Map();
			for (var pe : paths2.entrySet()) {
				var path = String.valueOf(pe.getKey());
				if (! (pe.getValue() instanceof Map<?,?> pathItem))
					continue;
				var newPathItem = new Json5Map();
				for (var oe : pathItem.entrySet()) {
					var method = String.valueOf(oe.getKey());
					if (! (oe.getValue() instanceof Map<?,?> opMap))
						continue;
					newPathItem.put(method, transformOperation(toJson5Map(opMap), topConsumes, topProduces));
				}
				newPaths.put(path, newPathItem);
			}
			out.put("paths", newPaths);
		}

		// definitions → components.schemas
		var components = new Json5Map();
		var defs = swagger.get("definitions");
		if (defs instanceof Map<?,?> defs2 && ! defs2.isEmpty()) {
			var schemas = new Json5Map();
			for (var e : defs2.entrySet())
				schemas.put(String.valueOf(e.getKey()), rewriteRefs(e.getValue()));
			components.put("schemas", schemas);
		}
		// securityDefinitions → components.securitySchemes
		var secDefs = swagger.get("securityDefinitions");
		if (secDefs instanceof Map<?,?> secDefs2 && ! secDefs2.isEmpty()) {
			var schemes = new Json5Map();
			for (var e : secDefs2.entrySet())
				schemes.put(String.valueOf(e.getKey()), rewriteRefs(e.getValue()));
			components.put("securitySchemes", schemes);
		}
		if (! components.isEmpty())
			out.put("components", components);

		// Rewrite all $refs anywhere in the document so subsequent passes work uniformly.
		var rewritten = (Json5Map) rewriteRefs(out);

		// Lift inline schemas that occur in two or more operation slots into components.schemas
		// and replace each occurrence with a $ref. Schemas already carrying a $ref are left alone.
		return deduplicateInlineSchemas(rewritten);
	}

	/**
	 * Walks all operation-level schema slots ({@code parameters[*].schema},
	 * {@code requestBody.content[*].schema}, {@code responses[*].content[*].schema} — and any
	 * {@code schema} key nested within those), and lifts every inline schema that appears two or
	 * more times into {@code components.schemas}, replacing each occurrence with a
	 * {@code $ref: "#/components/schemas/&lt;name&gt;"} pointer. Schemas already carrying a
	 * {@code $ref} are left in place. The lifted entry is keyed by its {@code title} when present
	 * (and that name is not already taken); otherwise a synthesized {@code Schema&lt;N&gt;} name is
	 * assigned with collision avoidance against any existing {@code components.schemas} entries.
	 *
	 * @param doc The OpenAPI document after the initial transform + $ref rewrite pass.
	 * @return The same document with duplicate inline schemas hoisted.
	 */
	static Json5Map deduplicateInlineSchemas(Json5Map doc) {
		var sites = new LinkedHashMap<String,List<SchemaSite>>();
		collectOperationSchemas(doc, sites);
		if (sites.isEmpty())
			return doc;

		var components = (Json5Map) doc.get("components");
		if (components == null)
			components = new Json5Map();
		var schemas = (Json5Map) components.get("schemas");
		if (schemas == null)
			schemas = new Json5Map();
		var existingNames = new LinkedHashSet<>(schemas.keySet());

		var counter = new int[]{0};
		var hoisted = false;
		for (var entry : sites.entrySet()) {
			var occurrences = entry.getValue();
			if (occurrences.size() < 2)
				continue;
			var inline = occurrences.get(0).schema();
			var name = pickSchemaName(inline, existingNames, counter);
			existingNames.add(name);
			schemas.put(name, inline);
			var ref = new Json5Map();
			ref.put("$ref", "#/components/schemas/" + name);
			for (var s : occurrences)
				s.replaceWith(new Json5Map(ref));
			hoisted = true;
		}

		if (hoisted) {
			if (! schemas.isEmpty())
				components.put("schemas", schemas);
			if (! components.isEmpty())
				doc.put("components", components);
		}
		return doc;
	}

	private static void collectOperationSchemas(Json5Map doc, Map<String,List<SchemaSite>> sites) {
		var paths = doc.get("paths");
		if (! (paths instanceof Map<?,?> paths2))
			return;
		for (var pe : paths2.entrySet()) {
			if (! (pe.getValue() instanceof Map<?,?> pathItem))
				continue;
			for (var oe : pathItem.entrySet()) {
				if (! (oe.getValue() instanceof Map<?,?> op))
					continue;
				visitOperation(toJson5Map(op), sites);
			}
		}
	}

	private static void visitOperation(Json5Map op, Map<String,List<SchemaSite>> sites) {
		var params = op.get("parameters");
		if (params instanceof List<?> params2) {
			for (var p : params2) {
				if (p instanceof Map<?,?> p2)
					visitSchemaSlot(toJson5Map(p2), "schema", sites);
			}
		}
		var requestBody = op.get("requestBody");
		if (requestBody instanceof Map<?,?> requestBody2)
			visitContent(toJson5Map(requestBody2), sites);
		var responses = op.get("responses");
		if (responses instanceof Map<?,?> responses2) {
			for (var re : responses2.entrySet()) {
				if (re.getValue() instanceof Map<?,?> r)
					visitContent(toJson5Map(r), sites);
			}
		}
	}

	private static void visitContent(Json5Map holder, Map<String,List<SchemaSite>> sites) {
		var content = holder.get("content");
		if (! (content instanceof Map<?,?> content2))
			return;
		for (var ce : content2.entrySet()) {
			if (ce.getValue() instanceof Map<?,?> media)
				visitSchemaSlot(toJson5Map(media), "schema", sites);
		}
	}

	private static void visitSchemaSlot(Json5Map parent, String key, Map<String,List<SchemaSite>> sites) {
		var v = parent.get(key);
		if (! (v instanceof Map<?,?> v2))
			return;
		var schema = toJson5Map(v2);
		// Re-attach the normalized map so subsequent replaceWith() updates the document.
		parent.put(key, schema);
		if (schema.containsKey("$ref"))
			return;
		if (schema.isEmpty())
			return;
		var canonical = canonicalize(schema);
		sites.computeIfAbsent(canonical, k -> l()).add(new SchemaSite(parent, key));
	}

	private static String canonicalize(Object o) {
		try {
			return Json5Serializer.DEFAULT.copy().sortMaps().build().toString(o);
		} catch (Exception e) {
			return String.valueOf(o);
		}
	}

	private static String pickSchemaName(Json5Map schema, Set<String> taken, int[] counter) {
		var title = schema.getString("title");
		if (nn(title) && ! title.isBlank() && ! taken.contains(title))
			return title;
		while (true) {
			counter[0]++;
			var candidate = "Schema" + counter[0];
			if (! taken.contains(candidate))
				return candidate;
		}
	}

	/** Tracks the parent map plus the slot key where an inline schema lives so we can replace it in-place. */
	private record SchemaSite(Json5Map parent, String key) {
		Json5Map schema() {
			return (Json5Map) parent.get(key);
		}
		void replaceWith(Json5Map replacement) {
			parent.put(key, replacement);
		}
	}

	@SuppressWarnings({
		"java:S6541" // Brain Method acceptable for OpenAPI operation transformation dispatch
	})
	private static Json5Map transformOperation(Json5Map op, List<String> topConsumes, List<String> topProduces) {
		var newOp = new Json5Map();
		for (var e : op.entrySet()) {
			var k = e.getKey();
			if (eqa(k, "parameters", "responses", "consumes", "produces"))
				continue;
			newOp.put(k, e.getValue());
		}
		var consumes = listOfStrings(op.get("consumes"));
		if (consumes.isEmpty())
			consumes = topConsumes;
		var produces = listOfStrings(op.get("produces"));
		if (produces.isEmpty())
			produces = topProduces;

		var oldParams = op.get("parameters");
		var newParams = new ArrayList<Object>();
		Json5Map requestBody = null;
		Json5Map formSchema = null;
		var formRequired = new LinkedHashSet<String>();

		if (oldParams instanceof List<?> oldParams2) {
			for (var p : oldParams2) {
				if (! (p instanceof Map<?,?> p2))
					continue;
				var pmap = toJson5Map(p2);
				var in = String.valueOf(pmap.getOrDefault("in", ""));
				if (eq(in, "body")) {
					requestBody = bodyParameterToRequestBody(pmap, consumes);
				} else if (eq(in, "formData")) {
					if (formSchema == null) {
						formSchema = new Json5Map();
						formSchema.put("type", "object");
						formSchema.put("properties", new Json5Map());
					}
					var name = String.valueOf(pmap.getOrDefault("name", ""));
					if (! name.isEmpty()) {
						var props = (Json5Map) formSchema.get("properties");
						props.put(name, extractInlineSchema(pmap));
						if (isTrue(pmap.get("required")))
							formRequired.add(name);
					}
				} else {
					newParams.add(transformQueryHeaderPathParameter(pmap));
				}
			}
		}

		if (requestBody == null && formSchema != null) {
			requestBody = new Json5Map();
			if (! formRequired.isEmpty())
				formSchema.put("required", new ArrayList<>(formRequired));
			var content = new Json5Map();
			var media = new Json5Map();
			media.put("schema", formSchema);
			content.put("application/x-www-form-urlencoded", media);
			requestBody.put("content", content);
		}
		if (requestBody != null)
			newOp.put("requestBody", requestBody);

		if (! newParams.isEmpty())
			newOp.put("parameters", newParams);

		var oldResponses = op.get("responses");
		if (oldResponses instanceof Map<?,?> oldResponses2) {
			var newResponses = new Json5Map();
			for (var re : oldResponses2.entrySet()) {
				var code = String.valueOf(re.getKey());
				if (re.getValue() instanceof Map<?,?> rm)
					newResponses.put(code, transformResponse(toJson5Map(rm), produces));
				else
					newResponses.put(code, re.getValue());
			}
			newOp.put("responses", newResponses);
		}
		return newOp;
	}

	private static Json5Map bodyParameterToRequestBody(Json5Map p, List<String> consumes) {
		var rb = new Json5Map();
		if (p.containsKey("description"))
			rb.put("description", p.get("description"));
		if (isTrue(p.get("required")))
			rb.put("required", Boolean.TRUE);
		var schema = p.get("schema");
		if (schema == null && p.containsKey("type"))
			schema = extractInlineSchema(p);
		var content = new Json5Map();
		var media = consumes.isEmpty() ? List.of("application/json") : consumes;
		for (var mt : media) {
			var entry = new Json5Map();
			if (schema != null)
				entry.put("schema", schema);
			content.put(mt, entry);
		}
		rb.put("content", content);
		return rb;
	}

	private static Json5Map transformResponse(Json5Map response, List<String> produces) {
		var newResp = new Json5Map();
		for (var e : response.entrySet()) {
			var k = e.getKey();
			if (eqa(k, "schema", "examples"))
				continue;
			newResp.put(k, e.getValue());
		}
		var schema = response.get("schema");
		if (schema != null) {
			var content = new Json5Map();
			var media = produces.isEmpty() ? List.of("application/json") : produces;
			for (var mt : media) {
				var entry = new Json5Map();
				entry.put("schema", schema);
				addExamplesForMedia(response, mt, entry);
				content.put(mt, entry);
			}
			newResp.put("content", content);
		} else if (response.containsKey("examples")) {
			// Examples without schema — still surface under content blocks.
			var content = new Json5Map();
			var media = produces.isEmpty() ? List.of("application/json") : produces;
			for (var mt : media) {
				var entry = new Json5Map();
				addExamplesForMedia(response, mt, entry);
				if (! entry.isEmpty())
					content.put(mt, entry);
			}
			if (! content.isEmpty())
				newResp.put("content", content);
		}
		return newResp;
	}

	private static void addExamplesForMedia(Json5Map response, String mediaType, Json5Map entry) {
		var examples = response.get("examples");
		if (examples instanceof Map<?,?> examples2 && examples2.containsKey(mediaType))
			entry.put("example", examples2.get(mediaType));
	}

	private static Json5Map transformQueryHeaderPathParameter(Json5Map p) {
		var newP = new Json5Map();
		var schema = new Json5Map();
		for (var e : p.entrySet()) {
			var k = e.getKey();
			if (PARAMETER_SCHEMA_KEYS.contains(k)) {
				if (neq(k, "collectionFormat"))
					schema.put(k, e.getValue());
			} else {
				newP.put(k, e.getValue());
			}
		}
		if (! schema.isEmpty()) {
			var existing = (Json5Map) newP.get("schema");
			if (existing == null)
				newP.put("schema", schema);
			else
				schema.forEach(existing::putIfAbsent);
		}
		return newP;
	}

	private static Json5Map extractInlineSchema(Json5Map p) {
		var schema = new Json5Map();
		for (var e : p.entrySet()) {
			var k = e.getKey();
			if ((PARAMETER_SCHEMA_KEYS.contains(k) && neq(k, "collectionFormat")) || eq(k, "$ref"))
				schema.put(k, e.getValue());
		}
		return schema;
	}

	private static List<Object> buildServers(Json5Map swagger) {
		var servers = new ArrayList<Object>();
		var host = String.valueOf(swagger.getOrDefault("host", ""));
		var basePath = String.valueOf(swagger.getOrDefault("basePath", ""));
		var schemes = listOfStrings(swagger.get("schemes"));
		if (host.isEmpty() && basePath.isEmpty() && schemes.isEmpty())
			return servers;
		if (schemes.isEmpty())
			schemes = List.of("http");
		for (var scheme : schemes) {
			var url = scheme + "://" + host + basePath;
			var server = new Json5Map();
			server.put("url", url);
			servers.add(server);
		}
		return servers;
	}

	private static List<String> listOfStrings(Object o) {
		if (! (o instanceof List<?> o2))
			return List.of();
		return o2.stream().map(String::valueOf).collect(Collectors.toCollection(ArrayList::new));
	}

	private static Json5Map toJson5Map(Map<?,?> m) {
		if (m instanceof Json5Map m2)
			return m2;
		var out = new Json5Map();
		for (var e : m.entrySet())
			out.put(String.valueOf(e.getKey()), e.getValue());
		return out;
	}

	private static void copyIfPresent(Json5Map src, Json5Map dst, String key) {
		if (src.containsKey(key))
			dst.put(key, src.get(key));
	}

	/**
	 * Walks any nested {@code Map}/{@code List} and rewrites {@code $ref: "#/definitions/Foo"}
	 * references to the OpenAPI 3.x form {@code "#/components/schemas/Foo"}.
	 *
	 * @param o The input object.
	 * @return A copy with all {@code $ref} entries rewritten.
	 */
	static Object rewriteRefs(Object o) {
		if (o instanceof Map<?,?> o2) {
			var copy = new Json5Map();
			for (var e : o2.entrySet()) {
				var k = String.valueOf(e.getKey());
				var v = e.getValue();
				if (eq(k, "$ref") && v instanceof String v2 && v2.startsWith("#/definitions/"))
					copy.put(k, "#/components/schemas/" + v2.substring("#/definitions/".length()));
				else
					copy.put(k, rewriteRefs(v));
			}
			return copy;
		}
		if (o instanceof List<?> o2) {
			return o2.stream().map(BasicOpenApiProviderSession::rewriteRefs).collect(Collectors.toCollection(ArrayList::new));
		}
		if (nn(o))
			return o;
		return null;
	}
}
