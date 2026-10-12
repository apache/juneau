# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *  http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
# * specific language governing permissions and limitations under the License.
"""juneau_scripts.git against real temporary repositories with a bare origin; nothing is mocked except PATH."""

import json
import os
import shlex
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import scripts_support  # noqa: F401

from juneau_scripts import git, testing
from juneau_scripts.git import GitError


class GitCase(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.tmp = Path(tmp.name)
        for cm in (testing.hermetic_git(self.tmp), testing.clean_run_env()):
            cm.__enter__()
            self.addCleanup(cm.__exit__, None, None, None)
        self.work, self.origin = testing.bare_remote(self.tmp)

    def g(self, *args, cwd=None):
        return subprocess.run(["git", *args], cwd=cwd or self.work, check=True, capture_output=True,
                              text=True).stdout.strip()

    def write(self, rel, text="x\n"):
        path = self.work / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        return path


class ReadersTest(GitCase):
    def test_clean_repo(self):
        self.assertEqual(git.staged_paths(self.work), [])
        self.assertEqual(git.unstaged_paths(self.work), [])
        self.assertEqual(git.untracked_paths(self.work), [])
        self.assertEqual(git.current_branch(self.work), "main")
        self.assertEqual(git.short_head(self.work), self.g("rev-parse", "--short", "HEAD"))
        self.assertEqual(git.remote_url(self.work), str(self.origin))
        self.assertIsNone(git.remote_url(self.work, "nope"))
        self.assertEqual(git.user_email(self.work), "test@example.com")

    def test_paths_with_spaces_and_unicode(self):
        self.write("space name.txt")
        self.write("ünï.txt")
        self.g("add", "space name.txt")
        self.write("README.md", "changed\n")
        self.assertEqual(git.staged_paths(self.work), ["space name.txt"])
        self.assertEqual(git.unstaged_paths(self.work), ["README.md"])
        self.assertEqual(git.untracked_paths(self.work), ["ünï.txt"])

    def test_detached_head(self):
        self.g("checkout", "-q", "--detach")
        self.assertIsNone(git.current_branch(self.work))
        r = git.push(self.work)
        self.assertFalse(r.ok)
        self.assertIn("detached", r.err)

    def test_no_commit_yet(self):
        fresh = self.tmp / "fresh"
        self.g("init", "-q", "-b", "main", str(fresh), cwd=self.tmp)
        self.assertIsNone(git.short_head(fresh))
        self.assertEqual(git.current_branch(fresh), "main")
        self.assertIsNone(git.user_email(fresh))

    def test_env(self):
        env = git.env({"GIT_SSL_CAINFO": "/ca.pem"})
        self.assertEqual(env["GIT_TERMINAL_PROMPT"], "0")
        self.assertEqual(env["GIT_SSL_CAINFO"], "/ca.pem")
        self.assertNotIn("GIT_SSL_CAINFO", os.environ)

    def test_changed_under(self):
        self.write("docs/a.md")
        self.g("add", "docs/a.md")
        self.g("commit", "-q", "-m", "docs")
        self.write("docs/a.md", "edited\n")
        self.write("docs/new.md")
        self.write("other.txt")
        self.assertEqual(git.changed_under(self.work, "docs"), ["docs/a.md", "docs/new.md"])
        self.assertEqual(git.changed_under(self.work, "docs", untracked=False), ["docs/a.md"])
        self.assertEqual(git.changed_under(self.work, "docs", base="HEAD~1", untracked=False), ["docs/a.md"])


class UpstreamTest(GitCase):
    def test_has_upstream_and_counts(self):
        self.assertTrue(git.has_upstream(self.work))
        self.assertEqual(git.ahead_behind(self.work, fetch=False), (0, 0, None))
        self.write("a.txt")
        self.g("add", "a.txt")
        self.g("commit", "-q", "-m", "a")
        self.assertEqual(git.ahead_behind(self.work, fetch=False), (1, 0, None))

    def test_fetch_sees_a_commit_on_origin(self):
        other = self.tmp / "other"
        self.g("clone", "-q", str(self.origin), str(other), cwd=self.tmp)
        for key, value in (("user.name", "O"), ("user.email", "o@example.com"), ("commit.gpgsign", "false")):
            self.g("config", key, value, cwd=other)
        (other / "b.txt").write_text("b\n", encoding="utf-8")
        self.g("add", "b.txt", cwd=other)
        self.g("commit", "-q", "-m", "b", cwd=other)
        self.g("push", "-q", "origin", "main", cwd=other)
        self.assertEqual(git.ahead_behind(self.work, fetch=False), (0, 0, None))
        self.assertEqual(git.ahead_behind(self.work, fetch=True), (0, 1, None))

    def test_no_upstream(self):
        self.g("checkout", "-q", "-b", "feature")
        self.assertFalse(git.has_upstream(self.work))
        self.assertEqual(git.ahead_behind(self.work, fetch=False), (None, None, "no upstream"))
        self.assertTrue(git.has_upstream(self.work, "main"))

    def test_fetch_failure_gives_err_and_no_counts(self):
        self.g("remote", "set-url", "origin", str(self.tmp / "missing.git"))
        ahead, behind, err = git.ahead_behind(self.work, fetch=True)
        self.assertEqual((ahead, behind), (None, None))
        self.assertTrue(err)

    def test_first_push_with_upstream(self):
        self.g("checkout", "-q", "-b", "feature")
        self.write("f.txt")
        self.g("add", "f.txt")
        self.g("commit", "-q", "-m", "f")
        r = git.push(self.work, set_upstream=True, capture=True)
        self.assertTrue(r.ok, r.err)
        self.assertTrue(git.has_upstream(self.work))
        self.assertEqual(self.g("rev-parse", "--abbrev-ref", "@{upstream}"), "origin/feature")

    def test_rebase_onto_upstream(self):
        self.assertTrue(git.rebase_onto_upstream(self.work, capture=True).ok)


class FailClosedTest(GitCase):
    def test_not_a_repository_raises_from_every_reader(self):
        plain = self.tmp / "plain"
        plain.mkdir()
        for fn in (git.staged_paths, git.unstaged_paths, git.untracked_paths, git.changed_under, git.has_upstream):
            with self.subTest(fn=fn.__name__), self.assertRaises(GitError) as caught:
                fn(plain)
            self.assertNotEqual(caught.exception.result.code, 0)
        with self.assertRaises(GitError):
            git.ahead_behind(plain, fetch=False)

    def test_git_missing_raises_with_code_127(self):
        empty = self.tmp / "empty-bin"
        empty.mkdir()
        with mock.patch.dict(os.environ, {"PATH": str(empty)}):
            for fn in (git.staged_paths, git.unstaged_paths, git.untracked_paths, git.changed_under, git.has_upstream,
                       lambda repo: git.ahead_behind(repo, fetch=False)):
                with self.subTest(fn=getattr(fn, "__name__", "ahead_behind")), self.assertRaises(GitError) as caught:
                    fn(self.work)
                self.assertEqual(caught.exception.result.code, 127)
            self.assertIsNone(git.current_branch(self.work))

    def test_message_names_the_command_and_the_exit_code(self):
        plain = self.tmp / "plain"
        plain.mkdir()
        with self.assertRaises(GitError) as caught:
            git.staged_paths(plain)
        self.assertIn("git rev-parse failed (exit code 128)", str(caught.exception))
        self.assertIn("not a git repository", str(caught.exception))
        self.assertIsInstance(caught.exception, RuntimeError)


class WritersTest(GitCase):
    def test_empty_index_fails_with_gits_message(self):
        r = git.commit_index("msg", self.work, capture=True)
        self.assertFalse(r.ok)
        self.assertEqual(r.code, 1)
        self.assertIn("nothing to commit", r.out + r.err)

    def test_commit_index_message_on_stdin(self):
        self.write("a.txt")
        self.g("add", "a.txt")
        message = "Subject line\n\nBody line one\nBody line two"
        self.assertTrue(git.commit_index(message, self.work, stdin=True, capture=True).ok)
        self.assertEqual(self.g("log", "-1", "--format=%B"), message)

    def test_commit_index_commits_the_index_as_found(self):
        self.write("a.txt")
        self.g("add", "a.txt")
        self.write("a.txt", "unstaged edit\n")
        self.assertTrue(git.commit_index("a", self.work, capture=True).ok)
        self.assertEqual(self.g("show", "HEAD:a.txt"), "x")
        self.assertEqual(git.unstaged_paths(self.work), ["a.txt"])

    def test_commit_paths_commits_only_the_named_paths(self):
        self.write("a.txt")
        self.write("b.txt")
        self.g("add", "a.txt", "b.txt")
        self.assertTrue(git.commit_paths(["a.txt"], "only a", self.work, capture=True).ok)
        self.assertEqual(self.g("show", "--name-only", "--format=", "HEAD"), "a.txt")
        self.assertEqual(git.staged_paths(self.work), ["b.txt"])

    def test_commit_paths_needs_a_path(self):
        with self.assertRaises(ValueError):
            git.commit_paths([], "msg", self.work)

    def test_stash_hint_round_trips_through_shlex(self):
        paths = ["space name.txt", "ünï.txt", "it's.txt"]
        first, second = git.stash_hint(paths).splitlines()
        self.assertEqual(shlex.split(first)[:4], ["git", "stash", "push", "--"])
        self.assertEqual(shlex.split(first)[4:], paths)
        self.assertEqual(second, "git stash pop")

    def test_tag_and_delete_tag_locally_and_on_the_remote(self):
        self.assertTrue(git.tag("v1", self.work, message="Release v1", capture=True).ok)
        self.assertEqual(self.g("cat-file", "-t", "v1"), "tag")
        self.assertTrue(git.tag("light", self.work, capture=True).ok)
        self.assertEqual(self.g("cat-file", "-t", "light"), "commit")
        self.g("push", "-q", "origin", "v1")
        self.assertIn("refs/tags/v1", self.g("ls-remote", "--tags", "origin"))
        self.assertTrue(git.delete_tag("v1", self.work, remote="origin", capture=True).ok)
        self.assertEqual(self.g("tag", "--list", "v1"), "")
        self.assertNotIn("refs/tags/v1", self.g("ls-remote", "--tags", "origin"))
        self.assertTrue(git.delete_tag("light", self.work, capture=True).ok)

    def test_plain_write_inherits_the_terminal(self):
        self.write("a.txt")
        self.g("add", "a.txt")
        r = git.commit_index("a", self.work)
        self.assertTrue(r.ok)
        self.assertEqual((r.out, r.err), ("", ""))


class ConsoleRelayTest(GitCase):
    class FakeJr:
        def __init__(self):
            self.said = []

        def console_active(self):
            return True

        def say(self, text="", level="info", step=None):
            self.said.append((level, text))

    def test_output_becomes_notes_under_a_console(self):
        fake = self.FakeJr()
        with mock.patch.dict(sys.modules, {"juneau_run": fake}):
            r = git.commit_index("msg", self.work)
        self.assertFalse(r.ok)
        self.assertEqual((r.out, r.err), ("", ""))
        self.assertTrue(fake.said)
        self.assertTrue(all(level == "error" for level, _ in fake.said))
        self.assertTrue(any("nothing to commit" in text for _, text in fake.said))

    def test_capture_wins_over_the_console(self):
        fake = self.FakeJr()
        with mock.patch.dict(sys.modules, {"juneau_run": fake}):
            r = git.commit_index("msg", self.work, capture=True)
        self.assertEqual(fake.said, [])
        self.assertIn("nothing to commit", r.out + r.err)


class NeverAddOrForceTest(GitCase):
    def test_no_helper_runs_add_force_no_verify_or_a_config_write(self):
        log = self.tmp / "git-argv.jsonl"
        real = shutil.which("git")
        bin_dir = self.tmp / "logging-bin"
        bin_dir.mkdir()
        wrapper = bin_dir / "git"
        wrapper.write_text(
            f"#!{sys.executable}\nimport json, os, sys\n"
            f"with open({str(log)!r}, 'a', encoding='utf-8') as f:\n"
            "    f.write(json.dumps(sys.argv[1:]) + '\\n')\n"
            f"os.execv({real!r}, [{real!r}, *sys.argv[1:]])\n", encoding="utf-8")
        wrapper.chmod(0o755)
        self.write("b.txt", "one\n")
        self.g("add", "b.txt")
        self.g("commit", "-q", "-m", "seed b")
        self.write("b.txt", "two\n")
        self.write("a.txt")
        self.g("add", "a.txt")
        with mock.patch.dict(os.environ, {"PATH": f"{bin_dir}{os.pathsep}{os.environ['PATH']}"}):
            git.staged_paths(self.work)
            git.unstaged_paths(self.work)
            git.untracked_paths(self.work)
            git.changed_under(self.work, "")
            git.current_branch(self.work)
            git.short_head(self.work)
            git.remote_url(self.work)
            git.user_email(self.work)
            git.ahead_behind(self.work, fetch=True)
            git.commit_index("a", self.work, capture=True)
            git.commit_paths(["b.txt"], "b", self.work, capture=True)
            git.push(self.work, set_upstream=True, capture=True)
            git.rebase_onto_upstream(self.work, capture=True)
            git.tag("v1", self.work, message="v1", capture=True)
            git.delete_tag("v1", self.work, remote="origin", capture=True)
        calls = [json.loads(line) for line in log.read_text(encoding="utf-8").splitlines()]
        self.assertTrue(calls)
        for argv in calls:
            with self.subTest(argv=argv):
                self.assertNotEqual(argv[:1], ["add"])
                self.assertNotIn("--force", argv)
                self.assertNotIn("--no-verify", argv)
                if argv[:1] == ["push"]:
                    self.assertNotIn("-f", argv)
                if argv[:1] == ["config"]:
                    self.assertIn("--get", argv)


if __name__ == "__main__":
    unittest.main()
