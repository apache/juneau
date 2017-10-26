// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.utils;

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.utils.IOPipe.*;

/**
 * Utility class for running operating system processes.
 *
 * <p>
 * Similar to {@link java.lang.ProcessBuilder} but with additional features.
 */
public class ProcBuilder {

	private java.lang.ProcessBuilder pb = new java.lang.ProcessBuilder();
	private TeeWriter outWriters = new TeeWriter(), logWriters = new TeeWriter();
	private LineProcessor lp;
	private Process p;
	private int maxExitStatus = 0;
	private boolean byLines;
	private String divider = "--------------------------------------------------------------------------------";

	/**
	 * Creates a process builder with the specified arguments.
	 *
	 * <p>
	 * Equivalent to calling <code>ProcessBuilder.create().command(args);</code>
	 *
	 * @param args The command-line arguments.
	 * @return A new process builder.
	 */
	public static ProcBuilder create(Object...args) {
		return new ProcBuilder().command(args);
	}

	/**
	 * Creates an empty process builder.
	 *
	 * @return A new process builder.
	 */
	public static ProcBuilder create() {
		return new ProcBuilder().command();
	}

	/**
	 * Command arguments.
	 *
	 * <p>
	 * Arguments can be collections or arrays and will be automatically expanded.
	 *
	 * @param args The command-line arguments.
	 * @return This object (for method chaining).
	 */
	public ProcBuilder command(Object...args) {
		return commandIf(ANY, args);
	}

	/**
	 * Command arguments if the specified matcher matches.
	 *
	 * <p>
	 * Can be used for specifying OS-specific commands.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	ProcBuilder pb = ProcBuilder
	 * 		.create()
	 * 		.commandIf(<jsf>WINDOWS</jsf>, <js>"cmd /c dir"</js>)
	 * 		.commandIf(<jsf>UNIX</jsf>, <js>"bash -c ls"</js>)
	 * 		.merge()
	 * 		.execute();
	 * </p>
	 *
	 * @param m The matcher.
	 * @param args The command line arguments if matcher matches.
	 * @return This object (for method chaining).
	 */
	public ProcBuilder commandIf(Matcher m, Object...args) {
		if (m.matches())
			pb.command(toList(args));
		return this;
	}

	/**
	 * Append to the command arguments.
	 *
	 * <p>
	 * Arguments can be collections or arrays and will be automatically expanded.
	 *
	 * @param args The command-line arguments.
	 * @return This object (for method chaining).
	 */
	public ProcBuilder append(Object...args) {
		return appendIf(ANY, args);
	}

	/**
	 * Append to the command arguments if the specified matcher matches.
	 *
	 * <p>
	 * Arguments can be collections or arrays and will be automatically expanded.
	 *
	 * @param m The matcher.
	 * @param args The command line arguments if matcher matches.
	 * @return This object (for method chaining).
	 */
	public ProcBuilder appendIf(Matcher m, Object...args) {
		if (m.matches())
			pb.command().addAll(toList(args));
		return this;
	}

	/**
	 * Merge STDOUT and STDERR into a single stream.
	 *
	 * @return This object (for method chaining).
	 */
	public ProcBuilder merge() {
		pb.redirectErrorStream(true);
		return this;
	}

	/**
	 * Use by-lines mode.
	 *
	 * <p>
	 * Flushes output after every line of input.
	 *
	 * @return This object (for method chaining).
	 */
	public ProcBuilder byLines() {
		this.byLines = true;
		return this;
	}

	/**
	 * Pipe output to the specified writer.
	 *
	 * <p>
	 * The method can be called multiple times to write to multiple writers.
	 *
	 * @param w The writer to pipe to.
	 * @param close Close the writer afterwards.
	 * @return This object (for method chaining).
	 */
	public ProcBuilder pipeTo(Writer w, boolean close) {
		this.outWriters.add(w, close);
		return this;
	}

	/**
	 * Pipe output to the specified writer, but don't close the writer.
	 *
	 * @param w The writer to pipe to.
	 * @return This object (for method chaining).
	 */
	public ProcBuilder pipeTo(Writer w) {
		return pipeTo(w, false);
	}

	/**
	 * Pipe output to the specified writer, including the command and return code.
	 *
	 * <p>
	 * The method can be called multiple times to write to multiple writers.
	 *
	 * @param w The writer to pipe to.
	 * @param close Close the writer afterwards.
	 * @return This object (for method chaining).
	 */
	public ProcBuilder logTo(Writer w, boolean close) {
		this.logWriters.add(w, close);
		this.outWriters.add(w, close);
		return this;
	}

	/**
	 * Pipe output to the specified writer, including the command and return code.
	 *
	 * <p>
	 * The method can be called multiple times to write to multiple writers.
	 * Don't close the writer afterwards.
	 *
	 * @param w The writer to pipe to.
	 * @return This object (for method chaining).
	 */
	public ProcBuilder logTo(Writer w) {
		return logTo(w, false);
	}

