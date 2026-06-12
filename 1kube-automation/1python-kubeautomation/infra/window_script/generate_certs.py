"""Generate TLS certs for a KWOK cluster — YAML-safe kubeconfig."""
import sys, os, datetime, ipaddress

from cryptography import x509
from cryptography.x509.oid import NameOID, ExtendedKeyUsageOID
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa

_UTC = datetime.timezone.utc
_NOW = lambda: datetime.datetime.now(tz=_UTC)
_LF   = "\n"   # Unix linefeeds only — safest for all YAML parsers


def _read(path):
    with open(path, "rb") as f:
        return f.read()


def _write(path, data):
    with open(path, "wb") as f:
        f.write(data)


def _write_str(path, text):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


def pem_b64(pem_text):
    lines = []
    capture = False
    for line in pem_text.splitlines():
        s = line.strip()
        if s.startswith("-----BEGIN"):
            capture = True
            continue
        if s.startswith("-----END"):
            break
        if capture:
            lines.append(s)
    return "\n".join(lines)


def generate_certs(cluster_dir: str) -> bool:
    os.makedirs(cluster_dir, exist_ok=True)

    ca_priv = rsa.generate_private_key(65537, 2048)
    ca_name = x509.Name([
        x509.NameAttribute(NameOID.COUNTRY_NAME, u"US"),
        x509.NameAttribute(NameOID.ORGANIZATION_NAME, u"kwok"),
        x509.NameAttribute(NameOID.COMMON_NAME, u"kwok-ca"),
    ])
    ca_cert = (
        x509.CertificateBuilder()
        .subject_name(ca_name).issuer_name(ca_name)
        .public_key(ca_priv.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(_NOW() - datetime.timedelta(days=1))
        .not_valid_after(_NOW() + datetime.timedelta(days=3650))
        .add_extension(x509.BasicConstraints(ca=True, path_length=None), critical=True)
        .sign(ca_priv, hashes.SHA256())
    )
    _write(os.path.join(cluster_dir, "ca.key"), ca_priv.private_bytes(
        serialization.Encoding.PEM, serialization.PrivateFormat.TraditionalOpenSSL,
        serialization.NoEncryption()))
    _write(os.path.join(cluster_dir, "ca.crt"), ca_cert.public_bytes(serialization.Encoding.PEM))

    ad_priv = rsa.generate_private_key(65537, 2048)
    ad_name = x509.Name([
        x509.NameAttribute(NameOID.COUNTRY_NAME, u"US"),
        x509.NameAttribute(NameOID.ORGANIZATION_NAME, u"system:masters"),
        x509.NameAttribute(NameOID.COMMON_NAME, u"admin"),
    ])
    san = x509.SubjectAlternativeName([
        x509.IPAddress(ipaddress.IPv4Address("127.0.0.1")),
        x509.DNSName("localhost"),
        x509.DNSName("kwok"),
    ])
    ad_cert = (
        x509.CertificateBuilder()
        .subject_name(ad_name).issuer_name(ca_name)
        .public_key(ad_priv.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(_NOW() - datetime.timedelta(days=1))
        .not_valid_after(_NOW() + datetime.timedelta(days=365))
        .add_extension(san, critical=False)
        .add_extension(x509.ExtendedKeyUsage(
            [ExtendedKeyUsageOID.CLIENT_AUTH, ExtendedKeyUsageOID.SERVER_AUTH]), critical=False)
        .sign(ca_priv, hashes.SHA256())
    )
    _write(os.path.join(cluster_dir, "admin.key"), ad_priv.private_bytes(
        serialization.Encoding.PEM, serialization.PrivateFormat.TraditionalOpenSSL,
        serialization.NoEncryption()))
    _write(os.path.join(cluster_dir, "admin.crt"), ad_cert.public_bytes(serialization.Encoding.PEM))

    ca_b64  = pem_b64(_read(os.path.join(cluster_dir, "ca.crt")).decode("ascii"))
    ad_b64  = pem_b64(_read(os.path.join(cluster_dir, "admin.crt")).decode("ascii"))
    key_b64 = pem_b64(_read(os.path.join(cluster_dir, "admin.key")).decode("ascii"))

    kubeconfig = (
        "apiVersion: v1" + _LF +
        "kind: Config" + _LF +
        "clusters:" + _LF +
        "- cluster:" + _LF +
        "    certificate-authority-data: " + ca_b64 + _LF +
        "    server: https://127.0.0.1:16443" + _LF +
        "  name: kwok" + _LF +
        "contexts:" + _LF +
        "- context:" + _LF +
        "    cluster: kwok" + _LF +
        "    user: admin" + _LF +
        "  name: kwok" + _LF +
        "current-context: kwok" + _LF +
        "preferences: {}" + _LF +
        "users:" + _LF +
        "- name: admin" + _LF +
        "  user:" + _LF +
        "    client-certificate-data: " + ad_b64 + _LF +
        "    client-key-data: " + key_b64 + _LF
    )
    kc_path = os.path.join(cluster_dir, "kubeconfig.yaml")
    _write_str(kc_path, kubeconfig)
    print("  kubeconfig.yaml")

    # Also place in ~/.kube/config so Python kubernetes client finds it
    kb_dir = os.path.join(os.path.expanduser("~"), ".kube")
    os.makedirs(kb_dir, exist_ok=True)
    _write_str(os.path.join(kb_dir, "config"), kubeconfig)
    print("  ~/.kube/config")
    return True


if __name__ == "__main__":
    generate_certs(os.path.join(os.path.expanduser("~"), ".kwok", "clusters", "kwok"))
