/*
 * Copyright (C) 2015 RoboVM AB
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/gpl-2.0.html>.
 */

package org.robovm.templater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.robovm.compiler.config.Arch;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.config.Config.TargetType;
import org.robovm.compiler.config.OS;
import org.robovm.compiler.config.Resource;
import org.robovm.compiler.log.ConsoleLogger;
import org.robovm.compiler.log.Logger;

/**
 * The RoboVM {@code Templater} generates a new RoboVM project from a template.
 *
 * @author BlueRiver Interactive
 */
public class Templater {
    private static final String PACKAGE_FOLDER_PLACEHOLDER = "__packageInPathFormat__";
    private static final String MAIN_CLASS_FILE_PLACEHOLDER = "__mainClass__";
    private static final List<String> SUBSTITUTED_PLACEHOLDER_FILES_EXTENSIONS = Arrays.asList("xml", "java");
    private static final String DOLLAR_SYMBOL_PLACEHOLDER = Pattern.quote("${symbol_dollar}");
    private static final String PACKAGE_PLACEHOLDER = Pattern.quote("package ${package};");
    private static final String MAIN_CLASS_PLACEHOLDER = Pattern.quote("${mainClass}");
    private static final String MAVEN_ARCHETYPE_SET_PLACEHOLDER = "#set\\(.*\\)\n";

    private final Logger logger;
    private final String template;
    private final URL templateURL;
    private String mainClass;
    private String mainClassName;
    private String packageName;
    private String packageDirName;
    private String appName;
    private String appId;
    private String executable;

    /**
     * Creates a new {@code Templater} with the specified {@code template}.
     * 
     * @throws NullPointerException if {@code template == null}.
     * @throws IllegalArgumentException if the specified {@code template} 
     *             doesn't exist.
     */
    public Templater(Logger logger, String template) {
        if (logger == null) {
            throw new NullPointerException("logger");
        }
        if (template == null) {
            throw new NullPointerException("template");
        }
        this.logger = logger;
        this.template = template;
        templateURL = Templater.class.getResource("/templates/robovm-" + template + "-template.tar.gz");
        if (templateURL == null) {
            throw new IllegalArgumentException(String.format("Template with name '%s' doesn't exist!", template));
        }
    }

    /**
     * <p>
     * Sets the main class for the generated project. This is the only required
     * parameter. All other parameters will derive from this one if not
     * explicitly specified.
     * </p>
     * 
     * Example: {@code com.company.app.MainClass}
     * 
     * @param mainClass
     * @return
     */
    public Templater mainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    /**
     * <p>
     * Sets the package name for the generated project. Will derive from the
     * main class name if not explicitly specified.
     * </p>
     * 
     * Example: {@code com.company.app}
     * 
     * @param packageName
     * @return
     */
    public Templater packageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    /**
     * <p>
     * Sets the app name for the generated project. Will derive from the main
     * class name if not explicitly specified.
     * </p>
     * 
     * Example: {@code MyApp}
     * 
     * @param appName
     * @return
     */
    public Templater appName(String appName) {
        this.appName = appName;
        return this;
    }

    /**
     * <p>
     * Sets the app id for the generated project. Will derive from the main
     * class name if not explicitly specified.
     * </p>
     * 
     * Example: {@code com.company.app}
     * 
     * @param appId
     * @return
     */
    public Templater appId(String appId) {
        this.appId = appId;
        return this;
    }

    /**
     * <p>
     * Sets the executable name for the generated project. Will derive from the
     * main class name if not explicitly specified.
     * </p>
     * 
     * Example: {@code MyApp}
     * 
     * @param executable
     * @return
     */
    public Templater executable(String executable) {
        this.executable = executable;
        return this;
    }

    /**
     * Builds the project and stores it at the specified {@code projectRoot}.
     * You need to have specified at least the main class name for the project
     * to be built.
     * 
     * @param projectRoot
     * 
     * @throws NullPointerException if {@code projectRoot == null}.
     * @throws IllegalArgumentException if no main class had been specified.
     */
    public void buildProject(File projectRoot) {
        if (projectRoot == null) {
            throw new NullPointerException("projectRoot");
        }
        if (mainClass == null) {
            throw new IllegalArgumentException("No main class specified");
        }
        mainClassName = mainClass.substring(mainClass.lastIndexOf('.') + 1);

        if (executable == null || executable.length() == 0) {
            executable = mainClassName;
        }
        if (appName == null || appName.length() == 0) {
            appName = mainClassName;
        }
        if (packageName == null || packageName.length() == 0) {
            int index = mainClass.lastIndexOf('.');
            if (index == -1) {
                packageName = "";
            } else {
                packageName = mainClass.substring(0, index);
            }
        }
        packageDirName = packageName.replaceAll("\\.", File.separator);
        if (appId == null || appId.length() == 0) {
            appId = packageName;
        }

        try {
            File targetFile = new File(projectRoot, FilenameUtils.getBaseName(templateURL.getPath()));
            FileUtils.copyURLToFile(templateURL, targetFile);
            extractArchive(targetFile, targetFile.getParentFile());
            targetFile.delete();

            generateConfig(projectRoot);
        } catch (IOException e) {
            throw new Error(e);
        }
        logger.info("Project with template '%s' successfully created in: %s", template,
                projectRoot.getAbsolutePath());
    }

