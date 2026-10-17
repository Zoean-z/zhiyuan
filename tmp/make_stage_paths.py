from pathlib import Path
from zipfile import ZipFile
import subprocess

root=Path(r'C:\Users\anoth\Documents\Qoder\2026-08-28\ff9cf40f\zhiyuan')
zip_path=Path(r'D:\智愿ai改版\智愿AI报考平台_参赛资料_整改版\07_源程序代码及可执行文件\源代码_zhiyuan_20260923_整改版.zip')
with ZipFile(zip_path) as z:
    names=[line.split('  ',1)[1] for line in z.read('SOURCE_SHA256_MANIFEST.txt').decode().splitlines()]
deleted=subprocess.check_output(['git','ls-files','-d','-z'],cwd=root).decode('utf-8').split('\0')
deleted=[p.replace('\\','/') for p in deleted if p and (p.startswith('frontend/src/') or p.startswith('src/') or p.startswith('sql/'))]
paths=sorted(set(names+deleted))
(root/'tmp/source_stage_paths.nul').write_bytes(('\0'.join(paths)+'\0').encode('utf-8'))
print('stage paths',len(paths),'deleted',deleted)
