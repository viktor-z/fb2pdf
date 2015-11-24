/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.trivee.fb2pdf;

import java.io.*;
import java.util.Vector;
import java.util.regex.Pattern;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;
import org.trivee.utils.Rotate;
import org.trivee.utils.TwoUp;

/**
 *
 * @author vikzel01
 */
public class CLIDriver {

    private static String hlpText = "fb2pdf [-h] [-s styles] [-l <log name>] [-e <log encoding>] <input file | directory> [-r] [<output file | directory>]"
            + "\n\nExamples:"
            + "\n\n\tfb2pdf test.fb2"
            + "\n\n\tfb2pdf \"c:\\My Books\""
            + "\n\n\tfb2pdf test.fb2 mybook.pdf"
            + "\n\n\tfb2pdf -s data\\myStylePart1.json -s data\\myStylePart2.json test.fb2"
            + "\n\n\tfb2pdf -l my_log.txt -e cp866 test.fb2";
    private static int succeeded = 0;
    private static int failed = 0;
    private static CommandLine cl;
    private static PrintWriter outWriter = new PrintWriter(System.out, true);
    private static String logEncoding;

    private static String getNonExistingFileName(String pdfname) {
        File f = new File(pdfname);
        for (int i = 1; f.exists(); i++) {
            String path = FilenameUtils.getFullPath(pdfname);
            String newName = String.format("%s (%s).%s", FilenameUtils.getBaseName(pdfname), i, FilenameUtils.getExtension(pdfname));
            f = new File(FilenameUtils.concat(path, newName));
        }
        return f.getAbsolutePath();
    }

    public static String getImplementationVersion() {
        return CLIDriver.class.getPackage().getImplementationVersion();
    }

    private static void printNameVersion() {
        String id = getImplementationVersion();
        id = id == null ? "" : "fb2pdf-j." + id;
        System.out.println(id);
    }

    /**
     * Prints out the specified string to user-visible output, never to the log.
     */
    private static void println(String s) {
        outWriter.println(s);
    }

