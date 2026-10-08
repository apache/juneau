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
package org.apache.juneau.rest.server.runreport;

import static org.apache.juneau.rest.server.runreport.ReportText.*;

import java.io.*;
import java.nio.file.*;

import javax.xml.stream.*;

import org.apache.commons.xml.secure.SecureXMLInputFactory;
import org.apache.juneau.rest.server.runreport.RunEvent.*;

/**
 * Streaming parser for the JUnit XML schema (Surefire <c>TEST-*.xml</c> and pytest <c>--junitxml</c> output).
 *
 * <p>
 * Nothing is loaded as a tree: a test case is handed to the builder as soon as its element closes, and text inside a
 * failing child is kept only up to the trace limit, so memory does not grow with the file.  The factory comes from
 * {@link SecureXMLInputFactory}, so DTDs and external entities are never resolved.
 */
final class JUnitXmlParser {

	private JUnitXmlParser() {}

	static void parse(Path file, ReportLimits lim, ReportBuilder out) {
		var name = fileName(file);
		long size;
		try {
			size = Files.size(file);
			if (size > lim.maxFileBytes()) {
				out.skippedFiles++;
				out.warnings.add("report skipped: " + name + " is larger than " + lim.maxFileBytes() + " bytes");
				return;
			}
		} catch (IOException e) {
			out.skippedFiles++;
			out.warnings.add("report unreadable: " + (e instanceof NoSuchFileException ? "file not found" : "cannot read") + " (" + name + ")");
			return;
		}
		XMLStreamReader r = null;
		try (var in = new BufferedInputStream(Files.newInputStream(file))) {
			var f = SecureXMLInputFactory.newInstance();
			f.setProperty(XMLInputFactory.IS_COALESCING, false);
			r = f.createXMLStreamReader(in);
			read(r, lim, out);
		} catch (XMLStreamException e) {
			out.truncated = true;
			var msg = firstLine(e.getMessage());
			if (atEnd(e, size, msg))
				out.warnings.add("report truncated: unexpected end of file after " + out.tests.size() + " tests (" + name + ")");
			else
				out.warnings.add("report unreadable: " + clip(msg, 200) + " after " + out.tests.size() + " tests (" + name + ")");
		} catch (IOException | RuntimeException e) {
			out.truncated = true;
			out.warnings.add("report unreadable: " + clip(firstLine(e.getMessage()), 200) + " (" + name + ")");
		} finally {
			if (r != null) {
				try {
					r.close();
				} catch (XMLStreamException e) {
					// nothing to do
				}
			}
		}
	}

	// An error at (or within a few chars of) the end of the input means the file was cut short; elsewhere it is malformed.
	private static boolean atEnd(XMLStreamException e, long size, String msg) {
		if (msg.contains("Premature end") || msg.contains("must start and end within the same entity") || msg.contains("nexpected end"))
			return true;
		var loc = e.getLocation();
		return loc != null && loc.getCharacterOffset() >= size - 8;
	}

	private static String firstLine(String s) {
		return RunViewChecks.firstLine(s);
	}

	private static void read(XMLStreamReader r, ReportLimits lim, ReportBuilder out) throws XMLStreamException {
		var suiteName = "";
		var inCase = false;
		String cName = null, cClass = null, cTime = null, cMsg = null;
		TestStatus cStatus = null;
		StringBuilder text = null;
		var childDepth = 0;
		var skipDepth = 0;
		while (r.hasNext()) {
			switch (r.next()) {
				case XMLStreamConstants.START_ELEMENT -> {
					var el = r.getLocalName();
					if (skipDepth > 0) {
						skipDepth++;
					} else if (el.equals("testsuite") && ! inCase) {
						suiteName = nz(r.getAttributeValue(null, "name"));
					} else if (el.equals("testcase") && ! inCase) {
						inCase = true;
						cName = r.getAttributeValue(null, "name");
						cClass = r.getAttributeValue(null, "classname");
						cTime = r.getAttributeValue(null, "time");
						cStatus = TestStatus.PASS;
						cMsg = null;
						text = null;
					} else if (inCase && childDepth == 0) {
						switch (el) {
							case "failure", "error", "skipped" -> {
								cStatus = el.equals("failure") ? TestStatus.FAIL : el.equals("error") ? TestStatus.ERROR : TestStatus.SKIP;
								cMsg = r.getAttributeValue(null, "message");
								text = new StringBuilder();
								childDepth = 1;
							}
							case "flakyFailure", "flakyError", "rerunFailure", "rerunError" -> skipDepth = 1;
							default -> { /* system-out, properties and the like */ skipDepth = 1; }
						}
					} else if (inCase) {
						childDepth++;
					}
				}
				case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> {
					if (text != null && skipDepth == 0 && text.length() <= lim.maxTraceChars())
						text.append(r.getText());
				}
				case XMLStreamConstants.END_ELEMENT -> {
					var el = r.getLocalName();
					if (skipDepth > 0) {
						skipDepth--;
					} else if (inCase && childDepth > 0) {
						childDepth--;
					} else if (inCase && el.equals("testcase")) {
						inCase = false;
						if (out.tests.size() >= lim.maxTests()) {
							out.truncated = true;
							out.warnings.add("report truncated at " + lim.maxTests() + " tests");
							return;
						}
						out.tests.add(build(suiteName, cClass, cName, cTime, cStatus, cMsg, text, lim));
						text = null;
					}
				}
				default -> { /* ignored */ }
			}
		}
	}

	private static ReportTest build(String suiteName, String cClass, String cName, String cTime, TestStatus status, String attrMsg, StringBuilder text, ReportLimits lim) {
		var suite = cClass != null && ! cClass.isEmpty() ? cClass : suiteName;
		String msg = null, trace = null;
		if (status != TestStatus.PASS) {
			var body = text == null ? "" : stripAnsi(text.toString()).strip();
			if (attrMsg != null && ! attrMsg.isEmpty())
				msg = clip(stripAnsi(attrMsg), lim.maxMsgChars());
			else if (! body.isEmpty())
				msg = clip(firstLine(body), lim.maxMsgChars());
			if (! body.isEmpty())
				trace = clip(body, lim.maxTraceChars());
		}
		return new ReportTest(clip(suite, lim.maxNameChars()), clip(nz(cName), lim.maxNameChars()), status, millis(cTime), msg, trace);
	}

	private static Long millis(String t) {
		if (t == null)
			return null;
		try {
			var d = Double.parseDouble(t.trim());
			return d >= 0 && d < 9.0e12 ? Long.valueOf(Math.round(d * 1000)) : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}
}
