#!/usr/bin/env python3
"""Check javap -c -p output of the exported, transformed Minecraft 26.3 MapItem.
An ordinal discriminator previously redirected the scale instead of the X cursor.
This checks actual loaded transformation, not just mixin annotation text.
"""
import pathlib,re,sys
lines=pathlib.Path(sys.argv[1]).read_text().splitlines()
calls=[i for i,s in enumerate(lines) if re.search(r'invoke.*startColumn:\(I\)I',s)]
assert len(calls)==1, f'Expected one cursor hook, found {len(calls)}'
i=calls[0]
assert re.search(r'\biload\s+15$',lines[i-1]), lines[i-1]
assert re.search(r'\bistore\s+15$',lines[i+1]), lines[i+1]
print('PASS actual transformed cursor reads and writes local15; scale local5 untouched by hook')
