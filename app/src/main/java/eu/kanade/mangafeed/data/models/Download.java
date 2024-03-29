package eu.kanade.mangafeed.data.models;

import java.io.File;
import java.util.List;

import eu.kanade.mangafeed.sources.base.Source;
import rx.subjects.PublishSubject;

public class Download {
    public Source source;
    public Manga manga;
    public Chapter chapter;
    public List<Page> pages;
    public File directory;

    public transient volatile int totalProgress;
    public transient volatile int downloadedImages;
    private transient volatile int status;

    private transient PublishSubject<Download> statusSubject;

    public static final int QUEUE = 0;
    public static final int DOWNLOADING = 1;
    public static final int DOWNLOADED = 2;
    public static final int ERROR = 3;


    public Download(Source source, Manga manga, Chapter chapter) {
        this.source = source;
        this.manga = manga;
        this.chapter = chapter;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
        notifyStatus();
    }

    public void setStatusSubject(PublishSubject<Download> subject) {
        this.statusSubject = subject;
    }

    private void notifyStatus() {
        if (statusSubject != null)
            statusSubject.onNext(this);
    }
}