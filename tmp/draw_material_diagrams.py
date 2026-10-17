from pathlib import Path
from PIL import Image,ImageDraw,ImageFont

out=Path('tmp/material_diagrams');out.mkdir(exist_ok=True)
font_path=r'C:\Windows\Fonts\msyh.ttc'
regular=lambda n:ImageFont.truetype(font_path,n)
orange='#F36B21';ink='#182338';muted='#64748B';pale='#FFF5ED'

def canvas(title):
 im=Image.new('RGB',(1600,1000),'white');d=ImageDraw.Draw(im)
 d.text((70,52),title,font=regular(46),fill=ink)
 d.line((70,118,1530,118),fill=orange,width=5)
 return im,d
def card(d,box,head,body='',accent=orange):
 d.rounded_rectangle(box,radius=18,fill=pale,outline=accent,width=3)
 x,y,x2,y2=box
 d.text((x+22,y+17),head,font=regular(32),fill=ink)
 if body:d.text((x+22,y+68),body,font=regular(24),fill=muted)
def arrow(d,x1,y1,x2,y2):
 d.line((x1,y1,x2,y2),fill=orange,width=5)
 if y2>y1:d.polygon([(x2-11,y2-13),(x2+11,y2-13),(x2,y2+5)],fill=orange)
 else:d.polygon([(x2-13,y2-11),(x2-13,y2+11),(x2+5,y2)],fill=orange)

im,d=canvas('Agent 意图决策与失败处理')
rows=[
 ('① 意图词典规则','29 组外置规则；强意图直接路由'),
 ('② 校准语义路由','可选增强；默认关闭；阈值与熔断'),
 ('③ 原生函数调用','10 个白名单业务工具 + 合成 reply'),
 ('④ JSON 约束输出','原生调用不可用时降级')]
for i,(h,b) in enumerate(rows):
 y=164+i*185;card(d,(72,y,950,y+138),h,b)
 if i<3:arrow(d,510,y+140,510,y+179)
card(d,(1015,345,1528,658),'只读工具硬失败','AI 补充通用知识')
d.text((1040,475),'必须标注“非本系统数据”',font=regular(24),fill=ink)
d.text((1040,520),'禁止编造分数、位次、概率',font=regular(24),fill=ink)
d.text((1040,565),'失败则回退原有提示',font=regular(24),fill=ink)
arrow(d,952,583,1005,583)
d.text((75,940),'删除确认：空白归一化 + 否定守卫；语义确认开关默认关闭',font=regular(23),fill=muted)
im.save(out/'agent-flow.png')

im,d=canvas('录取概率评分与位次一票否决')
card(d,(72,165,1528,282),'输入','考生分数/位次 + 每所院校最新录取年份记录')
arrow(d,800,285,800,340)
card(d,(72,350,750,490),'位次差 < -3000','最终概率强制 0；不进入推荐')
card(d,(850,350,1528,490),'位次差 ≥ -3000','进入三段位次概率计算')
arrow(d,1188,494,1188,550)
card(d,(530,562,1528,722),'三段位次映射','[-3000,1000]→35–54；(1000,10000]→55–74')
d.text((555,664),'>10000：75 起，封顶 96',font=regular(26),fill=ink)
arrow(d,1030,725,1030,777)
card(d,(530,790,1528,907),'双维度融合与排序','位次 75% + 分数 25%；按概率、位次差、分数差、校名排序')
d.text((72,815),'仅位次缺失时\n退回分数口径',font=regular(29),fill=muted)
im.save(out/'score-flow.png')
print('created',*(p.name for p in out.glob('*.png')))
