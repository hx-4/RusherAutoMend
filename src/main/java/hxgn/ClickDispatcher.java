package hxgn;

import org.rusherhack.client.api.utils.InventoryUtils;
import org.rusherhack.core.setting.NumberSetting;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public class ClickDispatcher {

    private final NumberSetting<Integer> delay;
    private final Deque<Runnable> queue = new ArrayDeque<>();
    private long lastClickTime = 0L;

    public ClickDispatcher(NumberSetting<Integer> delay) {
        this.delay = delay;
    }

    public void enqueueClick(int slotId, boolean shift) {
        queue.add(() -> InventoryUtils.clickSlot(slotId, shift));
    }

    public void enqueueSwap(int sourceIndex, int targetSlotId) {
        enqueueClick(sourceIndex, false);
        enqueueClick(targetSlotId, false);
        enqueueClick(sourceIndex, false);
    }

    public void drain() {
        if (!queue.isEmpty()) {
            long now = System.currentTimeMillis();
            if (now - lastClickTime >= delay.getValue()) {
                Objects.requireNonNull(queue.poll()).run();
                lastClickTime = now;
            }
        }
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void clear() {
        queue.clear();
    }
}
