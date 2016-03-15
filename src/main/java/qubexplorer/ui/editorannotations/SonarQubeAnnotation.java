package qubexplorer.ui.editorannotations;

import org.openide.text.Annotation;
import qubexplorer.Severity;

/**
 *
 * @author Victor
 */
public class SonarQubeAnnotation extends Annotation{
    private final Severity severity;
    private final String description;

    public SonarQubeAnnotation(Severity severity, String message) {
        this.severity = severity;
        this.description=severity+": "+message;
    }
    
    @Override
    public String getAnnotationType() {
        return "sonarqube-"+severity.name().toLowerCase()+"-annotation";
    }

    @Override
    public String getShortDescription() {
        return description;
    }
    
}
