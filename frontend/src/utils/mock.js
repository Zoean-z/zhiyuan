// Mock 数据 - 用于无后端的演示模式

const MOCK_USER = {
  id: 1,
  username: "testuser",
  score: 630,
  subjectType: "PHYSICS",
  examProvince: "浙江",
  token: "mock-token-12345"
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
    queryContent: "分数：630，省份：浙江，科类：物理",
    createdAt: "2026-08-07T09:30:00"
  },
  {
    id: 2,
    queryType: "text",
    queryContent: "我想学计算机专业，分数630，浙江考生",
    createdAt: "2026-08-06T14:20:00"
  },
  {
    id: 3,
    queryType: "score",
    queryContent: "分数：600，省份：浙江，科类：物理",
    createdAt: "2026-03-20T10:15:00"
  }
];

const MOCK_CONVERSATIONS = [
  {
    id: 1,
    title: "关于志愿填报的咨询",
    createdAt: "2026-08-07T08:00:00"
  },
  {
    id: 2,
    title: "推荐计算机专业院校",
    createdAt: "2026-08-06T15:30:00"
  }
];

// Mock API 拦截器
export function setupMockInterceptor() {
  const originalFetch = window.fetch;

  window.fetch = async function(url, options = {}) {
    if (!url.toString().startsWith("/api")) {
      return originalFetch.call(this, url, options);
    }

    // 模拟网络延迟
    await new Promise(resolve => setTimeout(resolve, 300 + Math.random() * 200));

    const method = options.method || "GET";
    const path = url.toString().replace("http://localhost:8080", "");

    // 登录
    if (path === "/api/auth/login" && method === "POST") {
      const body = JSON.parse(options.body);
      if (body.username === "admin" && body.password === "admin123") {
        return mockResponse({ ...MOCK_USER, role: "ADMIN" });
      }
      return mockResponse(MOCK_USER);
    }

    // 注册
    if (path === "/api/auth/register" && method === "POST") {
      return mockResponse(MOCK_USER);
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
      return mockResponse(MOCK_MAJORS.slice(0, 10));
    }

    // 推荐查询
    if (path === "/api/recommendations" && method === "POST") {
      return mockResponse(generateRecommendations(15));
    }

    // 自由文本推荐
    if (path === "/api/recommendations/free-text" && method === "POST") {
      return mockResponse(generateRecommendations(10));
    }

    // 获取历史记录
    if (path === "/api/history" && method === "GET") {
      return mockResponse(MOCK_HISTORY);
    }

    // 获取历史详情
    if (path.match(/^\/api\/history\/\d+$/) && method === "GET") {
      return mockResponse({
        ...MOCK_HISTORY[0],
        resultJson: JSON.stringify(generateRecommendations(5))
      });
    }

    // 获取方案列表
    if (path === "/api/plans" && method === "GET") {
      return mockResponse(MOCK_PLANS);
    }

    // 获取方案详情
    if (path.match(/^\/api\/plans\/\d+$/) && method === "GET") {
      const id = parseInt(path.split("/").pop());
      const plan = MOCK_PLANS.find(p => p.id === id) || MOCK_PLANS[0];
      return mockResponse(plan);
    }

    // 保存方案
    if (path === "/api/plans" && method === "POST") {
      const body = JSON.parse(options.body);
      return mockResponse({ id: Date.now(), ...body, createdAt: new Date().toISOString() });
    }

    // 更新方案
    if (path.match(/^\/api\/plans\/\d+$/) && method === "PUT") {
      const body = JSON.parse(options.body);
      return mockResponse({ id: parseInt(path.split("/").pop()), ...body });
    }

    // 删除方案
    if (path.match(/^\/api\/plans\/\d+$/) && method === "DELETE") {
      return mockResponse({ success: true });
    }

    // 获取对话列表
    if (path === "/api/agent/conversations" && method === "GET") {
      return mockResponse(MOCK_CONVERSATIONS);
    }

    // 创建对话
    if (path === "/api/agent/conversations" && method === "POST") {
      return mockResponse({ id: Date.now(), title: "新对话", createdAt: new Date().toISOString() });
    }

    // 发送消息
    if (path.match(/^\/api\/agent\/conversations\/\d+\/messages$/) && method === "POST") {
      const body = JSON.parse(options.body);
      return mockResponse({
        conversationId: parseInt(path.split("/")[3]),
        messages: [
          { role: "user", content: body.content, type: "text" },
          { role: "assistant", content: "这是AI的回复。由于是演示模式，这里显示的是模拟数据。", type: "text" }
        ]
      });
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
