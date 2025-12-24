package com.company.crm.view.catalog;

import com.company.crm.app.util.ui.listener.resize.WidthResizeListener;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.CategoryItemRepository;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import io.jmix.core.FetchPlan;
import io.jmix.core.FileRef;
import io.jmix.core.FileStorage;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.component.splitlayout.JmixSplitLayout;
import io.jmix.flowui.component.upload.FileStorageUploadField;
import io.jmix.flowui.download.Downloader;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static com.vaadin.flow.server.streams.DownloadHandler.fromInputStream;

@Route(value = "category-items/:id", layout = MainView.class)
@ViewController(id = "CategoryItem.detail")
@ViewDescriptor(path = "category-item-detail-view.xml")
@EditedEntityContainer("categoryItemDc")
public class CategoryItemDetailView extends StandardDetailView<CategoryItem> implements WidthResizeListener {

    @Autowired
    private CategoryItemRepository itemRepository;
    @ViewComponent
    private JmixSplitLayout split;
    @ViewComponent
    private JmixImage<?> image;
    @ViewComponent
    private FileStorageUploadField imageUpload;
    @Autowired
    private FileStorage fileStorage;
    @Autowired
    private Downloader downloader;

    @Override
    public void configureUiForWidth(int width) {
        if (width < 700) {
            split.setOrientation(SplitLayout.Orientation.VERTICAL);
        } else {
            split.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        }
    }

    @Subscribe
    private void onInit(InitEvent event) {
        imageUpload.setAcceptedFileTypes("image/*");
        imageUpload.addValueChangeListener(this::updateImagePreview);
    }

    private void updateImagePreview(AbstractField.ComponentValueChangeEvent<FileStorageUploadField, FileRef> e) {
        Optional.ofNullable(e.getValue()).ifPresentOrElse(
                value -> image.setSrc(createImageSrc(value)),
                () -> image.setSrc("images/no_image.svg"));
    }

    private DownloadHandler createImageSrc(FileRef fileRef) {
        return fromInputStream(downloadEvent ->
                new DownloadResponse(fileStorage.openStream(fileRef),
                        fileRef.getFileName(), fileRef.getContentType(), -1));
    }

    @Install(to = "categoryItemDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private Optional<CategoryItem> loadDelegate(UUID id, FetchPlan fetchPlan) {
        return itemRepository.findById(id, fetchPlan);
    }
}