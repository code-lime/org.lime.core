package org.lime.core.common.system;

import org.jetbrains.annotations.NotNull;
import org.lime.core.common.system.execute.Action2;
import org.lime.core.common.system.execute.Func0;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

public abstract class ProgressInputStream extends FilterInputStream {
    private long current = 0;
    private final long total;

    public ProgressInputStream(InputStream input) {
        super(input);
        long size;
        try { size = input.available(); }
        catch(IOException ioe) { size = 0; }
        this.total = size;
    }

    public abstract void onProgress(long current, long total);
    public abstract boolean isCanceled();

    private void onProgressRaw(long current) {
        onProgress(current, total);
    }
    private void tryThrowCancel() throws InterruptedIOException {
        if (!isCanceled()) return;
        InterruptedIOException exc = new InterruptedIOException("progress");
        exc.bytesTransferred = current > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)current;
        throw exc;
    }

    public int read() throws IOException {
        int c = in.read();
        if (c >= 0) onProgressRaw(++current);
        tryThrowCancel();
        return c;
    }
    public int read(byte @NotNull[] bytes) throws IOException {
        int nr = in.read(bytes);
        if (nr > 0) onProgressRaw(current += nr);
        tryThrowCancel();
        return nr;
    }
    public int read(byte @NotNull[] b, int off, int len) throws IOException {
        int nr = in.read(b, off, len);
        if (nr > 0) onProgressRaw(current += nr);
        tryThrowCancel();
        return nr;
    }
    public long skip(long n) throws IOException {
        long nr = in.skip(n);
        if (nr > 0) onProgressRaw(current += nr);
        return nr;
    }
    public synchronized void reset() throws IOException {
        in.reset();
        onProgressRaw(current = total - in.available());
    }

    public static ProgressInputStream of(InputStream stream, Action2<Long, Long> onProgress) {
        return new ProgressInputStream(stream) {
            @Override public void onProgress(long current, long total) { onProgress.invoke(current, total); }
            @Override public boolean isCanceled() { return false; }
        };
    }
    public static ProgressInputStream of(InputStream stream, Action2<Long, Long> onProgress, Func0<Boolean> isCanceled) {
        return new ProgressInputStream(stream) {
            @Override public void onProgress(long current, long total) { onProgress.invoke(current, total); }
            @Override public boolean isCanceled() { return isCanceled.invoke(); }
        };
    }
}
