from kubernetes import client, config
import subprocess
import os
import sys

REQUIREMENTS = os.path.join(os.path.dirname(os.path.dirname(__file__)), "requirements.txt")


def _pip_install_requirements():
    """Install Python dependencies from requirements.txt if not already satisfied."""
    for pkg in ("kubernetes", "behave"):
        try:
            __import__(pkg)
            return
        except ImportError:
            pass
    print("[+] Installing Python requirements...")
    subprocess.run(
        [sys.executable, "-m", "pip", "install", "-q", "-r", REQUIREMENTS],
        check=False,
    )


def _find_gobin():
    """Return the Go bin directory or None."""
    gopath = os.environ.get("GOPATH", "")
    for candidate in [
        os.path.join(gopath, "bin") if gopath else "",
        r"C:\Go\bin",
        os.path.expanduser(r"~\go\bin"),
    ]:
        if candidate and os.path.isdir(candidate):
            contents = os.listdir(candidate)
            if any(x.lower().startswith("kwok") for x in contents):
                return candidate
    try:
        r = subprocess.run(["go", "env", "GOPATH"], capture_output=True, text=True, timeout=5)
        gb = os.path.join(r.stdout.strip(), "bin")
        if os.path.isdir(gb):
            return gb
    except Exception:
        pass
    for d in os.environ.get("PATH", "").split(os.pathsep):
        if os.path.isdir(d) and any(
            os.path.isfile(os.path.join(d, x))
            for x in ("kwokctl.exe", "kwokctl", "kind.exe", "kind")
        ):
            return d
    return None


def _detect_shell(_infra_dir):
    if os.path.exists("/bin/bash") or os.environ.get("WSL_DISTRO_NAME"):
        try:
            subprocess.run(["bash", "--version"], capture_output=True, check=True, timeout=3)
            return "bash"
        except Exception:
            pass
    if os.path.isfile(r"C:\Program Files\Git\bin\bash.exe"):
        try:
            subprocess.run([r"C:\Program Files\Git\bin\bash.exe", "--version"],
                           capture_output=True, check=True, timeout=3)
            return "bash"
        except Exception:
            pass
    if os.path.isfile(r"C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe"):
        return "powershell"
    return "cmd"


def _run_infra_script(action, infra_dir):
    """Run setup_kwok.* or teardown_kwok.* using the best available shell."""
    shell = _detect_shell(infra_dir)
    script = os.path.join(infra_dir, f"{action}_kwok")
    if shell == "powershell":
        for ext in (".ps1", ".cmd"):
            path = script + ext
            if os.path.isfile(path):
                subprocess.run(["powershell", "-ExecutionPolicy", "Bypass", "-File", path], check=False)
                return
    elif shell == "bash":
        for ext in (".sh", ".cmd"):
            path = script + ext
            if os.path.isfile(path):
                subprocess.run(["bash", path], check=False)
                return
    else:
        for ext in (".cmd", ".sh"):
            path = script + ext
            if os.path.isfile(path):
                subprocess.run(["cmd", "/c", path], check=False)
                return


def _run_behave(feature_file):
    infra_dir = os.path.join(os.path.dirname(__file__), "..", "infra", "script")
    env = os.environ.copy()
    gobin = _find_gobin()
    if gobin and gobin not in env.get("PATH", ""):
        env["PATH"] = gobin + os.pathsep + env.get("PATH", "")
    subprocess.run([sys.executable, "-m", "behave", feature_file], check=False, env=env)


@given("the full environment is set up")
def setup_all(context):
    _pip_install_requirements()
    infra_dir = os.path.join(os.path.dirname(__file__), "..", "infra", "script")
    _run_infra_script("setup", infra_dir)


@when("all tests are executed")
def run_tests(context):
    _run_behave("features/health_check.feature")


@then("the system should be cleaned up")
def cleanup(context):
    infra_dir = os.path.join(os.path.dirname(__file__), "..", "infra", "script")
    _run_infra_script("teardown", infra_dir)
