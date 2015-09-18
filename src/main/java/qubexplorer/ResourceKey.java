package qubexplorer;

import java.io.Serializable;
import java.util.Arrays;

/**
 *
 * @author Victor
 */
public class ResourceKey implements Serializable{

    private final String[] parts;
    
    public ResourceKey(String... parts){
        this.parts=parts;
    }
    
    public String getPart(int index) {
        return parts[index];
    }
    
    public int getPartsCount(){
        return parts.length;
    }

    @Override
    public String toString() {
        StringBuilder builder=new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if(i > 0){
                builder.append(':');
            }
            builder.append(parts[i]);
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(parts);
    }

    @Override
    public boolean equals(Object obj) {
        if( !(obj instanceof ResourceKey) ) {
            return false;
        }
        ResourceKey anotherKey=(ResourceKey) obj;
        return Arrays.equals(parts, anotherKey.parts);
    }
    
    public static ResourceKey valueOf(String key) {
        return new ResourceKey(key.split(":"));
    }
    
}
