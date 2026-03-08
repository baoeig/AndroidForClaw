---
name: debugging
description: 调试技能和故障排查 - Debugging skills and troubleshooting techniques
metadata:
  {
    "openclaw": {
      "always": false,
      "emoji": "🐛",
      "version": "2.0.0",
      "category": "debugging"
    }
  }
---

# Debugging Skill

调试和故障排查的方法和技巧 - Methods and techniques for debugging and troubleshooting.

## 🎯 When to Use

Use this skill when you need to:

✅ **Diagnose failures** - Understand why operations aren't working
✅ **Locate bugs** - Find root cause of issues
✅ **Reproduce problems** - Stably reproduce issues for analysis
✅ **Verify fixes** - Confirm problems are resolved
✅ **Optimize tests** - Improve test reliability and effectiveness

## 📚 Available Tools

### Observation Tools

**screenshot()**
- Primary debugging tool
- Captures exact UI state at any moment
- Use before and after each operation
- Essential for understanding what went wrong

**get_view_tree()**
- Inspect UI element hierarchy
- Check element properties (bounds, text, clickable)
- Verify element existence and location

**log(message, level)**
- Document debugging process
- Record observations and decisions
- Track execution flow
- Levels: info, warn, error

### Action Tools (for reproduction)

**tap(x, y)** - Tap to reproduce user actions
**swipe(...)** - Swipe gestures
**type(text)** - Text input
**long_press(...)** - Long press actions

### Navigation Tools (for recovery)

**home()** - Reset to launcher (clean state)
**back()** - Go back (undo last action)
**open_app(package)** - Restart app

### Utility Tools

**wait(seconds)** - Add delays for timing issues
**notification(message)** - Mark debugging milestones
**stop(reason)** - Complete debugging session

## 🔄 Workflow Pattern

### Standard Debugging Flow

```
1. Identify Problem
   - What went wrong?
   - What was expected?
   - What actually happened?

2. Reproduce Problem
   - Find reliable steps to trigger
   - Verify it happens consistently
   - Document reproduction rate

3. Isolate Cause
   - Use binary search (eliminate half each time)
   - Compare working vs broken states
   - Remove variables one by one

4. Verify Fix
   - Reproduce problem again
   - Apply fix/workaround
   - Confirm problem gone
   - Test related functionality
```

### Binary Search Debugging

```
# Problem: Operation A → B → C → D → E fails

# 1. Test middle point
A → B → C (works) ✓
→ Problem is in D or E

# 2. Test next middle
A → B → C → D (works) ✓
→ Problem is in E ✓

# 3. Isolate E
Examine E step in detail
screenshot() before E
Perform E
screenshot() after E
Analyze difference
```

### Comparison Debugging

```
# Compare Working vs Broken states

# Working scenario:
Operation X succeeds
screenshot() → state A

# Broken scenario:
Operation X fails
screenshot() → state B

# Analysis:
What's different between A and B?
- Element positions?
- Text content?
- Loading state?
- Network status?
```

## 💡 Examples

### Example 1: Element Not Found

```
# Problem: tap() fails to find button

log("Debug: Button tap failure", "info")

# 1. Confirm current state
screenshot()
log("Initial screen captured", "info")

# 2. Inspect UI tree
get_view_tree()
# Check if button exists
# Note: bounds, text, clickable property

# 3. Possible causes:
#    A. Wrong coordinates
#    B. Element off-screen
#    C. Element not loaded yet
#    D. Element covered by dialog

# 4. Test hypothesis A: Wrong coordinates
screenshot()
# Manually verify button position
# Update coordinates if needed

# 5. Test hypothesis B: Off-screen
swipe(540, 1500, 540, 500, 300)  # Scroll down
wait(1)
screenshot()
get_view_tree()
# Check if button now visible

# 6. Test hypothesis C: Not loaded
wait(3)  # Wait for loading
screenshot()
get_view_tree()

# 7. Test hypothesis D: Dialog covering
back()  # Dismiss dialog
wait(0.5)
screenshot()

stop("Debug complete: Issue was [specific cause]")
```

### Example 2: Operation No Response

