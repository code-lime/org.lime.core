groovyScript("
def project = binding.getVariables()._editor.project;

def vf = project.getBaseDir().findChild('gradle.properties');
String output = 'org.lime.core.UNDEFINED__NAND';
if (vf != null) {
    def props = new java.util.Properties();

    try (def is = vf.getInputStream()) {
        props.load(is);
	    def names = props.stringPropertyNames();
	    if (names.contains('versionPaper')) {
            output = 'org.lime.core.paper';
        } else if (names.contains('versionFabric')) {
            output = 'org.lime.core.fabric';
        } else {
            output = 'org.lime.core.UNDEFINED';
        } 
    } catch (Exception e) {
        output = e.toString();
    }
} else {
    output = 'org.lime.core.UNDEFINED__NFND';
}

output
")