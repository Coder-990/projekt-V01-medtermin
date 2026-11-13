cat > dc-pregled.py << 'EOF'
#!/usr/bin/env python3
# Izvlači ranjive ovisnosti i CVE-ove iz Dependency-Check HTML izvještaja
import re, sys, html
from html.parser import HTMLParser

path = sys.argv[1] if len(sys.argv) > 1 else "alati/prije/dependency-check-report.html"
text = open(path, encoding="utf-8").read()

# 1) Sažetak ovisnosti (samo ranjive)
rows = re.findall(r'<tr class=" vulnerable">(.*?)</tr>', text, re.S)
def clean(s):
    return html.unescape(re.sub(r"<[^>]+>", " ", s)).split()

print("== RANJIVE OVISNOSTI ==")
for r in rows:
    tds = re.findall(r"<td[^>]*>(.*?)</td>", r, re.S)
    name = " ".join(clean(tds[0]))
    cves = " ".join(clean(tds[1]))
    pkg = " ".join(clean(tds[2]))
    sev = " ".join(clean(tds[3]))
    cnt = " ".join(clean(tds[4]))
    print(f"{sev:<10} {cnt:>4} CVE  {name}  [{pkg}]")

# 2) Detalji: svaki CVE s ozbiljnošću i CVSS
print("\n== CVE DETALJI ==")
for m in re.finditer(r'<b><a target="_blank" href="[^"]*" *>(CVE-[\d-]+)</a></b>(.*?)</p>\s*<p>\s*<pre>(.*?)</pre>', text, re.S):
    cve, _, desc = m.group(1), m.group(2), m.group(3)
    desc = " ".join(clean(desc))[:200]
    print(f"{cve}: {desc}")

print("\nIzvor: alati/prije/dependency-check-report.html")
EOF
python3 dc-pregled.py alati/prije/dependency-check-report.html | tee alati/prije/dependency-check-java.txt