```
# Problem: tap() executes but nothing happens

log("Debug: Tap no response", "info")

# 1. Verify tap executed
screenshot()  # Before
tap(540, 800)
log("Tap executed at (540, 800)", "info")
wait(0.5)
screenshot()  # After

# 2. Check for changes
# Compare before/after screenshots
# Look for: visual feedback, state change

# 3. Possible causes:
#    A. Too fast (need wait)
#    B. Loading indicator (need longer wait)
#    C. Wrong element tapped
#    D. Element disabled

# 4. Test cause A: Add wait
screenshot()
tap(540, 800)
wait(2)  # Longer wait
screenshot()

# 5. Test cause B: Check for loading
# Look for loading spinner, progress bar
wait(5)  # Wait for loading to complete
screenshot()

# 6. Test cause C: Verify coordinates
get_view_tree()
# Check element at (540, 800)
# Verify it's the intended target

# 7. Test cause D: Check if disabled
get_view_tree()
# Check "enabled" or "clickable" property

stop("Debug complete: Cause identified")
```

### Example 3: Intermittent Failure

```
# Problem: Operation sometimes works, sometimes fails

log("Debug: Intermittent failure", "info")

# 1. Reproduce multiple times
success_count = 0
failure_count = 0
max_attempts = 5

for attempt in range(max_attempts):
    log(f"Attempt {attempt + 1}/{max_attempts}", "info")

    screenshot()  # Initial state
    tap(540, 800)
    wait(2)
    screenshot()  # Result state

    # Check if succeeded
    # (look for expected result in screenshot)
    if success_indicators_present:
        success_count += 1
        log(f"Attempt {attempt + 1}: SUCCESS", "info")
    else:
        failure_count += 1
        log(f"Attempt {attempt + 1}: FAILURE", "warn")

    # Reset for next attempt
    home()
    wait(1)
    open_app("package.name")
    wait(2)

# 2. Analyze pattern
log(f"Results: {success_count} success, {failure_count} failures", "info")
# Failure rate: failure_count / max_attempts

# 3. Possible causes of intermittent issues:
#    - Network delays (variable response time)
#    - Race conditions (timing-dependent)
#    - Resource contention (CPU/memory load)
#    - Animation timing (variable duration)

# 4. Solution: Add buffer time
tap(540, 800)
wait(3)  # Longer, more reliable wait
# Or: wait for specific element to appear

stop(f"Debug complete: {failure_count}/{max_attempts} failures")
```

### Example 4: App Crash Investigation

```
# Problem: App crashes during operation

log("Debug: Crash investigation", "info")

# 1. Binary search to isolate crash step
# Assume steps: A → B → C → D → E (crashes)

# Test A → B → C
home()
open_app("package.name")
wait(2)
# Perform A
screenshot()
# Perform B
screenshot()
# Perform C
screenshot()
# (no crash) ✓

# Test A → B → C → D
home()
open_app("package.name")
wait(2)
# A, B, C...
# Perform D
screenshot()
# (no crash) ✓

# Test A → B → C → D → E
home()
open_app("package.name")
wait(2)
# A, B, C, D...
# Perform E
# (crashes) → E is the trigger

# 2. Investigate E in detail
log("Crash trigger identified: Operation E", "error")

# 3. Try variations of E
# - Different input
# - Different timing
# - Different state

# 4. Document crash conditions
stop("""Crash Analysis:
Trigger: Operation E (specific details)
Preconditions: After A → B → C → D
Input: [specific input that causes crash]
Reproduction rate: 100%
Workaround: [alternative approach]
""")
```

## 📋 Best Practices

### 1. Step-by-Step Execution

❌ **Wrong**: Execute multiple steps then check
```
tap(x1, y1)
tap(x2, y2)
tap(x3, y3)
screenshot()  # Can't tell which step failed
```

✅ **Correct**: Check after each step
```
tap(x1, y1)
screenshot()  # Verify step 1
tap(x2, y2)
screenshot()  # Verify step 2
tap(x3, y3)
screenshot()  # Verify step 3
```

### 2. Use notification() for Tracking

Mark important debugging milestones:
```
notification("Debug Phase 1: Reproduction")
notification("Debug Phase 2: Isolation")
notification("Debug Phase 3: Verification")
notification("Bug Found: Element not visible")
```

### 3. Save Key Screenshots

Screenshot critical moments:
```
screenshot()  # Initial state
# ... operation ...
screenshot()  # Failed state
# Save both for comparison
```

### 4. Detailed Logging

