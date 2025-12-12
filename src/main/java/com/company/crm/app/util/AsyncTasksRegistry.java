package com.company.crm.app.util;

import io.jmix.flowui.asynctask.UiAsyncTasks.SupplierConfigurer;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class AsyncTasksRegistry {

    private final Map<String, CompletableFuture<?>> registry = new ConcurrentHashMap<>();

    public static AsyncTasksRegistry newInstance() {
        return new AsyncTasksRegistry();
    }

    /**
     * Places a new task into the registry, replacing any existing task associated with the same {@param id}.
     * <p>
     * The existing task, if present, is canceled and removed before the new task is added.
     * <p>
     * Additionally, the new task is registered for
     * automatic removal from the registry upon its completion.
     * <p>
     * @param id the unique identifier for the task to be placed in the registry
     * @param task the CompletableFuture instance representing the task to add
     */
    public void placeTask(String id, CompletableFuture<?> task) {
        cancelAndRemoveTask(id);
        registry.put(id, task);
        registerTaskRemoving(id, task);
    }

    /**
     * @see AsyncTasksRegistry#placeTask(String, CompletableFuture)
     */
    public CompletableFuture<Void> placeTask(String id, SupplierConfigurer<?> supplierConfigurer) {
        cancelAndRemoveTask(id);
        CompletableFuture<Void> task = supplierConfigurer.supplyAsync();
        registerTaskRemoving(id, task);
        return task;
    }

    public void cancelAll() {
        registry.keySet().forEach(this::cancelAndRemoveTask);
    }

    private void registerTaskRemoving(String id, CompletableFuture<?> task) {
        task.whenComplete((r, e) -> registry.remove(id));
    }

    private void cancelAndRemoveTask(String id) {
        Optional.ofNullable(registry.get(id)).ifPresent(existing -> existing.cancel(true));
        registry.remove(id);
    }
}
