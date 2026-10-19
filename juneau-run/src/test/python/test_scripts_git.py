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

import contextlib
import io
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
from scripts_support import jr

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

    def test_rebase_onto_upstream_does_not_autostash_by_default(self):
        with mock.patch.object(git, "_write", return_value=git.Result(True, 0)) as write:
            git.rebase_onto_upstream(self.work)
            git.rebase_onto_upstream(self.work, autostash=True)
        self.assertEqual([c.args[0] for c in write.call_args_list],
                         [["git", "rebase", "@{upstream}"], ["git", "rebase", "--autostash", "@{upstream}"]])


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
        saved = [os.dup(1), os.dup(2)]            # the child inherits fds 1/2; point them at /dev/null so git's line stays off the test log
        null = os.open(os.devnull, os.O_WRONLY)
        try:
            os.dup2(null, 1)
            os.dup2(null, 2)
            r = git.commit_index("a", self.work)
        finally:
            os.dup2(saved[0], 1)
            os.dup2(saved[1], 2)
            for fd in saved + [null]:
                os.close(fd)
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


class ConsoleNoneTest(GitCase):
    """Under JUNEAU_RUN_CONSOLE=none with juneau_run loaded, a write's output goes to the full log, not the terminal."""

    def setUp(self):
        super().setUp()
        self.log = self.tmp / "full.log"
        patcher = mock.patch.dict(os.environ, {"JUNEAU_RUN_CONSOLE": "none", "JUNEAU_RUN_FULL_LOG": str(self.log)})
        patcher.start()
        self.addCleanup(patcher.stop)
        modules = mock.patch.dict(sys.modules, {"juneau_run": jr})
        modules.start()
        self.addCleanup(modules.stop)

    def test_failed_write_is_captured_and_logged(self):
        real = git._run
        captures = []

        def spy(cmd, **kw):
            captures.append(kw.get("capture"))
            return real(cmd, **kw)

        with mock.patch.object(git, "_run", spy):
            r = git.commit_index("msg", self.work)
        self.assertFalse(r.ok)
        self.assertEqual((r.out, r.err), ("", ""))
        self.assertEqual(captures, [True])
        self.assertIn(b"nothing to commit", self.log.read_bytes())

    def test_successful_write_is_logged(self):
        self.write("a.txt")
        self.g("add", "a.txt")
        self.assertTrue(git.commit_index("logged commit", self.work).ok)
        self.assertIn(b"logged commit", self.log.read_bytes())

    def test_capture_still_returns_the_output_and_logs_nothing(self):
        r = git.commit_index("msg", self.work, capture=True)
        self.assertIn("nothing to commit", r.out + r.err)
        self.assertFalse(self.log.exists() and b"nothing to commit" in self.log.read_bytes())

    def test_warning_goes_to_the_log_not_stderr(self):
        stderr = io.StringIO()
        with contextlib.redirect_stderr(stderr):
            git._warn("stash could not be dropped")
        self.assertEqual(stderr.getvalue(), "")
        self.assertIn(b"stash could not be dropped", self.log.read_bytes())


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
            git.unreviewed_paths(self.work)
            git.upstream_verdict(self.work)
            git.push_command(self.work)
        self.g("reset", "-q", "--hard", "HEAD~1")            # behind origin, so the rebase stashes and restores
        self.write("c.txt")
        self.g("add", "c.txt")
        with mock.patch.dict(os.environ, {"PATH": f"{bin_dir}{os.pathsep}{os.environ['PATH']}"}):
            self.assertEqual(git.rebase_keeping_index(self.work).status, "rebased")
            git.commit_index("c", self.work, capture=True)
            git.push_tracked(self.work, capture=True)
        calls = [json.loads(line) for line in log.read_text(encoding="utf-8").splitlines()]
        self.assertTrue(calls)
        autostash_rebases = []
        for argv in calls:
            with self.subTest(argv=argv):
                self.assertNotEqual(argv[:1], ["add"])
                self.assertNotIn("--force", argv)
                self.assertNotIn("--no-verify", argv)
                if argv[:1] == ["push"]:
                    self.assertNotIn("-f", argv)
                if argv[:1] == ["config"]:
                    self.assertIn("--get", argv)
                if argv[:1] == ["rebase"] and "--autostash" in argv:
                    autostash_rebases.append(argv)
        self.assertEqual(autostash_rebases, [])    # only rebase_onto_upstream(autostash=True) passes it


