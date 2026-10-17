from pathlib import Path
from hashlib import sha256
from zipfile import ZipFile, ZIP_DEFLATED

ROOT=Path(r'C:\Users\anoth\Documents\Qoder\2026-08-28\ff9cf40f\zhiyuan')
OUT=Path(r'D:\智愿ai改版\智愿AI报考平台_参赛资料_整改版\07_源程序代码及可执行文件')
OUT.mkdir(parents=True,exist_ok=True)
ZIP=OUT/'源代码_zhiyuan_20260923_整改版.zip'
top={
 '.dockerignore','.editorconfig','.env.example','.gitignore','Dockerfile','docker-compose.yml','docker-compose.full-data.yml',
 'docker-compose.override.yml','docker-compose.zhiyuan6.yml','pom.xml','mvnw','mvnw.cmd',
 'mvnwDebug','mvnwDebug.cmd','README.md','REPRODUCE_BUILD.sh','check-db.bat','init-db.bat',
 'COMPETITION_MATERIAL.md','AI_CHAT_API_DESIGN.md','AGENT_ENHANCEMENT_PLAN.md','PROJECT_LOG.md',
}
dirs=['.mvn','docker','src/main/java','src/main/resources','src/test','frontend/src','frontend/public','frontend/test','sql','scripts']
frontend_top={'index.html','package.json','package-lock.json','vite.config.js'}
bad_exact={'.env','.env.bak_20260829','.env.zhiyuan6','rec-body.json','rec-tmp.json','probe_out.log'}
files=[]
for name in top:
 p=ROOT/name
 if p.is_file():files.append(p)
for name in frontend_top:
 p=ROOT/'frontend'/name
 if p.is_file():files.append(p)
for name in dirs:
 folder=ROOT/name
 if not folder.exists():continue
 for p in folder.rglob('*'):
  if not p.is_file():continue
  parts=set(p.relative_to(ROOT).parts)
  if any(x in parts for x in {'node_modules','dist','target','tmp','.git','__pycache__','static'}):continue
  if p.name in bad_exact or p.name.endswith('.bak_20260829') or p.suffix in {'.log','.pyc'}:continue
  files.append(p)
files=sorted(set(files),key=lambda p:p.relative_to(ROOT).as_posix())
manifest=[]
with ZipFile(ZIP,'w',compression=ZIP_DEFLATED,compresslevel=6) as z:
 for p in files:
  rel=p.relative_to(ROOT).as_posix()
  blob=p.read_bytes()
  z.writestr(rel,blob)
  manifest.append(f'{sha256(blob).hexdigest()}  {rel}')
 z.writestr('SOURCE_SHA256_MANIFEST.txt','\n'.join(manifest)+'\n')
with ZipFile(ZIP) as z:
 for line in z.read('SOURCE_SHA256_MANIFEST.txt').decode().splitlines():
  digest,rel=line.split('  ',1)
  if sha256(z.read(rel)).hexdigest()!=digest:raise RuntimeError('hash mismatch '+rel)
names=[p.relative_to(ROOT).as_posix() for p in files]
print('zip',ZIP,'files',len(files),'logos',sum(x.startswith('frontend/public/logos/') for x in names),'bytes',ZIP.stat().st_size)
for forbidden in bad_exact:
 if forbidden in names:raise RuntimeError('forbidden '+forbidden)
