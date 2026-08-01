package net.minecraft.util;

import com.google.common.util.concurrent.ListenableFuture;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

public interface IThreadListener extends Executor {
    @Override
    default void execute(@NotNull Runnable command) {
        addScheduledTask(command);
    }

    ListenableFuture<Object> addScheduledTask(Runnable runnableToSchedule);

    boolean isCallingFromMinecraftThread();
}
