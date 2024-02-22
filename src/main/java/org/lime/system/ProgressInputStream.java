package org.lime.system;

import org.lime.system.execute.Action2;
import org.lime.system.execute.Func0;

import javax.annotation.Nonnull;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

public abstract class ProgressInputStream extends FilterInputStream {
    private int current = 0;
    private final int total;

    public ProgressInputStream(InputStream input) {
        super(input);
        int size;
        try { size = input.available(); }
        catch(IOException ioe) { size = 0; }
        this.total = size;
    }

    public abstract void onProgress(int current, int total);
    public abstract boolean isCanceled();

    private void onProgressRaw(int current) {
        onProgress(current, total);
    }
    private void tryThrowCancel() throws InterruptedIOException {
        if (!isCanceled()) return;
        InterruptedIOException exc = new InterruptedIOException("progress");
        exc.bytesTransferred = current;
        throw exc;
    }

    public int read() throws IOException {
        int c = in.read();
        if (c >= 0) onProgressRaw(++current);
        tryThrowCancel();
        return c;
    }
    public int read(@Nonnull byte[] bytes) throws IOException {
        int nr = in.read(bytes);
        if (nr > 0) onProgressRaw(current += nr);
        tryThrowCancel();
        return nr;
    }
    public int read(@Nonnull byte[] b, int off, int len) throws IOException {
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

    public static ProgressInputStream of(InputStream stream, Action2<Integer, Integer> onProgress) {
        return new ProgressInputStream(stream) {
            @Override public void onProgress(int current, int total) { onProgress.invoke(current, total); }
            @Override public boolean isCanceled() { return false; }
        };
    }
    public static ProgressInputStream of(InputStream stream, Action2<Integer, Integer> onProgress, Func0<Boolean> isCanceled) {
        return new ProgressInputStream(stream) {
            @Override public void onProgress(int current, int total) { onProgress.invoke(current, total); }
            @Override public boolean isCanceled() { return isCanceled.invoke(); }
        };
    }
}
