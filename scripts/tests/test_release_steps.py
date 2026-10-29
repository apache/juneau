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
# ***************************************************************************************************************************

import pytest

from jrm_release import details, rc
from jrm_release.ctx import StepError

pytestmark = pytest.mark.usefixtures("clean_env")

POM = "<project><parent><version>1</version></parent><version>9.2.1-SNAPSHOT</version></project>"


def test_details_validation_rejects_bad_versions():
    ok = dict(version="9.2.1", developmentVersion="9.2.2-SNAPSHOT", rc=1)
    assert details.validate(ok) is None
    assert "x.y.z" in details.validate({**ok, "version": "9.2"})
    assert "SNAPSHOT" in details.validate({**ok, "developmentVersion": "9.2.2"})
    assert "greater" in details.validate({**ok, "developmentVersion": "9.2.0-SNAPSHOT"})
    assert "rc" in details.validate({**ok, "rc": 0})


def test_workspace_argv(make_ctx):
    ctx = make_ctx()
    rc.workspace(ctx)
    ws = str(ctx.ws)
    assert ctx.fake.argvs()[0] == ["git", "clone", "--progress", "--branch", "juneau-9.2.1-branch",
                                   "https://gitbox.apache.org/repos/asf/juneau.git", ws]
    assert ["git", "-C", ws, "fetch", "--progress", "--tags", "origin"] not in ctx.fake.argvs()   # a fresh clone has the tags
    identity = [c for c in ctx.fake.calls if c["argv"][3:4] == ["config"]]
    assert [c["argv"][4:] for c in identity] == [["user.name", "jamesbognar"], ["user.email", "jamesbognar@apache.org"]]
    assert all(c.get("quiet") for c in identity)   # bookkeeping: kept off the board


def test_build_verify_and_javadoc_argv(make_ctx):
    ctx = make_ctx()
    rc.build_verify(ctx)
    rc.javadoc_verify(ctx)
    assert ["mvn", "-B", "clean", "verify"] in ctx.fake.argvs()
    assert ["mvn", "-B", "javadoc:aggregate"] in ctx.fake.argvs()
    assert all(c["cwd"] == ctx.ws for c in ctx.fake.calls if c["argv"][0] == "mvn")


def test_gpg_unlock_passphrase_on_stdin_only(make_ctx):
    ctx = make_ctx()
    rc.gpg_unlock(ctx)
    call = ctx.fake.calls[0]
    assert call["argv"][:2] == ["gpg", "--batch"]
    assert "--passphrase-fd" in call["argv"]
    assert call["input"] == b"s3cr3t-gpg\n"
    assert "s3cr3t-gpg" not in " ".join(call["argv"])


def test_deploy_snapshot_argv(make_ctx):
    ctx = make_ctx()
    rc.deploy_snapshot(ctx)
    assert ctx.fake.argvs()[-1] == ["mvn", "-B", "deploy", "-Daether.checksums.algorithms=MD5,SHA-1,SHA-512"]


def _write_pom(ctx, version):
    ctx.ws.mkdir(parents=True, exist_ok=True)
    (ctx.ws / "pom.xml").write_text(f"<project><version>{version}</version></project>")


def test_release_prepare_refuses_non_snapshot(make_ctx):
    ctx = make_ctx()
    _write_pom(ctx, "9.2.1")
    with pytest.raises(StepError, match="SNAPSHOT"):
        rc.release_prepare(ctx)
    assert not ctx.fake.ran("mvn")


def test_release_prepare_refuses_existing_tag(make_ctx):
    ctx = make_ctx()
    _write_pom(ctx, "9.2.1-SNAPSHOT")
    ctx.fake.script("git", "-C", str(ctx.ws), "tag", "--list", out="juneau-9.2.1-RC1\n")
    with pytest.raises(StepError, match="juneau-9.2.1-RC1"):
        rc.release_prepare(ctx)
    assert not ctx.fake.ran("mvn")


def test_release_prepare_argv_and_askpass_env(make_ctx):
    ctx = make_ctx()
    _write_pom(ctx, "9.2.1-SNAPSHOT")
    rc.release_prepare(ctx)
    call = ctx.fake.calls[-1]
    assert call["argv"] == ["mvn", "-B", "release:prepare", "-DautoVersionSubmodules=true",
                            "-DreleaseVersion=9.2.1", "-Dtag=juneau-9.2.1-RC1",
                            "-DdevelopmentVersion=9.2.2-SNAPSHOT"]
    assert call["env"]["GIT_ASKPASS"].endswith("jrm_release/askpass.py")
    assert call["env"]["GIT_TERMINAL_PROMPT"] == "0"
    assert call["env"]["JRM_ASKPASS_PASS"] == "s3cr3t-pw"
    assert "s3cr3t-pw" not in " ".join(call["argv"])