class UpstreamCase(GitCase):
    """GitCase plus a second clone of origin that plays upstream."""

    def other(self):
        other = self.tmp / "other"
        if not other.exists():
            self.g("clone", "-q", str(self.origin), str(other), cwd=self.tmp)
            for key, value in (("user.name", "O"), ("user.email", "o@example.com"), ("commit.gpgsign", "false")):
                self.g("config", key, value, cwd=other)
        return other

    def upstream_commit(self, files, message="upstream", branch="main"):
        """Commit files ({rel: bytes | str | None}; None deletes) in the other clone and push them to origin."""
        other = self.other()
        self.g("checkout", "-q", branch, cwd=other)
        for rel, data in files.items():
            path = other / rel
            if data is None:
                self.g("rm", "-q", "--", rel, cwd=other)
                continue
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data if isinstance(data, bytes) else data.encode())
            self.g("add", "--", rel, cwd=other)
        self.g("commit", "-q", "-m", message, cwd=other)
        self.g("push", "-q", "origin", branch, cwd=other)

    def commit(self, files, message="local"):
        for rel, data in files.items():
            path = self.work / rel
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data if isinstance(data, bytes) else data.encode())
            self.g("add", "--", rel)
        self.g("commit", "-q", "-m", message)

    def stash_count(self):
        return len(self.g("stash", "list").splitlines())

    def index_blob(self, rel):
        return subprocess.run(["git", "--literal-pathspecs", "show", f":{rel}"], cwd=self.work, check=True,
                              capture_output=True).stdout


class StagedPathsBytesTest(GitCase):
    def test_rename_lists_both_sides(self):
        self.g("mv", "README.md", "READ.md")
        self.assertEqual(git.staged_paths(self.work), ["READ.md", "README.md"])

    def test_name_with_a_cr_is_kept_exactly(self):
        self.write("a\rb.txt")
        self.g("add", "a\rb.txt")
        self.assertEqual(git.staged_paths(self.work), ["a\rb.txt"])


class UnreviewedTest(GitCase):
    def test_sorted_union_minus_exempt(self):
        self.write("README.md", "edited\n")
        self.write("z.txt")
        self.write("CHANGELOG.md")
        self.write("staged.txt")
        self.g("add", "staged.txt")
        self.assertEqual(git.unreviewed_paths(self.work), ["CHANGELOG.md", "README.md", "z.txt"])
        self.assertEqual(git.unreviewed_paths(self.work, exempt=[Path("CHANGELOG.md")]), ["README.md", "z.txt"])

    def test_ignored_files_are_not_unreviewed(self):
        self.write(".gitignore", "target/\n")
        self.commit_ignore()
        self.write("target/out.class")
        self.assertEqual(git.unreviewed_paths(self.work), [])

    def commit_ignore(self):
        self.g("add", ".gitignore")
        self.g("commit", "-q", "-m", "ignore")

    def test_refusal_wording(self):
        reason, hint = git.unreviewed_refusal(["a b.txt", "c.txt"], outcome="Nothing was pushed.",
                                              git_cmd="git -C ../docs")
        self.assertEqual(reason, "2 paths are not staged")
        self.assertIn("  a b.txt", hint)
        self.assertTrue(any(line.startswith("Why: ") for line in hint))
        self.assertTrue(any(line.startswith("Nothing was pushed. ") for line in hint))
        self.assertEqual(hint[-2:], ("  git -C ../docs stash push -- 'a b.txt' c.txt", "  git -C ../docs stash pop"))
        self.assertEqual(git.unreviewed_refusal(["x"], outcome="")[0], "1 path is not staged")


