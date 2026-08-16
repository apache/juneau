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

package org.apache.juneau.releng.nexus;

import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.function.Supplier;
import org.apache.juneau.http.Content;
import org.apache.juneau.http.Path;
import org.apache.juneau.http.header.ContentType;
import org.apache.juneau.http.response.BadRequest;
import org.apache.juneau.rest.server.Rest;
import org.apache.juneau.rest.server.RestGet;
import org.apache.juneau.rest.server.RestPost;
import org.apache.juneau.rest.server.RestResponse;
import org.apache.juneau.rest.server.servlet.BasicRestServlet;

/**
 * An in-app loopback mock of the Apache Nexus 2 staging suite, mounted at {@code /mock/nexus/*}. It
 * lets the real {@link NexusStagingClient} run its full discovery/close/promote/drop HTTP flow against a
 * stateful in-memory model ({@link NexusMockModel}) with zero canonical side effects. Always registered so
 * a Dry-run on a LIVE box can still hit it; LIVE runs use the real Nexus base URL and never call this servlet.
 *
 * <p>All request routing is centralized in {@link #route} so the same mapping backs both this servlet and the
 * in-process transport tests exercise.
 *
 * <p>{@link #route} returns pre-formatted JSON text, not a Java bean. Handler methods below return that text
 * wrapped in a {@link Reader} (rather than as a plain {@code String}) so the framework's {@code ReaderProcessor}
 * pipes it verbatim; a {@code String} return would instead be run through the JSON serializer like any other
 * POJO and come out double-encoded (quoted and escaped), which {@link NexusStagingClient} then fails to parse.
 * The {@code bulk/*} POST handlers take the request body the same way for the symmetric reason: parsing an
 * incoming {@code application/json} body into a plain {@code @Content String} would run it through the JSON
 * <em>parser</em>, which expects a quoted string literal and rejects the actual {@code {"data":...}} object.
 *
 * @serial exclude
 */
@Rest(path = "/mock/nexus/*", title = "Nexus staging loopback mock")
@SuppressWarnings({
	"resource" // Handlers return StringReaders the framework's ReaderProcessor owns and closes; Eclipse JDT @Owning warning is by design.
})
public class NexusMockRest extends BasicRestServlet {

	private static final long serialVersionUID = 1L;

	private final transient NexusMockModel model;

	public NexusMockRest(String profileId) {
		this.model = new NexusMockModel(profileId);
	}

	/** The backing model, for run-start reset wiring and tests. */
	public NexusMockModel model() {
		return model;
	}

	@RestGet(path = "/service/local/staging/profile_repositories/{profileId}", produces = "application/json")
	public Reader profileRepositories(@Path("profileId") String profileId, RestResponse res) {
		return json(res, guarded(() -> route(model, "GET", "/service/local/staging/profile_repositories/" + profileId, null)));
	}

	@RestGet(path = "/service/local/staging/repository/{repoId}", produces = "application/json")
	public Reader repository(@Path("repoId") String repoId, RestResponse res) {
		return json(res, guarded(() -> route(model, "GET", "/service/local/staging/repository/" + repoId, null)));
	}

	@RestPost(path = "/service/local/staging/bulk/close", produces = "application/json")
	public Reader close(@Content Reader body, RestResponse res) throws IOException {
		var text = read(body);
		return json(res, guarded(() -> route(model, "POST", "/service/local/staging/bulk/close", text)));
	}

	@RestPost(path = "/service/local/staging/bulk/promote", produces = "application/json")
	public Reader promote(@Content Reader body, RestResponse res) throws IOException {
		var text = read(body);
		return json(res, guarded(() -> route(model, "POST", "/service/local/staging/bulk/promote", text)));
	}

	@RestPost(path = "/service/local/staging/bulk/drop", produces = "application/json")
	public Reader drop(@Content Reader body, RestResponse res) throws IOException {
		var text = read(body);
		return json(res, guarded(() -> route(model, "POST", "/service/local/staging/bulk/drop", text)));
	}

	/**
	 * The single request-routing table, shared by the servlet endpoints and the transport-level tests. Model
	 * transition failures surface as a runtime exception (mapped to a Nexus-shaped 400 by {@link #guarded}).
	 */
	public static String route(NexusMockModel model, String method, String path, String body) {
		if ("GET".equals(method) && path.contains("/profile_repositories/"))
			return model.profileRepositories();
		if ("GET".equals(method) && path.contains("/repository/"))
			return model.repository(lastSegment(path));
		if ("POST".equals(method) && path.endsWith("/bulk/close")) {
			model.close(NexusMockModel.repoIdFromBody(body));
			return "";
		}
		if ("POST".equals(method) && path.endsWith("/bulk/promote")) {
			model.promote(NexusMockModel.repoIdFromBody(body));
			return "";
		}
		if ("POST".equals(method) && path.endsWith("/bulk/drop")) {
			model.drop(NexusMockModel.repoIdFromBody(body));
			return "";
		}
		throw isex("Unmapped Nexus mock request: %s %s", method, path);
	}

	private static String lastSegment(String path) {
		var i = path.lastIndexOf('/');
		return i < 0 ? path : path.substring(i + 1);
	}

	/** Wraps already-serialized JSON text so the framework streams it as-is instead of re-serializing it. */
	private static Reader json(RestResponse res, String text) {
		res.setHeader(ContentType.APPLICATION_JSON);
		return new StringReader(text);
	}

	private String guarded(Supplier<String> op) {
		try {
			return op.get();
		} catch (RuntimeException e) {
			throw new BadRequest("{\"errors\":[{\"id\":\"*\",\"msg\":\"" + e.getMessage() + "\"}]}");
		}
	}
}
