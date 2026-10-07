package com.sa.event_mng.shared.infrastructure.async;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Chạy các việc phụ (hoàn tất thanh toán, tạo PDF, gửi mail) sau khi xử lý request.
 *
 * app.async.enabled=true (mặc định): chạy trên thread nền, request trả về ngay.
 * app.async.enabled=false: chạy luôn trên thread của request. Dùng cho Cloud Run request-based billing,
 * vì ở đó CPU bị bóp ngay khi response đã trả về và instance có thể tắt, việc chạy nền sẽ bị treo hoặc mất.
 */
@Component
public class BackgroundTaskRunner {

    private final boolean async;

    public BackgroundTaskRunner(@Value("${app.async.enabled:true}") boolean async) {
        this.async = async;
    }

    public void run(Runnable task) {
        if (async) {
            CompletableFuture.runAsync(task);
        } else {
            task.run();
        }
    }

    /** Như run(), nhưng đợi transaction hiện tại commit xong mới chạy (không gửi mail cho dữ liệu bị rollback). */
    public void runAfterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            run(task);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                run(task);
            }
        });
    }
}