class UpstreamVerdictTest(UpstreamCase):
    def test_level_and_ahead_are_ok(self):
        self.assertEqual(git.upstream_verdict(self.work).status, "ok")
        self.commit({"a.txt": "a\n"})
        verdict = git.upstream_verdict(self.work)
        self.assertEqual((verdict.status, verdict.ahead, verdict.behind, verdict.ok), ("ok", 1, 0, True))

    def test_no_upstream_proceeds(self):
        self.g("checkout", "-q", "-b", "feature")
        verdict = git.upstream_verdict(self.work)
        self.assertEqual(verdict.status, "no-upstream")
        self.assertTrue(verdict.ok)

    def test_fetch_error_fails_closed(self):
        self.g("remote", "set-url", "origin", str(self.tmp / "missing.git"))
        verdict = git.upstream_verdict(self.work)
        self.assertEqual(verdict.status, "error")
        self.assertFalse(verdict.ok)
        self.assertTrue(verdict.reason.startswith("could not check upstream: "))

    def test_not_a_repository_is_an_error_not_a_raise(self):
        plain = self.tmp / "plain"
        plain.mkdir()
        self.assertEqual(git.upstream_verdict(plain).status, "error")

    def test_behind_only_hints_ff_only(self):
        self.upstream_commit({"b.txt": "b\n"})
        self.upstream_commit({"c.txt": "c\n"})
        verdict = git.upstream_verdict(self.work, git_cmd="git -C ../x")
        self.assertEqual((verdict.status, verdict.ahead, verdict.behind), ("behind", 0, 2))
        self.assertEqual(verdict.reason, "2 commits behind upstream")
        self.assertEqual(verdict.hint[-1], "  git -C ../x merge --ff-only @{upstream}")

    def test_behind_and_ahead_hints_stash_and_rebase_never_autostash(self):
        self.upstream_commit({"b.txt": "b\n"})
        self.commit({"a.txt": "a\n"})
        verdict = git.upstream_verdict(self.work)
        self.assertEqual((verdict.status, verdict.ahead, verdict.behind), ("diverged", 1, 1))
        self.assertEqual(verdict.reason, "1 commit behind upstream")
        self.assertEqual(verdict.hint[-1], "  git stash push && git pull --rebase && git stash pop --index")
        self.assertNotIn("--autostash", " ".join(verdict.hint))


