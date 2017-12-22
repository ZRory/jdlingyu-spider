package vip.rory.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vip.rory.entity.DetailsPage;
import vip.rory.queue.DetailsPageQueue;
import vip.rory.queue.Queue;
import vip.rory.util.HttpClientUtil;

/**
 * 下载图片任务
 */
public class SaveImageService implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(SaveImageService.class);

	// 保存路径
	public static final String ROOT_PATH = "C:" + File.separator + "jdlingyu" + File.separator;

	// 取出 实体类
	private Queue<DetailsPage> requestDetails;

	// saveImage 门闩
	private CountDownLatch saveImageLatch;

	public SaveImageService(DetailsPageQueue requestDetails, CountDownLatch saveImageLatch) {
		this.requestDetails = requestDetails;
		this.saveImageLatch = saveImageLatch;
	}

	@Override
	public void run() {
		HttpClientUtil httpClientUtil = HttpClientUtil.getInstance();
		DetailsPage DetailsPage = null;
		try {
			try {
				DetailsPage = requestDetails.take();
				if (DetailsPage == null) {
					return;
				}
			} catch (Exception e) {
				LOGGER.error("SaveImageService-5,从队列获取数据失败！");
			}

			LOGGER.info("开始获取输入流：" + DetailsPage.toString());
			int num = 0;
			for (String tempUrl : DetailsPage.getImages()) {
				CloseableHttpResponse response = httpClientUtil.sendGetRequestForResponse(tempUrl);
				if (null == response) {
					throw new Exception("SaveImageService-5,没有获取到响应");
				}
				InputStream inputStream = null;
				try {
					inputStream = response.getEntity().getContent();
					num++;
					writer(inputStream, DetailsPage.getId(), DetailsPage.getTitle(), num);
					LOGGER.info("写入文件到本地成功！---id:" + DetailsPage.getId() + "---titel:" + DetailsPage.getTitle());
				} catch (IOException e) {
					LOGGER.error("WriteToLocalTask 写入异常！");
				} finally {
					httpClientUtil.closeResponseAndIn(inputStream, response);
				}
			}

		} catch (Exception e) {
			LOGGER.error("DownloadmageTask 获取输入流错误！" + e.getMessage());
			e.printStackTrace();
			try {
				requestDetails.put(DetailsPage);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		} finally {
			saveImageLatch.countDown();
		}

	}

	public void writer(InputStream inputStream, int id, String title, Integer num) throws Exception {
		File file = new File(ROOT_PATH + id + "_" + title + File.separator + num + ".jpg");
		FileUtils.copyToFile(inputStream, file);
	}
}