def ver(a, b):
    return f"<version>{a}</version>", f"<version>{b}</version>"


def _hunk(path, old, new, line=5):
    return (f"diff --git a/{path} b/{path}\nindex 1..2 100644\n--- a/{path}\n+++ b/{path}\n@@ -{line} +{line} @@\n"
            f"-{old}\n+{new}\n")


REL = "[maven-release-plugin] prepare release juneau-9.2.1-RC1"
NXT = "[maven-release-plugin] prepare for next development iteration"
ROOT_POMS = {"pom.xml": "<module>a</module><version>9.2.1-SNAPSHOT</version>",
             "a/pom.xml": "<version>9.2.1-SNAPSHOT</version>"}


def _release_ctx(make_ctx, *, rel=None, nxt=None, rel_subject=REL, nxt_subject=NXT, poms=None, follow="abc123\n"):
    ctx = make_ctx()
    tag, ws = "juneau-9.2.1-RC1", str(ctx.ws)
    rel = rel if rel is not None else (_hunk("pom.xml", *ver("9.2.1-SNAPSHOT", "9.2.1"))
                                       + _hunk("a/pom.xml", *ver("9.2.1-SNAPSHOT", "9.2.1"))
                                       + _hunk("pom.xml", "<tag>HEAD</tag>", f"<tag>{tag}</tag>", 9))
    nxt = nxt if nxt is not None else (_hunk("pom.xml", *ver("9.2.1", "9.2.2-SNAPSHOT"))
                                       + _hunk("a/pom.xml", *ver("9.2.1", "9.2.2-SNAPSHOT"))
                                       + _hunk("pom.xml", f"<tag>{tag}</tag>", "<tag>HEAD</tag>", 9))
    f = ctx.fake
    f.script("git", "-C", ws, "diff", "-U0", f"{tag}^", tag, out=rel)
    f.script("git", "-C", ws, "log", "-1", "--format=%s", tag, out=rel_subject)
    f.script("git", "-C", ws, "log", "--reverse", "--ancestry-path", "--format=%H", f"{tag}..HEAD", out=follow)
    f.script("git", "-C", ws, "diff", "-U0", f"{tag}..abc123", out=nxt)
    f.script("git", "-C", ws, "log", "-1", "--format=%s", "abc123", out=nxt_subject)
    for path, text in (poms if poms is not None else ROOT_POMS).items():
        f.script("git", "-C", ws, "show", f"{tag}^:{path}", out=text)
    return ctx


def test_release_diff_clean_passes_without_asking(make_ctx):
    ctx = _release_ctx(make_ctx)
    out = rc.release_diff_review(ctx)
    assert out == "diff clean: 2 poms 9.2.1-SNAPSHOT -> 9.2.1, tag juneau-9.2.1-RC1; next iteration 9.2.1 -> 9.2.2-SNAPSHOT"
    assert all(c.get("quiet") for c in ctx.fake.calls if c["argv"][3] in ("diff", "log", "show"))


def test_release_diff_tag_change_is_optional(make_ctx):
    # juneau's branch pom already names the RC tag, so the real commits touch only versions
    ctx = _release_ctx(make_ctx, rel=_hunk("pom.xml", *ver("9.2.1-SNAPSHOT", "9.2.1")) + _hunk("a/pom.xml", *ver("9.2.1-SNAPSHOT", "9.2.1")),
                       nxt=_hunk("pom.xml", *ver("9.2.1", "9.2.2-SNAPSHOT")) + _hunk("a/pom.xml", *ver("9.2.1", "9.2.2-SNAPSHOT")))
    assert rc.release_diff_review(ctx).startswith("diff clean")


def test_release_diff_restores_the_last_releases_tag(make_ctx):
    # the plugin left the previous RC's tag in <scm><tag>; the release commit swaps it out and the next one restores it
    old = "juneau-9.2.0-RC3"
    poms = {**ROOT_POMS, "pom.xml": f"<module>a</module><version>9.2.1-SNAPSHOT</version><scm><tag>{old}</tag></scm>"}
    ctx = _release_ctx(make_ctx, poms=poms,
                       rel=_hunk("pom.xml", *ver("9.2.1-SNAPSHOT", "9.2.1")) + _hunk("a/pom.xml", *ver("9.2.1-SNAPSHOT", "9.2.1"))
                       + _hunk("pom.xml", f"<tag>{old}</tag>", "<tag>juneau-9.2.1-RC1</tag>", 9),
                       nxt=_hunk("pom.xml", *ver("9.2.1", "9.2.2-SNAPSHOT")) + _hunk("a/pom.xml", *ver("9.2.1", "9.2.2-SNAPSHOT"))
                       + _hunk("pom.xml", "<tag>juneau-9.2.1-RC1</tag>", f"<tag>{old}</tag>", 9))
    assert rc.release_diff_review(ctx).startswith("diff clean")