class RebaseKeepingIndexTest(UpstreamCase):
    def keep(self, **kw):
        return git.rebase_keeping_index(self.work, **kw)

    def test_up_to_date_does_nothing(self):
        self.write("a.txt")
        self.g("add", "a.txt")
        result = self.keep()
        self.assertEqual((result.ok, result.status), (True, "up-to-date"))
        self.assertEqual(self.stash_count(), 0)
        self.assertEqual(git.staged_paths(self.work), ["a.txt"])

    def test_refuses_unreviewed_before_stashing(self):
        self.upstream_commit({"b.txt": "b\n"})
        self.write("loose.txt")
        result = self.keep()
        self.assertEqual((result.ok, result.status, result.reason), (False, "refused", "1 path is not staged"))
        self.assertIn("  loose.txt", result.hint)
        self.assertEqual(self.stash_count(), 0)
        self.assertEqual(git.ahead_behind(self.work, fetch=True)[1], 1)    # nothing was rebased

    def test_exempt_path_is_not_refused(self):
        self.upstream_commit({"b.txt": "b\n"})
        self.write("CHANGELOG.md")
        self.assertEqual(self.keep(exempt=["CHANGELOG.md"]).status, "rebased")

    def test_staged_modify_delete_rename_and_new_come_back_staged(self):
        self.commit({"mod.txt": "one\n", "del.txt": "gone\n", "old.txt": "moved\n"}, "seed")
        self.g("push", "-q")
        self.upstream_commit({"up.txt": "upstream\n"})
        self.write("mod.txt", "two\n")
        self.g("add", "mod.txt")
        self.g("rm", "-q", "del.txt")
        self.g("mv", "old.txt", "new-name.txt")
        self.write("fresh.txt", "new\n")
        self.g("add", "fresh.txt")
        before = git.staged_paths(self.work)
        result = self.keep()
        self.assertEqual((result.ok, result.status), (True, "rebased"), result)
        self.assertEqual(git.staged_paths(self.work), before)
        self.assertEqual(self.index_blob("mod.txt"), b"two\n")
        self.assertEqual(self.index_blob("new-name.txt"), b"moved\n")
        self.assertTrue((self.work / "up.txt").exists())
        self.assertFalse((self.work / "del.txt").exists())
        self.assertEqual(git.unreviewed_paths(self.work), [])
        self.assertEqual(self.stash_count(), 0)

    def test_nothing_staged_rebases_without_a_stash(self):
        self.upstream_commit({"b.txt": "b\n"})
        self.commit({"a.txt": "a\n"})
        result = self.keep()
        self.assertEqual(result.status, "rebased")
        self.assertIsNone(result.stash_sha)
        self.assertEqual(git.ahead_behind(self.work, fetch=False), (1, 0, None))

    def test_same_file_upstream_change_keeps_the_stash_and_hints(self):
        self.upstream_commit({"README.md": "upstream readme\n"})
        self.write("README.md", "my readme\n")
        self.g("add", "README.md")
        result = self.keep()
        self.assertEqual((result.ok, result.status), (False, "restore-conflict"))
        self.assertEqual(result.stash_ref, "stash@{0}")
        self.assertEqual(self.g("rev-parse", "refs/stash"), result.stash_sha)
        self.assertIn(git.STASH_MESSAGE, self.g("stash", "list"))
        self.assertTrue(any("stash@{0}" in line for line in result.hint))
        self.assertTrue(any("git stash pop --index stash@{0}" in line for line in result.hint))
        self.assertTrue(any(f"The stash is commit {result.stash_sha[:12]};" in line for line in result.hint))

    def test_rebase_conflict_keeps_the_stash(self):
        self.upstream_commit({"README.md": "upstream readme\n"})
        self.commit({"README.md": "local readme\n"})
        self.write("other.txt")
        self.g("add", "other.txt")
        result = self.keep()
        self.assertEqual((result.ok, result.status), (False, "rebase-conflict"))
        self.assertEqual(self.stash_count(), 1)
        self.assertTrue(any("git rebase --abort" in line for line in result.hint))
        self.assertTrue(any(result.stash_ref in line for line in result.hint))
        self.assertTrue(any(f"The stash is commit {result.stash_sha[:12]};" in line for line in result.hint))
        self.g("rebase", "--abort")

    def test_both_sides_delete_proceeds_with_the_deletion_left_out(self):
        self.commit({"gone.txt": "x\n"}, "seed")
        self.g("push", "-q")
        self.upstream_commit({"gone.txt": None})
        self.g("rm", "-q", "gone.txt")
        self.write("keep.txt", "k\n")
        self.g("add", "keep.txt")
        result = self.keep()
        self.assertEqual(result.status, "rebased", result)
        self.assertEqual(git.staged_paths(self.work), ["keep.txt"])
        self.assertFalse((self.work / "gone.txt").exists())
        self.assertEqual(self.stash_count(), 0)

    def test_binary_and_cr_paths_restore_byte_for_byte(self):
        blob = bytes(range(256)) * 4
        self.upstream_commit({"b.txt": "b\n"})
        (self.work / "data.bin").write_bytes(blob)
        (self.work / "cr\rname.txt").write_bytes(b"line\r\n")
        self.g("add", "data.bin", "cr\rname.txt")
        result = self.keep()
        self.assertEqual(result.status, "rebased", result)
        self.assertEqual(self.index_blob("data.bin"), blob)
        self.assertEqual(self.index_blob("cr\rname.txt"), b"line\r\n")
        self.assertEqual(sorted(git.staged_paths(self.work)), ["cr\rname.txt", "data.bin"])

    def test_unicode_tab_space_and_glob_paths(self):
        names = ["ünï cödé.txt", "tab\there.txt", "with space.txt", "[ab].txt", "*.txt", "a?.txt"]
        self.commit({"a.txt": "decoy\n", "b.txt": "decoy\n"}, "decoys")   # a glob, if expanded, would match these
        self.g("push", "-q")
        self.upstream_commit({"up.txt": "u\n"})
        for name in names:
            (self.work / name).write_text(name + "\n", encoding="utf-8")
        self.g("add", "--", *names)
        self.g("rm", "-q", "--cached", "--", "b.txt")
        (self.work / "b.txt").unlink()
        before = sorted(git.staged_paths(self.work))
        result = self.keep()
        self.assertEqual(result.status, "rebased", result)
        self.assertEqual(sorted(git.staged_paths(self.work)), before)
        for name in names:
            self.assertEqual(self.index_blob(name), (name + "\n").encode())
        self.assertEqual(self.index_blob("a.txt"), b"decoy\n")

    def test_non_origin_remote(self):
        self.g("remote", "rename", "origin", "upstream")
        self.upstream_commit({"b.txt": "b\n"})
        self.write("a.txt")
        self.g("add", "a.txt")
        result = self.keep()
        self.assertEqual(result.status, "rebased", result)
        self.assertTrue((self.work / "b.txt").exists())
        self.assertEqual(git.staged_paths(self.work), ["a.txt"])

    def test_branch_name_with_a_slash(self):
        self.g("checkout", "-q", "-b", "feature/x")
        self.g("push", "-q", "-u", "origin", "feature/x")
        self.other()
        self.g("fetch", "-q", cwd=self.other())
        self.g("checkout", "-q", "-b", "feature/x", "origin/feature/x", cwd=self.other())
        self.upstream_commit({"b.txt": "b\n"}, branch="feature/x")
        self.write("a.txt")
        self.g("add", "a.txt")
        result = self.keep()
        self.assertEqual(result.status, "rebased", result)
        self.assertEqual(git.staged_paths(self.work), ["a.txt"])

    def test_detached_head_is_an_error_and_stashes_nothing(self):
        self.g("checkout", "-q", "--detach")
        self.write("a.txt")
        self.g("add", "a.txt")
        result = self.keep()
        self.assertEqual((result.ok, result.status), (False, "error"))
        self.assertIn("detached", result.reason)
        self.assertEqual(self.stash_count(), 0)

    def test_no_upstream_is_ok(self):
        self.g("checkout", "-q", "-b", "feature")
        self.assertEqual((self.keep().ok, self.keep().status), (True, "no-upstream"))

    def test_merges_keeps_a_merge_commit(self):
        self.g("checkout", "-q", "-b", "side")
        self.commit({"side.txt": "s\n"}, "side")
        self.g("checkout", "-q", "main")
        self.commit({"main.txt": "m\n"}, "main work")
        self.g("merge", "-q", "--no-ff", "--no-edit", "side")
        self.upstream_commit({"up.txt": "u\n"})
        self.write("a.txt")
        self.g("add", "a.txt")
        result = self.keep(merges=True)
        self.assertEqual(result.status, "rebased", result)
        self.assertEqual(len(self.g("rev-list", "--parents", "-n", "1", "HEAD").split()), 3)
        self.assertEqual(git.staged_paths(self.work), ["a.txt"])

    def test_failed_stash_drop_only_warns(self):
        self.upstream_commit({"b.txt": "b\n"})
        self.write("a.txt")
        self.g("add", "a.txt")
        real = git._run

        def failing_drop(cmd, **kw):
            if "drop" in cmd:
                return git.Result(False, 1, out=b"", err="cannot lock ref")
            return real(cmd, **kw)

        stderr = io.StringIO()
        with mock.patch.object(git, "_run", failing_drop), contextlib.redirect_stderr(stderr):
            result = self.keep()
        self.assertEqual((result.ok, result.status), (True, "rebased"))
        self.assertIn("could not be dropped: cannot lock ref", result.reason)
        self.assertIn("could not be dropped", stderr.getvalue())
        self.assertEqual(git.staged_paths(self.work), ["a.txt"])
        self.assertEqual(self.stash_count(), 1)

    def test_stash_that_did_not_move_is_an_error(self):
        self.upstream_commit({"b.txt": "b\n"})
        self.write("a.txt")
        self.g("add", "a.txt")
        real = git._run

        def fake_stash(cmd, **kw):
            if cmd[1:3] == ["stash", "push"]:
                return git.Result(True, 0)
            return real(cmd, **kw)

        with mock.patch.object(git, "_run", fake_stash):
            result = self.keep()
        self.assertEqual((result.ok, result.status), (False, "error"))
        self.assertIn("created no stash", result.reason)
        self.assertEqual(git.ahead_behind(self.work, fetch=False)[1], 1)


