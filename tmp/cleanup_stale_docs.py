from pathlib import Path
from docx import Document

base=Path(r'D:\智愿ai改版\智愿AI报考平台_参赛资料_整改版')
def setp(p,s):
    if p.runs:
        p.runs[0].text=s
        for r in p.runs[1:]: r.text=''
    else: p.add_run(s)
def patch(name, exact=None, replacements=None):
    path=base/name; d=Document(path); changed=0
    for p in d.paragraphs:
        s=p.text
        if exact:
            for start, value in exact:
                if s.startswith(start):
                    setp(p,value); changed+=1; s=value; break
        if replacements:
            out=s
            for old,new in replacements: out=out.replace(old,new)
            if out!=s: setp(p,out); changed+=1
    for t in d.tables:
        for row in t.rows:
            for cell in row.cells:
                s=cell.text; out=s
                if replacements:
                    for old,new in replacements: out=out.replace(old,new)
                if out!=s: cell.text=out; changed+=1
    d.save(path); print(name,'changed',changed)

patch('02_概要设计说明书.docx', replacements=[
 ('当前80条','当前 1,902 所'),('当前100条','当前 657 个'),('当前405条','当前 17,562 行'),
 ('当前2,613条','当前 166,176 行'),('当前3,405条','当前 9,726 行'),
 ('14个Controller、58个HTTP映射、46个Service目录Java文件','15个 Controller、59个 HTTP 映射、57个 Service 目录 Java 文件'),
 ('幂等数据库Schema迁移','独立增量脚本迁移；schema.sql 为 DROP/CREATE 重建基线'),
 ('本地正则优先、AI决策兜底的双层意图决策机制','外置意图词典、可选校准路由、原生函数调用与 JSON 降级的四层意图决策链路'),
])

patch('03_详细设计说明书.docx', replacements=[
 ('每组最多返回5条结果','各组返回全部达标候选'),('每组最多返回5条','各组返回全部达标候选'),
 ('截取Top5结果','返回全部达标候选'),('截取 Top5 结果','返回全部达标候选'),
 ('共15所院校推荐','返回数量随数据和筛选条件变化'),('共15所院校','返回数量随数据和筛选条件变化'),
 ('每梯度各5条','各梯度全部达标项'),('冲5稳5保5','冲稳保数量随数据变化'),
 ('前端通过轮询查询任务状态','当前前端主流程同步调用；异步任务仅作为后端 API 能力'),
 ('前端轮询获取结果','当前前端主流程同步调用；异步任务仅作为后端 API 能力'),
 ('异步任务+前端轮询','后端异步任务 API 与前端同步主流程'),
 ('拖拽排序（规划中）','拖拽排序已实现'),('18个Vue视图','19个 Vue 视图'),('18 个 Vue 视图','19 个 Vue 视图'),
 ('共12张业务表','共14张业务表'),('系统共12张业务表','系统共14张业务表'),
 ('80所院校','1,902 所院校'),('80 所院校','1,902 所院校'),('100个专业','657 个专业'),('100 个专业','657 个专业'),
 ('405条','17,562 行'),('2,613条','166,176 行'),('3,405条','9,726 行'),
 ('当前没有…可用的AI配置管理后台','AI 配置管理后台已实现；邮箱验证码与审计日志仍未实现'),
 ('当前没有AI配置管理后台','AI 配置管理后台已实现；邮箱验证码与审计日志仍未实现'),
 ('当前无后台接口/已提供管理端接口','AI 配置已提供管理端 GET/PUT/test 接口；数据删除接口未提供'),
 ('14个Controller、58个HTTP映射','15个 Controller、59个 HTTP 映射'),
 ('14 Controller / 58 mappings','15 Controller / 59 mappings'),
 ('各5所院校','全部达标候选'),
 ('不宣称删除/AI配置','不宣称数据删除；AI 配置已实现'),
 ('前端通过轮询/api/recommendations/tasks/{requestId}查询任务状态，获取最终推荐结果','当前前端主流程同步调用推荐接口；异步任务 API 仍可通过任务状态接口查询'),
 ('前端通过任务状态查询接口轮询获取结果','该状态查询接口供后端异步调用方使用；当前前端主流程同步调用推荐接口'),
 ('12张业务表分五类管理','14张业务表分六类管理'),
 ('12张业务表分五个模块','14张业务表分六个模块'),
 ('返回冲稳保三组各5所院校，概率分35-100','返回冲稳保三组全部达标项，概率分35-100'),
])
