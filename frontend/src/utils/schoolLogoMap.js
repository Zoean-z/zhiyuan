/**
 * Current university names mapped to the legacy 1137-school logo resource ids.
 * Every entry below was matched against the named source emblem; schools without
 * a verified match intentionally fall back to the first-character placeholder.
 */
export const SCHOOL_LOGO_IDS = Object.freeze({
  清华大学: 21,
  北京大学: 22,
  浙江大学: 1,
  复旦大学: 24,
  上海交通大学: 25,
  南京大学: 26,
  中国科学技术大学: 27,
  中国人民大学: 41,
  北京航空航天大学: 34,
  同济大学: 33,
  武汉大学: 29,
  华中科技大学: 28,
  中山大学: 30,
  四川大学: 932,
  西安交通大学: 32,
  哈尔滨工业大学: 31,
  南开大学: 94,
  天津大学: 35,
  东南大学: 37,
  北京师范大学: 42,
  北京邮电大学: 44,
  北京交通大学: 46,
  北京科技大学: 43,
  中国政法大学: 66,
  中央财经大学: 65,
  对外经济贸易大学: 53,
  中国传媒大学: 64,
  上海财经大学: 340,
  上海外国语大学: 338,
  华东理工大学: 336,
  南京航空航天大学: 372,
  南京理工大学: 371,
  河海大学: 370,
  苏州大学: 375,
  合肥工业大学: 479,
  郑州大学: 626,
  华中师范大学: 677,
  中南财经政法大学: 678,
  湖南师范大学: 716,
  暨南大学: 751,
  湘潭大学: 717,
  长沙理工大学: 721,
  湖南农业大学: 718,
  中南林业科技大学: 724,
  湖南科技大学: 719,
  南华大学: 722,
  湖南中医药大学: 720,
  湖南工商大学: 725,
  湖南第一师范学院: 745,
  长沙学院: 738,
  深圳大学: 757,
  扬州大学: 405,
  江苏大学: 379,
  燕山大学: 122,
  浙江工业大学: 434,
  首都师范大学: 78,
  天津师范大学: 106,
  哈尔滨医科大学: 306,
  杭州电子科技大学: 3
});

export function schoolNameOf(school = {}) {
  return String(school.name || school.universityName || "").trim();
}

export function resolveSchoolLogoId(school = {}) {
  return SCHOOL_LOGO_IDS[schoolNameOf(school)] || null;
}
