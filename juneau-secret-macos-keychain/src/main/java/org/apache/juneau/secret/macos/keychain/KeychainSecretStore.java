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
package org.apache.juneau.secret.macos.keychain;

import static java.nio.charset.StandardCharsets.*;
import static java.util.concurrent.TimeUnit.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.nio.*;
import java.util.*;

import org.apache.juneau.commons.secret.*;

/**
 * A {@link SecretStore} backed by the macOS <c>security</c> keychain CLI (generic passwords).
 *
 * <p>
 * Each operation shells out to <c>/usr/bin/security</c>:
 * <ul>
 * 	<li>{@link #store} &rarr; <c>add-generic-password -U</c> (create or update).
 * 	<li>{@link #find} &rarr; <c>find-generic-password -w</c> (print the password).
 * 	<li>{@link #exists} &rarr; <c>find-generic-password</c> (presence only, without the password).
 * 	<li>{@link #delete} &rarr; <c>delete-generic-password</c>.
 * </ul>
 * Items are namespaced by a caller-supplied <i>service</i> name (the keychain <c>-s</c> attribute) with the secret
 * key as the <i>account</i> (<c>-a</c>).  Choose a stable, collision-resistant service name (for example a
 * reverse-DNS string) so unrelated consumers do not clobber each other's entries.
 *
 * <p>
 * <b>Platform.</b> Only macOS is supported; on any other OS, or when <c>/usr/bin/security</c> is unavailable, calls
 * are treated as a backend failure and resolved per the configured {@link FailMode}.
 *
 * <p>
 * <b>Backend-unavailable behavior.</b> A key that is simply not present is a normal <i>absent</i> result, never a
 * failure.  A genuine failure &mdash; the tool missing, a non-zero exit that is not "item not found", or a timeout
 * &mdash; is resolved by the {@link FailMode} chosen at construction: {@link FailMode#FAIL_OPEN} degrades to
 * absent/no-op, while {@link FailMode#FAIL_CLOSED} (the default) throws.
 *
 * <p>
 * <b>Secret handling.</b> {@link #store} never places the secret on the {@code security} process's argv: it invokes
 * <c>add-generic-password</c> with a bare, valueless <c>-w</c> (which makes the CLI prompt) and writes the secret,
 * followed by a newline, twice to the child process's stdin &mdash; once for the value and once for the CLI's own
 * confirmation re-entry &mdash; so the secret is never visible in the process table (e.g. to {@code ps}).  Neither
 * {@link #store} nor {@link #find} materializes the secret as a {@link String}: {@code store} encodes the
 * {@code char[]} directly to the stdin byte payload, and {@code find} decodes the retrieved bytes into a
 * {@code char[]}.  The value is never logged or {@code toString()}'d by this class.  Because the confirmation
 * prompt splits input on newlines, a secret containing {@code '\n'} or {@code '\r'} cannot be delivered this way
 * and is rejected by {@link #store} before any process is started.
 *
 * @since 10.0.0
 */
public class KeychainSecretStore implements SecretStore {

	/** The macOS keychain "item not found" exit status ({@code errSecItemNotFound}). */
	static final int NOT_FOUND = 44;

	/** Default per-call timeout, in seconds, bounding a slow/hung {@code security} invocation. */
	public static final long DEFAULT_TIMEOUT_SECONDS = 15L;

	private static final String SECURITY = "/usr/bin/security";
	private static final String FIND_GENERIC_PASSWORD = "find-generic-password";

	private final String service;
	private final FailMode failMode;
	private final long timeoutSeconds;
	private final String binary;

	/**
	 * Constructor using {@link FailMode#FAIL_CLOSED} and the {@link #DEFAULT_TIMEOUT_SECONDS default} timeout.
	 *
	 * @param service The keychain service name that namespaces this store's items.  Must not be <jk>null</jk> or blank.
	 */
	public KeychainSecretStore(String service) {
		this(service, FailMode.FAIL_CLOSED);
	}

