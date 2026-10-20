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

// node --test cases for the Playwright reporter, against fake config, suite, test and result objects (no Playwright
// install).  test_playwright_reporter.py runs this file in the Maven gate.

const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { test } = require("node:test");

const Reporter = require(path.join(__dirname, "..", "..", "main", "js", "playwright_reporter", "juneau_run_playwright.cjs"));

const ROOT = path.join(path.sep, "repo");

let ids = 0;

function fakeTest(file, retries = 0, expectedStatus = "passed") {
  return { id: `t${++ids}`, location: { file: path.join(ROOT, ...file.split("/")) }, retries, expectedStatus };
}

function run(fn, env = {}) {
  const lines = [];
  const reporter = new Reporter({ write: (text) => lines.push(text), env });
  fn(reporter);
  return lines.map((line) => {
    assert.ok(line.startsWith("##playwright ") && line.endsWith("\n"), line);
    return JSON.parse(line.slice("##playwright ".length));
  });
}

test("begin says hello, then counts each file's tests relative to rootDir", () => {
  const tests = [fakeTest("tests/a.spec.ts"), fakeTest("tests/a.spec.ts"), fakeTest("tests/sub/b.spec.ts")];
  const events = run((r) => r.onBegin({ rootDir: ROOT }, { allTests: () => tests }));
  assert.deepEqual(events, [{ ev: "hello", v: 1 }, { ev: "begin", f: { "tests/a.spec.ts": 2, "tests/sub/b.spec.ts": 1 } }]);
});

test("a test under two projects counts twice", () => {
  const tests = [fakeTest("a.spec.ts"), fakeTest("a.spec.ts")];
  const [, begin] = run((r) => r.onBegin({ rootDir: ROOT }, { allTests: () => tests }));
  assert.deepEqual(begin.f, { "a.spec.ts": 2 });
});

test("start is sent on every attempt", () => {
  const t = fakeTest("a.spec.ts", 1);
  const events = run((r) => {
    r.onBegin({ rootDir: ROOT }, { allTests: () => [t] });
    r.onTestBegin(t, { retry: 0 });
    r.onTestBegin(t, { retry: 1 });
  });
  assert.deepEqual(events.slice(2), [{ ev: "start", f: "a.spec.ts" }, { ev: "start", f: "a.spec.ts" }]);
});

test("each status's final attempt sends one test line", () => {
  const cases = [["passed", "pass"], ["skipped", "skip"], ["failed", "fail"], ["timedOut", "fail"], ["interrupted", "skip"]];
  for (const [status, outcome] of cases) {
    const t = fakeTest("a.spec.ts");
    const events = run((r) => {
      r.onBegin({ rootDir: ROOT }, { allTests: () => [t] });
      r.onTestEnd(t, { status, retry: 0 });
    });
    assert.deepEqual(events.slice(2), [{ ev: "test", f: "a.spec.ts", o: outcome }], status);
  }
});

test("a test.fail() test passes when it fails and fails when it passes, retried only on the unexpected", () => {
  const cases = [[0, ["failed"], "pass"], [0, ["passed"], "fail"], [1, ["failed"], "pass"], [1, ["passed", "failed"], "pass"],
    [1, ["passed", "passed"], "fail"]];
  for (const [retries, statuses, outcome] of cases) {
    const t = fakeTest("a.spec.ts", retries, "failed");
    const events = run((r) => {
      r.onBegin({ rootDir: ROOT }, { allTests: () => [t] });
      statuses.forEach((status, retry) => r.onTestEnd(t, { status, retry }));
    });
    assert.deepEqual(events.slice(2), [{ ev: "test", f: "a.spec.ts", o: outcome }], `${retries} ${statuses}`);
  }
});

test("an interrupted test is final even with retries left, and a neutral dot: Playwright counts it apart", () => {
  const t = fakeTest("a.spec.ts", 2);
  const events = run((r) => {
    r.onBegin({ rootDir: ROOT }, { allTests: () => [t] });
    r.onTestEnd(t, { status: "interrupted", retry: 0 });
  });
  assert.deepEqual(events.slice(2), [{ ev: "test", f: "a.spec.ts", o: "skip" }]);
});

test("serial mode's mass skip waits for the retry, so a skipped test's real result is the one sent", () => {
  const [a, b, c] = [fakeTest("a.spec.ts", 1), fakeTest("a.spec.ts", 1), fakeTest("a.spec.ts", 1)];
  const events = run((r) => {
    r.onBegin({ rootDir: ROOT }, { allTests: () => [a, b, c] });
    r.onTestEnd(a, { status: "passed", retry: 0 });
    r.onTestEnd(b, { status: "failed", retry: 0 });
    r.onTestEnd(c, { status: "skipped", retry: 0 });     // skipped by B's failure, expected to pass
    r.onTestEnd(a, { status: "passed", retry: 1 });
    r.onTestEnd(b, { status: "passed", retry: 1 });
    r.onTestEnd(c, { status: "failed", retry: 1 });
  });
  assert.deepEqual(events.slice(2).map((e) => e.o), ["pass", "pass", "fail"]);
});