class PushCommandTest(UpstreamCase):
    def test_tracked_branch_pushes_head_to_its_merge_ref(self):
        self.assertEqual(git.push_command(self.work), ["git", "push", "origin", "HEAD:refs/heads/main"])

    def test_differently_named_merge_ref_and_remote(self):
        self.g("remote", "add", "mirror", str(self.origin))
        self.g("checkout", "-q", "-b", "local/name")
        self.g("config", "branch.local/name.remote", "mirror")
        self.g("config", "branch.local/name.merge", "refs/heads/release")
        self.assertEqual(git.push_command(self.work), ["git", "push", "mirror", "HEAD:refs/heads/release"])
        self.commit({"a.txt": "a\n"})
        self.assertTrue(git.push_tracked(self.work, capture=True).ok)
        self.assertIn("refs/heads/release", self.g("ls-remote", "origin"))

    def test_no_upstream_sets_one_on_origin(self):
        self.g("checkout", "-q", "-b", "feature")
        self.assertEqual(git.push_command(self.work), ["git", "push", "-u", "origin", "feature"])

    def test_refusals(self):
        def refused(setup):
            setup()
            with self.assertRaises(GitError) as caught:
                git.push_command(self.work)
            r = git.push_tracked(self.work, capture=True)
            self.assertEqual((r.ok, r.code), (False, 1))
            return str(caught.exception)

        self.assertIn("detached", refused(lambda: self.g("checkout", "-q", "--detach")))
        self.g("checkout", "-q", "main")
        self.assertIn("local branch", refused(lambda: self.g("config", "branch.main.remote", ".")))
        self.assertIn("could not resolve", refused(lambda: self.g("config", "--unset", "branch.main.remote")))
        self.g("config", "branch.main.remote", "origin")
        self.g("config", "branch.main.merge", "refs/tags/v1")
        with self.assertRaises(GitError) as caught:
            git.push_command(self.work)
        self.assertIn("not a branch", str(caught.exception))


