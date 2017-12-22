package vip.rory.queue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import vip.rory.entity.DetailsPage;

/**
 * 队列2
 * 保存每个套图的实体信息
 */
public class DetailsPageQueue implements Queue<DetailsPage>{

    private final LinkedBlockingQueue<DetailsPage> linkedBlockingQueue = new LinkedBlockingQueue<>();

    @Override
    public void put(DetailsPage imageDetail) throws Exception {
        linkedBlockingQueue.offer(imageDetail,10, TimeUnit.SECONDS);
    }

    @Override
    public DetailsPage take() throws Exception {
        return linkedBlockingQueue.poll(10,TimeUnit.SECONDS);
    }

    public int size(){
        return linkedBlockingQueue.size();
    }
}
