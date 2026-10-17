from pathlib import Path
from pypdf import PdfReader
terms=['图5-1 核心智能推荐','图5-2 AI Agent','图4-1 双维度概率','图5-3 智能推荐','图5-4 AI Agent',
       '图3-6 查大学','图3-7-1','图3-12-1','图3-14-1','图4-7 AI 管理',
       '图7-1-1','289','当前记录数：1,902']
for sub in ['01','02b','03b','04','05','06']:
 folder=Path('tmp/materials_render_final')/sub
 pdf=next(folder.glob('*.pdf'))
 reader=PdfReader(str(pdf))
 print(sub,'pages',len(reader.pages))
 for i,p in enumerate(reader.pages,1):
  t=p.extract_text()
  matches=[x for x in terms if x in t]
  if matches:print(i,*matches)
