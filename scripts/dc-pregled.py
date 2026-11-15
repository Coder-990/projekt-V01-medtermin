#!/usr/bin/env python3
"""Sažima Dependency-Check JSON, uključujući zasebno isključene nalaze."""
import json
from pathlib import Path
import sys

path = Path(sys.argv[1] if len(sys.argv) > 1 else 'alati/poslije/dependency-check-report.json')
if path.suffix.lower() == '.html':
    path = path.with_suffix('.json')
report = json.loads(path.read_text(encoding='utf-8'))

print('== RANJIVE OVISNOSTI ==')
for dep in report['dependencies']:
    for vuln in dep.get('vulnerabilities', []):
        print(f"{vuln.get('severity', 'UNKNOWN'):<10} {dep['fileName']}  {vuln['name']}")
        print(f"  {vuln.get('description', '')}")

print('\n== ISKLJUČENI NALAZI ==')
for dep in report['dependencies']:
    for vuln in dep.get('suppressedVulnerabilities', []):
        print(f"{dep['fileName']}  {vuln['name']}")
        print(f"  {vuln.get('notes', '').strip()}")

print(f'\nIzvor: {path}')
