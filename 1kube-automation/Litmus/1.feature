  Feature File Examples — Complete Litmus Workflow

  # === Full Litmus Chaos Test Scenario ===

  # Install Litmus infrastructure
  When litmus is installed
  When the litmus operator is installed

  # --- POD CPU HOG ---
  When the litmus engine POD_CPU_HOG is configured:
    | variable name   | value           |
    | experiment_name | pod-cpu-chaos   |
    | applabel        | app=eric-pc-sm  |
    | chaos_duration  | 300             |
    | cpu_core        | 1               |
    | cpu_load        | 100             |
    | affected_pods   | 30              |
    | sequence        | parallel        |
  When the litmus experiment POD_CPU_HOG is run
  When pod logs and describe outputs for the experiment POD_CPU_HOG is collected
  Then the litmus experiment POD_CPU_HOG result is validated

  # --- POD NETWORK LOSS ---
  When the litmus engine POD_NETWORK_LOSS is configured:
    | variable name                  | value                  |
    | experiment_name                | pod-network-loss-chaos |
    | applabel                       | app=eric-pc-sm         |
    | chaos_duration                 | 60                     |
    | network_packet_loss_percentage | 100                    |
    | affected_pods                  | 1                      |
    | sequence                       | serial                 |
  When the litmus experiment POD_NETWORK_LOSS is run
  When pod logs and describe outputs for the experiment POD_NETWORK_LOSS is collected
  Then the litmus experiment POD_NETWORK_LOSS result is validated

  # --- CONTAINER KILL ---
  When the litmus engine CONTAINER_KILL is configured:
    | variable name    | value                  |
    | experiment_name  | container-kill-chaos   |
    | applabel         | app=eric-pc-sm         |
    | chaos_duration   | 300                    |
    | chaos_interval   | 30                     |
    | affected_pods    | 30                     |
    | sequence         | parallel               |
    | target_container | smf                    |
  When the litmus experiment CONTAINER_KILL is run

  # --- POD MEMORY HOG ---
  When the litmus engine POD_MEMORY_HOG is configured:
    | variable name      | value             |
    | experiment_name    | pod-memory-chaos  |
    | applabel           | app=eric-pc-sm    |
    | chaos_duration     | 120               |
    | memory_consumption | 500               |
    | affected_pods      | 1                 |
    | sequence           | serial            |
  When the litmus experiment POD_MEMORY_HOG is run

  # Cleanup
  When the litmus experiment POD_CPU_HOG files are deleted

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  Summary: Litmus Execution Architecture

  Feature file (Gherkin)
    ↓
  LitmusExperimentSteps
    ├── installLitmus() → LitmusHelper.installLitmus()
    │     └── helm install litmus-chaos (from Artifactory)
    ├── installLitmusOperator() → kubectl apply litmus_operator.yaml
    ├── configureLitmusExperimentEngine() → template → engine YAML
    └── runLitmusExperiments() → LitmusExperimentsActionsImpl
          ├── applyRbac()       → kubectl apply pod_cpu_hog_rbac.yaml
          ├── createExperiment() → kubectl apply pod_cpu_hog_experiment.yaml
          └── startEngine()      → kubectl apply pod_cpu_hog_engine.yaml
                                      ↓
                                Litmus Operator picks up ChaosEngine CRD
                                      ↓
                                Creates experiment runner pod
                                      ↓
                                Injects chaos (CPU hog, network loss, etc.)
                                      ↓
                                Writes ChaosResult CRD with verdict

  ┌────────────────────────┬───────────────────────────────────┬─────────────────────────────────┐
  │ Experiment             │ Parameters                        │ Effect                          │
  ├────────────────────────┼───────────────────────────────────┼─────────────────────────────────┤
  │ POD_CPU_HOG            │ cpu_core, cpu_load, duration      │ Burns CPU in target pods        │
  ├────────────────────────┼───────────────────────────────────┼─────────────────────────────────┤
  │ POD_MEMORY_HOG         │ memory_consumption, duration      │ Allocates memory in target pods │
  ├────────────────────────┼───────────────────────────────────┼─────────────────────────────────┤
  │ POD_NETWORK_LOSS       │ network_packet_loss_percentage    │ Drops packets via tc netem      │
  ├────────────────────────┼───────────────────────────────────┼─────────────────────────────────┤
  │ POD_NETWORK_PARTITION  │ destination_ips, policy_types     │ Network policy isolation        │
  ├────────────────────────┼───────────────────────────────────┼─────────────────────────────────┤
  │ POD_NETWORK_LATENCY    │ network_latency                   │ Adds delay via tc netem         │
  ├────────────────────────┼───────────────────────────────────┼─────────────────────────────────┤
  │ POD_NETWORK_CORRUPTION │ network_packet_corruption         │ Corrupts packets                │
  ├────────────────────────┼───────────────────────────────────┼─────────────────────────────────┤
  │ CONTAINER_KILL         │ target_container, chaos_interval  │ Kills containers periodically   │
  ├────────────────────────┼───────────────────────────────────┼─────────────────────────────────┤
  │ POD_IO_STRESS          │ filesystem_utilization_percentage │ Disk I/O stress                 │
  ├────────────────────────┼───────────────────────────────────┼─────────────────────────────────┤
  │ KUBELET_SERVICE_KILL   │ target_node, node_label           │ Kills kubelet on node           │
  ├────────────────────────┼───────────────────────────────────┼─────────────────────────────────┤
  │ DOCKER_SERVICE_KILL    │ target_node, node_label           │ Kills docker daemon             │
  └────────────────────────┴───────────────────────────────────┴─────────────────────────────────┘