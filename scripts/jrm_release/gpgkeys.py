"""Read-only parsing of `gpg --list-secret-keys --with-colons`, to offer the user's own signing key as the default."""
import re
from dataclasses import dataclass, field


@dataclass
class Key:
    keyid: str
    fingerprint: str = ""
    expires: int = 0            # epoch seconds, 0 = never
    validity: str = ""
    caps: str = ""
    uids: list = field(default_factory=list)


def parse(text):
    """The secret (sec) keys in colon-listing text, each with its fingerprint and uids."""
    keys, cur = [], None
    for line in (text or "").splitlines():
        f = line.split(":")
        if f[0] == "sec" and len(f) > 11:
            cur = Key(keyid=f[4], validity=f[1], caps=f[11], expires=int(f[6]) if f[6].isdigit() else 0)
            keys.append(cur)
        elif f[0] == "ssb":
            cur = None
        elif f[0] == "fpr" and cur is not None and not cur.fingerprint and len(f) > 9:
            cur.fingerprint = f[9]
        elif f[0] == "uid" and cur is not None and len(f) > 9:
            cur.uids.append(f[9])
    return keys


def candidates(keys, email, now):
    """Usable keys for this address: signing-capable, not revoked or expired, a uid carrying <email>."""
    wanted = f"<{email}>".lower()
    return [k for k in keys
            if "s" in k.caps.lower() and k.validity not in ("r", "e", "i", "d")
            and not (k.expires and k.expires <= now)
            and any(wanted in u.lower() for u in k.uids)]


def in_keys_file(text, key):
    """True when KEYS text mentions the key's fingerprint or long id (spacing and case ignored)."""
    flat = re.sub(r"\s+", "", text or "").upper()
    return any(x and x.upper() in flat for x in (key.fingerprint, key.keyid))