def test_release_diff_tag_not_restored_is_flagged(make_ctx):
    poms = {**ROOT_POMS, "pom.xml": "<module>a</module><version>9.2.1-SNAPSHOT</version><scm><tag>juneau-9.2.0-RC3</tag></scm>"}
    ctx = _release_ctx(make_ctx, poms=poms,
                       rel=_hunk("pom.xml", *ver("9.2.1-SNAPSHOT", "9.2.1")) + _hunk("a/pom.xml", *ver("9.2.1-SNAPSHOT", "9.2.1"))
                       + _hunk("pom.xml", "<tag>juneau-9.2.0-RC3</tag>", "<tag>juneau-9.2.1-RC1</tag>", 9))
    with pytest.raises(StepError):
        rc.release_diff_review(ctx)
    assert any("+<tag>HEAD</tag>" in s for s in ctx.fake.said)


def test_release_diff_non_pom_file_fails_and_shows_it(make_ctx):
    extra = _hunk("README.md", "old", "new")
    ctx = _release_ctx(make_ctx)
    ctx.fake.script("git", "-C", str(ctx.ws), "diff", "-U0", "juneau-9.2.1-RC1^", "juneau-9.2.1-RC1",
                    out=_hunk("pom.xml", "<version>9.2.1-SNAPSHOT</version>", "<version>9.2.1</version>")
                    + _hunk("a/pom.xml", "<version>9.2.1-SNAPSHOT</version>", "<version>9.2.1</version>") + extra)
    with pytest.raises(StepError, match="unexpected changes"):
        rc.release_diff_review(ctx)
    assert any("not a pom.xml: README.md" in s for s in ctx.fake.said)


def test_release_diff_dependency_version_change_is_flagged(make_ctx):
    ctx = _release_ctx(make_ctx,
                       rel=_hunk("pom.xml", "<version>9.2.1-SNAPSHOT</version>", "<version>9.2.1</version>")
                       + _hunk("a/pom.xml", "<version>4.12</version>", "<version>4.13</version>"))
    with pytest.raises(StepError, match="unexpected changes"):
        rc.release_diff_review(ctx)
    said = "\n".join(ctx.fake.said)
    assert "-<version>4.12</version>" in said
    assert "+<version>4.13</version>" in said
    assert "9.2.1-SNAPSHOT" not in said.split("unexpected change")[1].split("[next")[0].replace("<version>9.2.1-SNAPSHOT", "")


def test_release_diff_missing_module_is_flagged(make_ctx):
    poms = {**ROOT_POMS, "b/pom.xml": "<version>9.2.1-SNAPSHOT</version>", "pom.xml": "<module>a</module><module>b</module><version>9.2.1-SNAPSHOT</version>"}
    ctx = _release_ctx(make_ctx, poms=poms)
    with pytest.raises(StepError):
        rc.release_diff_review(ctx)
    assert any("skipped by the release commit: b/pom.xml" in s for s in ctx.fake.said)


def test_release_diff_pom_outside_reactor_is_ignored(make_ctx):
    poms = {**ROOT_POMS, "orphan/pom.xml": "<version>9.2.1-SNAPSHOT</version>"}
    assert rc.release_diff_review(_release_ctx(make_ctx, poms=poms)).startswith("diff clean")


def test_release_diff_wrong_tag_is_flagged(make_ctx):
    ctx = _release_ctx(make_ctx,
                       rel=_hunk("pom.xml", *ver("9.2.1-SNAPSHOT", "9.2.1")) + _hunk("a/pom.xml", *ver("9.2.1-SNAPSHOT", "9.2.1"))
                       + _hunk("pom.xml", "<tag>HEAD</tag>", "<tag>juneau-9.2.1-RC2</tag>", 9))
    with pytest.raises(StepError):
        rc.release_diff_review(ctx)
    assert any("+<tag>juneau-9.2.1-RC2</tag>" in s for s in ctx.fake.said)


def test_release_diff_next_iteration_mismatch_is_flagged(make_ctx):
    ctx = _release_ctx(make_ctx,
                       nxt=_hunk("pom.xml", *ver("9.2.1", "9.3.0-SNAPSHOT")) + _hunk("a/pom.xml", *ver("9.2.1", "9.2.2-SNAPSHOT")))
    with pytest.raises(StepError):
        rc.release_diff_review(ctx)
    said = "\n".join(ctx.fake.said)
    assert "[next-iteration commit] unexpected change" in said
    assert "+<version>9.3.0-SNAPSHOT</version>" in said


