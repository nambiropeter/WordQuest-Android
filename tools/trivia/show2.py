import json,subprocess,re,sys
t=sys.argv[1]
A='/Users/nambzzy/Developer/wordsearch_android/app/src/main/assets/content/trivia_%s.json'%t
d=json.load(open(A))
def w(s): return re.findall(r"[a-z0-9']+",s.lower())
def props(s): return {x for x in re.findall(r"\b[A-Z][a-zé'\-]+",s)[1:]} - {'Which','What','In','On','The','How','Who','Where'}
n=0
for l in subprocess.run(['python3','audit.py',t],capture_output=True,text=True).stdout.strip().split('\n'):
    _,i,j,*_=l.split('\t'); i,j=int(i),int(j)
    a,b=w(d[i]['q']),w(d[j]['q'])
    if len(a)==len(b) and sum(x!=y for x,y in zip(a,b))<=2: continue
    pa,pb=props(d[i]['q']),props(d[j]['q'])
    if pa and pb and not (pa&pb): continue
    n+=1
    print(f"{i}{d[i]['difficulty'][0]}: {d[i]['q']} => {d[i]['options'][d[i]['answer']]} || {j}{d[j]['difficulty'][0]}: {d[j]['q']}")
print(n)
