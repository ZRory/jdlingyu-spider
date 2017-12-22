package vip.rory.queue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import vip.rory.entity.MainImageSet;

/**
 * 队列2
 * 保存每个套图的实体信息
 */
public class MainImageSetQueue implements Queue<MainImageSet>{

    private final LinkedBlockingQueue<MainImageSet> linkedBlockingQueue = new LinkedBlockingQueue<>();

    @Override
    public void put(MainImageSet mainImage) throws Exception {
        linkedBlockingQueue.offer(mainImage,10, TimeUnit.SECONDS);
    }

    @Override
    public MainImageSet take() throws Exception {
        return linkedBlockingQueue.poll(10,TimeUnit.SECONDS);
    }

    public int size(){
        return linkedBlockingQueue.size();
    }
}
