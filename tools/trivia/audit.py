#!/usr/bin/env python3
"""Lists same-answer question pairs that look like rewordings (possible repeats). Usage: audit.py [theme ...]"""
import json, re, sys, collections, subprocess
A = '/Users/nambzzy/Developer/wordsearch_android/app/src/main/assets/content/trivia_%s.json'
STOP=set('the a an of in is was which what who to and for by on at as with from that this does did its it are were has have how many first known called name'.split())
def toks(s): return {w for w in re.findall(r'[a-z0-9]+',s.lower()) if w not in STOP}
themes = sys.argv[1:] or 'animals food geography history movies music science sports'.split()
for t in themes:
    d=json.load(open(A%t)); g=collections.defaultdict(list)
    for i,x in enumerate(d): g[re.sub(r'[^a-z0-9]','',x['options'][x['answer']].lower())].append(i)
    for idx in g.values():
        for a in range(len(idx)):
            for b in range(a+1,len(idx)):
                i,j=idx[a],idx[b]; ti,tj=toks(d[i]['q']),toks(d[j]['q'])
                if ti and tj and len(ti&tj)/min(len(ti),len(tj))>=0.5:
                    print(f"{t}\t{i}\t{j}\t{d[i]['q']}\t||\t{d[j]['q']}")
