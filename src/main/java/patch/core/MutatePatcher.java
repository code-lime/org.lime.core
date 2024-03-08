package patch.core;

import org.lime.core;
import patch.*;

public class MutatePatcher extends BasePluginPatcher {
    public static void register() {
        Patcher.addPatcher(new MutatePatcher());
    }

    private MutatePatcher() {
        super(core.class);
    }

    @Override public void patch(JarArchive versionArchive, JarArchive bukkitArchive) {
    }
}
