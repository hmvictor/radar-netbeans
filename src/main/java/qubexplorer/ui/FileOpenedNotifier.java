package qubexplorer.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import qubexplorer.ui.issues.FileObjectOpenedListener;

/**
 *
 * @author Victor
 */
public class FileOpenedNotifier implements PropertyChangeListener {
    
    private final Map<String, List<FileObjectOpenedListener>> listenersByFilepath = new ConcurrentHashMap<>();

    public void init() {
        WindowManager.getDefault().getRegistry().addPropertyChangeListener(this);
    }

    public void registerFileOpenedListener(FileObject fileObject, FileObjectOpenedListener listener) {
        List<FileObjectOpenedListener> listeners = listenersByFilepath.get(fileObject.getPath());
        if (listeners == null) {
            listeners = new CopyOnWriteArrayList<>();
            listenersByFilepath.put(fileObject.getPath(), listeners);
        }
        listeners.add(listener);
    }

    public void fireFileOpenedNotification(FileObject fileOpened) {
        for (FileObjectOpenedListener listener : getFileOpenedListeners(fileOpened)) {
            listener.fileOpened(fileOpened);
        }
    }

    public List<FileObjectOpenedListener> getFileOpenedListeners(FileObject fileObject) {
        List<FileObjectOpenedListener> listeners = listenersByFilepath.get(fileObject.getPath());
        if (listeners == null) {
            listeners = Collections.emptyList();
        }
        return listeners;
    }

    public void unregisterCurrentFileOpenedListeners() {
        listenersByFilepath.clear();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if ("opened".equals(event.getPropertyName())) {
            for (TopComponent newOpenedComponent : getNewOpenedComponents(event)) {
                FileObject fileObject = getFileObject(newOpenedComponent);
                if (fileObject != null) {
                    fireFileOpenedNotification(fileObject);
                }
            }
        }
    }

    private Set<TopComponent> getNewOpenedComponents(PropertyChangeEvent event) {
        HashSet<TopComponent> newOpenedComponents = (HashSet<TopComponent>) event.getNewValue();
        HashSet<TopComponent> oldOpenedComponents = (HashSet<TopComponent>) event.getOldValue();
        newOpenedComponents.removeAll(oldOpenedComponents);
        return newOpenedComponents;
    }

    private FileObject getFileObject(TopComponent topComponent) {
        FileObject fileObject = null;
        DataObject dataObject = topComponent.getLookup().lookup(DataObject.class);
        if (dataObject != null) {
            fileObject = dataObject.getPrimaryFile();
        }
        return fileObject;
    }
    
}
