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
package org.apache.juneau.rest.client.remote;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Tests for {@code RemoteClient$RemoteInvocationHandler}'s return-mode materialization (BEAN/STATUS/NONE/BODY,
 * {@link Optional}/{@link CompletableFuture} wrappers, raw {@link InputStream}/{@link Reader} returns), the gated
 * automatic-retry loop, declared-exception mapping, and {@code @Part}/{@code @Multipart} body assembly.
 */
@SuppressWarnings({
	"resource" // RestClient/RestResponse instances used inline; closed via try-with-resources where needed.
})
class RemoteClient_ReturnModesAndRetry_Test extends TestBase {

	private static HttpServer server;
	private static int port;
	private static final AtomicInteger retryHits = new AtomicInteger();
	private static volatile int failFirstNAttempts;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		port = server.getAddress().getPort();

		server.createContext("/hello", exchange -> {
			var body = "Hello!".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});

		server.createContext("/not-found", exchange -> {
			var body = "missing".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(404, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});

		server.createContext("/error-400", exchange -> {
			exchange.sendResponseHeaders(400, -1);
			exchange.close();
		});

		server.createContext("/empty-body", exchange -> exchange.sendResponseHeaders(200, -1));

		// Fails with 503 for the first `failFirstNAttempts` hits (reset per-test), then succeeds.
		server.createContext("/flaky", exchange -> {
			var n = retryHits.incrementAndGet();
			if (n <= failFirstNAttempts) {
				exchange.sendResponseHeaders(503, -1);
				exchange.close();
				return;
			}
			var body = "recovered".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});

		server.createContext("/method", exchange -> {
			var body = exchange.getRequestMethod().getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});

		server.createContext("/echo-multipart", exchange -> {
			var body = exchange.getRequestBody().readAllBytes();
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});

		server.start();
	}

	@AfterAll
	static void stopServer() {
		if (server != null)
			server.stop(0);
	}

	@BeforeEach
	void resetFlaky() {
		retryHits.set(0);
		failFirstNAttempts = 0;
	}

	private String rootUrl() {
		return "http://localhost:" + port;
	}

	// -----------------------------------------------------------------------
	// A — BEAN return mode, and the BODY-mode BasicHttpResponse/BasicHttpException shortcut
	// -----------------------------------------------------------------------

	@Remote
	interface BeanReturnService {
		@RemoteGet(path = "/hello", returns = RemoteReturn.BEAN)
		Ok getBean();

		@RemoteGet(path = "/hello")
		Ok getBodyModeHttpResponseBean();

		@RemoteGet(path = "/hello")
		byte[] getBytes();

		// A BasicHttpException-typed (rather than BasicHttpResponse-typed) BODY-mode return, to exercise
		// materializeBufferedBody's second isAssignableFrom disjunct.
		@RemoteGet(path = "/not-found")
		NotFound getBodyModeHttpExceptionBean();

		// void/Void BODY-mode return (distinct from the dedicated RemoteReturn.NONE mode): materializeBufferedBody's
		// void.class/Void.class short-circuit.
		@RemoteGet(path = "/hello")
		void getVoidBodyMode();

		@RemoteGet(path = "/hello")
		Void getBoxedVoidBodyMode();
	}

