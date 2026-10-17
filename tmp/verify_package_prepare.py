from pathlib import Path
from zipfile import ZipFile
from hashlib import sha256
from secrets import token_urlsafe, token_bytes
from base64 import b64encode

root=Path(r'C:\Users\anoth\Documents\Qoder\2026-08-28\ff9cf40f\zhiyuan\tmp\package_verify_v3')
root.mkdir(parents=True,exist_ok=True)
zip_path=Path(r'D:\智愿ai改版\智愿AI报考平台_参赛资料_整改版\07_源程序代码及可执行文件\源代码_zhiyuan_20260923_整改版.zip')
with ZipFile(zip_path) as z:
    for info in z.infolist():
        rel=Path(info.filename)
        if rel.is_absolute() or '..' in rel.parts:raise RuntimeError('unsafe zip path')
    z.extractall(root)
    for line in (root/'SOURCE_SHA256_MANIFEST.txt').read_text().splitlines():
        digest,name=line.split('  ',1)
        if sha256((root/name).read_bytes()).hexdigest()!=digest:raise RuntimeError(name)
env=(root/'.env.example').read_text(encoding='utf-8')
for a,b in {
 'DB_HOST_PORT=3307':'DB_HOST_PORT=23307',
 'REDIS_HOST_PORT=6380':'REDIS_HOST_PORT=26380',
 'SERVER_HOST_PORT=8080':'SERVER_HOST_PORT=28081',
 'ROCKETMQ_NAMESERVER_HOST_PORT=9876':'ROCKETMQ_NAMESERVER_HOST_PORT=29876',
 'ROCKETMQ_BROKER_HOST_PORT=10911':'ROCKETMQ_BROKER_HOST_PORT=30911',
 'ROCKETMQ_FAST_REMOTING_HOST_PORT=10909':'ROCKETMQ_FAST_REMOTING_HOST_PORT=30909',
 'ROCKETMQ_HA_HOST_PORT=10912':'ROCKETMQ_HA_HOST_PORT=30912',
 'AUTH_ALLOW_LEGACY_PLAINTEXT_LOGIN=false':'AUTH_ALLOW_LEGACY_PLAINTEXT_LOGIN=true',
}.items():env=env.replace(a,b)
env=env.replace('AUTH_JWT_SECRET=','AUTH_JWT_SECRET='+b64encode(token_bytes(48)).decode())
env=env.replace('DB_PASSWORD=zhiyuan123','DB_PASSWORD='+token_urlsafe(24))
env=env.replace('MYSQL_ROOT_PASSWORD=zhiyuan_root_2026','MYSQL_ROOT_PASSWORD='+token_urlsafe(24))
(root/'.env').write_text(env,encoding='utf-8')
print('Package extracted and checksums verified:',len(list(root.rglob('*'))),'entries')
