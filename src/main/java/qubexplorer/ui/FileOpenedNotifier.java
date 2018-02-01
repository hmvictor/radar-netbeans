package qubexplorer.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import qubexplorer.ui.issues.FileObjectOpenedListener;

/**
 * A component to notify when files are opened.
 * 
 * Used for attaching editor annotations because the data object is not available if it is not opened in the editor.
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
    
    public void unregisterCurrentFileOpenedListeners() {
        listenersByFilepath.clear();
    }
    
    public void fireFileOpenedNotification(FileObject fileOpened) {
        getFileOpenedListeners(fileOpened).forEach(listener -> 
            listener.fileOpened(fileOpened)
        );
    }

    public List<FileObjectOpenedListener> getFileOpenedListeners(FileObject fileObject) {
        List<FileObjectOpenedListener> listeners = listenersByFilepath.get(fileObject.getPath());
        if (listeners == null) {
            listeners = Collections.emptyList();
        }
        return listeners;
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (TopComponent.Registry.PROP_OPENED.equals(event.getPropertyName())) {
            getNewOpenedComponents(event).forEach(newOpenedComponent -> 
                getFileObject(newOpenedComponent).ifPresent(fileObject -> 
                    fireFileOpenedNotification(fileObject)
                )
            );
        } else if (TopComponent.Registry.PROP_ACTIVATED.equals(event.getPropertyName())) {
            TopComponent activatedComponent = (TopComponent) event.getNewValue();
            if(activatedComponent != null) {
                getFileObject(activatedComponent).ifPresent(fileObject -> 
                    fireFileOpenedNotification(fileObject)
                );
            }
        }
    }

    private Set<TopComponent> getNewOpenedComponents(PropertyChangeEvent event) {
        HashSet<TopComponent> newOpenedComponents = (HashSet<TopComponent>) event.getNewValue();
        HashSet<TopComponent> oldOpenedComponents = (HashSet<TopComponent>) event.getOldValue();
        newOpenedComponents.removeAll(oldOpenedComponents);
        return newOpenedComponents;
    }

    private Optional<FileObject> getFileObject(TopComponent topComponent) {
        assert topComponent != null;
        FileObject fileObject = null;
        DataObject dataObject = topComponent.getLookup().lookup(DataObject.class);
        if (dataObject != null) {
            fileObject = dataObject.getPrimaryFile();
        }
        return Optional.ofNullable(fileObject);
    }

}
