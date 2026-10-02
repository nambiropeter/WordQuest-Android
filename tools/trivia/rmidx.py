#!/usr/bin/env python3
"""Usage: rmidx.py THEME idx idx ... — removes questions by index (as printed by audit.py)."""
import json, sys, subprocess, os
A='/Users/nambzzy/Developer/wordsearch_android/app/src/main/assets/content/trivia_%s.json'
t=sys.argv[1]; idx=set(map(int,sys.argv[2:]))
d=json.load(open(A%t)); qs=[d[i]['q'] for i in sorted(idx)]
for i in sorted(idx): print('rm', i, d[i]['difficulty'], d[i]['q'])
subprocess.run(['python3', os.path.join(os.path.dirname(os.path.abspath(__file__)),'rm.py'), t]+qs)
