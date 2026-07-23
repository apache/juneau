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
	"java:S115",  // Field/constant identifiers mirror OpenAPI/Swagger wire-format keys (camelCase, dollar-prefixed)
	"java:S1192"  // Duplicate string literals are OpenAPI wire-format keys used in JSON map construction; intentional
})
public class BasicOpenApiProviderSession {

	private static final String OPENAPI_VERSION = "3.1.0";

	private static final String K_openapi = "openapi";
	private static final String K_host = "host";
	private static final String K_basePath = "basePath";
	private static final String K_schemes = "schemes";
	private static final String K_servers = "servers";
	private static final String K_consumes = "consumes";
	private static final String K_produces = "produces";
	private static final String K_paths = "paths";
	private static final String K_parameters = "parameters";
	private static final String K_requestBody = "requestBody";
	private static final String K_responses = "responses";
	private static final String K_content = "content";
	private static final String K_schema = "schema";
	private static final String K_required = "required";
	private static final String K_in = "in";
	private static final String K_name = "name";
	private static final String K_definitions = "definitions";
	private static final String K_securityDefinitions = "securityDefinitions";
	private static final String K_components = "components";
	private static final String K_securitySchemes = "securitySchemes";
	private static final String K_schemas = "schemas";
	private static final String K_$ref = "$ref";
	private static final String K_examples = "examples";
	private static final String K_description = "description";
	private static final String K_type = "type";
	private static final String K_format = "format";
	private static final String K_enum = "enum";
	private static final String K_items = "items";
	private static final String K_default = "default";
	private static final String K_pattern = "pattern";
	private static final String K_minLength = "minLength";
	private static final String K_maxLength = "maxLength";
	private static final String K_minimum = "minimum";
	private static final String K_maximum = "maximum";
	private static final String K_exclusiveMinimum = "exclusiveMinimum";
	private static final String K_exclusiveMaximum = "exclusiveMaximum";
	private static final String K_minItems = "minItems";
	private static final String K_maxItems = "maxItems";
	private static final String K_uniqueItems = "uniqueItems";
	private static final String K_multipleOf = "multipleOf";
	private static final String K_collectionFormat = "collectionFormat";
	private static final String K_properties = "properties";

	private static final String DEFINITIONS_PREFIX = "#/definitions/";
	private static final String COMPONENTS_PREFIX = "#/components/schemas/";
	private static final String FORM_URLENCODED = "application/x-www-form-urlencoded";
	private static final String DEFAULT_MEDIA_TYPE = "application/json";
	private static final String IN_BODY = "body";
	private static final String IN_FORM_DATA = "formData";

