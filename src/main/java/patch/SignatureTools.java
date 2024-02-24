package patch;

import javax.annotation.Nullable;

public interface SignatureTools {
    default String signatureType(String className, @Nullable String... genericSignatures) {
        if (genericSignatures == null || genericSignatures.length == 0)
            return "L" + className + ";";
        return "L" + className + "<" + String.join("", genericSignatures) + ">;";
    }
    default String signatureMethod(String returnSignature, String... argsSignatures) {
        return "(" + String.join("", argsSignatures) + ")" + returnSignature;
    }
}