	/**
	 * Pipe output to the specified writer, including the command and return code.
	 * The method can be called multiple times to write to multiple writers.
	 *
	 * @param level The log level.
	 * @param logger The logger to log to.
	 * @return This object (for method chaining).
	 */
	public ProcBuilder logTo(final Level level, final Logger logger) {
		if (logger.isLoggable(level)) {
			logTo(new StringWriter() {
				private boolean isClosed;  // Prevents messages from being written twice.
				@Override /* Writer */
				public void close() {
					if (! isClosed)
						logger.log(level, this.toString());
					isClosed = true;
				}
			}, true);
		}
		return this;
	}

	/**
	 * Line processor to use to process/convert lines of output returned by the process.
	 *
	 * @param lp The new line processor.
	 * @return This object (for method chaining).
	 */
	public ProcBuilder lp(LineProcessor lp) {
		this.lp = lp;
		return this;
	}

	/**
	 * Append the specified environment variables to the process.
	 *
	 * @param env The new set of environment variables.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings({"rawtypes"})
	public ProcBuilder env(Map env) {
		if (env != null)
			for (Map.Entry e : (Set<Map.Entry>)env.entrySet())
				environment(e.getKey().toString(), e.getValue() == null ? null : e.getValue().toString());
		return this;
	}

	/**
	 * Append the specified environment variable.
	 *
	 * @param key The environment variable name.
	 * @param val The environment variable value.
	 * @return This object (for method chaining).
	 */
	public ProcBuilder environment(String key, String val) {
		pb.environment().put(key, val);
		return this;
	}

	/**
	 * Sets the directory where the command will be executed.
	 *
	 * @param directory The directory.
	 * @return This object (for method chaining).
	 */
	public ProcBuilder directory(File directory) {
		pb.directory(directory);
		return this;
	}

	/**
	 * Sets the maximum allowed return code on the process call.
	 *
	 * <p>
	 * If the return code exceeds this value, an IOException is returned on the {@link #run()} command.
	 * The default value is '0'.
	 *
	 * @param maxExitStatus The maximum exit status.
	 * @return This object (for method chaining).
	 */
	public ProcBuilder maxExitStatus(int maxExitStatus) {
		this.maxExitStatus = maxExitStatus;
		return this;
	}

	/**
	 * Run this command and pipes the output to the specified writer or output stream.
	 *
	 * @return The exit code from the process.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public int run() throws IOException, InterruptedException {
		if (pb.command().size() == 0)
			throw new IOException("No command specified in ProcBuilder.");
		try {
			logWriters.append(divider).append('\n').flush();
			logWriters.append(join(pb.command(), " ")).append('\n').flush();
			p = pb.start();
			IOPipe.create(p.getInputStream(), outWriters).lineProcessor(lp).byLines(byLines).run();
			int rc = p.waitFor();
			logWriters.append("Exit: ").append(String.valueOf(p.exitValue())).append('\n').flush();
			if (rc > maxExitStatus)
				throw new IOException("Return code "+rc+" from command " + join(pb.command(), " "));
			return rc;
		} finally {
			close();
		}
	}

	/**
	 * Run this command and returns the output as a simple string.
	 *
	 * @return The output from the command.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public String getOutput() throws IOException, InterruptedException {
		StringWriter sw = new StringWriter();
		pipeTo(sw).run();
		return sw.toString();
	}

	/**
	 * Returns the output from this process as a {@link Scanner}.
	 *
	 * @return The output from the process as a Scanner object.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public Scanner getScanner() throws IOException, InterruptedException {
		StringWriter sw = new StringWriter();
		pipeTo(sw, true);
		run();
		return new Scanner(sw.toString());
	}

	/**
	 * Destroys the underlying process.
	 *
	 * <p>
	 * This method is only needed if the {@link #getScanner()} method was used.
	 */
	private void close() {
		closeQuietly(logWriters, outWriters);
		if (p != null)
			p.destroy();
	}

	/**
	 * Specifies interface for defining OS-specific commands.
	 */
	public abstract static class Matcher {
		abstract boolean matches();
	}

	private static String OS = System.getProperty("os.name").toLowerCase();

	/** Operating system matcher: Any operating system. */
	public final static Matcher ANY = new Matcher() {
		@Override boolean matches() {
			return true;
		}
	};

	/** Operating system matcher: Any Windows system. */
	public final static Matcher WINDOWS = new Matcher() {
		@Override boolean matches() {
			return OS.indexOf("win") >= 0;
		}
	};

	/** Operating system matcher: Any Mac system. */
	public final static Matcher MAC = new Matcher() {
		@Override boolean matches() {
			return OS.indexOf("mac") >= 0;
		}
	};

	/** Operating system matcher: Any Unix or Linux system. */
	public final static Matcher UNIX = new Matcher() {
		@Override boolean matches() {
			return OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0;
		}
	};

	private static List<String> toList(Object...args) {
		List<String> l = new LinkedList<>();
		for (Object o : args) {
			if (o.getClass().isArray())
				for (int i = 0; i < Array.getLength(o); i++)
					l.add(Array.get(o, i).toString());
			else if (o instanceof Collection)
				for (Object o2 : (Collection<?>)o)
					l.add(o2.toString());
			else
				l.add(o.toString());
		}
		return l;
	}
}
