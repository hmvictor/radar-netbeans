package qubexplorer.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.services.Rule;
import qubexplorer.PassEncoder;
import qubexplorer.UserCredentials;

/**
 *
 * @author Victor
 */
public class RuleSearchClient {
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
            JsonObject rule = (JsonObject) ((JsonObject)jsonElement).get("rule");
            Rule rule1 = new Rule();
            rule1.setKey(rule.get("key").getAsString());
            rule1.setTitle(rule.get("name").getAsString());
            rule1.setDescription(rule.get("htmlDesc").getAsString());
            return rule1;
        }catch(JsonSyntaxException ex) {
            return null;
        }
    }
    
    public static void main(String[] args) {
        Rule rule = new RuleSearchClient("http://localhost:9000").getRule(new UserCredentials("admin", PassEncoder.encode("admin".toCharArray())), "squid:MethodCyclomaticComplexity");
        System.out.println(rule.getKey());
        System.out.println(rule.getTitle());
        System.out.println(rule.getDescription());
    }
    
}
