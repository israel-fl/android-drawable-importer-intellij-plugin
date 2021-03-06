package de.mprengemann.intellij.plugin.androidicons.widgets;

import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.ex.FileDrop;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import de.mprengemann.intellij.plugin.androidicons.controllers.settings.ISettingsController;
import de.mprengemann.intellij.plugin.androidicons.dialogs.ResourcesDialog;
import de.mprengemann.intellij.plugin.androidicons.util.AndroidFacetUtils;
import de.mprengemann.intellij.plugin.androidicons.util.TextUtils;
import org.intellij.images.fileTypes.ImageFileTypeManager;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.facet.ResourceFolderManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public class FileBrowserField extends TextFieldWithBrowseButton {

    public static final FileChooserDescriptor RESOURCE_DIR_CHOOSER = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    private static final FileType IMAGE_FILE_TYPE = ImageFileTypeManager.getInstance().getImageFileType();
    public static final FileChooserDescriptor IMAGE_FILE_CHOOSER = new FileChooserDescriptor(true,
                                                                                             false,
                                                                                             false,
                                                                                             false,
                                                                                             false,
                                                                                             false) {
        public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
            if (file.isDirectory()) {
                return super.isFileVisible(file, showHiddenFiles);
            }
            return file.getFileType().equals(IMAGE_FILE_TYPE);
        }

        public boolean isFileSelectable(VirtualFile file) {
            if (file.isDirectory()) {
                return super.isFileSelectable(file);
            }
            return super.isFileSelectable(file) && file.getFileType().equals(IMAGE_FILE_TYPE);
        }
    };
    public static final FileChooserDescriptor IMAGE_FILES_FOLDER_CHOOSER = new FileChooserDescriptor(true,
                                                                                                     true,
                                                                                                     false,
                                                                                                     false,
                                                                                                     false,
                                                                                                     true) {
        public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
            if (file.isDirectory()) {
                return super.isFileVisible(file, showHiddenFiles);
            }
            return file.getFileType().equals(IMAGE_FILE_TYPE);
        }

        public boolean isFileSelectable(VirtualFile file) {
            if (file.isDirectory()) {
                return super.isFileSelectable(file);
            }
            return super.isFileSelectable(file) && file.getFileType().equals(IMAGE_FILE_TYPE);
        }
    };

    static {
        RESOURCE_DIR_CHOOSER.setTitle("Select Resource Root");
        IMAGE_FILE_CHOOSER.setTitle("Select Image Asset");
        //noinspection DialogTitleCapitalization
        IMAGE_FILES_FOLDER_CHOOSER.setTitle("Select Image Asset(s)");
    }

    private Project project;
    private ISettingsController settingsController = null;
    private Consumer<File> listener;
    private FileChooserDescriptor descriptor;

    public FileBrowserField(FileChooserDescriptor descriptor) {
        super();
        this.descriptor = descriptor;
        getTextField().setEditable(false);
    }

    public void init(Project project, ISettingsController settingsController) {
        this.project = project;
        this.settingsController = settingsController;
        initFileChooser();
    }

    private void initFileChooser() {
        addBrowseFolderListener(new TextBrowseFolderListener(descriptor) {
            @Override
            protected void onFileChosen(@NotNull VirtualFile chosenFile) {
                super.onFileChosen(chosenFile);
                if (settingsController != null) {
                    settingsController.saveLastImageFolder(chosenFile.getCanonicalPath());
                }
                setText(chosenFile.getCanonicalPath());
            }

            @Nullable
            @Override
            protected Project getProject() {
                return project;
            }

            @Nullable
            @Override
            protected VirtualFile getInitialFile() {
                VirtualFile path = super.getInitialFile();
                if (path != null || settingsController == null) {
                    return path;
                }
                String directoryName = settingsController.getLastImageFolder();
                String expandPath = PathMacroManager.getInstance(project).expandPath(directoryName);
                if (expandPath == null) {
                    return null;
                }
                for (path = LocalFileSystem.getInstance().findFileByPath(expandPath);
                     path == null && directoryName.length() > 0;
                     path = LocalFileSystem.getInstance().findFileByPath(directoryName)) {
                    int pos = directoryName.lastIndexOf(47);
                    if (pos <= 0) {
                        break;
                    }
                    directoryName = directoryName.substring(0, pos);
                }
                return path;
            }
        });
        new FileDrop(getTextField(), new FileDrop.Target() {
            @Override
            public FileChooserDescriptor getDescriptor() {
                return descriptor;
            }

            @Override
            public boolean isHiddenShown() {
                return false;
            }

            @Override
            public void dropFiles(final List<VirtualFile> virtualFiles) {
                if (virtualFiles == null ||
                        virtualFiles.isEmpty()) {
                    return;
                }
                final VirtualFile file = virtualFiles.get(0);
                final String filePath = file.getCanonicalPath();
                settingsController.saveLastImageFolder(filePath);
                setText(filePath);
            }
        });
    }

    public void initWithResourceRoot(Project project, Module module, ISettingsController settings) {
        init(project, settings);
        final VirtualFile resourceDir = settingsController.getResourceRoot();
        if (resourceDir == null) {
            final ResourcesDialog.ResourceSelectionListener listener = new ResourcesDialog.ResourceSelectionListener() {
                @Override
                public void onResourceSelected(VirtualFile resDir) {
                    setText(resDir.getCanonicalPath());
                }
            };
            getResRootFile(this.project, module, listener);
        } else {
            setText(resourceDir.getCanonicalPath());
        }
    }

    private void getResRootFile(Project project, Module module, ResourcesDialog.ResourceSelectionListener listener) {
        final AndroidFacet currentFacet = AndroidFacetUtils.getCurrentFacet(project, module);
        //final List<VirtualFile> allResourceDirectories = currentFacet.getAllResourceDirectories();
        final List<VirtualFile> allResourceDirectories = ResourceFolderManager.getInstance(currentFacet).getFolders();
        if (allResourceDirectories.size() == 1) {
            listener.onResourceSelected(allResourceDirectories.get(0));
        } else if (allResourceDirectories.size() > 1) {
            ResourcesDialog dialog = new ResourcesDialog(project, allResourceDirectories, listener);
            dialog.show();
        }
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        notifyListener(text);
    }

    private void notifyListener(String filePath) {
        if (listener == null || TextUtils.isEmpty(filePath)) {
            return;
        }
        listener.consume(new File(filePath));
    }

    public void setSelectionListener(Consumer<File> fileConsumer) {
        this.listener = fileConsumer;
        notifyListener(getText());
    }
}
