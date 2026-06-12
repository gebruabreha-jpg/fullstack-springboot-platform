from kubernetes import client, config
import sys

def main():
    try:
        config.load_kube_config()
    except Exception as e:
        print(f"ERROR: Cannot load kubeconfig: {e}")
        return 1

    v1 = client.CoreV1Api()
    
    try:
        pods = v1.list_namespaced_pod("kwok-test")
    except Exception:
        print("No pods found in kwok-test namespace")
        return 1

    if len(pods.items) == 0:
        print("No pods found")
        return 1

    ready = [p for p in pods.items if p.status.phase == "Running"]
    print(f"✔ Cluster health OK ({len(ready)}/{len(pods.items)} pods Running)")
    return 0

if __name__ == "__main__":
    sys.exit(main())