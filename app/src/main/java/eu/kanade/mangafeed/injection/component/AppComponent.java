package eu.kanade.mangafeed.injection.component;

import android.app.Application;

import javax.inject.Singleton;

import dagger.Component;
import eu.kanade.mangafeed.data.services.DownloadService;
import eu.kanade.mangafeed.data.services.LibraryUpdateService;
import eu.kanade.mangafeed.injection.module.AppModule;
import eu.kanade.mangafeed.injection.module.DataModule;
import eu.kanade.mangafeed.presenter.CataloguePresenter;
import eu.kanade.mangafeed.presenter.DownloadQueuePresenter;
import eu.kanade.mangafeed.presenter.LibraryPresenter;
import eu.kanade.mangafeed.presenter.MangaChaptersPresenter;
import eu.kanade.mangafeed.presenter.MangaDetailPresenter;
import eu.kanade.mangafeed.presenter.MangaInfoPresenter;
import eu.kanade.mangafeed.presenter.ReaderPresenter;
import eu.kanade.mangafeed.presenter.SourcePresenter;
import eu.kanade.mangafeed.sources.base.Source;
import eu.kanade.mangafeed.ui.activity.ReaderActivity;
import eu.kanade.mangafeed.ui.fragment.SettingsAccountsFragment;
import eu.kanade.mangafeed.ui.fragment.SettingsDownloadsFragment;

@Singleton
@Component(
        modules = {
                AppModule.class,
                DataModule.class
        }
)
public interface AppComponent {

    void inject(LibraryPresenter libraryPresenter);
    void inject(MangaDetailPresenter mangaDetailPresenter);
    void inject(SourcePresenter sourcePresenter);
    void inject(CataloguePresenter cataloguePresenter);
    void inject(MangaInfoPresenter mangaInfoPresenter);
    void inject(MangaChaptersPresenter mangaChaptersPresenter);
    void inject(ReaderPresenter readerPresenter);
    void inject(DownloadQueuePresenter downloadQueuePresenter);

    void inject(ReaderActivity readerActivity);
    void inject(SettingsAccountsFragment settingsAccountsFragment);
    void inject(SettingsDownloadsFragment settingsDownloadsFragment);

    void inject(Source source);

    void inject(LibraryUpdateService libraryUpdateService);
    void inject(DownloadService downloadService);

    Application application();

}
