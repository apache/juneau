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
 * 	<li>{@link #find} &rarr; <c>find-generic-password -g</c> (print the password to stderr).
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
 * <b>Secret handling.</b> {@link #store} never places the secret on the {@code security} process's argv, and never
 * relies on the CLI's password prompt (a bare <c>-w</c> makes {@code security} read from the controlling terminal,
 * <c>/dev/tty</c>, whenever the JVM has one &mdash; not from stdin &mdash; so it would hang).  Instead it runs
 * <c>security -i</c> (interactive mode, which reads commands from stdin) and writes a single command line to stdin:
 * <c>add-generic-password -U -a "<i>key</i>" -s "<i>service</i>" -w "<i>secret</i>"</c>.  The secret is therefore
 * never visible in the process table (e.g. to {@code ps}), and no terminal is ever read.  Each of the three values is
 * double-quoted, with only <c>"</c> and <c>\</c> escaped by a preceding backslash (verified against {@code security -i}
 * for spaces, tabs, single and double quotes, backslashes, shell metacharacters, and non-ASCII text).  Neither
 * {@link #store} nor {@link #find} materializes the secret as a {@link String}: {@code store} encodes the
 * {@code char[]} directly to the stdin byte payload, and {@code find} decodes the retrieved bytes into a
 * {@code char[]} (zeroing the intermediate buffers).  The value is never logged or {@code toString()}'d by this class.  Because the command is
 * newline-terminated, a key, service, or secret containing {@code '\n'} or {@code '\r'} cannot be delivered this way
 * (it would end or inject a command) and is rejected by {@link #store} before any process is started.
 *
 * <p>
 * <b>Reading secrets.</b> {@link #find} deliberately does not use <c>-w</c>: for a secret holding a control character
 * (such as a tab) or any non-ASCII byte, <c>-w</c> prints a bare hex dump of the bytes instead of the text, which
 * cannot be told apart from a secret that merely looks like hex (e.g. <c>abcd</c>).  The <c>-g</c> flag instead writes
 * one unambiguous line to stderr: <c>password: "text"</c> for a plain printable secret, <c>password: 0x<i>HEX</i>
 * "<i>escaped</i>"</c> for anything else (including a secret containing <c>"</c> or <c>\</c>), or a bare
 * <c>password: </c> for an empty secret.  The hex form is decoded to bytes and then as UTF-8.  The stdout of
 * <c>-g</c> holds only the non-secret item attributes and is ignored.
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
	private final CommandRunner commandRunner;

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
		this(service, failMode, new ProcessCommandRunner(validateTimeout(timeoutSeconds), binary));
	}

	/**
	 * Constructor that bypasses the {@code security} subprocess entirely, invoking {@code commandRunner} directly.
	 *
	 * <p>
	 * This is the seam tests use to exercise this store's argument construction and output parsing against an
	 * in-memory fake keychain, so round-trip coverage runs deterministically on every OS without shelling out to
	 * {@code security} or touching a real keychain.
	 *
	 * @param service The keychain service name that namespaces this store's items.  Must not be <jk>null</jk> or blank.
	 * @param failMode The policy applied on a backend failure.  Must not be <jk>null</jk>.
	 * @param commandRunner The command invocation seam.  Must not be <jk>null</jk>.
	 */
	KeychainSecretStore(String service, FailMode failMode, CommandRunner commandRunner) {
		this.service = reqnb("service", service);
		this.failMode = reqnn("failMode", failMode);
		this.commandRunner = reqnn("commandRunner", commandRunner);
	}

	private static long validateTimeout(long timeoutSeconds) {
		req(timeoutSeconds > 0, "Argument 'timeoutSeconds' must be > 0.");
		return timeoutSeconds;
	}

	@Override /* SecretStore */
	public void store(String key, char[] secret) {
		reqnn("key", key);
		reqnn("secret", secret);
		req(! containsNewline(key), "Argument 'key' must not contain a newline or carriage return character.");
		req(! containsNewline(service), "The service name must not contain a newline or carriage return character.");
		req(! containsNewline(secret), "Argument 'secret' must not contain a newline or carriage return character.");
		var payload = addCommandPayload(key, secret);
		try {
			// "security -i" reads commands from stdin; a bare "-w" would prompt on /dev/tty (hanging when the JVM has
			// a terminal), and "-w <secret>" on argv would expose the secret to ps.  The command line goes on stdin.
			var r = run(payload, "-i");
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
		reqnn("key", key);
		try {
			var r = run(FIND_GENERIC_PASSWORD, "-a", key, "-s", service, "-g");
			if (r.exit() == 0) {
				try {
					return o(parsePassword(r.stderr()));
				} finally {
					Arrays.fill(r.stderr(), (byte) 0);
				}
			}
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
		reqnn("key", key);
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
		reqnn("key", key);
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

	/**
	 * The seam through which this store invokes the underlying keychain command.
	 *
	 * <p>
	 * Production code always runs against {@link ProcessCommandRunner}, which shells out to the real
	 * {@code security} CLI.  Tests substitute an in-memory fake keychain instead (via the package-private
	 * {@link #KeychainSecretStore(String,FailMode,CommandRunner) constructor}), so this store's argument
	 * construction and output parsing run deterministically on every OS without shelling out or touching a real
	 * keychain.
	 */
	@FunctionalInterface
	interface CommandRunner {

		/**
		 * Invokes the command with the given arguments, optionally writing {@code stdin} to it first.
		 *
		 * @param stdin The bytes to write to the command's stdin before closing it, or <jk>null</jk> to close stdin
		 * 	immediately without writing anything.
		 * @param args The command's arguments (not including the executable path/name itself).
		 * @return The command's exit code, captured stdout, and captured stderr.
		 */
		Result run(byte[] stdin, String[] args);
	}

	/** The outcome of a single {@code security} invocation. */
	@SuppressWarnings({
		"java:S6218" // stdout can hold retrieved SECRET bytes; identity-based equals/hashCode/toString are intentional so a content-based toString can never surface the secret. Result instances are never compared or printed.
	})
	record Result(int exit, byte[] stdout, byte[] stderr) {

		// stderr can hold the retrieved secret (find-generic-password -g), so it is kept as bytes that can be zeroed.
		Result(int exit, byte[] stdout, String stderr) {
			this(exit, stdout, stderr.getBytes(UTF_8));
		}

		String stderrText() {
			return new String(stderr, UTF_8);
		}
	}

	/** {@link CommandRunner} that shells out to the real {@code security} binary via {@link ProcessBuilder}. */
	private static final class ProcessCommandRunner implements CommandRunner {

		private final long timeoutSeconds;
		private final String binary;

		ProcessCommandRunner(long timeoutSeconds, String binary) {
			this.timeoutSeconds = timeoutSeconds;
			this.binary = binary;
		}

		/**
		 * Invokes {@link #binary} with the given arguments, optionally writing {@code stdin} to the child process
		 * before closing its input stream.  {@code stdin}, when non-<jk>null</jk>, is written and the stream closed
		 * <i>before</i> waiting on the process so a CLI that blocks on a stdin prompt (as {@code -w} with no value
		 * does) does not deadlock against this method.
		 */
		@Override /* CommandRunner */
		public Result run(byte[] stdin, String[] args) {
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
	}

	private Result run(String... args) {
		return run(null, args);
	}

	private Result run(byte[] stdin, String... args) {
		return commandRunner.run(stdin, args);
	}

	private static RuntimeException backendFailure(String subcommand, Result r) {
		return isex("Keychain '%s' failed with exit code %s: %s", subcommand, r.exit(), r.stderrText().strip());
	}

	private static boolean containsNewline(String s) {
		return s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
	}

	private static boolean containsNewline(char[] chars) {
		for (char c : chars)
			if (c == '\n' || c == '\r')
				return true;
		return false;
	}

	/**
	 * Builds the <c>security -i</c> stdin payload
	 * <c>add-generic-password -U -a "key" -s "service" -w "secret"\n</c>, double-quoting each value and escaping
	 * <c>"</c> and <c>\</c> with a backslash.  The secret is encoded and escaped directly from the {@code char[]},
	 * without an intermediate {@link String}, and the intermediate byte copy is zeroed.
	 */
	private byte[] addCommandPayload(String key, char[] secret) {
		var head = ("add-generic-password -U -a " + quote(key) + " -s " + quote(service) + " -w \"").getBytes(UTF_8);
		var encoded = UTF_8.encode(CharBuffer.wrap(secret));
		var secretBytes = new byte[encoded.remaining()];
		encoded.get(secretBytes);
		if (encoded.hasArray())
			Arrays.fill(encoded.array(), (byte) 0);
		try {
			var escapes = 0;
			for (var b : secretBytes)
				if (b == '"' || b == '\\')
					escapes++;
			var payload = new byte[head.length + secretBytes.length + escapes + 2];
			System.arraycopy(head, 0, payload, 0, head.length);
			var i = head.length;
			for (var b : secretBytes) {
				if (b == '"' || b == '\\')
					payload[i++] = '\\';
				payload[i++] = b;
			}
			payload[i++] = '"';
			payload[i] = '\n';
			return payload;
		} finally {
			Arrays.fill(secretBytes, (byte) 0);
		}
	}

	/** Double-quotes a non-secret value for <c>security -i</c>, escaping <c>"</c> and <c>\</c>. */
	private static String quote(String s) {
		return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
	}

	private static final byte[] PASSWORD_PREFIX = "password: ".getBytes(UTF_8);

	/**
	 * Extracts the secret from the stderr of <c>find-generic-password -g</c>.
	 *
	 * <p>
	 * Finds the line starting with <c>password: </c> and decodes it per its form: <c>"text"</c> (the bytes between the
	 * first and last quote), <c>0x<i>HEX</i></c> (hex digits up to the first non-hex character), or empty.  No
	 * intermediate {@link String} is created, and the hex byte buffer is zeroed.
	 */
	@SuppressWarnings({
		"java:S3776" // One scan with a branch per password form.
	})
	private static char[] parsePassword(byte[] err) {
		var start = -1;
		for (var i = 0; i + PASSWORD_PREFIX.length <= err.length && start < 0; i++)
			if ((i == 0 || err[i - 1] == '\n') && Arrays.equals(err, i, i + PASSWORD_PREFIX.length, PASSWORD_PREFIX, 0, PASSWORD_PREFIX.length))
				start = i + PASSWORD_PREFIX.length;
		if (start < 0)
			throw isex("Keychain '%s' succeeded but its output did not contain a password line.", FIND_GENERIC_PASSWORD);
		var end = start;
		while (end < err.length && err[end] != '\n')
			end++;
		if (end > start && err[end - 1] == '\r')
			end--;
		if (end == start)
			return new char[0];
		if (err[start] == '"') {
			var close = end - 1;
			while (close > start && err[close] != '"')
				close--;
			return decode(err, start + 1, close);
		}
		if (end - start > 2 && err[start] == '0' && err[start + 1] == 'x') {
			var digits = 0;
			while (start + 2 + digits < end && Character.digit(err[start + 2 + digits], 16) >= 0)
				digits++;
			if (digits % 2 != 0)
				throw isex("Keychain '%s' returned a malformed hex password.", FIND_GENERIC_PASSWORD);
			var bytes = new byte[digits / 2];
			try {
				for (var i = 0; i < bytes.length; i++)
					bytes[i] = (byte)((Character.digit(err[start + 2 + 2 * i], 16) << 4) | Character.digit(err[start + 3 + 2 * i], 16));
				return decode(bytes, 0, bytes.length);
			} finally {
				Arrays.fill(bytes, (byte) 0);
			}
		}
		throw isex("Keychain '%s' returned an unrecognized password format.", FIND_GENERIC_PASSWORD);
	}

	/** Decodes UTF-8 bytes into a char[] without an intermediate String, zeroing the decoder's char buffer. */
	private static char[] decode(byte[] bytes, int from, int to) {
		var cb = UTF_8.decode(ByteBuffer.wrap(bytes, from, to - from));
		var chars = new char[cb.remaining()];
		cb.get(chars);
		if (cb.hasArray())
			Arrays.fill(cb.array(), '\0');
		return chars;
	}
}
