import { MAJORS, SCHOOLS } from "./publicData.js";

const EMPLOYMENT_BY_CATEGORY = {
  工学: ["研发工程师", "技术工程师", "产品经理", "科研人员"],
  医学: ["医疗机构", "医学科研", "公共卫生", "健康管理"],
  法学: ["律师", "法务专员", "公务员", "合规审查"],
  文学: ["教育", "编辑出版", "内容运营", "公共文化"],
  经济学: ["金融机构", "经济分析", "风险管理", "公共部门"],
  管理学: ["企业管理", "财务分析", "市场运营", "公共管理"]
};

export const MOCK_EXPLORE_MAJORS = MAJORS.map((major, index) => ({
  ...major,
  subcategory: major.sub,
  genderRatio: ["71:29", "72:28", "74:26", "68:32", "70:30", "76:24"][index % 6],
  averageSalary: `${(8.4 + (index % 8) * 0.7).toFixed(1)}万`,
  popularity: index + 1,
  offeringSchoolCount: SCHOOLS.length
}));

export function mockMajorDetail(code) {
  const major = MOCK_EXPLORE_MAJORS.find((item) => item.code === String(code));
  if (!major) return null;
  return {
    major,
    description: `${major.name}专业属于${major.category}门类下的${major.subcategory}，主要培养掌握相关基础理论与专业技能、能够解决实际问题的复合型人才。`,
    employmentDirections: EMPLOYMENT_BY_CATEGORY[major.category] || ["教育科研", "企事业单位", "公共服务", "继续深造"],
    demoData: true
  };
}

export const MOCK_OFFERING_SCHOOLS = SCHOOLS.map((school) => ({
  ...school,
  logoId: school.id
}));