class PolledTest(GitCase):
    class TickingJr:
        def __init__(self):
            self.ticks = 0

        def console_active(self):
            return True

        def tick(self):
            self.ticks += 1

        def say(self, text="", level="info", step=None):
            pass

    def test_ticks_while_a_command_runs(self):
        fake = self.TickingJr()
        with mock.patch.dict(sys.modules, {"juneau_run": fake}):
            r = git._run([sys.executable, "-c", "import time, sys; time.sleep(0.35); print('x' * 200000)"])
        self.assertTrue(r.ok)
        self.assertEqual(len(r.out.strip()), 200000)
        self.assertGreaterEqual(fake.ticks, 2)

    def test_input_and_bytes_through_the_polled_path(self):
        fake = self.TickingJr()
        with mock.patch.dict(sys.modules, {"juneau_run": fake}):
            r = git._run([sys.executable, "-c", "import sys; sys.stdout.buffer.write(sys.stdin.buffer.read())"],
                         input=b"a\rb\0", binary=True)
        self.assertEqual(r.out, b"a\rb\0")

    def test_input_survives_a_poll_timeout(self):
        fake = self.TickingJr()
        child = "import sys, time; time.sleep(0.4); sys.stdout.write(sys.stdin.read())"
        with mock.patch.dict(sys.modules, {"juneau_run": fake}), mock.patch.object(git, "POLL_SECONDS", 0.05):
            r = git._run([sys.executable, "-c", child], input="payload\n")
        self.assertTrue(r.ok, r.err)
        self.assertEqual(r.out, "payload\n")
        self.assertGreaterEqual(fake.ticks, 2)

    def test_no_console_never_ticks(self):
        fake = self.TickingJr()
        fake.console_active = lambda: False
        with mock.patch.dict(sys.modules, {"juneau_run": fake}):
            git._run([sys.executable, "-c", "import time; time.sleep(0.25)"])
        self.assertEqual(fake.ticks, 0)


if __name__ == "__main__":
    unittest.main()
