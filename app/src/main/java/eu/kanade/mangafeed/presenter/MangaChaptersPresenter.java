package eu.kanade.mangafeed.presenter;

import android.os.Bundle;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import eu.kanade.mangafeed.data.helpers.DatabaseHelper;
import eu.kanade.mangafeed.data.helpers.DownloadManager;
import eu.kanade.mangafeed.data.helpers.PreferencesHelper;
import eu.kanade.mangafeed.data.helpers.SourceManager;
import eu.kanade.mangafeed.data.models.Chapter;
import eu.kanade.mangafeed.data.models.Manga;
import eu.kanade.mangafeed.events.ChapterCountEvent;
import eu.kanade.mangafeed.events.DownloadChaptersEvent;
import eu.kanade.mangafeed.events.SourceMangaChapterEvent;
import eu.kanade.mangafeed.sources.base.Source;
import eu.kanade.mangafeed.ui.fragment.MangaChaptersFragment;
import eu.kanade.mangafeed.util.EventBusHook;
import eu.kanade.mangafeed.util.PostResult;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MangaChaptersPresenter extends BasePresenter<MangaChaptersFragment> {

    @Inject DatabaseHelper db;
    @Inject SourceManager sourceManager;
    @Inject PreferencesHelper preferences;
    @Inject DownloadManager downloadManager;

    private Manga manga;
    private Source source;

    private static final int DB_CHAPTERS = 1;
    private static final int ONLINE_CHAPTERS = 2;

    private Subscription markReadSubscription;
    private Subscription downloadSubscription;
    private Subscription deleteSubscription;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        restartableLatestCache(DB_CHAPTERS,
                this::getDbChaptersObs,
                (view, chapters) -> {
                    view.onNextChapters(chapters);
                    EventBus.getDefault().postSticky( new ChapterCountEvent(chapters.size()) );
                }
        );

        restartableLatestCache(ONLINE_CHAPTERS,
                this::getOnlineChaptersObs,
                (view, result) -> view.onNextOnlineChapters()
        );
    }

    @Override
    protected void onTakeView(MangaChaptersFragment view) {
        super.onTakeView(view);
        registerForStickyEvents();
    }

    @Override
    protected void onDropView() {
        unregisterForEvents();
        super.onDropView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().removeStickyEvent(ChapterCountEvent.class);
    }

    @EventBusHook
    public void onEventMainThread(Manga manga) {
        if (this.manga == null) {
            this.manga = manga;
            source = sourceManager.get(manga.source);
            start(DB_CHAPTERS);

            // Get chapters if it's an online source
            if (getView() != null && getView().isOnlineManga()) {
                refreshChapters();
            }
        }
    }

    public void refreshChapters() {
        if (getView() != null)
            getView().setSwipeRefreshing();

        start(ONLINE_CHAPTERS);
    }

    private Observable<List<Chapter>> getDbChaptersObs() {
        return db.getChapters(manga.id).createObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<PostResult> getOnlineChaptersObs() {
        return source
                .pullChaptersFromNetwork(manga.url)
                .subscribeOn(Schedulers.io())
                .flatMap(chapters -> db.insertOrRemoveChapters(manga, chapters))
                .observeOn(AndroidSchedulers.mainThread());
    }

    public void onChapterClicked(Chapter chapter) {
        EventBus.getDefault().postSticky(new SourceMangaChapterEvent(source, manga, chapter));
    }

    public void markChaptersRead(Observable<Chapter> selectedChapters, boolean read) {
        add(markReadSubscription = selectedChapters
                .subscribeOn(Schedulers.io())
                .map(chapter -> {
                    chapter.read = read;
                    return chapter;
                })
                .toList()
                .flatMap(chapters -> db.insertChapters(chapters).createObservable())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnCompleted(() -> remove(markReadSubscription))
                .subscribe(result -> {
                }));
    }

    public void downloadChapters(Observable<Chapter> selectedChapters) {
        add(downloadSubscription = selectedChapters
                .toList()
                .subscribe(chapters -> {
                    EventBus.getDefault().postSticky(new DownloadChaptersEvent(manga, chapters));
                    remove(downloadSubscription);
                }));
    }

    public void deleteChapters(Observable<Chapter> selectedChapters) {
        deleteSubscription = selectedChapters
                .doOnCompleted( () -> remove(deleteSubscription) )
                .subscribe(chapter -> {
                    downloadManager.deleteChapter(source, manga, chapter);
                    chapter.downloaded = Chapter.NOT_DOWNLOADED;
                });
    }

    public void checkIsChapterDownloaded(Chapter chapter) {
        File dir = downloadManager.getAbsoluteChapterDirectory(source, manga, chapter);
        File pageList = new File(dir, DownloadManager.PAGE_LIST_FILE);

        if (dir.exists() && pageList.exists() && downloadManager
                .getSavedPageList(source, manga, chapter).size() + 1 == dir.listFiles().length) {
            chapter.downloaded = Chapter.DOWNLOADED;
        } else {
            chapter.downloaded = Chapter.NOT_DOWNLOADED;
        }
    }

}
