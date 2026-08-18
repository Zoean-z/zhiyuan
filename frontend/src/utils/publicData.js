export const SCHOOLS = [
  { id: 1, name: "清华大学", province: "北京", city: "北京", type: "综合类", nature: "公办", belong: "教育部", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 2, name: "北京大学", province: "北京", city: "北京", type: "综合类", nature: "公办", belong: "教育部", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 3, name: "浙江大学", province: "浙江", city: "杭州", type: "综合类", nature: "公办", belong: "教育部", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 4, name: "复旦大学", province: "上海", city: "上海", type: "综合类", nature: "公办", belong: "教育部", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 5, name: "上海交通大学", province: "上海", city: "上海", type: "综合类", nature: "公办", belong: "教育部", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 6, name: "南京大学", province: "江苏", city: "南京", type: "综合类", nature: "公办", belong: "教育部", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 7, name: "中国科学技术大学", province: "安徽", city: "合肥", type: "理工类", nature: "公办", belong: "中国科学院", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 8, name: "华中科技大学", province: "湖北", city: "武汉", type: "综合类", nature: "公办", belong: "教育部", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 9, name: "武汉大学", province: "湖北", city: "武汉", type: "综合类", nature: "公办", belong: "教育部", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 10, name: "中山大学", province: "广东", city: "广州", type: "综合类", nature: "公办", belong: "教育部", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 11, name: "哈尔滨工业大学", province: "黑龙江", city: "哈尔滨", type: "理工类", nature: "公办", belong: "工业和信息化部", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 12, name: "西安交通大学", province: "陕西", city: "西安", type: "综合类", nature: "公办", belong: "教育部", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 13, name: "同济大学", province: "上海", city: "上海", type: "理工类", nature: "公办", belong: "教育部", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 14, name: "北京航空航天大学", province: "北京", city: "北京", type: "理工类", nature: "公办", belong: "工业和信息化部", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 15, name: "天津大学", province: "天津", city: "天津", type: "理工类", nature: "公办", belong: "教育部", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 16, name: "华南理工大学", province: "广东", city: "广州", type: "理工类", nature: "公办", belong: "教育部", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 17, name: "东南大学", province: "江苏", city: "南京", type: "综合类", nature: "公办", belong: "教育部", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 18, name: "大连理工大学", province: "辽宁", city: "大连", type: "理工类", nature: "公办", belong: "教育部", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 19, name: "山东大学", province: "山东", city: "济南", type: "综合类", nature: "公办", belong: "教育部", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 20, name: "厦门大学", province: "福建", city: "厦门", type: "综合类", nature: "公办", belong: "教育部", is985: true, is211: true, isDoubleFirstClass: true }
];

export const SCHOOL_PROVINCES = ["全部", ...new Set(SCHOOLS.map((school) => school.province))];
export const SCHOOL_TYPES = ["全部", ...new Set(SCHOOLS.map((school) => school.type))];

export function selectSchoolShowcase(type = "全部", offset = 0, count = 8) {
  const source = type === "全部" ? SCHOOLS : SCHOOLS.filter((school) => school.type === type);
  if (!source.length || count <= 0) return [];
  const start = ((Number(offset) || 0) % source.length + source.length) % source.length;
  return Array.from({ length: Math.min(count, source.length) }, (_, index) => source[(start + index) % source.length]);
}