test("an unexpected skip on the last attempt, or a test.skip(), is final", () => {
  const late = fakeTest("a.spec.ts", 1);
  const skipped = fakeTest("a.spec.ts", 2, "skipped");
  const events = run((r) => {
    r.onBegin({ rootDir: ROOT }, { allTests: () => [late, skipped] });
    r.onTestEnd(late, { status: "skipped", retry: 1 });
    r.onTestEnd(skipped, { status: "skipped", retry: 0 });
  });
  assert.deepEqual(events.slice(2).map((e) => e.o), ["skip", "skip"]);
});

test("with markers off it prints nothing", () => {
  const t = fakeTest("a.spec.ts");
  const events = run((r) => {
    r.onBegin({ rootDir: ROOT }, { allTests: () => [t] });
    r.onTestBegin(t, { retry: 0 });
    r.onTestEnd(t, { status: "passed", retry: 0 });
    r.onEnd({ status: "passed", duration: 1 });
  }, { JUNEAU_RUN_PLAYWRIGHT_MARKERS: "0" });
  assert.deepEqual(events, []);
});

test("serial mode's rerun of a passed test sends no second test line", () => {
  const first = fakeTest("a.spec.ts", 1);
  const second = fakeTest("a.spec.ts", 1);
  const events = run((r) => {
    r.onBegin({ rootDir: ROOT }, { allTests: () => [first, second] });
    r.onTestEnd(first, { status: "passed", retry: 0 });
    r.onTestEnd(second, { status: "failed", retry: 0 });
    r.onTestEnd(first, { status: "passed", retry: 1 });
    r.onTestEnd(second, { status: "passed", retry: 1 });
  });
  assert.deepEqual(events.slice(2), [{ ev: "test", f: "a.spec.ts", o: "pass" }, { ev: "test", f: "a.spec.ts", o: "pass" }]);
});

test("an empty suite begins with no files", () => {
  const events = run((r) => r.onBegin({ rootDir: ROOT }, { allTests: () => [] }));
  assert.deepEqual(events, [{ ev: "hello", v: 1 }, { ev: "begin", f: {} }]);
});

test("a test with no location has an empty file name, which juneau_run.py draws no line for", () => {
  const t = { id: "x", retries: 0, expectedStatus: "passed" };
  const events = run((r) => {
    r.onBegin({ rootDir: ROOT }, { allTests: () => [t] });
    r.onTestBegin(t, { retry: 0 });
    r.onTestEnd(t, { status: "passed", retry: 0 });
  });
  assert.deepEqual(events.slice(1), [{ ev: "begin", f: { "": 1 } }, { ev: "start", f: "" }, { ev: "test", f: "", o: "pass" }]);
});

test("a failed attempt with a retry to come sends nothing, so a flaky test draws one pass", () => {
  const t = fakeTest("a.spec.ts", 2);
  const events = run((r) => {
    r.onBegin({ rootDir: ROOT }, { allTests: () => [t] });
    r.onTestEnd(t, { status: "failed", retry: 0 });
    r.onTestEnd(t, { status: "timedOut", retry: 1 });
    r.onTestEnd(t, { status: "passed", retry: 2 });
  });
  assert.deepEqual(events.slice(2), [{ ev: "test", f: "a.spec.ts", o: "pass" }]);
});

test("the last retry's failure is final", () => {
  const t = fakeTest("a.spec.ts", 1);
  const events = run((r) => {
    r.onBegin({ rootDir: ROOT }, { allTests: () => [t] });
    r.onTestEnd(t, { status: "failed", retry: 0 });
    r.onTestEnd(t, { status: "failed", retry: 1 });
  });
  assert.deepEqual(events.slice(2), [{ ev: "test", f: "a.spec.ts", o: "fail" }]);
});

test("at the end, a test still waiting for its retry sends its outcome, so its file closes with it", () => {
  // --max-failures counts only last attempts, so the run can stop with a retry queued; Ctrl-C and the global timeout too
  const outcomeOf = (t, outcome, statuses) => Object.assign(t, { outcome: () => outcome,
    results: statuses.map((status, retry) => ({ status, retry })) });
  const queued = outcomeOf(fakeTest("a.spec.ts", 1), "unexpected", ["failed"]);
  const last = outcomeOf(fakeTest("b.spec.ts", 0), "unexpected", ["failed"]);
  const massSkipped = outcomeOf(fakeTest("c.spec.ts", 1), "skipped", ["skipped"]);
  const neverRan = outcomeOf(fakeTest("d.spec.ts", 1), "skipped", []);
  const events = run((r) => {
    r.onBegin({ rootDir: ROOT }, { allTests: () => [queued, last, massSkipped, neverRan] });
    r.onTestEnd(queued, queued.results[0]);
    r.onTestEnd(last, last.results[0]);
    r.onTestEnd(massSkipped, massSkipped.results[0]);
    r.onEnd({ status: "failed", duration: 1 });
  });
  assert.deepEqual(events.slice(2), [{ ev: "test", f: "b.spec.ts", o: "fail" }, { ev: "test", f: "a.spec.ts", o: "fail" },
    { ev: "test", f: "c.spec.ts", o: "skip" }]);
});

