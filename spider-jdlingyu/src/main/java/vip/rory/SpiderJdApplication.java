package vip.rory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import vip.rory.queue.DetailsPageQueue;
import vip.rory.queue.MainImageSetQueue;
import vip.rory.service.DetailsPageService;
import vip.rory.service.MainPageService;
import vip.rory.service.SaveImageService;

@SpringBootApplication
public class SpiderJdApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(SpiderJdApplication.class);

	// 任务队列
	static MainImageSetQueue queueOne = new MainImageSetQueue();
	static DetailsPageQueue queueTwo = new DetailsPageQueue();

	// MainPage 门闩(页面数)
	public static CountDownLatch mainPageLatch = new CountDownLatch(352);
	// DetailsPage 门闩
	public static CountDownLatch detailsPageLatch = null;
	// saveImage 门闩决定程序退出
	public static CountDownLatch saveImageLatch = null;

	// 套图统计
	public static AtomicInteger imageSetCount = new AtomicInteger(0);
	// 套图统计
	public static AtomicInteger successImageSetCount = new AtomicInteger(0);

	private static ExecutorService mainPageServicePool = null;

	private static ExecutorService detailsPageServicePool = null;

	private static ExecutorService saveImageServicePool = null;

	public static void main(String[] args) throws InterruptedException {

		SpringApplication.run(SpiderJdApplication.class, args);

		// 测试
		// MainPageService mainPageService = new MainPageService(queueOne, 1,
		// imageSetCount, mainPageLatch);
		// mainPageService.run();
		// detailsPageLatch = new CountDownLatch(imageSetCount.get());
		// DetailsPageService detailsPageService = new DetailsPageService(queueOne,
		// queueTwo, successImageSetCount, detailsPageLatch);
		// detailsPageService.run();
		// saveImageLatch = new CountDownLatch(successImageSetCount.get());
		// SaveImageService saveImageService = new SaveImageService(queueTwo,
		// saveImageLatch);
		// saveImageService.run();

		Thread mainPageThread = new Thread(new Runnable() {
			@Override
			public void run() {
				mainPageServicePool = Executors.newFixedThreadPool(12);
				for (int i = 1; i <= 352; i++) {
					mainPageServicePool.submit(new MainPageService(queueOne, i, imageSetCount, mainPageLatch));
				}
				mainPageServicePool.shutdown();
			}
		});

		Thread detailsPageThread = new Thread(new Runnable() {
			@Override
			public void run() {
				detailsPageServicePool = Executors.newFixedThreadPool(12);
				while (imageSetCount.getAndDecrement() > 0) {
					detailsPageServicePool
							.submit(new DetailsPageService(queueOne, queueTwo, successImageSetCount, detailsPageLatch));
				}
				detailsPageServicePool.shutdown();
			}
		});

		Thread saveImageThread = new Thread(new Runnable() {
			@Override
			public void run() {
				saveImageServicePool = Executors.newFixedThreadPool(6);
				while (successImageSetCount.getAndDecrement() > 0) {
					saveImageServicePool.submit(new SaveImageService(queueTwo, saveImageLatch));
				}
				saveImageServicePool.shutdown();
			}
		});

		// 开始正式启动爬虫程序
		mainPageThread.start();
		// MainPage 门闩(页面数)等待
		mainPageLatch.await();
		// 根据套图数初始化DetailsPage 门闩
		detailsPageLatch = new CountDownLatch(imageSetCount.get());
		// 启动二级处理
		detailsPageThread.start();
		// DetailsPage 门闩等待
		detailsPageLatch.await();
		// 根据成功套图数初始化saveImage 门闩
		saveImageLatch = new CountDownLatch(successImageSetCount.get());
		// 启动图片流存储
		saveImageThread.start();
		// saveImage 门闩等待
		saveImageLatch.await();
		LOGGER.info("----------------------|套图存储完毕|----------------------");
		LOGGER.info("----------------------|程序即将退出|----------------------");
	}

}