def test_release_diff_next_iteration_wrong_message_or_missing(make_ctx):
    ctx = _release_ctx(make_ctx, nxt_subject="Fix a bug")
    with pytest.raises(StepError):
        rc.release_diff_review(ctx)
    assert any("commit message is 'Fix a bug'" in s for s in ctx.fake.said)
    none = _release_ctx(make_ctx, follow="")
    with pytest.raises(StepError):
        rc.release_diff_review(none)
    assert any("none found after juneau-9.2.1-RC1" in s for s in none.fake.said)


def test_release_perform_drops_a_stale_repo_without_asking(make_ctx):
    from conftest import FakeNexus
    nx = FakeNexus()
    nx.repos = [{"repositoryId": "orgapachejuneau-1001", "state": "open", "created": "2026-10-01"}]
    nx.repos_after = [{"repositoryId": "orgapachejuneau-1002", "state": "open", "created": "2026-10-19"}]
    ctx = make_ctx(nexus=nx)
    rc.release_perform(ctx)
    assert ("drop", "orgapachejuneau-1001") in nx.posts
    assert ctx.rf.get("nexusRepoId") == "orgapachejuneau-1002"


def test_nexus_close_polls_until_closed(make_ctx):
    from conftest import FakeNexus
    nx = FakeNexus()
    nx.states["r-1"] = [{"state": "open", "transitioning": True}, {"state": "open", "transitioning": True},
                        {"state": "closed", "transitioning": False}]
    ctx = make_ctx(nexus=nx, state={"nexusRepoId": "r-1"})
    rc.nexus_close(ctx)
    assert nx.posts == [("close", "r-1")]


def test_nexus_close_fails_on_rule_notifications(make_ctx):
    from conftest import FakeNexus
    nx = FakeNexus()
    nx.states["r-1"] = [{"state": "open", "transitioning": False, "notifications": ["Missing signature: x.jar"]}]
    ctx = make_ctx(nexus=nx, state={"nexusRepoId": "r-1"})
    with pytest.raises(StepError, match="Missing signature"):
        rc.nexus_close(ctx)


WANTED = ["juneau-9.2.1-source-release.zip", "juneau-9.2.1-source-release.zip.asc",
          "juneau-9.2.1-source-release.zip.sha512", "juneau-distrib-9.2.1-bin.zip",
          "juneau-distrib-9.2.1-bin.zip.asc", "juneau-distrib-9.2.1-bin.zip.sha512"]


def _staged(ctx, extra=(), missing=()):
    ctx.nexus.files = {n: f"https://repository.apache.org/content/repositories/r-1/org/apache/juneau/x/{n}"
                       for n in [*WANTED, *extra] if n not in missing}


def test_dist_stage_lists_entries_no_glob(make_ctx):
    ctx = make_ctx(state={"nexusRepoId": "r-1"})
    _staged(ctx, extra=["juneau-9.2.1-source-release.zip.sha1", "juneau-9.2.1-source-release.zip.md5",
                        "juneau-core-9.2.1.jar"])
    ctx.fake.script("svn", "ls", out="juneau-9.2.0-RC1/\njuneau-9.1.0-RC2/\n.hidden/\n")
    rc.dist_stage(ctx)
    assert all("*" not in " ".join(a) for a in ctx.fake.argvs())
    mucc = ctx.fake.ran("svnmucc")[0]
    flat = " ".join(mucc)
    assert "rm https://dist.apache.org/repos/dist/dev/juneau/source/juneau-9.2.0-RC1" in flat
    assert "juneau-9.1.0-RC2" not in flat
    assert ".hidden" not in flat  # other lines are left alone


def test_dist_stage_renames_and_drops_sha1_md5(make_ctx):
    ctx = make_ctx(state={"nexusRepoId": "r-1"})
    _staged(ctx, extra=["juneau-9.2.1-source-release.zip.sha1", "juneau-9.2.1-source-release.zip.md5"])
    rc.dist_stage(ctx)
    local = sorted(p.name for p in (ctx.run_dir / "dist").iterdir())
    assert local == ["apache-juneau-9.2.1-bin.zip", "apache-juneau-9.2.1-bin.zip.asc",
                     "apache-juneau-9.2.1-bin.zip.sha512", "apache-juneau-9.2.1-src.zip",
                     "apache-juneau-9.2.1-src.zip.asc", "apache-juneau-9.2.1-src.zip.sha512"]
    mucc = ctx.fake.ran("svnmucc")[0]
    assert mucc.count("put") == 6
    assert mucc.count("mkdir") == 2
    assert "--password-from-stdin" in mucc
    assert "s3cr3t-pw" not in " ".join(mucc)
    call = next(c for c in ctx.fake.calls if c["argv"][0] == "svnmucc")
    assert call["input"] == b"s3cr3t-pw\n"