test("the reporter says it doesn't print, so Playwright still adds its own line reporter to an html-only config", () => {
  assert.equal(new Reporter({ write: () => {} }).printsToStdio(), false);
});

// The Java report reader's fixture is this reporter's report of tree(), so the two can't drift apart.
const FIXTURE = path.join(__dirname, "..", "..", "..", "..", "juneau-rest", "juneau-run-report", "src", "test", "resources",
  "org", "apache", "juneau", "rest", "server", "runreport", "playwright-juneau-run.json");
const BOOM = "\u001b[31mboom\u001b[39m\nat a.spec.ts:11"; // Playwright colours its error messages

// A suite tree as Playwright's reporter API gives it: root, project, file and describe suites, and finished tests.
function tree() {
  const project = (name) => ({ type: "project", title: name, project: () => ({ name }) });
  const chromium = project("chromium");
  const firefox = project("firefox");
  const suite = (type, title, parent) => ({ type, title, parent, project: parent.project });
  const done = (parent, title, line, outcome, results, expectedStatus = "passed") => ({
    id: `t${++ids}`, title, parent, retries: 1, expectedStatus, outcome: () => outcome, results,
    location: { file: path.join(ROOT, "tests", "a.spec.ts"), line, column: 5 },
  });
  const fileC = suite("file", "tests/a.spec.ts", chromium);
  const loginC = suite("describe", "Login", fileC);
  const fileF = suite("file", "tests/a.spec.ts", firefox);
  const tests = [
    done(loginC, "works", 3, "expected", [{ retry: 0, status: "passed", duration: 40 }]),
    done(loginC, "flaky one", 7, "flaky", [{ retry: 0, status: "failed", duration: 30, error: { message: "first try" } },
      { retry: 1, status: "passed", duration: 50 }]),
    done(fileF, "top", 11, "unexpected", [{ retry: 0, status: "failed", duration: 5, error: { message: BOOM } },
      { retry: 1, status: "timedOut", duration: 9, error: { message: "Test timeout of 9ms exceeded." } }]),
    done(fileC, "top", 11, "skipped", [{ retry: 0, status: "skipped", duration: 0 }], "skipped"),
  ];
  return { allTests: () => tests };
}

function reportOf(fn) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "juneau-run-pw-"));
  try {
    const file = path.join(dir, "out", "pw-playwright.json");
    const errors = [];
    const r = new Reporter({ write: () => {}, stderr: (text) => errors.push(text), env: { JUNEAU_RUN_PLAYWRIGHT_JSON: file } });
    fn(r);
    return { report: fs.existsSync(file) ? JSON.parse(fs.readFileSync(file, "utf8")) : null, errors, file };
  } finally {
    fs.rmSync(dir, { recursive: true, force: true });
  }
}

test("onEnd writes a JSON report in the json reporter's shape: file and describe suites, specs, tests and results", () => {
  const { report, errors } = reportOf((r) => {
    r.onBegin({ rootDir: ROOT, version: "1.62.1" }, tree());
    r.onEnd({ status: "failed", duration: 1234 });
  });
  assert.deepEqual(errors, []);
  // the fixture holds the full shape: two projects, a describe block, and expected, flaky, unexpected and skipped tests
  assert.deepEqual({ ...report, config: { ...report.config, rootDir: "/repo" } }, JSON.parse(fs.readFileSync(FIXTURE, "utf8")));
});

test("a global error goes in the report's errors", () => {
  const { report } = reportOf((r) => {
    r.onBegin({ rootDir: ROOT }, { allTests: () => [] });
    r.onError({ message: "global setup failed" });
    r.onEnd({ status: "failed", duration: 2 });
  });
  assert.deepEqual(report.errors, [{ message: "global setup failed" }]);
});

test("no report is written without a path", () => {
  const r = new Reporter({ write: () => {}, env: {} });
  r.onBegin({ rootDir: ROOT }, tree());
  assert.equal(r.onEnd({ status: "passed", duration: 1 }), undefined);
});

test("a report that can't be written is said once on stderr and doesn't throw", () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "juneau-run-pw-"));
  try {
    const blocker = path.join(dir, "file");
    fs.writeFileSync(blocker, "");
    const errors = [];
    const r = new Reporter({ write: () => {}, stderr: (text) => errors.push(text),
      env: { JUNEAU_RUN_PLAYWRIGHT_JSON: path.join(blocker, "pw.json") } });
    r.onBegin({ rootDir: ROOT }, tree());
    r.onEnd({ status: "passed", duration: 1 });
    assert.equal(errors.length, 1);
    assert.match(errors[0], /^juneau-run: could not write the Playwright report .*pw\.json: /);
  } finally {
    fs.rmSync(dir, { recursive: true, force: true });
  }
});
