package qubexplorer;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import qubexplorer.filter.IssueFilter;
import qubexplorer.filter.SeverityFilter;

/**
 *
 * @author Victor
 */
public enum Severity implements Classifier {
    
    BLOCKER("Blocker", "/qubexplorer/ui/images/blocker.png"),
    CRITICAL("Critical", "/qubexplorer/ui/images/critical.png"),
    MAJOR("Major", "/qubexplorer/ui/images/major.png"),
    MINOR("Minor", "/qubexplorer/ui/images/minor.png"),
    INFO("Info", "/qubexplorer/ui/images/info.png");
    
    private final String userDescription;
    private final String resourcePath;

    private Severity(String userDescription, String resourcePath) {
        this.userDescription = userDescription;
        this.resourcePath = resourcePath;
    }
    
    @Override
    public IssueFilter createFilter() {
        return new SeverityFilter(this);
    }

    @Override
    public Icon getIcon() {
        return new ImageIcon(getClass().getResource(resourcePath));
    }

    @Override
    public String getUserDescription() {
        return userDescription;
    }
    
}