export const MAJORS = [
  { name: "计算机科学与技术", code: "080901", category: "工学", sub: "计算机类", duration: "四年", degree: "工学学士" },
  { name: "软件工程", code: "080902", category: "工学", sub: "计算机类", duration: "四年", degree: "工学学士" },
  { name: "人工智能", code: "080717T", category: "工学", sub: "电子信息类", duration: "四年", degree: "工学学士" },
  { name: "数据科学与大数据技术", code: "080910T", category: "工学", sub: "计算机类", duration: "四年", degree: "工学学士" },
  { name: "电子信息工程", code: "080701", category: "工学", sub: "电子信息类", duration: "四年", degree: "工学学士" },
  { name: "电气工程及其自动化", code: "080601", category: "工学", sub: "电气类", duration: "四年", degree: "工学学士" },
  { name: "自动化", code: "080801", category: "工学", sub: "自动化类", duration: "四年", degree: "工学学士" },
  { name: "机械设计制造及其自动化", code: "080202", category: "工学", sub: "机械类", duration: "四年", degree: "工学学士" },
  { name: "土木工程", code: "081001", category: "工学", sub: "土木类", duration: "四年", degree: "工学学士" },
  { name: "数学与应用数学", code: "070101", category: "理学", sub: "数学类", duration: "四年", degree: "理学学士" },
  { name: "临床医学", code: "100201K", category: "医学", sub: "临床医学类", duration: "五年", degree: "医学学士" },
  { name: "口腔医学", code: "100301K", category: "医学", sub: "口腔医学类", duration: "五年", degree: "医学学士" },
  { name: "护理学", code: "101101", category: "医学", sub: "护理学类", duration: "四年", degree: "理学学士" },
  { name: "中医学", code: "100501K", category: "医学", sub: "中医学类", duration: "五年", degree: "医学学士" },
  { name: "法学", code: "030101K", category: "法学", sub: "法学类", duration: "四年", degree: "法学学士" },
  { name: "汉语言文学", code: "050101", category: "文学", sub: "中国语言文学类", duration: "四年", degree: "文学学士" },
  { name: "英语", code: "050201", category: "文学", sub: "外国语言文学类", duration: "四年", degree: "文学学士" },
  { name: "新闻学", code: "050301", category: "文学", sub: "新闻传播学类", duration: "四年", degree: "文学学士" },
  { name: "金融学", code: "020301K", category: "经济学", sub: "金融学类", duration: "四年", degree: "经济学学士" },
  { name: "经济学", code: "020101", category: "经济学", sub: "经济学类", duration: "四年", degree: "经济学学士" },
  { name: "会计学", code: "120203K", category: "管理学", sub: "工商管理类", duration: "四年", degree: "管理学学士" },
  { name: "学前教育", code: "040106", category: "教育学", sub: "教育学类", duration: "四年", degree: "教育学学士" },
  { name: "动物医学", code: "090401", category: "农学", sub: "动物医学类", duration: "五年", degree: "农学学士" },
  { name: "视觉传达设计", code: "130502", category: "艺术学", sub: "设计学类", duration: "四年", degree: "艺术学学士" }
];

export const MAJOR_CATEGORIES = ["全部", ...new Set(MAJORS.map((major) => major.category))];

export const NEWS_ARTICLES = [
  { id: "hunan-2026-admission", tag: "录取", date: "2026-08-04", source: "中国教育在线", title: "湖南本科批录取结束，专科批次录取启动", summary: "湖南省2026年本科批录取工作结束，高职专科批随后进入投档录取和征集志愿阶段。具体日程以湖南省教育考试院公告为准。", url: "https://gaokao.eol.cn/news/202608/t20260804_2762535.shtml" },
  { id: "sichuan-college-admission", tag: "录取", date: "2026-08-11", source: "中国教育在线", title: "四川普通类高职（专科）批次开始录取", summary: "四川普通类高职（专科）批次开始录取，考生可通过省教育考试院官方渠道查询录取进度与征集志愿安排。", url: "https://gaokao.eol.cn/news/202608/t20260811_2764248.shtml" },
  { id: "jiangxi-student-file", tag: "资讯", date: "2026-08-07", source: "中国教育在线", title: "高考纸质档案如何领取和保管", summary: "被录取考生通常需按当地要求领取密封纸质档案并交至高校，档案不得自行拆封，办理方式以当地考试机构通知为准。", url: "https://gaokao.eol.cn/news/202608/t20260807_2763535.shtml" },
  { id: "sichuan-major-transfer", tag: "专业", date: "2026-08-05", source: "中国教育在线", title: "哪些招生类型入校后转专业受限", summary: "强基计划、定向培养、艺体类和部分中外合作办学专业可能限制转专业，填报前应仔细阅读目标高校招生章程。", url: "https://gaokao.eol.cn/news/202608/t20260805_2762894.shtml" },
  { id: "chongqing-volunteer", tag: "志愿", date: "2026-08-11", source: "中国教育在线", title: "重庆高职专科批征集志愿安排发布", summary: "重庆发布高职专科批征集志愿安排，考生应在规定时间内通过考试院官方志愿填报系统操作。", url: "https://gaokao.eol.cn/news/202608/t20260811_2764252.shtml" },
  { id: "anhui-undergraduate", tag: "录取", date: "2026-08-07", source: "中国教育在线", title: "安徽普通本科批次录取工作结束", summary: "安徽普通本科批录取结束，高职专科批随后开展；考生应通过考试院官方渠道核验录取结果，警惕招生诈骗。", url: "https://gaokao.eol.cn/news/202608/t20260807_2763565.shtml" }
];

export function newsById(id) {
  return NEWS_ARTICLES.find((article) => article.id === String(id)) || null;
}
