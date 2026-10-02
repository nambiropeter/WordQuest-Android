import json,sys,shutil
A='/Users/nambzzy/Developer/wordsearch_android/app/src/main/assets/content/trivia_%s.json'
I='/Users/nambzzy/Developer/wordsearch_ios/WordQuest/Resources/Content/trivia_%s.json'
t=sys.argv[1]; qs=set(sys.argv[2:]); p=A%t; d=json.load(open(p)); n=len(d)
d=[x for x in d if x['q'] not in qs]; print('removed',n-len(d))
open(p,'w').write('[\n'+',\n'.join(json.dumps(x,ensure_ascii=False) for x in d)+'\n]\n'); shutil.copyfile(p,I%t)
import subprocess, os
subprocess.run(['python3', os.path.join(os.path.dirname(os.path.abspath(__file__)), 'status.py')], stdout=subprocess.DEVNULL)
