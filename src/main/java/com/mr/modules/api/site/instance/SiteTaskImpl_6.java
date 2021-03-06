package com.mr.modules.api.site.instance;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mr.common.OCRUtil;
import com.mr.common.util.SpringUtils;
import com.mr.modules.api.model.FinanceMonitorPunish;
import com.mr.modules.api.site.SiteTaskExtend;
import io.jsonwebtoken.lang.Assert;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * Created by feng on 18-3-16
 * 上交所
 * 交易监管
 */

@Slf4j
@Component("site6")
@Scope("prototype")
public class SiteTaskImpl_6 extends SiteTaskExtend {

	protected OCRUtil ocrUtil = SpringUtils.getBean(OCRUtil.class);

	ArrayList<String> filterTags = Lists.newArrayList("<SPAN>",
			"<br />",
			" &nbsp;",
			"<p align=\"center\">",
			"</strong>",
			"<strong>",
			"</p>",
			" ");

	/**
	 * @return ""或者null为成功， 其它为失败
	 * @throws Throwable
	 */
	@Override
	protected String execute() throws Throwable {
		log.info("*******************call site6 task********************");

		/**
		 * get请求
		 * 1.监管类型：纪律处分
		 * 2.处理事由：
		 */

		String targetUri = "http://www.sse.com.cn/disclosure/credibility/regulatory/punishment/";
		String pageTxt = getData(targetUri);
		Document doc = Jsoup.parse(pageTxt);
		String pageCount = doc.getElementById("createPage").attr("PAGE_COUNT");
		log.info("pageCount:" + pageCount);
		List<FinanceMonitorPunish> lists = extract(pageCount);
		if (!CollectionUtils.isEmpty(lists)) {
			exportToXls("site6.xlsx", lists);
		}

		return null;

	}

	@Override
	protected String executeOne() throws Throwable {
		log.info("*******************call site6 task for One Record**************");

		Assert.notNull(oneFinanceMonitorPunish.getPunishTitle());
		Assert.notNull(oneFinanceMonitorPunish.getPunishDate());
		Assert.notNull(oneFinanceMonitorPunish.getSource());
		oneFinanceMonitorPunish.setSupervisionType("纪律处分");
		oneFinanceMonitorPunish.setObject("上交所-纪律处分");

		initDate();
		doFetch(oneFinanceMonitorPunish, true);
		return null;
	}

	/**
	 * 提取所需信息
	 * 处分对象、处分对象类型、函号、函件标题、发函日期、涉及债券
	 */
	private List<FinanceMonitorPunish> extract(String pageCount) throws Exception {
		List<FinanceMonitorPunish> lists = Lists.newLinkedList();

		java.util.Map<String, String> requestParams = Maps.newHashMap();
		Map<String, String> headParams = Maps.newHashMap();
		headParams.put("Referer", "http://www.szse.cn/main/disclosure/zqxx/jlcf/");
		headParams.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36");

		for (int pageNo = 1; pageNo <= Integer.parseInt(pageCount); pageNo++) {
			String url = String.format("http://www.sse.com.cn" +
					"/disclosure/credibility/regulatory/punishment/s_index_%d.htm", pageNo);
			if (pageNo == 1)
				url = "http://www.sse.com.cn/disclosure/credibility/regulatory/punishment/";
			String fullTxt = postData(url, requestParams, headParams);
			Document doc = Jsoup.parse(fullTxt);
			Elements dds = doc.getElementsByClass("sse_list_1 js_listPage").get(0)
					.getElementsByTag("dd");
			for (Element dd : dds) {
				FinanceMonitorPunish financeMonitorPunish = new FinanceMonitorPunish();
				Element aElement = dd.getElementsByTag("a").first();
				String title = aElement.attr("title");
				String docUrl = "http://www.sse.com.cn" + aElement.attr("href");
				String punishDate = dd.getElementsByTag("span").first().text();

				financeMonitorPunish.setPunishTitle(title);
				financeMonitorPunish.setPunishDate(punishDate);
				financeMonitorPunish.setSource(docUrl);
				financeMonitorPunish.setSupervisionType("纪律处分");
				financeMonitorPunish.setObject("上交所-纪律处分");

				//增量抓取
				if (!doFetch(financeMonitorPunish, false)) {
					return lists;
				}

				lists.add(financeMonitorPunish);
			}
		}
		return lists;
	}

	/**
	 * 抓取并解析单条数据
	 *
	 * @param financeMonitorPunish
	 * @param isForce
	 */
	private boolean doFetch(FinanceMonitorPunish financeMonitorPunish,
							Boolean isForce) throws Exception {
		String docUrl = financeMonitorPunish.getSource();
		String fullTxt = "";

		if (docUrl.endsWith("doc") || docUrl.endsWith("docx")) {
			fullTxt = ocrUtil.getTextFromDoc(downLoadFile(docUrl));
		} else if (docUrl.endsWith("shtml")) {
			Document pDoc = Jsoup.parse(getData(docUrl));
			Element allZoomDiv = pDoc.getElementsByClass("allZoom").get(0);
			fullTxt = allZoomDiv.text();
		}
		extractTxt(fullTxt, financeMonitorPunish);
		return saveOne(financeMonitorPunish, isForce);
	}

	/**
	 * 提取所需要的信息
	 * 处罚文号、处罚对象、处理事由
	 */
	private void extractTxt(String fullTxt, FinanceMonitorPunish financeMonitorPunish) {
		log.debug(financeMonitorPunish.getSource());
		//处罚文号
		String punishNo = "";
		//当事人
		String person = "";
		//处理事由
		String violation = "";

		{
			String tmp = fullTxt;
			if (tmp.indexOf("20") < tmp.indexOf("号")) {
				tmp = tmp.substring(tmp.indexOf("20") - 1, tmp.indexOf("号") + 1)
						.replace("\n", "")
						.replace(" ", "");
				if (tmp.length() >= 5 && tmp.length() < 10) {
					punishNo = tmp;
					financeMonitorPunish.setPunishNo(punishNo);
				}

			}
		}

		int sIndx = fullTxt.indexOf("当事人：") == -1 ?
				fullTxt.indexOf("当事人") : fullTxt.indexOf("当事人：");

		//经查明关键字
		String pString = "";
		int pIndx = -1;
		String[] p = {"经查明，", "经查明", "经审核，", "经查，", "经核查，"};

		for (int i = 0; i < p.length; i++) {
			if (fullTxt.indexOf(p[i]) > -1) {
				pString = p[i];
				pIndx = fullTxt.indexOf(p[i]);
				break;
			}
		}


		if (pIndx < 0) {
			log.error("文本格式不规则，无法识别");
			return;
		}

		if (sIndx >= 0) {
			person = fullTxt.substring(sIndx, pIndx).replace("当事人：", "")
					.replace("当事人", "");
		} else {
			if (fullTxt.indexOf("的决定") > 0 && fullTxt.indexOf("的决定") < pIndx) {
				person = fullTxt.substring(fullTxt.indexOf("的决定"), pIndx);
			}
		}
		financeMonitorPunish.setPartyPerson(filterErrInfo(person));

		{
			String tmp = fullTxt.substring(pIndx);
			if (tmp.lastIndexOf("上海证券交易所") > pIndx) {
				violation = tmp.substring(4, tmp.lastIndexOf("上海证券交易所"));
			} else {
				violation = tmp.substring(4);
			}
		}

		if (StringUtils.isEmpty(violation)) {
			log.error("内容不规则 URL:" + financeMonitorPunish.getSource());
			return;
		}
		financeMonitorPunish.setIrregularities(filterErrInfo(violation));
	}

}
