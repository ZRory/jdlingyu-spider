package vip.rory.service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import vip.rory.entity.MainImageSet;
import vip.rory.queue.Queue;
import vip.rory.util.HttpClientUtil;
import vip.rory.util.ResolveUtil;

/**
 * 任务类--第一阶段--NO.1 爬取主页的每个分页的html详情页
 */
public class MainPageService implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MainPageService.class);

	// 队列操作接口
	private Queue<MainImageSet> queue;

	// 爬取的url,固定格式，再在最后加个页码即可
	private static final String URL = "http://www.jdlingyu.fun/page/";

	// 页码
	private Integer page;

	// 套图统计
	private AtomicInteger imageSetCount;

	private CountDownLatch MainPageLatch;

	public MainPageService(Queue<MainImageSet> queue, Integer page, AtomicInteger imageSetCount,
			CountDownLatch MainPageLatch) {
		this.queue = queue;
		this.page = page;
		this.imageSetCount = imageSetCount;
		this.MainPageLatch = MainPageLatch;
	}

	/**
	 * 执行线程，获取每个套图的URL
	 */
	@Override
	public void run() {

		/**
		 * 获取到网站的html string
		 */
		try {
			String url = URL + page;
			LOGGER.info("MainPageService-1:" + "正在爬取的网页:" + url);
			HttpClientUtil httpClientUtil = HttpClientUtil.getInstance();
			String html = httpClientUtil.sendGetRequestForHtml(url);
			if (StringUtils.isEmpty(html)) {
				throw new Exception("html string 为空!");
			}

			// 开始处理数据
			LOGGER.info("MainPageService-1:" + "正在解析网站数据:" + url);
			ResolveUtil resolveUtil = ResolveUtil.getInstance();
			Element element = resolveUtil.getElement(html, "div#postlist");
			if (element == null) {
				return;
			}
			Elements elements = element.select("div > div.pin-coat");
			for (Element tempElement : elements) {

				MainImageSet mainImageSet = new MainImageSet();

				// 把url 放入实体类
				Element urlElement = tempElement.select("a").first();
				mainImageSet.setUrl(urlElement.attr("href"));
				// 丢队列中
				queue.put(mainImageSet);

				// 套图统计+1
				imageSetCount.incrementAndGet();

			}

		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("MainPageService：" + "数据获取/处理失败!");
		} finally {
			// MainPage 门闩(页面数) - 1
			MainPageLatch.countDown();
		}

	}
}
