package com.mr.modules.api.site.instance;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mr.common.OCRUtil;
import com.mr.common.util.SpringUtils;
import com.mr.modules.api.model.FinanceMonitorPunish;
import com.mr.modules.api.site.SiteTaskExtend;
import io.jsonwebtoken.lang.Assert;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by feng on 18-3-16
 * 深交所
 * 债券纪律处分
 */

@Slf4j
@Component("site9")
@Scope("prototype")
public class SiteTaskImpl_9 extends SiteTaskExtend {

	protected OCRUtil ocrUtil = SpringUtils.getBean(OCRUtil.class);

	/**
	 * @return ""或者null为成功， 其它为失败
	 * @throws Throwable
	 */
	@Override
	protected String execute() throws Throwable {
		log.info("*******************call site9 task**************");

		//download pdf
		//pdf转img
		//img提取文本
		String targetUri = "http://www.szse.cn/main/disclosure/zqxx/jlcf/";
		String fullTxt = getData(targetUri, "GB2312");
		String fltxt = fullTxt.substring(fullTxt.indexOf("当前第1页  共")).replace("当前第1页  共", "");
		String pageCount = fltxt.substring(0, fltxt.indexOf("页"));
		log.info("pageCount:" + pageCount);
		List<FinanceMonitorPunish> lists = extract(pageCount);

		if (!CollectionUtils.isEmpty(lists)) {
			exportToXls("site9.xlsx", lists);
		}
		return null;
	}

	@Override
	protected String executeOne() throws Throwable {
		log.info("*******************call site9 task for One Record**************");

		Assert.notNull(oneFinanceMonitorPunish.getPartyInstitution());
		Assert.notNull(oneFinanceMonitorPunish.getPunishCategory());
		Assert.notNull(oneFinanceMonitorPunish.getPunishNo());
		Assert.notNull(oneFinanceMonitorPunish.getPunishTitle());
		Assert.notNull(oneFinanceMonitorPunish.getPublishDate());
		Assert.notNull(oneFinanceMonitorPunish.getRelatedBond());
		Assert.notNull(oneFinanceMonitorPunish.getSource());

		oneFinanceMonitorPunish.setObject("深交所-中介机构处罚与处分记录-债券信息");
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

		String url = "http://www.szse.cn/szseWeb/FrontController.szse?randnum=0.8706381259752185";
		java.util.Map<String, String> requestParams = Maps.newHashMap();
		requestParams.put("ACTIONID", "7");
		requestParams.put("AJAX", "AJAX-TRUE");
		requestParams.put("CATALOGID", "ZQ_JLCF");
		requestParams.put("TABKEY", "tab1");
		requestParams.put("tab1PAGECOUNT", pageCount);
		requestParams.put("tab1RECORDCOUNT", "12");
		requestParams.put("REPORT_ACTION", "navigate");

		Map<String, String> headParams = Maps.newHashMap();
		headParams.put("Referer", "http://www.szse.cn/main/disclosure/zqxx/jlcf/");
		headParams.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36");

		for (int pageNo = 1; pageNo <= Integer.parseInt(pageCount); pageNo++) {
			requestParams.put("tab1PAGENO", String.valueOf(pageNo));
			String bodyStr = postData(url, requestParams, headParams);
			Document doc = Jsoup.parse(bodyStr);
			Element tableElement = doc.getElementsByClass("cls-data-table-common cls-data-table").get(0);
			Elements trElements = tableElement.getElementsByTag("tr");
			for (int tr = 1; tr < trElements.size(); tr++) {
				Elements tdElements = trElements.get(tr).getElementsByTag("td");
				String punishObj = tdElements.get(0).text();    //处分对象
				String objType = tdElements.get(1).text();        //处分对象类型
				String pCode = tdElements.get(2).text();        //函号
				String title = tdElements.get(3).getElementsByTag("a").text();

				String fileName = tdElements.get(3).getElementsByTag("a")
						.attr("onclick")
						.replace("window.open('/UpFiles/zqjghj/'+encodeURIComponent('", "")
						.replace("'))", "");
				String contentUri = "http://www.szse.cn/UpFiles/zqjghj/" + fileName;    //函件标题URI
				String pDate = tdElements.get(4).text();        //发函日期
				String pStock = tdElements.get(5).text();        //涉及债券

				FinanceMonitorPunish financeMonitorPunish = new FinanceMonitorPunish();
				financeMonitorPunish.setPartyInstitution(punishObj);
				financeMonitorPunish.setPartyCategory(objType);
				financeMonitorPunish.setPunishNo(pCode);
				financeMonitorPunish.setPunishTitle(title);
				financeMonitorPunish.setPublishDate(pDate);
				financeMonitorPunish.setRelatedBond(pStock);
				financeMonitorPunish.setSource(contentUri);
				financeMonitorPunish.setObject("深交所-中介机构处罚与处分记录-债券信息");

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
	 * map[fileName; punishObj; objType; pCode; title; pDate; pStock; contentUri]
	 *
	 * @param financeMonitorPunish
	 * @param isForce
	 */
	private boolean doFetch(FinanceMonitorPunish financeMonitorPunish,
							Boolean isForce) throws Exception {
		String contentUri = financeMonitorPunish.getSource();
		log.info(contentUri);
		String fileName = contentUri.substring(contentUri.lastIndexOf("/") + 1);
		String content = null;    //函件内容
		downLoadFile(contentUri, fileName);
		if (fileName.toLowerCase().endsWith("doc")) {
			content = ocrUtil.getTextFromDoc(fileName);
		} else if (fileName.toLowerCase().endsWith("pdf")) {
			content = ocrUtil.getTextFromPdf(fileName);
			if(!content.contains("深圳证券交易所")){
				downLoadFile(contentUri, fileName);
				content = ocrUtil.getTextFromImg(fileName);
			}
		}
		financeMonitorPunish.setDetails(filterErrInfo(content));

		return saveOne(financeMonitorPunish, isForce);
	}
}