    /**
     * @param args the command line arguments
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) throws FileNotFoundException, IOException, UnsupportedEncodingException, ParseException {

        Options options = new Options();
        options.addOption("h", "help", false, "Show usage information and quit");
        options.addOption("r", "recursive", false, "Process subdirectories");
        options.addOption("o", "overwrite", false, "Overwrite existing pdf files");
        options.addOption(OptionBuilder
                .withLongOpt("stylesheet")
                .hasArg()
                .withArgName("PATH")
                .withDescription("Stylesheet file")
                .create('s'));
        options.addOption("l", "log", true, "Log creation, use 'false' to disable");
        options.addOption("e", "encoding", true, "Log's encoding (default is cp1251)");
        options.addOption("t", "twoup", false, "Create two-up pdf");
        options.addOption(OptionBuilder
                .withLongOpt("rotate")
                .hasArg()
                .withArgName("ROTATION")
                .withDescription("90, 180 or 270")
                .create("rt"));
        options.addOption("x", "experiment", true, "Enable experimental features");

        cl = new PosixParser().parse(options, args);

        HelpFormatter formatter = new HelpFormatter();
        if (args.length == 0 || cl.hasOption('h')) {
            printNameVersion();
            formatter.printHelp(hlpText, options);
            return;
        }

        if (cl.getArgs().length < 1 || cl.getArgs().length > 2) {
            printNameVersion();
            formatter.printHelp(hlpText, options);
            return;
        }

        //println("Options detected:");
        //for (Option option: cl.getOptions()) {
        //    println(option.getOpt());
        //}
        if (cl.hasOption("x")) {
            System.setProperty("fb2pdf.experiment", cl.getOptionValue("x"));
        }

        String[] stylesheetNames = cl.hasOption('s') ? cl.getOptionValues('s') : new String[]{new File(Utilities.getBaseDir() + "/data/stylesheet.json").getCanonicalPath()};

        logEncoding = cl.hasOption('e') ? cl.getOptionValue('e') : "cp1251";
        try {
            outWriter = new PrintWriter(new OutputStreamWriter(System.out, logEncoding), true);
        } catch (UnsupportedEncodingException e) {
            System.err.println(String.format(
                    "Unknown encoding: %s, will use the default one.", logEncoding));
        }

        String fb2name = cl.getArgs()[0].replaceAll("\"", "");
        File fb2file = new File(fb2name);

        if (!fb2file.exists()) {
            println(String.format("Input file or directory %s not found.", fb2name));
            return;
        }

        println(String.format("Converting %s...\n", fb2name));

        if (fb2file.isDirectory()) {
            String outpath = cl.getArgs().length == 1 ? fb2file.getPath() : cl.getArgs()[1];
            processDirectory(fb2file, outpath, stylesheetNames);
        } else {
            String pdfname = cl.getArgs().length == 1 ? getPdfName(fb2name) : cl.getArgs()[1];
            if ((new File(pdfname)).isDirectory()) {
                pdfname = pdfname + "/" + getPdfName((new File(fb2name)).getName());
            }
            translate(fb2name, pdfname, stylesheetNames);

        }

        println(String.format("\nResults: succeeded: %s, failed: %s", succeeded, failed));

    }

    private static String getPdfName(String srcName) {
        srcName = Pattern.compile("\\.fbz$", Pattern.CASE_INSENSITIVE).matcher(srcName).replaceAll("");
        srcName = Pattern.compile("\\.zip$", Pattern.CASE_INSENSITIVE).matcher(srcName).replaceAll("");
        srcName = Pattern.compile("\\.fb2$", Pattern.CASE_INSENSITIVE).matcher(srcName).replaceAll("");
        return srcName + ".pdf";
    }

    private static void processDirectory(File inputDir, String outputPath, String[] stylesheetNames) throws FileNotFoundException, UnsupportedEncodingException {
        File outDir = new File(outputPath);
        if (outDir.exists() && !outDir.isDirectory()) {
            println(String.format("File %s exists.", outputPath));
            return;
        }

        if (!outDir.exists()) {
            outDir.mkdir();
        }

        File[] files = inputDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile()
                        && (pathname.getPath().endsWith(".fb2") || pathname.getPath().endsWith(".fb2.zip"));
            }
        });

        String normalOutputPath = FilenameUtils.normalize(outDir.getAbsolutePath());
        for (File file : files) {
            String outputFile = FilenameUtils.concat(normalOutputPath, getPdfName(file.getName()));
            translate(file.getAbsolutePath(), outputFile, stylesheetNames);
        }

        if (cl.hasOption('r')) {
            File[] subdirs = inputDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory();
                }
            });
            for (File subdir : subdirs) {
                processDirectory(subdir, FilenameUtils.concat(outputPath, subdir.getName()), stylesheetNames);
            }
        }
    }

    private static void translate(String fb2name, String pdfname, String[] stylesheetNames) throws FileNotFoundException, UnsupportedEncodingException {

        Vector<FileInputStream> streams = new Vector<FileInputStream>(stylesheetNames.length);
        for (String name : stylesheetNames) {
            FileInputStream stream = new FileInputStream(name);
            streams.add(stream);
        }
        SequenceInputStream stylesheet = new SequenceInputStream(streams.elements());

        PrintStream saveOut = System.out;

        // logging is on by default
        boolean createLog = true;
        String logFileName = null;

        if (cl.hasOption('l')) {
            String lValue = cl.getOptionValue('l');
                    // specifying false turns logging off,
            // otherwise we use the value as the file name
            createLog = !("false".equalsIgnoreCase(lValue));

                    // Protect against user error like this:
            // fb2pdf --log book.fb2,
            // since book.fb2 would be considered
            // a log file and the book will be overridden.
            if (!lValue.endsWith(".fb2")) {
                logFileName = lValue;
            } else {
                throw new FileNotFoundException("Wrong log file specified: " + lValue);
            }
        }

        if (createLog) {
            if (logFileName == null) {
                logFileName = String.format("%s.fb2pdf.log", FilenameUtils.removeExtension(pdfname));
            }
            try {
                Log.setup(logFileName, logEncoding);
            } catch (Exception ex) {
                println("Can't setup logger: " + ex.getMessage());
            }
        }

        try {
            if (!cl.hasOption("o")) {
                pdfname = getNonExistingFileName(pdfname);
            }
            FB2toPDF.translate(fb2name, pdfname, stylesheet);
            println(String.format("Success: %s\n", pdfname));
            succeeded++;
            if (cl.hasOption("t")) {
                TwoUp.execute(pdfname, pdfname + ".booklet.pdf");
            }
            if (cl.hasOption("rt")) {
                Rotate.execute(pdfname, pdfname + ".rotated.pdf", cl.getOptionValue("rt"));
            }
        } catch (Exception ex) {
            println(String.format("Failed:  %s \n", fb2name));
            Log.error(ex.toString());
            failed++;
        } finally {
            if (createLog) {
                System.setOut(saveOut);
            }
        }
    }

    private CLIDriver() {
    }

}
