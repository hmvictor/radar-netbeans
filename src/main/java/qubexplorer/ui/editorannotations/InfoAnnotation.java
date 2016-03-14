package qubexplorer.ui.editorannotations;

import org.openide.text.Annotation;

/**
 *
 * @author Victor
 */
public class InfoAnnotation extends Annotation{

    @Override
    public String getAnnotationType() {
        return "sonarqube-info-annotation";
    }

    @Override
    public String getShortDescription() {
        return "Info";
    }
    
}