Document reasoning and observations:
```
log("Hypothesis: Button covered by dialog", "info")
log("Test: Dismissing dialog", "info")
back()
log("Result: Button now visible", "info")
```

### 5. Binary Search for Efficiency

For complex flows, use binary search:
```
# Instead of testing A, B, C, D, E one by one
# Test middle: A → B → C
# If works, problem in D or E
# If fails, problem in A, B, or C
# Repeat until isolated
```

## 🔍 Troubleshooting

### Issue: Can't Reproduce Problem

**Symptom**: Bug reported but can't trigger it

**Debug approach**:
```
# 1. Verify starting state
screenshot()
# Ensure same state as original bug

# 2. Follow exact steps
# Use precise coordinates, timing, input

# 3. Try multiple times
# May be intermittent (5-10 attempts)

# 4. Vary conditions
# Different network, timing, input length

# 5. Check environment
# Device model, OS version, app version
```

### Issue: Too Many Variables

**Symptom**: Can't isolate cause, too many possibilities

**Solution**: Systematic elimination
```
# List all variables:
# - Input content
# - Network state
# - Previous operations
# - Timing

# Test with minimal variables:
# Fresh app start
# Simplest input
# Only essential steps

# Add variables one by one:
# Identify which variable triggers issue
```

### Issue: Problem Appears/Disappears Randomly

**Symptom**: Inconsistent behavior

**Common causes**:
- **Network delays**: Solution → Add longer waits, check loading states
- **Animations**: Solution → wait() for animations to complete
- **Race conditions**: Solution → Ensure sequential execution
- **Background processes**: Solution → Close other apps

**Debugging technique**:
```
# Test with extreme timing
wait(5)  # Very long wait
# If always works → timing issue

# Test many iterations
for i in range(10):
    # Attempt operation
    # Track success/failure rate
```

### Issue: Different Behavior on Different Devices

**Symptom**: Works on device A, fails on device B

**Check**:
- Screen resolution (coordinates may differ)
- Android version (API differences)
- Device performance (slower device = longer loading)
- Manufacturer customizations

**Solution**:
```
# Use relative coordinates (screen percentage)
# Or use get_view_tree() to find dynamic coordinates
# Add device-specific wait times
```

### Issue: State Inconsistency

**Symptom**: UI shows one thing, reality is different

**Debug**:
```
# 1. Refresh state
back()
wait(1)
# Re-enter screen
tap(540, 800)
wait(1)
screenshot()

# 2. Or force refresh
# Pull-to-refresh if available
swipe(540, 500, 540, 1500, 300)
wait(2)
screenshot()

# 3. Or restart app
home()
open_app("package.name")
wait(2)
screenshot()
```

## 🎓 Debugging Techniques

### Binary Search Method

```
Fastest way to isolate failures in sequences:
Steps: A → B → C → D → E → F (fails)

Round 1: Test A → B → C
- Works → problem in D, E, or F
- Fails → problem in A, B, or C

Round 2: Test A → B → C → D → E
- Works → problem in F ✓
- Fails → problem in D or E

Isolated with log₂(n) tests
```

### Comparison Method

```
Compare working vs broken:
Scenario 1 (works): Input "abc" → Success
Scenario 2 (fails): Input "xyz" → Failure

Difference analysis:
- Input content?
- State before input?
- Timing?
screenshot() both scenarios, compare
```

### Reproduction Rate Testing

```
# Quantify reliability
attempts = 10
failures = 0

for i in range(attempts):
    result = perform_operation()
    if not result.success:
        failures += 1

rate = failures / attempts
# 0% = always works
# 100% = always fails
# 30% = intermittent (timing/race condition likely)
```

## 📊 Debug Report Format

```
Debug Report: [Issue Title]

Problem Statement:
[Clear description of the issue]

Reproduction Steps:
1. [Step 1]
2. [Step 2]
...

Reproduction Rate:
[X%] (tested Y times)

Root Cause:
[Identified cause]

Evidence:
- Screenshot 1: [Path] - shows [what]
- Screenshot 2: [Path] - shows [what]
- Log excerpt: [relevant log lines]

Workaround:
[Temporary solution]

Fix Verification:
[Steps to verify fix works]

Related Issues:
[Similar problems, if any]
```

---

**Remember**: 重现 → 隔离 → 验证 - Reproduce → Isolate → Verify 🔍
