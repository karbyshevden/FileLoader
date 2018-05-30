package com.karbyshev.fileloader;

import android.os.Handler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ArrayBlockingQueue;

public class FileLoader {

    private static FileLoader instance;

    private volatile boolean inProgress = false;
    private volatile boolean inStopped = false;

    private Thread workerThread;

    private Handler handler;

    private volatile DownloadingStateCallback callback;

    private ArrayBlockingQueue<Task> queue;

    private String myFilePath;

    public static void initInstance() {
        instance = new FileLoader();
    }

    public static FileLoader getInstance() {
        return instance;
    }

    private Runnable threadRunnable = new Runnable() {
        @Override
        public void run() {
            while (!inStopped) {
                try {
                    Task task = queue.take();
                    try {
                        download(task);
                        myFilePath = task.destFileName;
                        finishWithSuccess(myFilePath);
                    } catch (DownloadException ex) {
                        finishWithFail(ex.getMessage());
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private FileLoader() {
        handler = new Handler();
        queue = new ArrayBlockingQueue<>(1);
        workerThread = new Thread(threadRunnable);
        workerThread.start();
    }

    public boolean isDownloadingInProgress() {
        return inProgress;
    }

    public void unsubscribe() {
        callback = null;
    }

    public void subscribe(DownloadingStateCallback callback) {
        if (inProgress) {
            this.callback = callback;
        } else {
            throw new IllegalStateException("No downloading to subscribe");
        }
    }

    public void downloadFile(String sourceUrl, String destFile, DownloadingStateCallback callback) {
        if (inProgress) {
            throw new IllegalStateException("Downloading already started");
        }
        if (inStopped) {
            throw new IllegalStateException("Loader shut down already");
        }
        Task task = new Task(sourceUrl, destFile);
        inProgress = true;
        this.callback = callback;
        queue.add(task);
    }

    public void shutdown() {
        inStopped = true;
    }

    private void download(Task task) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(task.sourceURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new DownloadException("Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage());
            }

            int fileLength = connection.getContentLength();

            input = connection.getInputStream();
            output = new FileOutputStream(task.destFileName);

            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {

                if (inStopped) {
                    input.close();
                    throw new DownloadException("downloader shut down");
                }
                total += count;

                if (fileLength > 0)
                    updateProgress((int) (total * 100 / fileLength));
                output.write(data, 0, count);
            }
        } catch (Exception e) {
            throw new DownloadException(e.toString());
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }

            if (connection != null)
                connection.disconnect();
        }
    }

    private void finishWithSuccess(final String filePath) {
        inProgress = false;
        if (callback != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onDownloadingComplete(filePath);
                    callback = null;
                }
            });
        }
    }

    private void finishWithFail(final String reason) {
        inProgress = false;
        if (callback != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onDownloadingError(reason);
                    callback = null;
                }
            });
        }
    }

    private void updateProgress(final int progress) {
        if (callback != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onProgressUpdated(progress);
                }
            });
        }
    }

    public interface DownloadingStateCallback {
        void onProgressUpdated(int progress);
        void onDownloadingComplete(String filePath);
        void onDownloadingError(String error);
    }

    public static class DownloadException extends RuntimeException {

        public DownloadException(String message) {
            super(message);
        }
    }

    public static class Task {
        String sourceURL;
        String destFileName;

        public Task(String sourceURL, String destFileName) {
            this.sourceURL = sourceURL;
            this.destFileName = destFileName;
        }
    }

    public String getMyFilePath() {
        return myFilePath;
    }
}
