#!/usr/bin/env python3
"""
Test driver for Relay protocol tests. Runs all numbered test files and reports results.
"""
import os
import subprocess
import sys

def main():
    test_dir = os.path.dirname(os.path.abspath(__file__))
    test_files = sorted(f for f in os.listdir(test_dir) if f.endswith('_test.py') and f[0:2].isdigit())
    total = len(test_files)
    failed = 0
    print(f"🧪 Running {total} tests...\n")
    for fname in test_files:
        print(f"=== 🚀 Running {fname} ===")
        result = subprocess.run([sys.executable, os.path.join(test_dir, fname)])
        if result.returncode == 0:
            print(f"✅ [PASS] {fname}\n")
        else:
            print(f"❌ [FAIL] {fname}\n")
            failed += 1
    print(f"📊 Summary: {total - failed} passed, {failed} failed, {total} total.")
    sys.exit(failed)

if __name__ == "__main__":
    main()
