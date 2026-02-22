import io
import re
import sys

log_path = r'C:\Users\kasey\AppData\Roaming\Hytale\UserData\Logs\2026-02-22_08-51-27_client.log'

try:
    with io.open(log_path, 'r', encoding='utf-8', errors='replace') as f:
        lines = f.readlines()
        
    keywords = ['error', 'exception', 'warn', 'stitch', 'workbench', 'water', 'texture', 'asset']
    
    matches = []
    for i, line in enumerate(lines):
        lower_line = line.lower()
        for kw in keywords:
            if kw in lower_line:
                matches.append(f"{i+1}: {line.strip()}")
                break
                
    print(f"Found {len(matches)} matches.")
    for m in matches[-100:]:  # Print last 100 max
        print(m)
except Exception as e:
    print(f"Failed to read: {e}")