def test_dist_stage_refuses_missing_file(make_ctx):
    ctx = make_ctx(state={"nexusRepoId": "r-1"})
    _staged(ctx, missing=["juneau-distrib-9.2.1-bin.zip.asc"])
    with pytest.raises(StepError, match="juneau-distrib-9.2.1-bin.zip.asc"):
        rc.dist_stage(ctx)
    assert not ctx.fake.ran("svnmucc")


def _local_dist(ctx, digest_ok=True):
    import hashlib
    out = ctx.run_dir / "dist"
    out.mkdir(parents=True, exist_ok=True)
    for name in ("apache-juneau-9.2.1-src.zip", "apache-juneau-9.2.1-bin.zip"):
        (out / name).write_bytes(b"payload " + name.encode())
        digest = hashlib.sha512((out / name).read_bytes()).hexdigest()
        (out / (name + ".sha512")).write_text(digest if digest_ok else "0" * 128)
        (out / (name + ".asc")).write_text("sig")
    listing = "".join(f"apache-juneau-9.2.1-{k}.zip{s}\n" for k in ("src", "bin") for s in ("", ".asc", ".sha512"))
    ctx.fake.script("svn", "ls", out=listing)


def test_dist_verify_checks_digest_and_signature(make_ctx):
    ctx = make_ctx()
    _local_dist(ctx)
    assert rc.dist_verify(ctx) == "dist/dev verified"
    assert len(ctx.fake.ran("gpg", "--batch", "--verify")) == 2
    bad = make_ctx()
    _local_dist(bad, digest_ok=False)
    with pytest.raises(StepError, match="SHA-512"):
        rc.dist_verify(bad)


def test_dist_verify_refuses_listing_gap(make_ctx):
    ctx = make_ctx()
    ctx.fake.script("svn", "ls", out="apache-juneau-9.2.1-src.zip\n")
    with pytest.raises(StepError, match="lacks"):
        rc.dist_verify(ctx)


def test_compose_vote_email_facts(make_ctx):
    ctx = make_ctx(state={"nexusRepoId": "orgapachejuneau-1002"})
    _local_dist(ctx)
    ctx.fake.script("git", "-C", str(ctx.ws), "rev-parse", out="abc123\n")
    with pytest.raises(ctx.pause):
        rc.compose_vote_email(ctx)
    mail = (ctx.run_dir / "emails" / "vote.eml.txt").read_text()
    assert "Subject: [VOTE] Release Apache Juneau 9.2.1 RC1\n" in mail
    assert "h=abc123" in mail
    assert "orgapachejuneau-1002" in mail
    assert "SHA512_NOT_FOUND" not in mail
    assert ctx.rf.get("voteDeadline")
    assert rc.compose_vote_email(ctx) == "vote email sent"
    assert ctx.rf.get("rcCommit") == "abc123"
    assert ctx.rf.get("voteDeadline")


# ---- vote phase

from datetime import datetime, timedelta, timezone  # noqa: E402

from jrm_release import promote, vote  # noqa: E402


def test_vote_gate_pauses_phase_vote(make_ctx):
    ctx = make_ctx()
    with pytest.raises(ctx.pause) as e:
        vote.vote_gate(ctx)
    assert ctx.rf.get("phase") == "vote"
    assert e.value.done is True
    assert "--resume" in e.value.message


def test_vote_tally_early_pause_not_done(make_ctx):
    soon = (datetime.now(timezone.utc) + timedelta(hours=10)).isoformat()
    ctx = make_ctx(state={"voteDeadline": soon})
    ctx.now = lambda: datetime.now(timezone.utc)
    with pytest.raises(ctx.pause) as e:
        vote.vote_tally(ctx)
    assert e.value.done is False


def test_result_failed_sets_vote_failed(make_ctx):
    ctx = make_ctx(state={"vote": {"outcome": "failed", "plus1": 2, "zero": 0, "minus1": 0, "binding": "A, B"}})
    with pytest.raises(ctx.pause):
        vote.compose_result_email(ctx)       # the mail is written and the run pauses for it
    with pytest.raises(ctx.pause):
        vote.compose_result_email(ctx)       # the resume records it as sent, then the failed vote pauses
    assert ctx.rf.get("phase") == "vote-failed"


