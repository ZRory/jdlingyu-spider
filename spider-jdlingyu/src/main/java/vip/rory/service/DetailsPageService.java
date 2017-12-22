package vip.rory.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import vip.rory.entity.DetailsPage;
import vip.rory.entity.MainImageSet;
import vip.rory.queue.DetailsPageQueue;
import vip.rory.queue.MainImageSetQueue;
import vip.rory.queue.Queue;
import vip.rory.util.HttpClientUtil;
import vip.rory.util.ResolveUtil;

/**
 * 套图详情 任务类 url ：http://www.jdlingyu.fun/xxxxx/
 */
public class DetailsPageService implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(DetailsPageService.class);

	// 队列操作接口--取
	private Queue<MainImageSet> requestQueue;
	// 队列操作接口--存
	private Queue<DetailsPage> responseQueue;
	// 成功获取套图数统计
	private AtomicInteger successImageSetCount;
	// DetailsPage 门闩
	private CountDownLatch detailsPageLatch;

	/**
	 * @param queueOne
	 * @param queueTwo
	 * @param successImageSetCount
	 * @param detailsPageLatch2
	 */
	public DetailsPageService(MainImageSetQueue requestQueue, DetailsPageQueue responseQueue,
			AtomicInteger successImageSetCount, CountDownLatch detailsPageLatch) {
		this.requestQueue = requestQueue;
		this.responseQueue = responseQueue;
		this.successImageSetCount = successImageSetCount;
		this.detailsPageLatch = detailsPageLatch;

	}

	@Override
	public void run() {

		HttpClientUtil httpClientUtil = HttpClientUtil.getInstance();
		MainImageSet mainImageSet = null;
		try {

			try {
				mainImageSet = requestQueue.take();
				if (mainImageSet == null) {
					return;
				}
			} catch (Exception e) {
				LOGGER.error("DetailsPageService 从队列获取数据失败！");
			}

			LOGGER.info("DetailsPageService-2:" + "正在爬取的网站:" + mainImageSet.getUrl());
			String html = httpClientUtil.sendGetRequestForHtml(mainImageSet.getUrl());
			if (StringUtils.isEmpty(html)) {
				throw new Exception("html string 为空");
			}

			// 开始解析html数据
			ResolveUtil resolveUtil = ResolveUtil.getInstance();
			DetailsPage detailsPage = new DetailsPage();
			// 设置url
			detailsPage.setUrl(mainImageSet.getUrl());
			// 提取出id
			detailsPage.setId(Integer.valueOf(mainImageSet.getUrl().split("/")[3]));
			Element bodyElement = resolveUtil.getElement(html, "body");
			// 提取出标题
			Element titleElement = bodyElement.selectFirst("h2.main-title");
			detailsPage.setTitle(titleElement.html());
			// 设置图片
			Elements imageElements = bodyElement.select("div.main-body > p > a");
			List<String> images = new ArrayList<>();
			for (Element tempElement : imageElements) {
				images.add(tempElement.attr("href"));
			}
			detailsPage.setImages(images);
			// 入队
			responseQueue.put(detailsPage);
			// 成功套图数+1
			successImageSetCount.incrementAndGet();
			LOGGER.info("DetailsPageService-2:" + "成功解析出套图:" + detailsPage.getTitle());

		} catch (Exception e) {
			LOGGER.error("MainImageTask 获取/解析HTML失败");
			e.printStackTrace();
		} finally {
			// DetailsPage 门闩 - 1
			detailsPageLatch.countDown();
		}

	}
}
