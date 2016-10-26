package qubexplorer;

import java.io.Serializable;
import java.util.Arrays;

/**
 *
 * @author Victor
 */
public class ResourceKey implements Serializable {

    private final String[] parts;

    public ResourceKey(String... parts) {
        this.parts = parts;
    }

    public String getPart(int index) {
        return parts[index];
    }

    public int getPartsCount() {
        return parts.length;
    }
    
    public String toString(int begin, int end) {
        StringBuilder builder = new StringBuilder();
        for (int i = begin; i < end; i++) {
            if (builder.length() > 0) {
                builder.append(':');
            }
            builder.append(parts[i]);
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return toString(0, parts.length);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(parts);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ResourceKey)) {
            return false;
        }
        ResourceKey anotherKey = (ResourceKey) obj;
        return Arrays.equals(parts, anotherKey.parts);
    }

    public String getLastPart() {
        return parts[parts.length - 1];
    }

//    public String removeLastPart() {
//        //        String path = _componentKey;
////        int index = path.lastIndexOf(':');
////        if (index != -1) {
////            path = path.substring(0, index);
////        }
////        return path;
//        return toString(0, parts.length -1);
//    }
    
    public ResourceKey subkey(int start, int end) {
        return new ResourceKey(Arrays.copyOfRange(parts, start, end));
    }

    public static ResourceKey valueOf(String key) {
        return new ResourceKey(key.split(":"));
    }

}