def test_result_passed_continues(make_ctx):
    ctx = make_ctx(state={"vote": {"outcome": "passed", "plus1": 3, "zero": 0, "minus1": 0, "binding": "A, B, C"}})
    with pytest.raises(ctx.pause):
        vote.compose_result_email(ctx)
    assert vote.compose_result_email(ctx) == "result email sent"
    assert ctx.rf.get("phase") != "vote-failed"


# ---- promote phase

PASSED = {"vote": {"outcome": "passed"}, "nexusRepoId": "r-1", "rcCommit": "abc123", "phase": "vote"}


def test_nexus_release_refuses_without_passed_vote(make_ctx):
    from conftest import FakeNexus
    nx = FakeNexus()
    ctx = make_ctx(nexus=nx, state={"nexusRepoId": "r-1"})
    with pytest.raises(StepError, match="vote"):
        promote.nexus_release(ctx)
    assert nx.posts == []


def test_dist_promote_single_svnmucc_with_prior_rm(make_ctx):
    ctx = make_ctx(state=PASSED)
    ctx.fake.script("git", "-C", str(ctx.ws), "tag", "--list", out="juneau-9.2.0\njuneau-9.1.5\njuneau-9.2.1-RC1\n")
    ctx.fake.script("svn", "ls", out="9.2.0/\nKEYS\n")
    promote.dist_promote(ctx)
    mucc = ctx.fake.ran("svnmucc")
    assert len(mucc) == 1
    flat = " ".join(mucc[0])
    assert flat.index("rm https://dist.apache.org/repos/dist/release/juneau/9.2.0") < flat.index("mkdir")
    assert flat.count(" cp ") == 6


def test_dist_promote_skips_prior_not_listed(make_ctx):
    ctx = make_ctx(state=PASSED)
    ctx.fake.script("git", "-C", str(ctx.ws), "tag", "--list", out="juneau-9.2.0\n")
    ctx.fake.script("svn", "ls", out="KEYS\n")
    promote.dist_promote(ctx)
    assert " rm " not in " ".join(ctx.fake.ran("svnmucc")[0])


def test_final_tag_on_rc_commit_and_skip_when_present(make_ctx):
    ctx = make_ctx(state=PASSED)
    promote.final_tag(ctx)
    assert ["git", "-C", str(ctx.ws), "tag", "-a", "juneau-9.2.1", "abc123", "-m", "Apache Juneau 9.2.1"] in \
        ctx.fake.argvs()
    assert any(a[:3] == ["git", "-C", str(ctx.ws)] and "push" in a for a in ctx.fake.argvs())
    ctx2 = make_ctx(state=PASSED)
    ctx2.fake.script("git", "-C", str(ctx2.ws), "ls-remote", out="abc123\trefs/tags/juneau-9.2.1\n")
    promote.final_tag(ctx2)
    assert not [a for a in ctx2.fake.argvs() if "tag" in a and "-a" in a]


def test_github_release_on_final_tag(make_ctx):
    ctx = make_ctx(state=PASSED)
    ctx.fake.script("git", "-C", str(ctx.ws), "tag", "--list", out="juneau-9.2.1\njuneau-9.2.0\njuneau-10.0.0\n")
    ctx.fake.script("gh", "release", "create", out="https://github.com/apache/juneau/releases/tag/juneau-9.2.1\n")
    promote.github_release(ctx)
    call = ctx.fake.ran("gh")[0]
    assert call[:4] == ["gh", "release", "create", "juneau-9.2.1"]
    assert "--verify-tag" in call
    assert "--latest=false" in call            # 10.0.0 exists and is higher
    gh = next(c for c in ctx.fake.calls if c["argv"][0] == "gh")
    assert gh["env"]["GH_TOKEN"] == "s3cr3t-gh"
    assert "s3cr3t-gh" not in " ".join(gh["argv"])
    assert ctx.rf.get("githubReleaseUrl").endswith("juneau-9.2.1")


def test_github_release_latest_left_to_gh_when_highest(make_ctx):
    ctx = make_ctx(state=PASSED)
    ctx.fake.script("git", "-C", str(ctx.ws), "tag", "--list", out="juneau-9.2.1\njuneau-9.2.0\njuneau-9.2.1-RC1\n")
    promote.github_release(ctx)
    assert "--latest=false" not in ctx.fake.ran("gh")[0]


