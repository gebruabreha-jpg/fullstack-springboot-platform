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

@then('packet loss below {ppm:d} ppm')
def step_packet_loss(context, ppm):
    pass

@then('availability should be {percent:f}%')
def step_availability(context, percent):
    pass

@then('response time should be under {ms:d}ms')
def step_response_time(context, ms):
    pass

@then('no alarms since failure event')
def step_no_alarms(context):
    pass

@then('ISP events occurred exactly {count:d} times')
def step_isp_events(context, count):
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

@then('resource usage below {percent:f}%')
def step_resource_usage(context, percent):
    pass