from pathlib import Path
import re

files=[
 'frontend/src/views/HistoryRecordsView.vue',
 'frontend/src/views/MajorsView.vue',
 'frontend/src/views/NewsDetailView.vue',
 'frontend/src/views/NewsView.vue',
 'frontend/src/views/PlansView.vue',
 'frontend/src/views/RankView.vue',
 'frontend/src/views/RecommendationView.vue',
 'frontend/src/views/SchoolDetailView.vue',
 'frontend/src/views/SegmentsView.vue',
]
for name in files:
 p=Path(name);src=p.read_bytes();dst=src.replace(bytes([13,13,10]),bytes([13,10]));dst=re.sub(rb'[ \t]+(?=\r?\n|\Z)',b'',dst)
 if dst!=src:p.write_bytes(dst)
 print(name,'removed',len(src)-len(dst),'trailing bytes')