	/**
	 * Constructor using the {@link #DEFAULT_TIMEOUT_SECONDS default} timeout.
	 *
	 * @param service The keychain service name that namespaces this store's items.  Must not be <jk>null</jk> or blank.
	 * @param failMode The policy applied on a backend failure.  Must not be <jk>null</jk>.
	 */
	public KeychainSecretStore(String service, FailMode failMode) {
		this(service, failMode, DEFAULT_TIMEOUT_SECONDS);
	}

	/**
	 * Constructor.
	 *
	 * @param service The keychain service name that namespaces this store's items.  Must not be <jk>null</jk> or blank.
	 * @param failMode The policy applied on a backend failure.  Must not be <jk>null</jk>.
	 * @param timeoutSeconds The per-call timeout, in seconds, bounding a slow/hung {@code security} invocation.  Must be {@code > 0}.
	 */
	public KeychainSecretStore(String service, FailMode failMode, long timeoutSeconds) {
		this(service, failMode, timeoutSeconds, SECURITY);
	}

	KeychainSecretStore(String service, FailMode failMode, long timeoutSeconds, String binary) {
		this.service = assertArgNotNullOrBlank("service", service);
		this.failMode = assertArgNotNull("failMode", failMode);
		assertArg(timeoutSeconds > 0, "Argument 'timeoutSeconds' must be > 0.");
		this.timeoutSeconds = timeoutSeconds;
		this.binary = binary;
	}

	@Override /* SecretStore */
	public void store(String key, char[] secret) {
		assertArgNotNull("key", key);
		assertArgNotNull("secret", secret);
		assertArg(! containsNewline(secret), "Argument 'secret' must not contain a newline or carriage return character.");
		var payload = stdinConfirmationPayload(secret);
		try {
			// A bare, valueless "-w" makes the CLI prompt (twice, for confirmation) on stdin/stderr instead
			// of accepting the password as an argv element, so the secret never appears in the process table.
			var r = run(payload, "add-generic-password", "-U", "-a", key, "-s", service, "-w");
			if (r.exit() != 0)
				throw backendFailure("add-generic-password", r);
		} catch (RuntimeException e) {
			if (failMode == FailMode.FAIL_OPEN)
				return;
			throw e;
		} finally {
			Arrays.fill(payload, (byte) 0);
		}
	}

	@Override /* SecretStore */
	public Optional<char[]> find(String key) {
		assertArgNotNull("key", key);
		try {
			var r = run(FIND_GENERIC_PASSWORD, "-a", key, "-s", service, "-w");
			if (r.exit() == 0)
				return o(decodeTrimmed(r.stdout()));
			if (r.exit() == NOT_FOUND)
				return oe();
			throw backendFailure(FIND_GENERIC_PASSWORD, r);
		} catch (RuntimeException e) {
			if (failMode == FailMode.FAIL_OPEN)
				return oe();
			throw e;
		}
	}

	@Override /* SecretStore */
	public boolean exists(String key) {
		assertArgNotNull("key", key);
		try {
			var r = run(FIND_GENERIC_PASSWORD, "-a", key, "-s", service);
			if (r.exit() == 0)
				return true;
			if (r.exit() == NOT_FOUND)
				return false;
			throw backendFailure(FIND_GENERIC_PASSWORD, r);
		} catch (RuntimeException e) {
			if (failMode == FailMode.FAIL_OPEN)
				return false;
			throw e;
		}
	}

	@Override /* SecretStore */
	public boolean delete(String key) {
		assertArgNotNull("key", key);
		try {
			var r = run("delete-generic-password", "-a", key, "-s", service);
			if (r.exit() == 0)
				return true;
			if (r.exit() == NOT_FOUND)
				return false;
			throw backendFailure("delete-generic-password", r);
		} catch (RuntimeException e) {
			if (failMode == FailMode.FAIL_OPEN)
				return false;
			throw e;
		}
	}

