// Mock 数据 - 用于无后端的演示模式

const MOCK_USER = {
  id: 1,
  username: "testuser",
  score: 630,
  subjectType: "PHYSICS",
  examProvince: "浙江",
  role: "USER",
  token: "mock-token-12345"
};

const MOCK_ADMIN_USER = {
  id: 100,
  username: "admin",
  score: null,
  subjectType: null,
  examProvince: null,
  role: "ADMIN",
  token: "mock-admin-token-12345"
};

const MOCK_PROVINCES = [
  "北京", "天津", "河北", "山西", "内蒙古",
  "辽宁", "吉林", "黑龙江", "上海", "江苏",
  "浙江", "安徽", "福建", "江西", "山东",
  "河南", "湖北", "湖南", "广东", "广西",
  "海南", "重庆", "四川", "贵州", "云南",
  "西藏", "陕西", "甘肃", "青海", "宁夏", "新疆"
];

const MOCK_SCHOOLS = [
  { id: 1, name: "清华大学", province: "北京", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 2, name: "北京大学", province: "北京", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 3, name: "浙江大学", province: "浙江", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 4, name: "复旦大学", province: "上海", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 5, name: "上海交通大学", province: "上海", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 6, name: "南京大学", province: "江苏", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 7, name: "中国科学技术大学", province: "安徽", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 8, name: "华中科技大学", province: "湖北", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 9, name: "武汉大学", province: "湖北", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 10, name: "中山大学", province: "广东", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 11, name: "哈尔滨工业大学", province: "黑龙江", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 12, name: "西安交通大学", province: "陕西", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 13, name: "同济大学", province: "上海", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 14, name: "北京航空航天大学", province: "北京", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 15, name: "天津大学", province: "天津", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 16, name: "华南理工大学", province: "广东", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 17, name: "东南大学", province: "江苏", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 18, name: "大连理工大学", province: "辽宁", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 19, name: "山东大学", province: "山东", tier: "985", is985: true, is211: true, isDoubleFirstClass: true },
  { id: 20, name: "厦门大学", province: "福建", tier: "985", is985: true, is211: true, isDoubleFirstClass: true }
];

const MOCK_MAJORS = [
  "计算机科学与技术", "软件工程", "人工智能", "数据科学与大数据技术",
  "电子信息工程", "通信工程", "自动化", "电气工程及其自动化",
  "机械工程", "车辆工程", "土木工程", "建筑学",
  "临床医学", "口腔医学", "药学", "护理学",
  "法学", "金融学", "会计学", "工商管理",
  "英语", "日语", "法语", "德语"
];

function generateRecommendation(school, major, strategy) {
  const baseScore = 620 + Math.floor(Math.random() * 40);
  const scoreGap = Math.floor(Math.random() * 30) - 15;
  const rankGap = Math.floor(Math.random() * 2000) - 1000;
  const probability = strategy === "rush" ? 35 + Math.floor(Math.random() * 20)
    : strategy === "safe" ? 55 + Math.floor(Math.random() * 20)
    : 75 + Math.floor(Math.random() * 20);

  return {
    universityId: school.id,
    universityName: school.name,
    universityProvince: school.province,
    universityTier: school.tier,
    is985: school.is985,
    is211: school.is211,
    isDoubleFirstClass: school.isDoubleFirstClass,
    majorName: major,
    cutoffScore: baseScore,
    scoreGap: scoreGap,
    userRank: 5000 + Math.floor(Math.random() * 3000),
    minRank: 4000 + Math.floor(Math.random() * 4000),
    rankGap: rankGap,
    admissionProbability: probability,
    strategy: strategy,
    strategyLabel: strategy === "rush" ? "冲刺" : strategy === "safe" ? "稳妥" : "保底",
    recommendationBasis: "SCORE",
    riskScore: 100 - probability,
    matchReasons: ["分数匹配", "位次相近", "专业对口"],
    explanation: `该校${major}专业近年录取分数相对稳定，根据你的分数和位次，有一定录取机会。`
  };
}

