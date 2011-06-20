/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.trivee.fb2pdf;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Vector;
import java.util.regex.Pattern;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.trivee.utils.Rotate;
import org.trivee.utils.TwoUp;

/**
 *
 * @author vikzel01
 */
public class CLIDriver {

    private static String hlpText = "fb2pdf [-h] [-s styles] [-l <log name>] [-e <log encoding>] <input file | directory> [-r] [<output file | directory>]" +
            "\n\nExamples:" +
            "\n\n\tfb2pdf test.fb2" +
            "\n\n\tfb2pdf \"c:\\My Books\"" +
            "\n\n\tfb2pdf test.fb2 mybook.pdf" +
            "\n\n\tfb2pdf -s data\\myStylePart1.json -s data\\myStylePart2.json test.fb2" +
            "\n\n\tfb2pdf -l my_log.txt -e cp866 test.fb2";
    private static int succeeded = 0;
    private static int failed = 0;
    private static CommandLine cl;
    private static PrintWriter outWriter = new PrintWriter(System.out, true);
    private static String logEncoding;

    private static void printNameVersion() {
        String id = CLIDriver.class.getPackage().getImplementationVersion();
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
        options.addOption(OptionBuilder
                .withLongOpt("stylesheet")
                .hasArg()
                .withArgName("PATH")
                .withDescription("Stylesheet file")
                .create('s'));
        options.addOption("l", "log", true, "Log creation");
        options.addOption("e", "encoding", true, "Log's encoding (default is cp1251)");
        options.addOption("t", "twoup", false, "Create two-up pdf");
        options.addOption(OptionBuilder
                .withLongOpt("rotate")
                .hasArg()
                .withArgName("ROTATION")
                .withDescription("90, 180 or 270")
                .create("rt"));

        cl = new PosixParser().parse(options, args);

        HelpFormatter formatter = new HelpFormatter();
        if(args.length == 0 || cl.hasOption('h')){
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

        String[] stylesheetNames = cl.hasOption('s') ? cl.getOptionValues('s') : new String[]{new File(new File(getBaseDir()).getParent() + "/data/stylesheet.json").getCanonicalPath()};

        logEncoding = cl.hasOption('e') ? cl.getOptionValue('e') : "cp1251";
        try {
            outWriter = new PrintWriter(new OutputStreamWriter(System.out, logEncoding), true);
        } catch (UnsupportedEncodingException e) {
            System.err.println(String.format(
                    "Unknown encoding: %s, will use the default one.", logEncoding));
        }

        String fb2name = cl.getArgs()[0].replaceAll("\"", "");
        File fb2file = new File(fb2name);

        if(!fb2file.exists()){
                println(String.format("Input file or directory %s not found.", fb2name));
                return;
        }

        println(String.format("Converting %s...\n", fb2name));

        if(fb2file.isDirectory()) {
                processDirectory(fb2file, stylesheetNames);
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
        srcName =  Pattern.compile("\\.fbz$", Pattern.CASE_INSENSITIVE).matcher(srcName).replaceAll("");
        srcName =  Pattern.compile("\\.zip$", Pattern.CASE_INSENSITIVE).matcher(srcName).replaceAll("");
        srcName =  Pattern.compile("\\.fb2$", Pattern.CASE_INSENSITIVE).matcher(srcName).replaceAll("");
        return srcName + ".pdf";
    }

    private static void processDirectory(File dir, String[] stylesheetNames) throws FileNotFoundException, UnsupportedEncodingException {
                File[] files = dir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isFile() && 
                                (pathname.getPath().endsWith(".fb2") || pathname.getPath().endsWith(".fb2.zip"));
                    }
                });
                for (File file: files){
                    translate(file.getPath(), getPdfName(file.getPath()), stylesheetNames);
                }

                if(cl.hasOption('r')) {
                    File[] subdirs = dir.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            return pathname.isDirectory();
                        }
                    });
                    for(File subdir: subdirs) {
                        processDirectory(subdir, stylesheetNames);
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

                PrintStream saveOut =  System.out;

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
                        logFileName = String.format("%s.fb2pdf.log", fb2name);
                    }
                    FileOutputStream log = new FileOutputStream(logFileName);
                    PrintStream newOut = new PrintStream(log, true, logEncoding);
                    System.setOut(newOut);
                    System.setErr(newOut);
                }

                try
                {
                        FB2toPDF.translate(fb2name, pdfname, stylesheet);
                        println(String.format("Success: %s\n", fb2name));
                        succeeded++;
                        if (cl.hasOption("t")){
                            TwoUp.execute(pdfname, pdfname+".booklet.pdf");
                        }
                        if (cl.hasOption("rt")){
                            Rotate.execute(pdfname, pdfname+".rotated.pdf", cl.getOptionValue("rt"));
                        }
                }
                catch (Exception ex)
                {
                        println(String.format("Failed:  %s \n", fb2name));
                        System.out.println("ERROR: " + ex.toString());
                        failed++;
                }
                finally
                {
                    if(createLog) {
                        System.setOut(saveOut);
                    }
                }
    }

    public static String getBaseDir()  throws IOException {
        return URLDecoder.decode(new File(CLIDriver.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent(), "UTF-8");
    }

    private CLIDriver() {
    }

}



