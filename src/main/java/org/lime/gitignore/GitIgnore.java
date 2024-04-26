package org.lime.gitignore;

import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class GitIgnore {
    public static final String FILE_NAME = ".gitignore";
    private final File rootDir;
    private final Map<File, PathPatternList> patternListCache = new HashMap<>();
    private final List<PathPatternList> patternDefaults = new LinkedList<>();

    public GitIgnore(File rootDir) {
        this.rootDir = rootDir;
        this.addPatterns(rootDir);
    }

    public GitIgnore addPatterns(File dir) {
        return addPatterns(dir, "");
    }
    public GitIgnore addPatterns(File dir, String basePath) {
        PathPatternList patterns = getDirectoryPattern(dir, basePath);
        if (patterns != null)
            patternDefaults.add(patterns);
        return this;
    }

    public boolean isExcluded(File file) {
        File curDir = rootDir;
        String filePath = ExcludeUtils.getRelativePath(curDir, file);
        Vector<PathPatternList> stack = new Vector<>(10);
        StringBuilder pathBuilder = new StringBuilder(filePath.length());
        stack.addAll(patternDefaults);

        while (true) {
            int length = pathBuilder.length();
            int offset = filePath.indexOf('/', pathBuilder.length() + 1);
            boolean isDirectory = true;
            if (offset == -1) {
                offset = filePath.length();
                isDirectory = file.isDirectory();
            }

            pathBuilder.insert(pathBuilder.length(), filePath, pathBuilder.length(), offset);
            String currentPath = pathBuilder.toString();

            for (int i = stack.size() - 1; i >= 0; i--) {
                PathPatternList patterns = stack.get(i);
                PathPattern pattern = patterns.findPattern(currentPath, isDirectory);
                if (pattern != null) {
                    return pattern.isExclude();
                }
            }

            if (!isDirectory || pathBuilder.length() >= filePath.length()) {
                return false;
            }

            curDir = new File(curDir, pathBuilder.substring(length, offset));
            PathPatternList patterns = getDirectoryPattern(curDir, currentPath);
            if (patterns != null) {
                stack.add(patterns);
            }
        }
    }

    private PathPatternList getDirectoryPattern(File dir, String dirPath) {
        return getPatternList(new File(dir, FILE_NAME), dirPath);
    }
    private PathPatternList getPatternList(File file, String basePath) {
        return patternListCache.computeIfAbsent(file, v -> ExcludeUtils.readExcludeFile(v, basePath));
    }
}
