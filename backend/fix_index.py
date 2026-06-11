#!/usr/bin/env python3
import re

path = '/home/muizz9900/web/index.html'
with open(path, 'r') as f:
    content = f.read()

print("Before (tail):", repr(content[-300:]))

# Remove any bad script tags we may have inserted
content = re.sub(r'\s*<script src=["\s]*app\.js[">]*</script>', '', content)

# Insert correct script tag before </body>
content = content.replace('</body>', '    <script src="app.js"></script>\n</body>')

with open(path, 'w') as f:
    f.write(content)

print("After (tail):", repr(content[-300:]))
print("Done! Script tags now:")
for i, line in enumerate(content.split('\n'), 1):
    if 'script' in line.lower():
        print(f"  Line {i}: {line.strip()}")