function generateRecommendations(count = 15) {
  const rush = [];
  const safe = [];
  const guarantee = [];

  for (let i = 0; i < count; i++) {
    const school = MOCK_SCHOOLS[i % MOCK_SCHOOLS.length];
    const major = MOCK_MAJORS[Math.floor(Math.random() * MOCK_MAJORS.length)];

    if (i < 5) {
      rush.push(generateRecommendation(school, major, "rush"));
    } else if (i < 10) {
      safe.push(generateRecommendation(school, major, "safe"));
    } else {
      guarantee.push(generateRecommendation(school, major, "guarantee"));
    }
  }

  return {
    rush,
    safe,
    guarantee,
    summary: `根据你的分数630分和位次信息，为你推荐了${count}所院校。其中冲刺${rush.length}所，稳妥${safe.length}所，保底${guarantee.length}所。`,
    aiSummary: "综合分析你的分数和位次情况，推荐策略以冲稳保梯度为主，建议重点关注稳妥档位的院校。",
    finalAdvice: "建议优先考虑稳妥档位的院校，冲刺档位可以作为备选。保底档位确保有学可上。",
    tips: ["关注目标院校的招生计划变化", "注意专业调剂风险", "建议多了解目标城市的就业环境"]
  };
}

const MOCK_PLANS = [
  {
    id: 1,
    planName: "当前方案草稿",
    sourceType: "score",
    sourceQuery: "模式：专业优先，分数：650，省份：浙江，科类：物理，专业：计算机科学与技术",
    createdAt: "2026-08-07T08:53:23",
    resultJson: JSON.stringify(generateRecommendations(5))
  },
  {
    id: 2,
    planName: "冲稳保方案-A",
    sourceType: "score",
    sourceQuery: "模式：学校优先，分数：630，省份：浙江，科类：物理",
    createdAt: "2026-03-21T11:22:17",
    resultJson: JSON.stringify(generateRecommendations(8))
  },
  {
    id: 3,
    planName: "最终提交版",
    sourceType: "score",
    sourceQuery: "模式：学校优先，分数：600，省份：浙江，科类：物理",
    createdAt: "2026-03-21T11:18:44",
    resultJson: JSON.stringify(generateRecommendations(6))
  }
];

const MOCK_HISTORY = [
  {
    id: 1,
    queryType: "score",
    queryContent: "模式:专业优先，分数:650，省份:浙江，科类:PHYSICS，专业:计算机科学与技术",
    createdAt: "2026-08-07T09:00:33",
    summary: "返回 3 个稳妥院校"
  },
  {
    id: 2,
    queryType: "score",
    queryContent: "模式:专业优先，分数:630，省份:浙江，科类:PHYSICS，专业:计算机科学与技术",
    createdAt: "2026-08-07T08:57:07",
    summary: "返回 5 个推荐院校"
  },
  {
    id: 3,
    queryType: "score",
    queryContent: "模式:专业优先，分数:630，省份:浙江，科类:PHYSICS，专业:计算机科学与技术",
    createdAt: "2026-08-07T08:53:22",
    summary: "返回 5 个推荐院校"
  },
  {
    id: 4,
    queryType: "score",
    queryContent: "模式:学校优先，分数:630，省份:浙江，科类:PHYSICS",
    createdAt: "2026-08-07T08:53:04",
    summary: "返回 8 个推荐院校"
  },
  {
    id: 5,
    queryType: "text",
    queryContent: "推荐几个华东地区的学校",
    createdAt: "2026-03-21T11:28:03",
    summary: "生成 8 条推荐"
  },
  {
    id: 6,
    queryType: "text",
    queryContent: "推荐几个华东地区的学校",
    createdAt: "2026-03-21T11:22:54",
    summary: "生成 8 条推荐"
  },
  {
    id: 7,
    queryType: "score",
    queryContent: "模式:学校优先，分数:630，省份:浙江，科类:PHYSICS",
    createdAt: "2026-03-21T11:21:53",
    summary: "返回 8 个推荐院校"
  },
  {
    id: 8,
    queryType: "agent",
    queryContent: "帮我推荐计算机专业",
    createdAt: "2026-03-21T10:59:16",
    summary: "进入 AI 顾问对话"
  }
];