def test_announce_waits_for_downloads(make_ctx):
    seen = []
    results = iter([False, False, True])
    ctx = make_ctx(state=PASSED, http_ok=lambda u: (seen.append(u), next(results))[1])
    with pytest.raises(ctx.pause):               # the mirrors are up on the third poll; then the mail pauses
        promote.compose_announce_email(ctx)
    assert len(seen) == 3
    assert seen[0] == "https://downloads.apache.org/juneau/9.2.1/"


def test_finalize_marks_released(make_ctx):
    ctx = make_ctx(state=PASSED)
    assert "released" in promote.finalize(ctx)
    assert ctx.rf.get("phase") == "released"
    assert ctx.rf.get("last") == "finalize"


@pytest.mark.parametrize("refs,want", [
    ("origin/master\norigin/juneau-9.2.1-branch\n", "juneau-9.2.1-branch"),
    ("origin/master\norigin/9.2.x\n", "9.2.x"),
    ("origin/master\norigin/9.3.x\n", "master"),
    ("", "master")])
def test_branch_default_picks_the_release_branch(make_ctx, refs, want):
    ctx = make_ctx(with_details=False)
    ctx.fake.script("git", "-C", details.checkout(), "for-each-ref", out=refs)
    assert details.branch_default(ctx, "9.2.1") == want


# --- R0047: honest dry-run results -------------------------------------

def test_dry_run_build_says_it_skipped(make_ctx):
    ctx = make_ctx(dry_run=True)
    assert rc.build_verify(ctx) == "skipped (dry run): would run mvn -B clean verify"
    assert "would run: mvn -B clean verify" in ctx.fake.said


def test_real_run_build_keeps_its_result(make_ctx):
    assert rc.build_verify(make_ctx()) == "build verified"


def test_dry_run_mixed_step_names_every_skipped_command(make_ctx):
    out = rc.workspace(make_ctx(dry_run=True))
    assert out.startswith("skipped (dry run): would run git clone --progress --branch juneau-9.2.1-branch ")
    assert "; +1 more" in out


def test_dist_verify_dry_run_does_not_claim_verified(make_ctx):
    assert rc.dist_verify(make_ctx(dry_run=True)) == "skipped (dry run): nothing was staged to verify"


def test_prerequisites_row_replays_the_early_verdicts_as_bar_cells(make_ctx, monkeypatch):
    from jrm_release import prereqs, preflight
    monkeypatch.setattr(preflight.shutil, "which", lambda n: "/usr/bin/" + n if n != "gh" else None)
    ctx = make_ctx(with_details=False)
    failed = prereqs.check(ctx)
    n = len(ctx.fake.calls)
    cells = prereqs.checks(ctx)
    assert len(ctx.fake.calls) == n
    assert [c.name for c in cells][:3] == ["git", "mvn", "java"]
    assert [c.name for c in cells if c.fn().status == "fail"] == ["gh"]
    assert len(failed) == 1


def test_release_checks_replay_the_preflight_repo_group(make_ctx):
    from jrm_release import preflight
    ctx = make_ctx()
    ctx.preflight_results = [("tools", "tool git", preflight.Result("pass", "found")),
                             ("repo", "branch on origin", preflight.Result("pass", "b")),
                             ("repo", "RC tag absent", preflight.Result("fail", "tag exists")),
                             ("notes", "RELEASE-NOTES has 9.2.1", preflight.Result("pass", "ok"))]
    cells = rc.release_checks(ctx)
    assert [c.name for c in cells] == ["branch on origin", "RC tag absent"]
    assert [c.fn().status for c in cells] == ["pass", "fail"]
    assert cells[1].fn().reason == "tag exists"
    assert ctx.fake.calls == []                 # a replay runs nothing


def test_release_checks_run_the_repo_group_when_no_preflight_ran(make_ctx):
    ctx = make_ctx()
    ctx.fake.script("git", "ls-remote", "https://gitbox.apache.org/repos/asf/juneau.git", "refs/heads/juneau-9.2.1-branch",
                    out="abc\trefs/heads/juneau-9.2.1-branch\n")
    names = [c.name for c in rc.release_checks(ctx)]
    assert names[:2] == ["branch on origin", "RC tag absent"]
    assert ctx.fake.ran("git", "ls-remote")


def test_registry_runs_both_check_groups_as_labelled_bars():
    import release
    from conftest import FakeRunner
    reg = release.registry(release.Deps(run=FakeRunner(), steps=__import__("juneau_terminal_scripts.steps", fromlist=["x"]),
                                        lock=None), release.impl_map())
    assert reg._labels["prerequisites"] == "01 Prerequisites"
    assert reg._labels["prechecks"] == "02 Release checks"
    assert len(reg.names) == 25
    assert "test-workspace-verify" not in reg.names


