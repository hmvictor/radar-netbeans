package qubexplorer.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.services.Rule;
import qubexplorer.PassEncoder;
import qubexplorer.UserCredentials;

/**
 *
 * @author Victor
 */
public class RuleSearchClient {
    private static final Logger LOGGER=Logger.getLogger(RuleSearchClient.class.getName());
    private final String baseUrl;

    public RuleSearchClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public Rule getRule(UserCredentials userCredentials, String key) {
        Map<String, Object> params=new HashMap<>();
        params.put("key", key);
        HttpRequestFactory httpRequestFactory = new HttpRequestFactory(baseUrl);
        if(userCredentials != null) {
            httpRequestFactory.setLogin(userCredentials.getUsername()).setPassword(PassEncoder.decodeAsString(userCredentials.getPassword()));
        } 
        String jsonRule = httpRequestFactory.get("/api/rules/show", params);
        try{
            JsonElement jsonElement = new JsonParser().parse(new StringReader(jsonRule));
            JsonObject jsonObject = (JsonObject) ((JsonObject)jsonElement).get("rule");
            Rule rule = new Rule();
            rule.setKey(jsonObject.get("key").getAsString());
            rule.setTitle(jsonObject.get("name").getAsString());
            JsonElement description = jsonObject.get("htmlDesc");
            if(description == null){
                description=jsonObject.get("description");
            }
            rule.setDescription(description.getAsString());
            return rule;
        }catch(JsonSyntaxException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return null;
        }
    }
    
}