	/** The outcome of a single {@code security} invocation. */
	@SuppressWarnings({
		"java:S6218" // stdout can hold retrieved SECRET bytes; identity-based equals/hashCode/toString are intentional so a content-based toString can never surface the secret. Result instances are never compared or printed.
	})
	private record Result(int exit, byte[] stdout, String stderr) {}

	private Result run(String... args) {
		return run(null, args);
	}

	/**
	 * Invokes {@link #binary} with the given arguments, optionally writing {@code stdin} to the child process
	 * before closing its input stream.  {@code stdin}, when non-<jk>null</jk>, is written and the stream closed
	 * <i>before</i> waiting on the process so a CLI that blocks on a stdin prompt (as {@code -w} with no value
	 * does) does not deadlock against this method.
	 */
	private Result run(byte[] stdin, String... args) {
		var cmd = new ArrayList<String>(args.length + 1);
		cmd.add(binary);
		cmd.addAll(Arrays.asList(args));
		try {
			var p = new ProcessBuilder(cmd).start();
			if (stdin != null) {
				try (var os = p.getOutputStream()) {
					os.write(stdin);
				}
			} else {
				p.getOutputStream().close();
			}
			if (! p.waitFor(timeoutSeconds, SECONDS)) {
				p.destroyForcibly();
				throw isex("Timed out invoking '%s' after %s seconds.", binary, timeoutSeconds);
			}
			byte[] out;
			String err;
			try (var is = p.getInputStream(); var es = p.getErrorStream()) {
				out = is.readAllBytes();
				err = new String(es.readAllBytes(), UTF_8);
			}
			return new Result(p.exitValue(), out, err);
		} catch (IOException e) {
			throw rex(e, "Unable to invoke '%s' (is this macOS with the keychain CLI available?).", binary);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw rex(e, "Interrupted while invoking '%s'.", binary);
		}
	}

	private static RuntimeException backendFailure(String subcommand, Result r) {
		return isex("Keychain '%s' failed with exit code %s: %s", subcommand, r.exit(), r.stderr().strip());
	}

	private static boolean containsNewline(char[] secret) {
		for (char c : secret)
			if (c == '\n' || c == '\r')
				return true;
		return false;
	}

	/**
	 * Encodes {@code secret} as UTF-8 and doubles it with a trailing newline after each copy (<c>secret\nsecret\n</c>),
	 * matching the value-then-confirmation prompt that <c>security ... -w</c> (with no value) issues on stdin.
	 * Built directly from the {@code char[]}, without an intermediate {@link String}.
	 */
	private static byte[] stdinConfirmationPayload(char[] secret) {
		var encoded = UTF_8.encode(CharBuffer.wrap(secret));
		var secretBytes = new byte[encoded.remaining()];
		encoded.get(secretBytes);
		try {
			var payload = new byte[(secretBytes.length + 1) * 2];
			System.arraycopy(secretBytes, 0, payload, 0, secretBytes.length);
			payload[secretBytes.length] = '\n';
			System.arraycopy(secretBytes, 0, payload, secretBytes.length + 1, secretBytes.length);
			payload[payload.length - 1] = '\n';
			return payload;
		} finally {
			Arrays.fill(secretBytes, (byte) 0);
		}
	}

	/** Decodes UTF-8 stdout bytes into a char[], dropping a single trailing newline, without an intermediate String. */
	private static char[] decodeTrimmed(byte[] bytes) {
		var len = bytes.length;
		if (len > 0 && bytes[len - 1] == '\n')
			len--;
		if (len > 0 && bytes[len - 1] == '\r')
			len--;
		var cb = UTF_8.decode(ByteBuffer.wrap(bytes, 0, len));
		var chars = new char[cb.remaining()];
		cb.get(chars);
		return chars;
	}
}