	private static final Set<String> PARAMETER_SCHEMA_KEYS = Set.of(
		K_type, K_format, K_enum, K_items, K_default, K_pattern,
		K_minLength, K_maxLength, K_minimum, K_maximum, K_exclusiveMinimum, K_exclusiveMaximum,
		K_minItems, K_maxItems, K_uniqueItems, K_multipleOf, K_collectionFormat
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
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for sequential field-by-field rewrite of spec map
	})
	static Json5Map transform(Json5Map swagger) {
		var out = new Json5Map();
		out.put(K_openapi, OPENAPI_VERSION);

		copyIfPresent(swagger, out, "info");
		copyIfPresent(swagger, out, "tags");
		copyIfPresent(swagger, out, "externalDocs");

		// Build servers from host / basePath / schemes.
		var servers = buildServers(swagger);
		if (! servers.isEmpty())
			out.put(K_servers, servers);

		var topConsumes = listOfStrings(swagger.get(K_consumes));
		var topProduces = listOfStrings(swagger.get(K_produces));

		// Paths: rewrite each operation.
		var paths = swagger.get(K_paths);
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
			out.put(K_paths, newPaths);
		}

		// definitions → components.schemas
		var components = new Json5Map();
		var defs = swagger.get(K_definitions);
		if (defs instanceof Map<?,?> defs2 && ! defs2.isEmpty()) {
			var schemas = new Json5Map();
			for (var e : defs2.entrySet())
				schemas.put(String.valueOf(e.getKey()), rewriteRefs(e.getValue()));
			components.put(K_schemas, schemas);
		}
		// securityDefinitions → components.securitySchemes
		var secDefs = swagger.get(K_securityDefinitions);
		if (secDefs instanceof Map<?,?> secDefs2 && ! secDefs2.isEmpty()) {
			var schemes = new Json5Map();
			for (var e : secDefs2.entrySet())
				schemes.put(String.valueOf(e.getKey()), rewriteRefs(e.getValue()));
			components.put(K_securitySchemes, schemes);
		}
		if (! components.isEmpty())
			out.put(K_components, components);

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

		var components = (Json5Map) doc.get(K_components);
		if (components == null)
			components = new Json5Map();
		var schemas = (Json5Map) components.get(K_schemas);
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
			ref.put(K_$ref, COMPONENTS_PREFIX + name);
			for (var s : occurrences)
				s.replaceWith(new Json5Map(ref));
			hoisted = true;
		}

		if (hoisted) {
			if (! schemas.isEmpty())
				components.put(K_schemas, schemas);
			if (! components.isEmpty())
				doc.put(K_components, components);
		}
		return doc;
	}

	private static void collectOperationSchemas(Json5Map doc, Map<String,List<SchemaSite>> sites) {
		var paths = doc.get(K_paths);
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
		var params = op.get(K_parameters);
		if (params instanceof List<?> params2) {
			for (var p : params2) {
				if (p instanceof Map<?,?> p2)
					visitSchemaSlot(toJson5Map(p2), K_schema, sites);
			}
		}
		var requestBody = op.get(K_requestBody);
		if (requestBody instanceof Map<?,?> requestBody2)
			visitContent(toJson5Map(requestBody2), sites);
		var responses = op.get(K_responses);
		if (responses instanceof Map<?,?> responses2) {
			for (var re : responses2.entrySet()) {
				if (re.getValue() instanceof Map<?,?> r)
					visitContent(toJson5Map(r), sites);
			}
		}
	}

	private static void visitContent(Json5Map holder, Map<String,List<SchemaSite>> sites) {
		var content = holder.get(K_content);
		if (! (content instanceof Map<?,?> content2))
			return;
		for (var ce : content2.entrySet()) {
			if (ce.getValue() instanceof Map<?,?> media)
				visitSchemaSlot(toJson5Map(media), K_schema, sites);
		}
	}

	private static void visitSchemaSlot(Json5Map parent, String key, Map<String,List<SchemaSite>> sites) {
		var v = parent.get(key);
		if (! (v instanceof Map<?,?> v2))
			return;
		var schema = toJson5Map(v2);
		// Re-attach the normalized map so subsequent replaceWith() updates the document.
		parent.put(key, schema);
		if (schema.containsKey(K_$ref))
			return;
		if (schema.isEmpty())
			return;
		var canonical = canonicalize(schema);
		sites.computeIfAbsent(canonical, k -> new ArrayList<>()).add(new SchemaSite(parent, key));
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
		"java:S3776", // Cognitive complexity acceptable for OpenAPI operation transformation logic
		"java:S6541"  // Brain Method acceptable for OpenAPI operation transformation dispatch
	})
	private static Json5Map transformOperation(Json5Map op, List<String> topConsumes, List<String> topProduces) {
		var newOp = new Json5Map();
		for (var e : op.entrySet()) {
			var k = e.getKey();
			if (K_parameters.equals(k) || K_responses.equals(k) || K_consumes.equals(k) || K_produces.equals(k))
				continue;
			newOp.put(k, e.getValue());
		}
		var consumes = listOfStrings(op.get(K_consumes));
		if (consumes.isEmpty())
			consumes = topConsumes;
		var produces = listOfStrings(op.get(K_produces));
		if (produces.isEmpty())
			produces = topProduces;

		var oldParams = op.get(K_parameters);
		var newParams = new ArrayList<Object>();
		Json5Map requestBody = null;
		Json5Map formSchema = null;
		var formRequired = new LinkedHashSet<String>();

		if (oldParams instanceof List<?> oldParams2) {
			for (var p : oldParams2) {
				if (! (p instanceof Map<?,?> p2))
					continue;
				var pmap = toJson5Map(p2);
				var in = String.valueOf(pmap.getOrDefault(K_in, ""));
				if (IN_BODY.equals(in)) {
					requestBody = bodyParameterToRequestBody(pmap, consumes);
				} else if (IN_FORM_DATA.equals(in)) {
					if (formSchema == null) {
						formSchema = new Json5Map();
						formSchema.put(K_type, "object");
						formSchema.put(K_properties, new Json5Map());
					}
					var name = String.valueOf(pmap.getOrDefault(K_name, ""));
					if (! name.isEmpty()) {
						var props = (Json5Map) formSchema.get(K_properties);
						props.put(name, extractInlineSchema(pmap));
						if (isTrue(pmap.get(K_required)))
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
				formSchema.put(K_required, new ArrayList<>(formRequired));
			var content = new Json5Map();
			var media = new Json5Map();
			media.put(K_schema, formSchema);
			content.put(FORM_URLENCODED, media);
			requestBody.put(K_content, content);
		}
		if (requestBody != null)
			newOp.put(K_requestBody, requestBody);

		if (! newParams.isEmpty())
			newOp.put(K_parameters, newParams);

		var oldResponses = op.get(K_responses);
		if (oldResponses instanceof Map<?,?> oldResponses2) {
			var newResponses = new Json5Map();
			for (var re : oldResponses2.entrySet()) {
				var code = String.valueOf(re.getKey());
				if (re.getValue() instanceof Map<?,?> rm)
					newResponses.put(code, transformResponse(toJson5Map(rm), produces));
				else
					newResponses.put(code, re.getValue());
			}
			newOp.put(K_responses, newResponses);
		}
		return newOp;
	}

	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for OpenAPI body-parameter to request-body conversion
	})
	private static Json5Map bodyParameterToRequestBody(Json5Map p, List<String> consumes) {
		var rb = new Json5Map();
		if (p.containsKey(K_description))
			rb.put(K_description, p.get(K_description));
		if (isTrue(p.get(K_required)))
			rb.put(K_required, Boolean.TRUE);
		var schema = p.get(K_schema);
		if (schema == null && p.containsKey(K_type))
			schema = extractInlineSchema(p);
		var content = new Json5Map();
		var media = consumes.isEmpty() ? List.of(DEFAULT_MEDIA_TYPE) : consumes;
		for (var mt : media) {
			var entry = new Json5Map();
			if (schema != null)
				entry.put(K_schema, schema);
			content.put(mt, entry);
		}
		rb.put(K_content, content);
		return rb;
	}

	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for the schema vs examples-only content-block emission; the two branches differ in how each media entry is populated and are asserted by OpenAPI output tests.
	})
	private static Json5Map transformResponse(Json5Map response, List<String> produces) {
		var newResp = new Json5Map();
		for (var e : response.entrySet()) {
			var k = e.getKey();
			if (K_schema.equals(k) || K_examples.equals(k))
				continue;
			newResp.put(k, e.getValue());
		}
		var schema = response.get(K_schema);
		if (schema != null) {
			var content = new Json5Map();
			var media = produces.isEmpty() ? List.of(DEFAULT_MEDIA_TYPE) : produces;
			for (var mt : media) {
				var entry = new Json5Map();
				entry.put(K_schema, schema);
				addExamplesForMedia(response, mt, entry);
				content.put(mt, entry);
			}
			newResp.put(K_content, content);
		} else if (response.containsKey(K_examples)) {
			// Examples without schema — still surface under content blocks.
			var content = new Json5Map();
			var media = produces.isEmpty() ? List.of(DEFAULT_MEDIA_TYPE) : produces;
			for (var mt : media) {
				var entry = new Json5Map();
				addExamplesForMedia(response, mt, entry);
				if (! entry.isEmpty())
					content.put(mt, entry);
			}
			if (! content.isEmpty())
				newResp.put(K_content, content);
		}
		return newResp;
	}

	private static void addExamplesForMedia(Json5Map response, String mediaType, Json5Map entry) {
		var examples = response.get(K_examples);
		if (examples instanceof Map<?,?> examples2 && examples2.containsKey(mediaType))
			entry.put("example", examples2.get(mediaType));
	}

	private static Json5Map transformQueryHeaderPathParameter(Json5Map p) {
		var newP = new Json5Map();
		var schema = new Json5Map();
		for (var e : p.entrySet()) {
			var k = e.getKey();
			if (PARAMETER_SCHEMA_KEYS.contains(k)) {
				if (! K_collectionFormat.equals(k))
					schema.put(k, e.getValue());
			} else {
				newP.put(k, e.getValue());
			}
		}
		if (! schema.isEmpty()) {
			var existing = (Json5Map) newP.get(K_schema);
			if (existing == null)
				newP.put(K_schema, schema);
			else
				schema.forEach(existing::putIfAbsent);
		}
		return newP;
	}

	private static Json5Map extractInlineSchema(Json5Map p) {
		var schema = new Json5Map();
		for (var e : p.entrySet()) {
			var k = e.getKey();
			if ((PARAMETER_SCHEMA_KEYS.contains(k) && ! K_collectionFormat.equals(k)) || K_$ref.equals(k))
				schema.put(k, e.getValue());
		}
		return schema;
	}

	private static List<Object> buildServers(Json5Map swagger) {
		var servers = new ArrayList<Object>();
		var host = String.valueOf(swagger.getOrDefault(K_host, ""));
		var basePath = String.valueOf(swagger.getOrDefault(K_basePath, ""));
		var schemes = listOfStrings(swagger.get(K_schemes));
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
		var out = new ArrayList<String>(o2.size());
		for (var v : o2)
			out.add(String.valueOf(v));
		return out;
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
				if (K_$ref.equals(k) && v instanceof String v2 && v2.startsWith(DEFINITIONS_PREFIX))
					copy.put(k, COMPONENTS_PREFIX + v2.substring(DEFINITIONS_PREFIX.length()));
				else
					copy.put(k, rewriteRefs(v));
			}
			return copy;
		}
		if (o instanceof List<?> o2) {
			var copy = new ArrayList<>(o2.size());
			for (var v : o2)
				copy.add(rewriteRefs(v));
			return copy;
		}
		if (nn(o))
			return o;
		return null;
	}
}