const MOCK_CONVERSATIONS = [
  {
    id: 1,
    title: "关于志愿填报的咨询",
    status: "ACTIVE",
    lastMessageAt: "2026-08-07T08:05:00",
    messageCount: 2,
    createdAt: "2026-08-07T08:00:00",
    updatedAt: "2026-08-07T08:05:00"
  },
  {
    id: 2,
    title: "推荐计算机专业院校",
    status: "ACTIVE",
    lastMessageAt: "2026-08-06T15:35:00",
    messageCount: 2,
    createdAt: "2026-08-06T15:30:00",
    updatedAt: "2026-08-06T15:35:00"
  }
];

const MOCK_CONVERSATION_MESSAGES = new Map([
  [1, [
    { id: 1, role: "user", messageType: "text", content: "帮我看看当前的志愿方案", toolName: null, payload: null, createdAt: "2026-08-07T08:04:00" },
    { id: 2, role: "assistant", messageType: "text", content: "可以。我会结合当前志愿表逐项分析冲稳保梯度。", toolName: null, payload: null, createdAt: "2026-08-07T08:05:00" }
  ]],
  [2, [
    { id: 3, role: "user", messageType: "text", content: "推荐计算机专业院校", toolName: null, payload: null, createdAt: "2026-08-06T15:34:00" },
    { id: 4, role: "assistant", messageType: "text", content: "已准备好按专业优先模式为你推荐。", toolName: null, payload: null, createdAt: "2026-08-06T15:35:00" }
  ]]
]);

const MOCK_ADMIN_USERS = [
  {
    id: 100, username: "admin", score: null, subjectType: null, examProvince: null,
    role: "ADMIN", enabled: true, createdAt: "2026-03-01T09:10:00", updatedAt: "2026-08-08T08:30:00",
    recommendationCount: 0, planCount: 0, conversationCount: 0
  },
  {
    id: 1, username: "testuser", score: 630, subjectType: "PHYSICS", examProvince: "浙江",
    role: "USER", enabled: true, createdAt: "2026-03-21T10:18:00", updatedAt: "2026-08-07T09:00:33",
    recommendationCount: 8, planCount: 3, conversationCount: 2
  },
  {
    id: 2, username: "linxi", score: 598, subjectType: "HISTORY", examProvince: "江苏",
    role: "USER", enabled: true, createdAt: "2026-05-12T14:26:00", updatedAt: "2026-08-06T16:42:00",
    recommendationCount: 5, planCount: 2, conversationCount: 4
  },
  {
    id: 3, username: "chenyu", score: 655, subjectType: "PHYSICS", examProvince: "山东",
    role: "USER", enabled: true, createdAt: "2026-06-19T11:05:00", updatedAt: "2026-08-05T19:20:00",
    recommendationCount: 12, planCount: 4, conversationCount: 6
  },
  {
    id: 4, username: "pending_user", score: null, subjectType: null, examProvince: null,
    role: "USER", enabled: false, createdAt: "2026-07-28T17:40:00", updatedAt: "2026-08-02T10:12:00",
    recommendationCount: 0, planCount: 0, conversationCount: 1
  }
];

const MOCK_ADMIN_UNIVERSITIES = MOCK_SCHOOLS.map((school) => ({
  ...school,
  tags: [school.is985 ? "985" : null, school.is211 ? "211" : null, school.isDoubleFirstClass ? "双一流" : null]
    .filter(Boolean)
    .join(",")
}));

