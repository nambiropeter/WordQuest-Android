#!/usr/bin/env python3
"""Usage: add.py THEME BATCHFILE [--dry]
Batch lines: difficulty|question|correct|wrong1|wrong2|wrong3
Appends unique questions to Android file and mirrors to iOS."""
import json, re, sys, collections, shutil
A='/Users/nambzzy/Developer/wordsearch_android/app/src/main/assets/content/trivia_%s.json'
I='/Users/nambzzy/Developer/wordsearch_ios/WordQuest/Resources/Content/trivia_%s.json'
STOP=set('the a an of in is was which what who to and for by on at as with from that this does did its it are were has have how many first known called name'.split())
def norm(s): return re.sub(r'[^a-z0-9]','',s.lower())
def toks(s): return {w for w in re.findall(r'[a-z0-9]+',s.lower()) if w not in STOP}
def dump(d): return '[\n'+',\n'.join(json.dumps(x,ensure_ascii=False) for x in d)+'\n]\n'
theme,batch=sys.argv[1],sys.argv[2]; dry='--dry' in sys.argv
path=A%theme; d=json.load(open(path))
assert dump(d)==open(path).read(), 'format mismatch'
seen={norm(x['q']) for x in d}
byans=collections.defaultdict(list)
for x in d: byans[norm(x['options'][x['answer']])].append(toks(x['q']))
added=0; skipped=[]
for ln in open(batch):
    ln=ln.strip()
    if not ln or ln.startswith('#'): continue
    p=[s.strip() for s in ln.split('|')]
    if len(p)!=6 or p[0] not in ('easy','medium','hard'): skipped.append(('BAD',ln)); continue
    diff,q,*opts=p
    if len({norm(o) for o in opts})!=4: skipped.append(('OPTS',ln)); continue
    if norm(q) in seen: skipped.append(('DUP',q)); continue
    t=toks(q); a=norm(opts[0])
    near=[o for o in byans[a] if t and o and (len(t&o)/len(t|o)>=0.6 or len(t&o)/min(len(t),len(o))>=0.5)]
    if near: skipped.append(('NEAR',q)); continue
    d.append({"q":q,"options":opts,"answer":0,"difficulty":diff})
    seen.add(norm(q)); byans[a].append(t); added+=1
for r,s in skipped: print(r,s)
c=collections.Counter(x['difficulty'] for x in d)
print(f'{theme}: +{added}, skipped {len(skipped)}; total {len(d)} easy {c["easy"]}/500 medium {c["medium"]}/700 hard {c["hard"]}/800')
if not dry:
    import subprocess, os; subprocess.run(["python3", os.path.join(os.path.dirname(os.path.abspath(__file__)), "status.py")], stdout=subprocess.DEVNULL)
    open(path,'w').write(dump(d)); shutil.copyfile(path,I%theme)
