export const UI_TEXT = {
  common: {
    requestFailed: "请求失败，请稍后重试",
    operationFailed: "操作失败，请稍后重试",
    networkError: "网络连接异常，请检查网络后重试",
    timeout: "请求超时，请稍后重试",
    unknownError: "系统开小差了，请稍后重试",
    loginRequired: "登录状态已失效，请重新登录",
    noDisplayContent: "暂无可展示内容"
  },
  form: {
    usernameRequired: "请输入用户名",
    passwordRequired: "请输入密码",
    scoreRequired: "请输入分数",
    provinceRequired: "请选择省份",
    subjectTypeRequired: "请选择科类",
    registerScoreRequired: "注册时请填写分数",
    registerProvinceRequired: "注册时请选择省份",
    registerSubjectTypeRequired: "注册时请选择科类",
    recommendationModeRequired: "请选择推荐模式",
    majorRequired: "请输入专业名称",
    requirementTextRequired: "请输入需求描述",
    planNameRequired: "请输入方案名称",
    currentPlanEmpty: "当前方案为空，请先加入条目",
    selectMajorRequired: "请先选择专业"
  },
  success: {
    login: "登录成功",
    register: "注册成功，已自动登录",
    addToPlan: "加入方案成功",
    removeFromPlan: "已从当前方案移除",
    clearCurrentPlan: "当前方案已清空",
    savePlan: "保存方案成功",
    deleteHistory: "删除历史记录成功",
    deletePlan: "删除方案成功"
  },
  failure: {
    register: "注册失败，请稍后重试",
    savePlan: "保存方案失败，请稍后重试",
    deleteHistory: "删除历史记录失败，请稍后重试",
    deletePlan: "删除方案失败，请稍后重试",
    loadHistory: "加载历史记录失败，请稍后重试",
    loadPlans: "加载志愿方案失败，请稍后重试",
    loadHistoryDetail: "加载历史结果失败，请稍后重试",
    loadPlanDetail: "加载方案详情失败，请稍后重试",
    loadSchoolDetail: "加载学校专业失败，请稍后重试",
    queryRecommendation: "查询推荐结果失败，请稍后重试",
    queryFreeText: "文本推荐失败，请稍后重试",
    login: "登录失败，请稍后重试"
  },
  empty: {
    history: "暂无历史记录",
    plans: "暂无志愿方案",
    currentPlan: "当前方案为空，请从右侧推荐结果中加入条目",
    aiSummary: "暂无 AI 总结",
    recommendation: "暂无推荐结果",
    noRush: "暂无冲刺结果",
    noSafe: "暂无稳妥结果",
    noGuarantee: "暂无保底结果"
  }
};

const STATUS_MESSAGE_MAP = {
  400: "请求参数有误",
  401: UI_TEXT.common.loginRequired,
  403: "无权限执行该操作",
  404: "请求资源不存在",
  408: UI_TEXT.common.timeout,
  500: "服务器异常，请稍后重试",
  502: "服务暂时不可用，请稍后重试",
  503: "服务暂时不可用，请稍后重试",
  504: UI_TEXT.common.timeout
};

const KNOWN_MESSAGE_MAPPINGS = [
  [/network error|failed to fetch|load failed/i, UI_TEXT.common.networkError],
  [/timeout|aborted|aborterror|timed out/i, UI_TEXT.common.timeout],
  [/score is required at first login/i, "首次登录请补充分数"],
  [/subject type is required at first login/i, "首次登录请补充科类"],
  [/exam province is required at first login/i, "首次登录请选择省份"],
  [/majorkeyword is required/i, UI_TEXT.form.majorRequired],
  [/subjecttype is required/i, UI_TEXT.form.subjectTypeRequired],
  [/score is required/i, UI_TEXT.form.scoreRequired],
  [/examprovince is required|province is required/i, UI_TEXT.form.provinceRequired],
  [/please login|unauthorized|login required/i, UI_TEXT.common.loginRequired],
  [/request failed|internal server error/i, UI_TEXT.common.requestFailed],
  [/cannot read properties of undefined|undefined|null/i, UI_TEXT.common.unknownError]
];

function extractRawMessage(error) {
  if (!error) {
    return "";
  }
  if (typeof error === "string") {
    return error.trim();
  }
  if (typeof error?.message === "string") {
    return error.message.trim();
  }
  if (typeof error?.data?.message === "string") {
    return error.data.message.trim();
  }
  return "";
}

function hasChinese(text) {
  return /[\u4e00-\u9fa5]/.test(text);
}

function isTechnicalMessage(text) {
  return /cannot read|undefined|null|internal server error|network error|failed to fetch|request failed/i.test(text);
}

function mapKnownMessage(text) {
  for (const [pattern, message] of KNOWN_MESSAGE_MAPPINGS) {
    if (pattern.test(text)) {
      return message;
    }
  }
  return "";
}

export function createHttpError(response, data, fallbackMessage = "") {
  const error = new Error(
    (data && typeof data.message === "string" && data.message.trim()) || fallbackMessage || `HTTP_${response.status}`
  );
  error.status = response.status;
  error.data = data;
  return error;
}

export function normalizeUserError(error, fallbackMessage = UI_TEXT.common.operationFailed) {
  console.error("[ui-error]", error);
  const status = error?.status;
  const rawMessage = extractRawMessage(error);
  const knownMessage = rawMessage ? mapKnownMessage(rawMessage) : "";
  if (knownMessage) {
    return knownMessage;
  }
  if (status && STATUS_MESSAGE_MAP[status]) {
    return STATUS_MESSAGE_MAP[status];
  }
  if (!rawMessage) {
    return fallbackMessage;
  }
  if (hasChinese(rawMessage) && !isTechnicalMessage(rawMessage)) {
    return rawMessage;
  }
  return fallbackMessage;
}
