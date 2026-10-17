from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
from hashlib import sha256
import subprocess,os

root=Path(r'C:\Users\anoth\Documents\Qoder\2026-08-28\ff9cf40f\zhiyuan')
zip_path=Path(r'D:\智愿ai改版\智愿AI报考平台_参赛资料_整改版\07_源程序代码及可执行文件\源代码_zhiyuan_20260923_整改版.zip')
with ZipFile(zip_path) as z:
    names=[line.split('  ',1)[1] for line in z.read('SOURCE_SHA256_MANIFEST.txt').decode().splitlines()]
head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=root).decode().strip()
tree=subprocess.check_output(['git','ls-tree','-rz','HEAD'],cwd=root)
oids={}
for rec in tree.split(b'\0'):
    if not rec:continue
    meta,name=rec.split(b'\t',1)
    mode,kind,oid=meta.split(b' ')
    if kind==b'blob':oids[name.decode('utf-8')]=oid
missing=[x for x in names if x not in oids]
if missing:raise RuntimeError('Not in HEAD: '+str(missing[:10]))
tmp=zip_path.with_suffix('.head.tmp')
proc=subprocess.Popen(['git','cat-file','--batch'],cwd=root,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
manifest=[]
try:
    with ZipFile(tmp,'w',compression=ZIP_DEFLATED,compresslevel=6) as z:
        for name in names:
            oid=oids[name]
            proc.stdin.write(oid+b'\n');proc.stdin.flush()
            header=proc.stdout.readline().strip().split(b' ')
            if header[0]!=oid or header[1]!=b'blob':raise RuntimeError('bad cat-file header '+name)
            size=int(header[2]);data=proc.stdout.read(size)
            if len(data)!=size or proc.stdout.read(1)!=b'\n':raise RuntimeError('short blob '+name)
            z.writestr(name,data)
            manifest.append(f'{sha256(data).hexdigest()}  {name}')
        z.writestr('SOURCE_SHA256_MANIFEST.txt','\n'.join(manifest)+'\n')
        z.writestr('SOURCE_COMMIT.txt',head+'\n')
finally:
    proc.stdin.close();proc.terminate();proc.wait(timeout=30)
with ZipFile(tmp) as z:
    for line in z.read('SOURCE_SHA256_MANIFEST.txt').decode().splitlines():
        digest,name=line.split('  ',1)
        if sha256(z.read(name)).hexdigest()!=digest:raise RuntimeError('archive mismatch '+name)
os.replace(tmp,zip_path)
print('HEAD package',head[:12],'files',len(names),'bytes',zip_path.stat().st_size)
