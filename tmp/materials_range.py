import json,sys
d=next(x for x in [json.loads(s) for s in open('tmp/materials_structure.jsonl',encoding='utf-8-sig')] if x['file'].startswith(sys.argv[1]))
a,b=map(int,sys.argv[2:4])
for x in d['items']:
 if x['kind']=='p' and a<=x['i']<=b: print(x['i'],x['text'][:700])