def test_dry_run_emails_answer_themselves(make_ctx):
    from jrm_release import emails
    ctx = make_ctx(dry_run=True)   # no email.* answer: a prompt would raise
    assert emails.send(ctx, "propose", "dev@juneau.apache.org", "Subj", "Body") == "skipped (dry run): propose email not sent"
    assert any("nothing is sent" in s for s in ctx.fake.said)
    assert ctx.rf.get("emails") is None


def test_dry_run_read_only_command_in_a_missing_workspace_is_not_run(make_ctx):
    ctx = make_ctx(dry_run=True)
    assert ctx.runner.run(["git", "-C", str(ctx.ws), "tag", "--list"], title="t", mutating=False).out == ""
    assert ctx.runner.run(["git", "tag"], title="t", mutating=False, cwd=ctx.ws).ok
    assert ctx.fake.calls == []
    assert ctx.runner.skipped == []
    assert any("would run (dry run created no" in s for s in ctx.fake.said)
    ctx.ws.mkdir(parents=True)   # a workspace from an earlier real run is used
    ctx.runner.run(["git", "-C", str(ctx.ws), "tag", "--list"], title="t", mutating=False)
    assert len(ctx.fake.calls) == 1
    live = make_ctx()            # and a real run never skips it
    live.runner.run(["git", "-C", str(live.ws / "nope"), "tag"], title="t", mutating=False)
    assert len(live.fake.calls) == 1


def test_dry_run_steps_touching_the_workspace_skip_cleanly(make_ctx):
    from jrm_release import promote
    ctx = make_ctx(dry_run=True, state={"rcCommit": "abc"})
    assert rc.release_prepare(ctx).startswith("skipped (dry run): would run mvn")
    promote.final_tag(ctx)
    promote._release_tags(ctx)
    assert not [a for a in ctx.fake.argvs() if "-C" in a and str(ctx.ws) in a]


def test_retired_step_in_a_saved_run_resumes_after_the_previous_one(home):
    import json
    from jrm_release.runfile import RunFile
    path = home / "runs" / "9.2.1" / "run.json"
    path.parent.mkdir(parents=True)
    path.write_text(json.dumps({"schema": 1, "phase": "rc", "last": "test-workspace-verify",
                                "details": {"version": "9.2.1", "rc": 1, "branch": "b"}}))
    rf = RunFile(path)
    assert rf.last() == "javadoc-verify"
    import release
    assert release.plan_for(home, resume=True)[0] == "gpg-unlock"


def test_quiet_passes_env_and_input_to_the_command(make_ctx):
    ctx = make_ctx()
    ctx.runner.quiet(["gh", "api", "user"], title="gh user", env={"GH_TOKEN": "tok"}, input=b"pw\n")
    call = ctx.fake.calls[-1]
    assert call["env"] == {"GH_TOKEN": "tok"} and call["input"] == b"pw\n" and call["quiet"] is True
    assert "tok" not in " ".join(call["argv"]) and "pw" not in " ".join(call["argv"])


def test_quiet_real_path_merges_env_and_decodes_bytes_output(make_ctx, monkeypatch):
    from types import SimpleNamespace
    from jrm_release import runner as runner_module
    seen = {}

    def fake_run(argv, **kw):
        seen.update(kw)
        return SimpleNamespace(returncode=0, stdout=b"out", stderr=b"err")
    monkeypatch.setattr(runner_module, "subprocess", SimpleNamespace(run=fake_run, TimeoutExpired=TimeoutError))
    out = runner_module.Runner(SimpleNamespace()).quiet(["x"], title="t", env={"GH_TOKEN": "tok"}, input=b"pw\n",
                                                        merge_err=True)
    assert seen["env"]["GH_TOKEN"] == "tok" and seen["input"] == b"pw\n" and seen["text"] is False
    assert out.out == "outerr"


@pytest.mark.parametrize("ok, out, expected", [
    (True, "file.zip\n", True),
    (False, "svn: E170000: URL 'https://x/y' doesn't exist\n", False),
    (False, "svn: warning: W160013: '/y' path not found; non-existent in revision 5", False),
    (False, "svn: warning: W160013: '/repos/dist/!svn/rvr/88862/dev/juneau/source/x' path not found\n"
            "svn: E200009: Could not list all targets because some targets don't exist\n", False),
    (False, "svn: E170013: Unable to connect to a repository at URL\n", None),
])
def test_svn_exists_tells_absent_from_cannot_list(make_ctx, ok, out, expected):
    ctx = make_ctx()
    ctx.fake.script("svn", "ls", out=out, ok=ok)
    assert ctx.svn.exists("https://dist.apache.org/repos/dist/dev/juneau/source/x") is expected
