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
"use strict";

// juneau-run's Playwright reporter: the Playwright counterpart of the pytest plugin.  juneau_run.py names it in
// PW_TEST_REPORTER, which Playwright appends to the config's reporters, and reads one "##playwright {json}" line per
// event from stdout, drawing a line per running test file:
//   {"ev":"hello","v":1}                      once, first
//   {"ev":"begin","f":{<file>: <tests>}}      the tests in each file (once per project), so a file closes exactly
//   {"ev":"start","f":<file>}                 a test starts (every attempt)
//   {"ev":"test","f":<file>,"o":<outcome>}    a test's final attempt ended: pass, fail or skip, once per test (at the
//                                             end for a test whose retry never ran)
// Files are relative to the config's rootDir, with "/" separators.  JUNEAU_RUN_PLAYWRIGHT_MARKERS=0 sends no lines.
// With JUNEAU_RUN_PLAYWRIGHT_JSON set, onEnd writes a report there in the shape of Playwright's json reporter (suites,
// specs, tests, results).  No dependencies.

const fs = require("fs");
const path = require("path");

const PREFIX = "##playwright ";

class JuneauRunReporter {
  constructor(options = {}) {
    const env = options.env || process.env;
    this.write = options.write || ((text) => process.stdout.write(text));
    this.stderr = options.stderr || ((text) => process.stderr.write(text));
    this.markers = env.JUNEAU_RUN_PLAYWRIGHT_MARKERS !== "0";
    this.jsonPath = env.JUNEAU_RUN_PLAYWRIGHT_JSON || "";
    this.rootDir = "";
    this.version = undefined;
    this.suite = undefined;
    this.errors = [];
    this.ended = new Set(); // ids of tests that sent their test line
  }

  printsToStdio() {
    return false; // Playwright's own output stays as in a plain run: it still adds line or dot to an html-only config
  }

  emit(event) {
    if (this.markers) {
      this.write(PREFIX + JSON.stringify(event) + "\n");
    }
  }

  file(test) {
    const file = (test.location && test.location.file) || "";
    return file && path.relative(this.rootDir, file).split(path.sep).join("/"); // "" draws no line
  }

  onBegin(config, suite) {
    this.rootDir = config.rootDir || "";
    this.version = config.version;
    this.suite = suite;
    const files = {};
    for (const test of suite.allTests()) {
      const file = this.file(test);
      files[file] = (files[file] || 0) + 1;
    }
    this.emit({ ev: "hello", v: 1 });
    this.emit({ ev: "begin", f: files });
  }

  onTestBegin(test) {
    this.emit({ ev: "start", f: this.file(test) });
  }

  onTestEnd(test, result) {
    // pass means as expected, so a test.fail() test that fails passes.  An unexpected skip (serial mode skips the rest
    // of its group after a failure) is retried like a failure; interrupted is final and neutral, never retried.
    const status = result.status;
    const ok = status === (test.expectedStatus || "passed");
    if (!ok && status !== "interrupted" && result.retry < test.retries) {
      return; // a retry follows: only the final attempt draws a dot
    }
    if (this.ended.has(test.id)) {
      return; // serial mode reruns the group's passed tests on a retry: the first final result stands
    }
    this.ended.add(test.id);
    const outcome = status === "skipped" || status === "interrupted" ? "skip" : ok ? "pass" : "fail";
    this.emit({ ev: "test", f: this.file(test), o: outcome });
  }

  onError(error) {
    this.errors.push({ message: (error && error.message) || String(error) });
  }

  onEnd(result) {
    // --max-failures counts only last attempts, so the run can stop with a retry queued (Ctrl-C and the global timeout
    // too): such a test sends its outcome so far, and its file closes with it
    for (const test of this.suite ? this.suite.allTests() : []) {
      if ((test.results || []).length && !this.ended.has(test.id)) {
        this.ended.add(test.id);
        const outcome = test.outcome();
        const o = outcome === "unexpected" ? "fail" : outcome === "skipped" ? "skip" : "pass";
        this.emit({ ev: "test", f: this.file(test), o });
      }
    }
    if (!this.jsonPath) {
      return;
    }
    try {
      fs.mkdirSync(path.dirname(this.jsonPath), { recursive: true });
      fs.writeFileSync(this.jsonPath, JSON.stringify(this.report(result)));
    } catch (e) {
      this.stderr(`juneau-run: could not write the Playwright report ${this.jsonPath}: ${e.message}\n`);
    }
  }

  report(result) {
    const files = [];
    const suites = new Map(); // file and describe path -> its suite
    const specs = new Map(); // suite path, title and location -> its spec
    const stats = { duration: result.duration, expected: 0, skipped: 0, unexpected: 0, flaky: 0 };
    for (const test of this.suite ? this.suite.allTests() : []) {
      const file = this.file(test);
      let key = file;
      let node = suites.get(key);
      if (!node) {
        node = { title: file, file, specs: [], suites: [] };
        suites.set(key, node);
        files.push(node);
      }
      for (const title of describes(test)) {
        key += "\0" + title;
        let child = suites.get(key);
        if (!child) {
          child = { title, file, specs: [], suites: [] };
          suites.set(key, child);
          node.suites.push(child);
        }
        node = child;
      }
      const { line, column } = test.location || {};
      const specKey = `${key}\0\0${test.title}\0${line}:${column}`;
      let spec = specs.get(specKey);
      if (!spec) {
        spec = { title: test.title, ok: true, file, line, column, tests: [] };
        specs.set(specKey, spec);
        node.specs.push(spec);
      }
      const outcome = test.outcome();
      spec.ok = spec.ok && outcome !== "unexpected";
      stats[outcome] = (stats[outcome] || 0) + 1;
      const project = test.parent && test.parent.project && test.parent.project();
      spec.tests.push({
        projectName: (project && project.name) || "",
        expectedStatus: test.expectedStatus,
        status: outcome,
        retries: test.retries,
        results: (test.results || []).map((r) => ({
          retry: r.retry,
          status: r.status,
          duration: r.duration,
          ...(r.error ? { error: { message: r.error.message } } : {}),
        })),
      });
    }
    return { config: { rootDir: this.rootDir, version: this.version }, suites: files, errors: this.errors, stats,
      status: result.status };
  }
}

// The titles of the describe blocks around a test, outermost first.
function describes(test) {
  const titles = [];
  for (let s = test.parent; s && s.type === "describe"; s = s.parent) {
    titles.unshift(s.title);
  }
  return titles;
}

module.exports = JuneauRunReporter;
