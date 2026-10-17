from pathlib import Path

source=Path('tmp/zhiyuan6-public-data.sql')
target=Path('sql/full-competition-data-20260923.sql')
header=(
 '-- zhiyuan6 public reference-data snapshot, 2026-09-23.\n'
 '-- Exported from zv6-mysql; contains six public catalog/fact tables only.\n'
 '-- No users, plans, conversations, AI configuration, or credentials are included.\n'
 '-- Use only on a fresh schema after sql/data.sql and the unique-key upgrade.\n'
 'SET NAMES utf8mb4;\n'
 'SET @OLD_FK_CHECKS = @@FOREIGN_KEY_CHECKS;\n'
 'SET FOREIGN_KEY_CHECKS = 0;\n'
 'DELETE FROM `major_admission_cutoff`;\n'
 'DELETE FROM `admission_group_cutoff`;\n'
 'DELETE FROM `admission_cutoff`;\n'
 'DELETE FROM `score_rank_mapping`;\n'
 'DELETE FROM `major`;\n'
 'DELETE FROM `university`;\n'
).encode('utf-8')
footer=b'SET FOREIGN_KEY_CHECKS = @OLD_FK_CHECKS;\n'
target.write_bytes(header+source.read_bytes()+footer)
print(target,target.stat().st_size)
