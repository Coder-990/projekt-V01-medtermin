#!/usr/bin/env python3
import sys
import xml.etree.ElementTree as ET

path = sys.argv[1] if len(sys.argv) > 1 else "target/spotbugsXml.xml"
max_prio = int(sys.argv[2]) if len(sys.argv) > 2 else 3

root = ET.parse(path).getroot()
rows = []
for b in root.iter("BugInstance"):
    prio = int(b.get("priority", "3"))
    if prio > max_prio:
        continue
    src = next((sl for sl in b.iter("SourceLine")
                if sl.get("sourcepath") and sl.get("primary") == "true"), None)
    if src is None:
        src = next((sl for sl in b.iter("SourceLine") if sl.get("sourcepath")), None)
    loc = f"{src.get('sourcepath')}:{src.get('start')}" if src is not None else "?"
    rows.append((prio, b.get("type", ""), loc, b.findtext("ShortMessage", "")))

rows.sort()
for prio, typ, loc, msg in rows:
    print(f"P{prio}  {typ:<32} {loc:<75} {msg}")
print(f"\nUkupno: {len(rows)}")