const MOCK_ADMIN_MAJORS = [
  { id: 1, name: "计算机科学与技术", category: "计算机类", degreeType: "工学", tags: "热门,信息技术", subjectRequirement: "物理", description: "研究计算机系统、算法与软件设计。" },
  { id: 2, name: "软件工程", category: "计算机类", degreeType: "工学", tags: "热门,工程实践", subjectRequirement: "物理", description: "面向软件系统分析、设计、开发与维护。" },
  { id: 3, name: "人工智能", category: "电子信息类", degreeType: "工学", tags: "新兴,交叉学科", subjectRequirement: "物理", description: "涵盖机器学习、智能系统与数据处理。" },
  { id: 4, name: "电子信息工程", category: "电子信息类", degreeType: "工学", tags: "信息技术", subjectRequirement: "物理", description: "学习电子技术、信息获取与处理。" },
  { id: 5, name: "临床医学", category: "临床医学类", degreeType: "医学", tags: "医学", subjectRequirement: "物理,化学", description: "培养临床诊疗与医学研究能力。" },
  { id: 6, name: "法学", category: "法学类", degreeType: "法学", tags: "人文社科", subjectRequirement: "不限", description: "学习法律规范、法学理论与实务。" },
  { id: 7, name: "金融学", category: "金融学类", degreeType: "经济学", tags: "财经", subjectRequirement: "不限", description: "研究金融市场、机构与风险管理。" },
  { id: 8, name: "建筑学", category: "建筑类", degreeType: "工学", tags: "设计,工程", subjectRequirement: "物理", description: "学习建筑设计、技术与城市空间。" }
];

const MOCK_ADMIN_CUTOFFS = [
  { id: 1, universityId: 3, admissionYear: 2025, province: "浙江", subjectType: "PHYSICS", cutoffScore: 664, minRank: 6790 },
  { id: 2, universityId: 4, admissionYear: 2025, province: "浙江", subjectType: "PHYSICS", cutoffScore: 697, minRank: 1980 },
  { id: 3, universityId: 6, admissionYear: 2025, province: "江苏", subjectType: "HISTORY", cutoffScore: 635, minRank: 620 },
  { id: 4, universityId: 8, admissionYear: 2025, province: "湖北", subjectType: "PHYSICS", cutoffScore: 648, minRank: 4200 },
  { id: 5, universityId: 10, admissionYear: 2024, province: "广东", subjectType: "PHYSICS", cutoffScore: 624, minRank: 9630 }
];

const MOCK_ADMIN_MAJOR_CUTOFFS = [
  { id: 1, universityId: 3, majorId: 1, majorName: "计算机科学与技术", admissionYear: 2025, province: "浙江", subjectType: "PHYSICS", cutoffScore: 685, minRank: 3520 },
  { id: 2, universityId: 4, majorId: 2, majorName: "软件工程", admissionYear: 2025, province: "浙江", subjectType: "PHYSICS", cutoffScore: 699, minRank: 1760 },
  { id: 3, universityId: 6, majorId: 6, majorName: "法学", admissionYear: 2025, province: "江苏", subjectType: "HISTORY", cutoffScore: 642, minRank: 430 },
  { id: 4, universityId: 8, majorId: 3, majorName: "人工智能", admissionYear: 2025, province: "湖北", subjectType: "PHYSICS", cutoffScore: 655, minRank: 3380 },
  { id: 5, universityId: 10, majorId: 5, majorName: "临床医学", admissionYear: 2024, province: "广东", subjectType: "PHYSICS", cutoffScore: 640, minRank: 6800 }
];

function nextId(items) {
  return Math.max(0, ...items.map((item) => Number(item.id) || 0)) + 1;
}

function readJsonBody(options) {
  return options.body ? JSON.parse(options.body) : {};
}

function filterAdmissionRows(items, searchParams, includeMajor = false) {
  const universityId = searchParams.get("universityId");
  const admissionYear = searchParams.get("admissionYear");
  const province = searchParams.get("province")?.trim().toLowerCase();
  const subjectType = searchParams.get("subjectType");
  const majorKeyword = searchParams.get("majorKeyword")?.trim().toLowerCase();
  return items.filter((item) => {
    if (universityId && Number(item.universityId) !== Number(universityId)) return false;
    if (admissionYear && Number(item.admissionYear) !== Number(admissionYear)) return false;
    if (province && !item.province.toLowerCase().includes(province)) return false;
    if (subjectType && item.subjectType !== subjectType) return false;
    return !includeMajor || !majorKeyword || item.majorName.toLowerCase().includes(majorKeyword);
  });
}

