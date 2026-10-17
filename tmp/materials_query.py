import json
import re
import sys

data = [json.loads(s) for s in open('tmp/materials_structure.jsonl', encoding='utf-8-sig')]
number = sys.argv[1]
terms = re.compile(sys.argv[2], re.I) if len(sys.argv) > 2 else None
d = next(x for x in data if x['file'].startswith(number))
print(d['file'], 'paragraphs', d['paragraphs'], 'tables', d['tables'], 'images', d['images'])
for it in d['items']:
    if it['kind'] == 'p':
        if not terms or terms.search(it['text']):
            print('P', it['i'], it['style'], it['text'].replace('\n',' ')[:350])
    elif not terms or terms.search(str(it['head'])):
        print('T', it['i'], it['rows'], it['cols'], str(it['head'])[:350])
