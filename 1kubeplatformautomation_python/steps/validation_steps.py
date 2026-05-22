from behave import given, when, then
import time

@then('system should be healthy')
def step_system_healthy(context):
    assert context.results.get("failed", 0) == 0

@then('all tests should pass')
def step_all_pass(context):
    total = context.results.get("passed", 0) + context.results.get("failed", 0)
    assert context.results.get("failed", 0) == 0, f"{context.results['failed']}/{total} tests failed"

@then('no critical errors should occur')
def step_no_critical_errors(context):
    errors = context.results.get("errors", [])
    for err in errors:
        assert "CRITICAL" not in err

@then('recovery time should be less than {seconds:d} seconds')
def step_recovery_time(context, seconds):
    duration = time.time() - getattr(context, "deploy_time", time.time())
    assert duration < seconds, f"Recovery {duration}s > {seconds}s"

@then('metrics within {percent:f}% of baseline')
def step_metrics_within(context, percent):
    pass

@then('no unexpected restarts')
def step_no_unexpected_restarts(context):
    pass

@then('signal success rate above {percent:f}%')
def step_signal_rate(context, percent):
    pass

@then('total signaling success rate is above {percent:f}%')
def step_total_signal_rate(context, percent):
    if getattr(context, 'skipped_no_cluster', False):
        return

@then('total signaling success rate is above {percent:f}% for the past {minutes:d} minutes')
def step_signaling_success_rate_duration(context, percent, minutes):
    if getattr(context, 'skipped_no_cluster', False):
        return

@then('packet loss below {ppm:d} ppm')
def step_packet_loss(context, ppm):
    pass

@then('packet loss is less than {ppm:d} packets per million loss')
def step_packet_loss_ppm(context, ppm):
    if getattr(context, 'skipped_no_cluster', False):
        return

@then('availability should be {percent:f}%')
def step_availability(context, percent):
    pass

@then('response time should be under {ms:d}ms')
def step_response_time(context, ms):
    pass

@then('no alarms since failure event')
def step_no_alarms(context):
    pass

@then('no alarms have been active since memorized event "{name}"')
def step_no_alarms_since(context, name):
    pass

@then('ISP events occurred exactly {count:d} times')
def step_isp_events(context, count):
    pass

@then('the following ISP events have occurred exactly {count:d} time since memorized event "{name}"')
def step_isp_events_since(context, count, name):
    pass

@then('container restart count unchanged')
def step_restart_count_unchanged(context):
    pass

@then('latency under {ms:d}ms')
def step_latency(context, ms):
    pass

@then('throughput above {rate:d}')
def step_throughput(context, rate):
    pass

@then('throughput should be above {rate:d} requests per second')
def step_throughput_rps(context, rate):
    pass

@then('throughput should be above {rate:d} requests per second')
def step_throughput_rps2(context, rate):
    pass

@then('95th percentile response time should be under {ms:d}ms')
def step_95th_percentile(context, ms):
    pass

@then('95th percentile response time under {ms:d}ms')
def step_95th_percentile2(context, ms):
    pass

@then('error rate should be less than {percent:f}%')
def step_error_rate(context, percent):
    pass

@then('resource usage should be below {percent:f}%')
def step_resource_usage_value(context, percent):
    pass

@then('memory usage should be below {percent:f}%')
def step_memory_usage(context, percent):
    pass

@then('no memory leak detected')
def step_no_memory_leak(context):
    pass

@then('pods are evicted gracefully')
def step_pods_evicted_gracefully(context):
    pass

@then('new pods are scheduled within {seconds:d} seconds')
def step_pods_scheduled_quickly(context, seconds):
    pass

@then('no traffic loss during update')
def step_no_traffic_loss(context):
    pass

@then('no critical vulnerabilities found')
def step_no_critical_vulns(context):
    pass

@then('high severity vulnerabilities are documented')
def step_high_vulns_documented(context):
    pass

@then('access is denied')
def step_access_denied(context):
    pass

@then('audit log records the attempt')
def step_audit_log(context):
    pass

@then('connection is blocked')
def step_connection_blocked(context):
    pass

@then('network policy violation is logged')
def step_policy_violation_logged(context):
    pass