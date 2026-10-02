import json,subprocess,sys
A='/Users/nambzzy/Developer/wordsearch_android/app/src/main/assets/content/trivia_%s.json'
t=sys.argv[1]; d=json.load(open(A%t))
for l in subprocess.run(['python3','audit.py',t],capture_output=True,text=True).stdout.strip().split('\n'):
    if not l: continue
    _,i,j,*_=l.split('\t'); i,j=int(i),int(j)
    print(f"{i}{d[i]['difficulty'][0]}: {d[i]['q']} => {d[i]['options'][d[i]['answer']]} || {j}{d[j]['difficulty'][0]}: {d[j]['q']}")