function saveAdminRecord(items, body, id = null) {
  if (id == null) {
    const created = { id: nextId(items), ...body };
    items.unshift(created);
    return created;
  }
  const index = items.findIndex((item) => Number(item.id) === Number(id));
  if (index < 0) return null;
  items[index] = { ...items[index], ...body, id: Number(id) };
  return items[index];
}

function savePlan(body, id = null) {
  const now = new Date().toISOString();
  if (id == null) {
    const created = { id: nextId(MOCK_PLANS), ...body, createdAt: now };
    MOCK_PLANS.unshift(created);
    return created;
  }
  const index = MOCK_PLANS.findIndex((item) => Number(item.id) === Number(id));
  if (index < 0) return null;
  MOCK_PLANS[index] = { ...MOCK_PLANS[index], ...body, id: Number(id) };
  return MOCK_PLANS[index];
}

function currentPlan() {
  return MOCK_PLANS.find((item) => item.planName === "当前方案草稿") || null;
}

function conversationDetail(conversation) {
  if (!conversation) return null;
  return {
    ...conversation,
    messages: [...(MOCK_CONVERSATION_MESSAGES.get(Number(conversation.id)) || [])]
  };
}

// Mock API 拦截器
export function setupMockInterceptor({ latencyMs = null } = {}) {
  const originalFetch = window.fetch;

  window.fetch = async function(url, options = {}) {
    if (!url.toString().startsWith("/api")) {
      return originalFetch.call(this, url, options);
    }

    // 模拟网络延迟
    const delay = latencyMs == null ? 300 + Math.random() * 200 : Math.max(0, latencyMs);
    if (delay > 0) await new Promise(resolve => setTimeout(resolve, delay));

    const method = (options.method || "GET").toUpperCase();
    const requestUrl = new URL(url.toString(), window.location.origin);
    const path = requestUrl.pathname;
    const searchParams = requestUrl.searchParams;

    // 登录
    if (path === "/api/auth/login" && method === "POST") {
      const body = readJsonBody(options);
      if (body.username === "admin" && body.password === "admin123") {
        return mockResponse(MOCK_ADMIN_USER);
      }
      return mockResponse(MOCK_USER);
    }

    // 注册
    if (path === "/api/auth/register" && method === "POST") {
      return mockResponse(MOCK_USER);
    }

    if (path === "/api/auth/logout" && method === "POST") {
      return mockResponse({ success: true });
    }

    // 获取用户信息
    if (path === "/api/auth/profile" && method === "POST") {
      return mockResponse(MOCK_USER);
    }

    // 获取省份选项
    if (path === "/api/meta/options") {
      return mockResponse({ provinces: MOCK_PROVINCES });
    }

    // 获取专业选项
    if (path === "/api/meta/major-options") {
      const keyword = (searchParams.get("keyword") || "").trim().toLowerCase();
      return mockResponse(MOCK_MAJORS.filter((major) => major.toLowerCase().includes(keyword)).slice(0, 10));
    }

    // 推荐查询
    if (path === "/api/recommendations" && method === "POST") {
      return mockResponse(generateRecommendations(15));
    }

    // 自由文本推荐
    if (path === "/api/recommendations/free-text" && method === "POST") {
      const grouped = generateRecommendations(10);
      return mockResponse({
        requestId: `mock-text-${Date.now()}`,
        parsed: {
          score: MOCK_USER.score,
          recommendationMode: "SCHOOL_FIRST",
          schoolLevels: [],
          schoolTypes: [],
          provinces: [],
          majorKeywords: [],
          normalizedMajors: [],
          candidateProvince: MOCK_USER.examProvince,
          subjectType: MOCK_USER.subjectType
        },
        recommendations: [...grouped.rush, ...grouped.safe, ...grouped.guarantee],
        summary: grouped.summary,
        aiSummary: grouped.aiSummary,
        finalAdvice: grouped.finalAdvice,
        tips: grouped.tips
      });
    }

    const schoolDetailMatch = path.match(/^\/api\/recommendations\/schools\/(\d+)\/majors$/);
    if (schoolDetailMatch && method === "GET") {
      const school = MOCK_SCHOOLS.find((item) => Number(item.id) === Number(schoolDetailMatch[1]));
      if (!school) return mockResponse({ message: "University not found" }, 404);
      return mockResponse({
        universityId: school.id,
        universityName: school.name,
        universityProvince: school.province,
        universityTier: school.tier,
        is985: school.is985,
        is211: school.is211,
        isDoubleFirstClass: school.isDoubleFirstClass,
        schoolTags: [school.is985 ? "985" : null, school.is211 ? "211" : null, school.isDoubleFirstClass ? "双一流" : null].filter(Boolean),
        universityTags: "综合类",
        majors: MOCK_MAJORS.slice(0, 6).map((major, index) => ({ majorName: major, cutoffScore: 640 + index, minRank: 5000 + index * 300 }))
      });
    }

    // 获取历史记录
    if (path === "/api/history" && method === "GET") {
      return mockResponse(MOCK_HISTORY);
    }

    // 获取历史详情
    if (path.match(/^\/api\/history\/\d+$/) && method === "GET") {
      const id = Number(path.split("/").pop());
      const record = MOCK_HISTORY.find((item) => Number(item.id) === id);
      if (!record) return mockResponse({ message: "History not found" }, 404);
      return mockResponse({
        ...record,
        resultJson: JSON.stringify(generateRecommendations(5))
      });
    }

    if (path.match(/^\/api\/history\/\d+$/) && method === "DELETE") {
      const id = Number(path.split("/").pop());
      const index = MOCK_HISTORY.findIndex((item) => Number(item.id) === id);
      if (index < 0) return mockResponse({ message: "History not found" }, 404);
      MOCK_HISTORY.splice(index, 1);
      return mockResponse({ success: true });
    }

    // 获取方案列表
    if (path === "/api/plans" && method === "GET") {
      return mockResponse(MOCK_PLANS);
    }

    if (path === "/api/plans/current" && method === "GET") {
      const plan = currentPlan();
      return plan ? mockResponse(plan) : mockResponse({ message: "Current plan not found" }, 404);
    }

    if (path === "/api/plans/current" && method === "PUT") {
      const body = readJsonBody(options);
      const plan = currentPlan();
      return mockResponse(plan
        ? savePlan({ ...body, planName: "当前方案草稿" }, plan.id)
        : savePlan({ ...body, planName: "当前方案草稿" }));
    }

    if (path === "/api/plans/current" && method === "DELETE") {
      const plan = currentPlan();
      if (plan) MOCK_PLANS.splice(MOCK_PLANS.indexOf(plan), 1);
      return mockResponse({ success: true });
    }

    // 获取方案详情
    if (path.match(/^\/api\/plans\/\d+$/) && method === "GET") {
      const id = parseInt(path.split("/").pop());
      const plan = MOCK_PLANS.find(p => p.id === id);
      return plan ? mockResponse(plan) : mockResponse({ message: "Plan not found" }, 404);
    }

    // 保存方案
    if (path === "/api/plans" && method === "POST") {
      return mockResponse(savePlan(readJsonBody(options)));
    }

    // 更新方案
    if (path.match(/^\/api\/plans\/\d+$/) && method === "PUT") {
      const saved = savePlan(readJsonBody(options), parseInt(path.split("/").pop()));
      return saved ? mockResponse(saved) : mockResponse({ message: "Plan not found" }, 404);
    }

    // 删除方案
    if (path.match(/^\/api\/plans\/\d+$/) && method === "DELETE") {
      const id = parseInt(path.split("/").pop());
      const index = MOCK_PLANS.findIndex((item) => Number(item.id) === id);
      if (index < 0) return mockResponse({ message: "Plan not found" }, 404);
      MOCK_PLANS.splice(index, 1);
      return mockResponse({ success: true });
    }

    // 获取对话列表
    if (path === "/api/agent/conversations" && method === "GET") {
      return mockResponse(MOCK_CONVERSATIONS);
    }

    // 创建对话
    if (path === "/api/agent/conversations" && method === "POST") {
      const body = readJsonBody(options);
      const now = new Date().toISOString();
      const conversation = {
        id: nextId(MOCK_CONVERSATIONS),
        title: body.title || "新的志愿对话",
        status: "ACTIVE",
        lastMessageAt: null,
        messageCount: 0,
        createdAt: now,
        updatedAt: now
      };
      MOCK_CONVERSATIONS.unshift(conversation);
      MOCK_CONVERSATION_MESSAGES.set(conversation.id, []);
      return mockResponse(conversationDetail(conversation));
    }

    const conversationDetailMatch = path.match(/^\/api\/agent\/conversations\/(\d+)$/);
    if (conversationDetailMatch && method === "GET") {
      const conversation = MOCK_CONVERSATIONS.find((item) => Number(item.id) === Number(conversationDetailMatch[1]));
      return conversation
        ? mockResponse(conversationDetail(conversation))
        : mockResponse({ message: "Conversation not found" }, 404);
    }

    // 发送消息
    const conversationMessageMatch = path.match(/^\/api\/agent\/conversations\/(\d+)\/messages$/);
    if (conversationMessageMatch && method === "POST") {
      const body = readJsonBody(options);
      const conversationId = Number(conversationMessageMatch[1]);
      const conversation = MOCK_CONVERSATIONS.find((item) => Number(item.id) === conversationId);
      if (!conversation) return mockResponse({ message: "Conversation not found" }, 404);
      const messages = MOCK_CONVERSATION_MESSAGES.get(conversationId) || [];
      const now = new Date().toISOString();
      const userMessage = {
        id: nextId([...MOCK_CONVERSATION_MESSAGES.values()].flat()),
        role: "user",
        messageType: "text",
        content: body.content,
        toolName: null,
        payload: null,
        createdAt: now
      };
      const assistantMessage = {
        id: userMessage.id + 1,
        role: "assistant",
        messageType: "text",
        content: "这是 AI 的演示回复。我会结合你选择的志愿表继续给出建议。",
        toolName: null,
        payload: null,
        createdAt: now
      };
      messages.push(userMessage, assistantMessage);
      MOCK_CONVERSATION_MESSAGES.set(conversationId, messages);
      Object.assign(conversation, { lastMessageAt: now, updatedAt: now, messageCount: messages.length });
      return mockResponse({
        conversationId,
        generatedMessages: [assistantMessage]
      });
    }

    // 管理员用户概览与账号管理
    if (path === "/api/admin/users/overview" && method === "GET") {
      return mockResponse({
        totalCount: MOCK_ADMIN_USERS.length,
        userCount: MOCK_ADMIN_USERS.filter((item) => item.role === "USER").length,
        adminCount: MOCK_ADMIN_USERS.filter((item) => item.role === "ADMIN").length,
        disabledCount: MOCK_ADMIN_USERS.filter((item) => item.enabled === false).length
      });
    }

    if (path === "/api/admin/users" && method === "GET") {
      const keyword = searchParams.get("keyword")?.trim().toLowerCase();
      const role = searchParams.get("role");
      const enabled = searchParams.get("enabled");
      const users = MOCK_ADMIN_USERS.filter((item) => {
        if (keyword && !item.username.toLowerCase().includes(keyword)) return false;
        if (role && item.role !== role) return false;
        if (enabled != null && String(item.enabled) !== enabled) return false;
        return true;
      });
      return mockResponse(users);
    }

    const adminUserDetailMatch = path.match(/^\/api\/admin\/users\/(\d+)$/);
    if (adminUserDetailMatch && method === "GET") {
      const user = MOCK_ADMIN_USERS.find((item) => Number(item.id) === Number(adminUserDetailMatch[1]));
      return user ? mockResponse(user) : mockResponse({ message: "User not found" }, 404);
    }

    const adminUserSettingsMatch = path.match(/^\/api\/admin\/users\/(\d+)\/settings$/);
    if (adminUserSettingsMatch && method === "PUT") {
      const user = MOCK_ADMIN_USERS.find((item) => Number(item.id) === Number(adminUserSettingsMatch[1]));
      if (!user) return mockResponse({ message: "User not found" }, 404);
      const body = readJsonBody(options);
      if (user.username === MOCK_ADMIN_USER.username && (body.role !== "ADMIN" || body.enabled !== true)) {
        return mockResponse({ message: "当前管理员不能停用或降级自己的账号" }, 400);
      }
      Object.assign(user, { role: body.role, enabled: body.enabled, updatedAt: new Date().toISOString() });
      return mockResponse(user);
    }

    // 管理员基础数据：查询、新增和编辑均只修改当前页面会话内存
    if (path === "/api/admin/universities" && method === "GET") {
      return mockResponse(MOCK_ADMIN_UNIVERSITIES);
    }
    if (path === "/api/admin/universities" && method === "POST") {
      return mockResponse(saveAdminRecord(MOCK_ADMIN_UNIVERSITIES, readJsonBody(options)));
    }
    const adminUniversityMatch = path.match(/^\/api\/admin\/universities\/(\d+)$/);
    if (adminUniversityMatch && method === "PUT") {
      const saved = saveAdminRecord(MOCK_ADMIN_UNIVERSITIES, readJsonBody(options), adminUniversityMatch[1]);
      return saved ? mockResponse(saved) : mockResponse({ message: "University not found" }, 404);
    }

    if (path === "/api/admin/majors" && method === "GET") {
      return mockResponse(MOCK_ADMIN_MAJORS);
    }
    if (path === "/api/admin/majors" && method === "POST") {
      return mockResponse(saveAdminRecord(MOCK_ADMIN_MAJORS, readJsonBody(options)));
    }
    const adminMajorMatch = path.match(/^\/api\/admin\/majors\/(\d+)$/);
    if (adminMajorMatch && method === "PUT") {
      const saved = saveAdminRecord(MOCK_ADMIN_MAJORS, readJsonBody(options), adminMajorMatch[1]);
      return saved ? mockResponse(saved) : mockResponse({ message: "Major not found" }, 404);
    }

    if (path === "/api/admin/admission-cutoffs" && method === "GET") {
      return mockResponse(filterAdmissionRows(MOCK_ADMIN_CUTOFFS, searchParams));
    }
    if (path === "/api/admin/admission-cutoffs" && method === "POST") {
      return mockResponse(saveAdminRecord(MOCK_ADMIN_CUTOFFS, readJsonBody(options)));
    }
    const adminCutoffMatch = path.match(/^\/api\/admin\/admission-cutoffs\/(\d+)$/);
    if (adminCutoffMatch && method === "PUT") {
      const saved = saveAdminRecord(MOCK_ADMIN_CUTOFFS, readJsonBody(options), adminCutoffMatch[1]);
      return saved ? mockResponse(saved) : mockResponse({ message: "Admission cutoff not found" }, 404);
    }

    if (path === "/api/admin/major-admission-cutoffs" && method === "GET") {
      return mockResponse(filterAdmissionRows(MOCK_ADMIN_MAJOR_CUTOFFS, searchParams, true));
    }
    if (path === "/api/admin/major-admission-cutoffs" && method === "POST") {
      return mockResponse(saveAdminRecord(MOCK_ADMIN_MAJOR_CUTOFFS, readJsonBody(options)));
    }
    const adminMajorCutoffMatch = path.match(/^\/api\/admin\/major-admission-cutoffs\/(\d+)$/);
    if (adminMajorCutoffMatch && method === "PUT") {
      const saved = saveAdminRecord(MOCK_ADMIN_MAJOR_CUTOFFS, readJsonBody(options), adminMajorCutoffMatch[1]);
      return saved ? mockResponse(saved) : mockResponse({ message: "Major admission cutoff not found" }, 404);
    }

    // 默认返回404
    return mockResponse({ error: "Not Found" }, 404);
  };
}

function mockResponse(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "Content-Type": "application/json"
    }
  });
}

// 检查是否启用 Mock 模式
export function isMockMode() {
  return import.meta.env.VITE_MOCK === "true";
}