    private void extractArchive(File archive, File destDir) throws IOException {
        TarArchiveInputStream in = null;
        try {
            in = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(archive)));
            ArchiveEntry entry = null;
            while ((entry = in.getNextEntry()) != null) {
                File f = new File(destDir, substitutePlaceholdersInFileName(entry.getName()));
                if (entry.isDirectory()) {
                    f.mkdirs();
                } else {
                    f.getParentFile().mkdirs();
                    OutputStream out = null;
                    try {
                        out = new FileOutputStream(f);
                        IOUtils.copy(in, out);
                    } finally {
                        IOUtils.closeQuietly(out);
                    }
                    substitutePlaceholdersInFile(f);
                }
            }
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private void generateConfig(File projectRoot) throws IOException {
        File propsFile = new File(projectRoot, "robovm.properties");
        File configFile = new File(projectRoot, "robovm.xml");

        Properties props = new Properties();
        Config.Builder configBuilder = new Config.Builder();

        props.setProperty("app.mainclass", mainClass);
        props.setProperty("app.name", appName);
        props.setProperty("app.executable", mainClassName);
        props.setProperty("app.id", appId);
        props.setProperty("app.version", "1.0");
        props.setProperty("app.build", "1");

        File infoPList = new File(projectRoot, "Info.plist.xml");
        File resources = new File(projectRoot, "resources");
        resources.mkdirs();

        configBuilder.os(OS.ios);
        configBuilder.arch(Arch.thumbv7);
        configBuilder.targetType(TargetType.ios);
        configBuilder.mainClass("${app.mainclass}");
        configBuilder.executableName("${app.executable}");
        configBuilder.iosInfoPList(infoPList);
        configBuilder.addResource(new Resource(resources, null));

        configBuilder.write(configFile);

        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(propsFile));
            props.store(writer, "");
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    private String substitutePlaceholdersInFileName(String fileName) {
        fileName = fileName.replaceAll(PACKAGE_FOLDER_PLACEHOLDER, packageDirName);
        fileName = fileName.replaceAll(MAIN_CLASS_FILE_PLACEHOLDER, mainClassName);
        return fileName;
    }

    private void substitutePlaceholdersInFile(File file) throws IOException {
        String extension = FilenameUtils.getExtension(file.getName());
        if (SUBSTITUTED_PLACEHOLDER_FILES_EXTENSIONS.contains(extension)) {
            String content = FileUtils.readFileToString(file, "UTF-8");
            content = content.replaceAll(MAVEN_ARCHETYPE_SET_PLACEHOLDER, "");
            content = content.replaceAll(DOLLAR_SYMBOL_PLACEHOLDER, Matcher.quoteReplacement("$"));
            content = content.replaceAll(PACKAGE_PLACEHOLDER, getPackageNameReplacement(packageName));
            content = content.replaceAll(MAIN_CLASS_PLACEHOLDER, mainClassName);
            FileUtils.writeStringToFile(file, content, "UTF-8");
        }
    }

    private static String getPackageNameReplacement(String packageName) {
        if (packageName == null || packageName.length() == 0) {
            return "";
        }
        return String.format("package %s;", packageName);
    }

    public static void main(String[] args) {
        if (args.length <= 1) {
            printHelpText();
            return;
        }

        String template = null;
        String mainClass = null;
        String packageName = null;
        String appName = null;
        String appId = null;
        String executable = null;
        File projectRoot = null;

        for (int i = 0; i < args.length; i += 2) {
            if (isOption(args[i])) {
                if (args[i].length() <= 1) {
                    throw new IllegalArgumentException("invalid option: " + args[i]);
                }

                char option = args[i].charAt(1);
                switch (option) {
                case 't': // template
                    template = getOptionValue(args, i);
                    break;
                case 'c': // main class
                    mainClass = getOptionValue(args, i);
                    break;
                case 'p': // package name
                    packageName = getOptionValue(args, i);
                    break;
                case 'n': // app name
                    appName = getOptionValue(args, i);
                    break;
                case 'i': // app id
                    appId = getOptionValue(args, i);
                    break;
                case 'e': // executable name
                    executable = getOptionValue(args, i);
                    break;
                case 'g': // generate
                    projectRoot = new File(getOptionValue(args, i));
                    break;
                default:
                    // ignore
                    break;
                }
            } else {
                printHelpText();
            }
        }

        new Templater(new ConsoleLogger(true), template).mainClass(mainClass).packageName(packageName).appName(appName)
                .appId(appId)
                .executable(executable).buildProject(projectRoot);
    }

    private static boolean isOption(String arg) {
        return arg.startsWith("-");
    }

    private static String getOptionValue(String[] args, int option) {
        if (args.length < option + 1) {
            throw new IllegalArgumentException("value missing for option: " + args[option]);
        }
        String value = args[option + 1];
        if (isOption(value)) {
            throw new IllegalArgumentException("illegal value for option: " + args[option]);
        }
        return value;
    }

    private static void printHelpText() {
        System.out.println("RoboVM Templater\n"
                + "Generates a new RoboVM project from a template\n"
                + "Usage: templater\n"
                + "-t <TEMPLATE>       template (required)\n"
                + "-c <CLASS>          main class (required)\n"
                + "-p <PACKAGE>        package name\n"
                + "-n <NAME>           app name\n"
                + "-i <ID>             app id\n"
                + "-e <EXECUTABLE>     executable name\n"
                + "-g <PROJECT_ROOT>   generates project to project root (required)");
    }
}
