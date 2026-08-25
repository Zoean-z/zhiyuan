#!/usr/bin/env bash
# 部署后前端产物自检：防止"构建产物混叠"复发（20260825 根因）。
# 用法: scripts/verify-frontend-assets.sh [容器名]  默认 zhiyuan-backend
set -u
CONTAINER="${1:-zhiyuan-backend}"
echo "== 检查容器: $CONTAINER =="

echo "[1/4] index.html 引用的主 chunk（应唯一且存在）"
MAIN_INDEX=$(docker exec "$CONTAINER" sh -c "unzip -p /app/app.jar 'BOOT-INF/classes/static/index.html' | grep -oE 'assets/index-[A-Za-z0-9_]+\.js'" 2>/dev/null)
echo "  引用: $MAIN_INDEX"
COUNT=$(docker exec "$CONTAINER" sh -c "unzip -l /app/app.jar | grep -c 'static/assets/index-[A-Za-z0-9_]*\.js'" 2>/dev/null)
echo "  jar 内 index 主 chunk 数量: $COUNT（应为 1）"
if [ "$COUNT" -ne 1 ]; then echo "  ❌ 存在多个主 chunk，产物混叠！"; exit 1; fi

echo "[2/4] 同名 View 重复 chunk（应无输出）"
DUP=$(docker exec "$CONTAINER" sh -c "unzip -l /app/app.jar | grep -oE 'static/assets/[A-Za-z]+View-[A-Za-z0-9_]+\.js' | sort | uniq -d" 2>/dev/null)
if [ -n "$DUP" ]; then echo "  ❌ 重复 chunk:"; echo "$DUP"; exit 1; else echo "  ✅ 无重复"; fi

echo "[3/4] 懒加载 chunk 完整性（应无 MISSING）"
docker exec "$CONTAINER" sh -c "unzip -p /app/app.jar 'BOOT-INF/classes/static/assets/index-*.js' 2>/dev/null | grep -oE 'assets/[A-Za-z0-9_]+-[A-Za-z0-9_]{8}\.js' | sort -u | while read f; do unzip -l /app/app.jar | grep -q \"static/\$f\" || echo \"  ❌ MISSING: \$f\"; done" 2>/dev/null
echo "  ✅ 检查完成（上方无 ❌ 即全部存在）"

echo "[4/4] 首页响应头（Cache-Control 应为 no-cache）"
curl -s -m 8 -o /dev/null -D - "http://localhost:${PORT:-8080}/" 2>/dev/null | grep -i "cache-control" || echo "  ⚠ 未获取到（确认端口/服务）"

echo "== 自检完成 =="
