#!/usr/bin/env python3
"""
Verification script for FourFingerSwipeDetector algorithmic logic and parameters.
Validates multi-pointer gesture tracking, displacement calculation, perpendicular drift,
timeout rejection, and trigger conditions.
"""

import sys
import math

class SwipeDirection:
    UP = "UP"
    DOWN = "DOWN"
    LEFT = "LEFT"
    RIGHT = "RIGHT"
    INSUFFICIENT_MOVEMENT = "INSUFFICIENT_MOVEMENT"

class FourFingerSwipeDetector:
    def __init__(self, required_pointers=4, min_distance_px=150.0, max_duration_ms=800, max_perpendicular_drift_ratio=0.6):
        self.required_pointers = required_pointers
        self.min_distance_px = min_distance_px
        self.max_duration_ms = max_duration_ms
        self.max_perpendicular_drift_ratio = max_perpendicular_drift_ratio
        self.start_points = {}
        self.current_points = {}
        self.gesture_start_time = 0

    def on_pointer_down(self, pointer_id, x, y, time_ms):
        self.start_points[pointer_id] = (x, y, time_ms)
        self.current_points[pointer_id] = (x, y, time_ms)
        if len(self.start_points) == self.required_pointers:
            self.gesture_start_time = time_ms

    def on_pointer_move(self, pointer_id, x, y, time_ms):
        if pointer_id in self.start_points:
            self.current_points[pointer_id] = (x, y, time_ms)

    def on_pointer_up(self, pointer_id, time_ms):
        result = self.evaluate(time_ms)
        self.start_points.pop(pointer_id, None)
        self.current_points.pop(pointer_id, None)
        if not self.start_points:
            self.gesture_start_time = 0
        return result

    def evaluate(self, time_ms):
        count = len(self.start_points)
        if count != self.required_pointers:
            return {"detected": False, "direction": SwipeDirection.INSUFFICIENT_MOVEMENT, "count": count}

        duration = time_ms - self.gesture_start_time if self.gesture_start_time > 0 else 0
        if duration > self.max_duration_ms or duration < 30:
            return {"detected": False, "direction": SwipeDirection.INSUFFICIENT_MOVEMENT, "count": count, "reason": "duration_out_of_range"}

        total_dx = sum(self.current_points[i][0] - self.start_points[i][0] for i in self.start_points)
        total_dy = sum(self.current_points[i][1] - self.start_points[i][1] for i in self.start_points)
        avg_dx = total_dx / count
        avg_dy = total_dy / count

        abs_dx = abs(avg_dx)
        abs_dy = abs(avg_dy)

        if abs_dy > abs_dx:
            if abs_dx <= (abs_dy * self.max_perpendicular_drift_ratio) and abs_dy >= self.min_distance_px:
                direction = SwipeDirection.UP if avg_dy < 0 else SwipeDirection.DOWN
                return {"detected": True, "direction": direction, "count": count, "avg_dy": avg_dy}
        else:
            if abs_dy <= (abs_dx * self.max_perpendicular_drift_ratio) and abs_dx >= self.min_distance_px:
                direction = SwipeDirection.LEFT if avg_dx < 0 else SwipeDirection.RIGHT
                return {"detected": True, "direction": direction, "count": count, "avg_dx": avg_dx}

        return {"detected": False, "direction": SwipeDirection.INSUFFICIENT_MOVEMENT, "count": count}


def run_tests():
    detector = FourFingerSwipeDetector()

    # Test 1: Valid 4-finger swipe up
    t0 = 1000
    t1 = 1200
    for i in range(4):
        detector.on_pointer_down(i, 100 * (i + 1), 600, t0)
    for i in range(4):
        detector.on_pointer_move(i, 100 * (i + 1) + 5, 350, t1)
    res1 = detector.on_pointer_up(0, t1)
    assert res1["detected"] is True, f"Expected detected=True, got {res1}"
    assert res1["direction"] == SwipeDirection.UP, f"Expected UP, got {res1['direction']}"
    print("PASS: Test 1 - Valid 4-finger swipe up detected")

    # Test 2: 3-finger swipe rejected
    detector = FourFingerSwipeDetector()
    for i in range(3):
        detector.on_pointer_down(i, 100 * (i + 1), 600, t0)
    for i in range(3):
        detector.on_pointer_move(i, 100 * (i + 1), 350, t1)
    res2 = detector.on_pointer_up(0, t1)
    assert res2["detected"] is False, f"Expected detected=False for 3 fingers, got {res2}"
    print("PASS: Test 2 - 3 fingers rejected")

    # Test 3: Insufficient displacement rejected (< 150px)
    detector = FourFingerSwipeDetector()
    for i in range(4):
        detector.on_pointer_down(i, 100 * (i + 1), 600, t0)
    for i in range(4):
        detector.on_pointer_move(i, 100 * (i + 1), 550, t1) # only 50px delta
    res3 = detector.on_pointer_up(0, t1)
    assert res3["detected"] is False, f"Expected detected=False for small delta, got {res3}"
    print("PASS: Test 3 - Small movement (<150px) rejected")

    # Test 4: Excessive perpendicular drift rejected
    detector = FourFingerSwipeDetector()
    for i in range(4):
        detector.on_pointer_down(i, 100 * (i + 1), 600, t0)
    for i in range(4):
        # 200px upward, but 180px horizontal (> 60% drift)
        detector.on_pointer_move(i, 100 * (i + 1) + 180, 400, t1)
    res4 = detector.on_pointer_up(0, t1)
    assert res4["detected"] is False, f"Expected detected=False for excessive drift, got {res4}"
    print("PASS: Test 4 - Excessive perpendicular drift rejected")

    # Test 5: Drag duration > 800ms rejected
    detector = FourFingerSwipeDetector()
    for i in range(4):
        detector.on_pointer_down(i, 100 * (i + 1), 600, t0)
    for i in range(4):
        detector.on_pointer_move(i, 100 * (i + 1), 350, 2000) # 1000ms
    res5 = detector.on_pointer_up(0, 2000)
    assert res5["detected"] is False, f"Expected detected=False for slow drag, got {res5}"
    print("PASS: Test 5 - Slow drag (>800ms) rejected")

    print("\nALL 5 GESTURE DETECTOR VERIFICATION TESTS PASSED SUCCESSFULLY.")

if __name__ == "__main__":
    run_tests()
