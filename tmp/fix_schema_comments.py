from pathlib import Path
import re

path = Path('sql/schema.sql')
data = path.read_text(encoding='utf-8')
comments = {
    'nature': '办学性质：公办/民办/中外合作办学',
    'school_type': '院校类型：综合/理工/师范/医药/财经/政法/语言/艺术/体育/农林/民族',
    'soft_ranking': '软科中国大学排名',
    'postgraduate_rate': '保研率%',
    'has_graduate_school': '是否设有研究生院',
    'has_doctor_program': '是否设有博士点',
}
for field, comment in comments.items():
    pat = rf'(`{field}`[^\r\n]*?COMMENT )\'[^\']*\''
    data, count = re.subn(pat, lambda m: m.group(1) + "'" + comment + "'", data, count=1)
    if count != 1:
        raise RuntimeError(f'{field}: {count} matches')
path.write_text(data, encoding='utf-8', newline='')
print('Fixed six university comments')
