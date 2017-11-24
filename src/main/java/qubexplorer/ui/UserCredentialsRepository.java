package qubexplorer.ui;

import java.util.HashMap;
import java.util.Map;
import qubexplorer.ResourceKey;
import qubexplorer.UserCredentials;

/**
 *
 * @author Victor
 */
public class UserCredentialsRepository {
    private static UserCredentialsRepository repository;
    private final Map<String, Map<ResourceKey, UserCredentials>> cache = new HashMap<>();

    public UserCredentials getUserCredentials(String serverUrl, ResourceKey resourceKey) {
        if (cache.containsKey(serverUrl)) {
            if (cache.get(serverUrl).containsKey(resourceKey)) {
                return cache.get(serverUrl).get(resourceKey);
            } else {
                return cache.get(serverUrl).get(null);
            }
        } else {
            return null;
        }
    }

    public void saveUserCredentials(String serverUrl, ResourceKey resourceKey, UserCredentials authentication) {
        if (!cache.containsKey(serverUrl)) {
            cache.put(serverUrl, new HashMap<>());
        }
        cache.get(serverUrl).put(null, authentication);
        if (resourceKey != null) {
            cache.get(serverUrl).put(resourceKey, authentication);
        }
    }

    public static synchronized UserCredentialsRepository getInstance() {
        if (repository == null) {
            repository = new UserCredentialsRepository();
        }
        return repository;
    }

}
