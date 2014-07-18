package qubexplorer.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import qubexplorer.server.SonarQube;
import qubexplorer.ui.options.SonarQubeOptionsPanel;

@ActionID(
        category = "Build",
        id = "qubexplorer.ui.SonarDialogAction")
@ActionRegistration(
        displayName = "#CTL_SonarDialogAction")
@Messages("CTL_SonarDialogAction=See Sonar Issues")
@ActionReferences(value = {
    @ActionReference(path = "Projects/Actions"),
    @ActionReference(path = "Menu/Source", position = 8962, separatorBefore = 8956, separatorAfter = 8968)})
public final class ServerSummaryAction implements ActionListener {

    private final Project context;

    public ServerSummaryAction(Project context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        try {
            String serverUrl = NbPreferences.forModule(SonarQubeOptionsPanel.class).get("address", "http://localhost:9000");
            SummaryWorker worker=new SummaryWorker(new SonarQube(serverUrl), context, serverUrl, SonarQube.toResource(context));
            worker.setTriggerActionPlans(true);
            worker.execute();
        } catch (IOException | XmlPullParserException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

}