	@Test void a01_beanReturn_materializesFromStatusAndBody() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			var bean = client.remote(BeanReturnService.class).getBean();
			assertNotNull(bean);
			assertEquals(200, bean.getStatusCode());
		}
	}

	@Test void a02_bodyMode_httpResponseBeanShortcut_bypassesParsing() throws Exception {
		// materializeBufferedBody's BasicHttpResponse.isAssignableFrom(returnType) shortcut (line 920) applies even
		// though returns() is left at its BODY default.
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			var bean = client.remote(BeanReturnService.class).getBodyModeHttpResponseBean();
			assertNotNull(bean);
			assertEquals(200, bean.getStatusCode());
		}
	}

	@Test void a03_bodyMode_byteArrayReturn() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			assertArrayEquals("Hello!".getBytes(StandardCharsets.UTF_8), client.remote(BeanReturnService.class).getBytes());
		}
	}

	@Test void a04b_bodyMode_voidReturnType_yieldsNullWithoutError() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			assertDoesNotThrow(() -> client.remote(BeanReturnService.class).getVoidBodyMode());
		}
	}

	@Test void a04c_bodyMode_boxedVoidReturnType_yieldsNull() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			assertNull(client.remote(BeanReturnService.class).getBoxedVoidBodyMode());
		}
	}

	@Test void a04_bodyMode_httpExceptionBeanShortcut_bypassesParsing() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			var bean = client.remote(BeanReturnService.class).getBodyModeHttpExceptionBean();
			assertNotNull(bean);
			assertEquals(404, bean.getStatusCode());
		}
	}

	// -----------------------------------------------------------------------
	// B — STATUS return mode: boolean false branch (>=400), and declared-exception mapping
	// -----------------------------------------------------------------------

	@Remote
	interface StatusAndExceptionService {
		@RemoteGet(path = "/error-400", returns = RemoteReturn.STATUS)
		boolean getStatusBooleanFalse();

		@RemoteGet("/not-found")
		String getMappedToDeclaredException() throws NotFound;

		@RemoteGet(path = "/not-found", throwOnError = true)
		String getUndeclaredErrorThrowsGeneric();

		// A 400 response against a method that declares a non-BasicHttpException type (IOException, never matches
		// the isAssignableFrom check) followed by a BasicHttpException type whose STATUS_CODE doesn't match (404 !=
		// 400): neither declared type matches, so the response falls through to normal body handling.
		@RemoteGet("/error-400")
		String getNeitherDeclaredExceptionMatches() throws IOException, NotFound;

		@RemoteGet(path = "/hello", returns = RemoteReturn.STATUS)
		Integer getStatusBoxedInteger();

		@RemoteGet(path = "/hello", returns = RemoteReturn.STATUS)
		Boolean getStatusBoxedBoolean();

		// Neither int/Integer nor boolean/Boolean -- exercises STATUS mode's final "yield sc;" fallback (line 840).
		@RemoteGet(path = "/hello", returns = RemoteReturn.STATUS)
		Object getStatusFallbackType();
	}

	@Test void b01_statusBoolean_errorStatus_isFalse() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			assertFalse(client.remote(StatusAndExceptionService.class).getStatusBooleanFalse());
		}
	}

	@Test void b02_declaredExceptionType_matchingStatus_thrown() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			assertThrows(NotFound.class, () -> client.remote(StatusAndExceptionService.class).getMappedToDeclaredException());
		}
	}

	@Test void b03_undeclaredError_throwOnError_throwsGenericBasicHttpException() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			var ex = assertThrows(BasicHttpException.class, () -> client.remote(StatusAndExceptionService.class).getUndeclaredErrorThrowsGeneric());
			assertEquals(404, ex.getStatusCode());
		}
	}

	@Test void b04_neitherDeclaredExceptionMatches_fallsThroughToNormalBodyHandling() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			assertDoesNotThrow(() -> client.remote(StatusAndExceptionService.class).getNeitherDeclaredExceptionMatches());
		}
	}

	@Test void b05_statusMode_boxedIntegerReturnType() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			assertEquals(200, client.remote(StatusAndExceptionService.class).getStatusBoxedInteger());
		}
	}

	@Test void b06_statusMode_boxedBooleanReturnType() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			assertEquals(Boolean.TRUE, client.remote(StatusAndExceptionService.class).getStatusBoxedBoolean());
		}
	}

	@Test void b07_statusMode_neitherIntNorBoolean_fallsBackToYieldingStatusCode() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			assertEquals(200, client.remote(StatusAndExceptionService.class).getStatusFallbackType());
		}
	}

	// -----------------------------------------------------------------------
	// C — Optional<T> / CompletableFuture<T> / Future<T> BODY wrappers
	// -----------------------------------------------------------------------

	@Remote
	interface WrapperReturnService {
		@RemoteGet("/hello")
		Optional<String> getOptional();

		@RemoteGet("/empty-body")
		Optional<String> getOptionalFromEmptyBody();

		@RemoteGet("/hello")
		CompletableFuture<String> getCompletableFuture();

		@RemoteGet("/hello")
		Future<String> getFuture();
	}

	@Test void c01_optionalReturn_nonEmptyBody_present() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			var result = client.remote(WrapperReturnService.class).getOptional();
			assertTrue(result.isPresent());
			assertEquals("Hello!", result.get());
		}
	}

	@Test void c02_optionalReturn_emptyBody_presentButBlank() throws Exception {
		// The JDK HttpClient transport's BodyHandlers.ofInputStream() always yields a (possibly zero-length) stream
		// rather than a null body reference, so a Content-Length: 0 response still parses to Optional.of("").
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			var result = client.remote(WrapperReturnService.class).getOptionalFromEmptyBody();
			assertTrue(result.isPresent());
			assertEquals("", result.get());
		}
	}

	@Test void c03_completableFutureReturn_completedWithValue() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			var future = client.remote(WrapperReturnService.class).getCompletableFuture();
			assertEquals("Hello!", future.get(5, TimeUnit.SECONDS));
		}
	}

	@Test void c04_futureReturn_completedWithValue() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			var future = client.remote(WrapperReturnService.class).getFuture();
			assertEquals("Hello!", future.get(5, TimeUnit.SECONDS));
		}
	}

	// -----------------------------------------------------------------------
	// D — Raw InputStream / Reader returns (both the live-body and empty-body paths)
	// -----------------------------------------------------------------------

	@Remote
	interface StreamReturnService {
		@RemoteGet("/hello")
		InputStream getStream();

		@RemoteGet("/empty-body")
		InputStream getStreamFromEmptyBody();

		@RemoteGet("/hello")
		Reader getReader();

		@RemoteGet("/empty-body")
		Reader getReaderFromEmptyBody();

		@RemoteGet(path = "/not-found", throwOnError = true)
		InputStream getStreamThrowsBeforeHandingBackStream();

		@RemoteGet(path = "/not-found", throwOnError = true)
		Reader getReaderThrowsBeforeHandingBackReader();
	}

	@Test void d01_streamReturn_liveBody_readableAndClosable() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			try (var in = client.remote(StreamReturnService.class).getStream()) {
				assertEquals("Hello!", new String(in.readAllBytes(), StandardCharsets.UTF_8));
			}
		}
	}

	@Test void d01b_streamReturn_readWithZeroLength_returnsZeroWithoutConsuming() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			try (var in = client.remote(StreamReturnService.class).getStream()) {
				var buf = new byte[4];
				assertEquals(0, in.read(buf, 0, 0));
				assertEquals("Hello!", new String(in.readAllBytes(), StandardCharsets.UTF_8));
			}
		}
	}

	@Test void d02_streamReturn_emptyBody_yieldsEmptyStream() throws Exception {
		// The JDK HttpClient transport always yields a (possibly zero-length) InputStream rather than a null body
		// reference for a Content-Length: 0 response, so processStreamReturn's stream==null short-circuit (which
		// only null-checks resp.getBodyStream()) is not reachable through this transport; other TransportResponse
		// producers may still supply a genuinely null body, which is why the check remains in the source.
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			try (var in = client.remote(StreamReturnService.class).getStreamFromEmptyBody()) {
				assertNotNull(in);
				assertEquals(-1, in.read());
			}
		}
	}

	@Test void d03_readerReturn_liveBody_readableAndClosable() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			try (var r = client.remote(StreamReturnService.class).getReader()) {
				var buf = new char[16];
				var n = r.read(buf);
				assertEquals("Hello!", new String(buf, 0, n));
			}
		}
	}

	@Test void d04_readerReturn_emptyBody_yieldsEmptyReader() throws Exception {
		// See d02's note: this transport never actually produces a null body reference, so the analogous
		// stream==null branch in processReaderReturn is likewise not reachable through it.
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			try (var r = client.remote(StreamReturnService.class).getReaderFromEmptyBody()) {
				assertNotNull(r);
				assertEquals(-1, r.read());
			}
		}
	}

	@Test void d05_streamReturn_throwOnErrorBeforeHandingBackStream_closesResponse() throws Exception {
		// throwIfError(...) fires before the stream is ever handed back, exercising processStreamReturn's
		// "!ok" finally-close branch rather than the normal success path.
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			assertThrows(BasicHttpException.class, () -> client.remote(StreamReturnService.class).getStreamThrowsBeforeHandingBackStream());
		}
	}

	@Test void d06_readerReturn_throwOnErrorBeforeHandingBackReader_closesResponse() throws Exception {
		// Same as d05 but for processReaderReturn's analogous "!ok" finally-close branch.
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			assertThrows(BasicHttpException.class, () -> client.remote(StreamReturnService.class).getReaderThrowsBeforeHandingBackReader());
		}
	}

	// -----------------------------------------------------------------------
	// E — Gated automatic retries
	// -----------------------------------------------------------------------

	@Remote
	interface RetryService {
		@RemoteGet(path = "/flaky", retries = 3)
		String getWithRetries();

		@RemoteGet(path = "/flaky", retries = 1)
		String getWithInsufficientRetries();

		@RemotePost(path = "/flaky", retries = 3)
		String postWithoutRetryNonIdempotent(@Content String body);

		@RemotePost(path = "/flaky", retries = 3, retryNonIdempotent = true)
		String postWithRetryNonIdempotentOptIn(@Content String body);
	}

	@Remote(retryNonIdempotent = true)
	interface InterfaceLevelRetryNonIdempotentService {
		@RemotePost(path = "/flaky", retries = 3)
		String post(@Content String body);
	}

	@Remote
	interface NonRepeatableBodyRetryService {
		// PUT is a retryable verb, but a streaming InputStream body is never repeatable, so the retry loop must
		// refuse to retry regardless (processReturn's "attempt==0 && !isBodyRepeatable()" short-circuit).
		@RemotePut(path = "/flaky", retries = 3)
		String putWithStreamBody(@Content InputStream body);
	}

	@Remote
	interface UnreachableHostRetryService {
		@RemoteGet(path = "/hello", retries = 2)
		String get() throws TransportException;
	}

	@Test void e01_retry_recoversAfterTransientFailures() throws Exception {
		failFirstNAttempts = 2;
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			assertEquals("recovered", client.remote(RetryService.class).getWithRetries());
		}
		assertEquals(3, retryHits.get(), "Expected exactly 3 attempts (2 failures + 1 success)");
	}

	@Test void e02_retry_exhaustsAttempts_returnsLastErrorStatus() throws Exception {
		// More failures than the retry budget: the loop gives up and returns/materializes the last (still-503) response
		// rather than throwing, since throwOnError is not set.
		failFirstNAttempts = 100;
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			assertDoesNotThrow(() -> client.remote(RetryService.class).getWithInsufficientRetries());
		}
		assertEquals(2, retryHits.get(), "Expected exactly 2 attempts (1 retry after the first failure)");
	}

	@Test void e03_retry_postWithoutOptIn_doesNotRetry() throws Exception {
		failFirstNAttempts = 2;
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			assertDoesNotThrow(() -> client.remote(RetryService.class).postWithoutRetryNonIdempotent("x"));
		}
		assertEquals(1, retryHits.get(), "POST without retryNonIdempotent must not auto-retry");
	}

	@Test void e04_retry_postWithOptIn_retries() throws Exception {
		failFirstNAttempts = 2;
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			assertEquals("recovered", client.remote(RetryService.class).postWithRetryNonIdempotentOptIn("x"));
		}
		assertEquals(3, retryHits.get(), "POST with retryNonIdempotent=true should auto-retry like an idempotent verb");
	}

	@Test void e05_retry_interfaceLevelRetryNonIdempotent_retries() throws Exception {
		// Method-level retryNonIdempotent is left at its false default; only the interface-level @Remote attribute
		// opts in, exercising isRetryableVerb's second (interface-level) disjunct.
		failFirstNAttempts = 2;
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			assertEquals("recovered", client.remote(InterfaceLevelRetryNonIdempotentService.class).post("x"));
		}
		assertEquals(3, retryHits.get(), "Interface-level retryNonIdempotent=true should auto-retry POST");
	}

	@Test void e06_retry_nonRepeatableStreamBody_neverRetriedEvenOnRetryableVerb() throws Exception {
		// processReturn's "attempt==0 && !req.isBodyRepeatable()" guard (line 784) short-circuits straight to a
		// single processReturnOnce call, bypassing the retry loop entirely despite PUT being retryable and
		// retries being configured.
		failFirstNAttempts = 100;
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			assertDoesNotThrow(() -> client.remote(NonRepeatableBodyRetryService.class)
				.putWithStreamBody(new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8))));
		}
		assertEquals(1, retryHits.get(), "A non-repeatable streaming body must never be retried");
	}

	@Test void e07_retry_transportExceptionOnEveryAttempt_exhaustsBudgetThenRethrows() throws Exception {
		// A connection that fails at the transport layer (rather than an HTTP error status) on every attempt
		// exercises both outcomes of the TransportException catch: backoff-and-retry while budget remains, then
		// give up and rethrow once attempt exceeds the retry budget (lines 790-794).
		try (var client = RestClient.builder().rootUrl("http://127.0.0.1:1").build()) {
			assertThrows(TransportException.class, () -> client.remote(UnreachableHostRetryService.class).get());
		}
	}

	// -----------------------------------------------------------------------
	// F — @Multipart / @Part body assembly
	// -----------------------------------------------------------------------

	@Remote
	interface MultipartService {
		@RemotePost("/echo-multipart")
		@Multipart
		String upload(@Part("title") String title, @Part(name = "data", contentType = "application/octet-stream") byte[] data);

		@RemotePost("/echo-multipart")
		@Multipart
		String uploadFile(@Part("report") File file);

		@RemotePost("/echo-multipart")
		@Multipart
		String uploadStream(@Part("blob") InputStream in);

		@RemotePost("/echo-multipart")
		@Multipart
		String uploadReader(@Part("text") Reader in);

		@RemotePost("/echo-multipart")
		@Multipart
		String uploadBean(@Part("bean") MultipartBean bean);

		@RemotePost("/echo-multipart")
		@Multipart
		String uploadNullPart(@Part("optional") String optional);
	}

	public static final class MultipartBean {
		public String getX() { return "y"; }
	}

	@Test void f01_multipart_scalarAndByteArrayParts() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			var result = client.remote(MultipartService.class).upload("my-title", new byte[]{1, 2, 3});
			assertNotNull(result);
			assertTrue(result.contains("my-title"), "Expected the title part to appear in the multipart body: " + result);
		}
	}

	@Test void f02_multipart_filePart() throws Exception {
		var tmp = File.createTempFile("juneau-remoteclient-test-", ".txt");
		tmp.deleteOnExit();
		try (var out = new FileWriter(tmp)) {
			out.write("file-contents");
		}
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			var result = client.remote(MultipartService.class).uploadFile(tmp);
			assertTrue(result.contains("file-contents"), "Expected file contents in: " + result);
		}
	}

	@Test void f03_multipart_streamPart() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			var result = client.remote(MultipartService.class).uploadStream(new ByteArrayInputStream("stream-data".getBytes(StandardCharsets.UTF_8)));
			assertTrue(result.contains("stream-data"), "Expected stream contents in: " + result);
		}
	}

	@Test void f04_multipart_readerPart() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			var result = client.remote(MultipartService.class).uploadReader(new StringReader("reader-data"));
			assertTrue(result.contains("reader-data"), "Expected reader contents in: " + result);
		}
	}

	@Test void f05_multipart_beanPart_serializedViaDefaultSerializer() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).defaultSerializer(org.apache.juneau.marshall.json.JsonSerializer.DEFAULT).allowPrivateUrls(true).build()) {
			var result = client.remote(MultipartService.class).uploadBean(new MultipartBean());
			assertTrue(result.contains("\"x\""), "Expected the serialized bean property in: " + result);
		}
	}

	@Test void f06_multipart_nullPart_omitted() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).allowPrivateUrls(true).build()) {
			assertDoesNotThrow(() -> client.remote(MultipartService.class).uploadNullPart(null));
		}
	}
}
