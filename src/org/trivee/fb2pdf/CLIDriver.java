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
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.trivee.utils.TwoUp;

/**
 *
 * @author vikzel01
 */
public class CLIDriver {

    private static String hlpText = "fb2pdf [-h] [-s styles] <input file | directory> [-r] [<output file | directory>]" +
            "\n\nExamples:" +
            "\n\n\tfb2pdf test.fb2" +
            "\n\n\tfb2pdf \"c:\\My Books\"" +
            "\n\n\tfb2pdf test.fb2 mybook.pdf" +
            "\n\n\tfb2pdf -s data\\mystyle.json test.fb2";
    private static int succeeded = 0;
    private static int failed = 0;
    private static CommandLine cl;

    private static void println(String s) {
        System.out.println(s);
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
        options.addOption("t", "twoup", false, "Create two-up pdf");

        cl = new PosixParser().parse(options, args);

        HelpFormatter formatter = new HelpFormatter();
        if(args.length == 0 || cl.hasOption('h')){
                formatter.printHelp(hlpText, options);
                return;
        }

        if (cl.getArgs().length < 1 || cl.getArgs().length > 2) {
                formatter.printHelp(hlpText, options);
                return;
        }

        //println("Options detected:");
        //for (Option option: cl.getOptions()) {
        //    println(option.getOpt());
        //}

        String stylesheetName = cl.hasOption('s') ? cl.getOptionValue('s') : new File(new File(getBaseDir()).getParent() + "/data/stylesheet.json").getCanonicalPath();

        String fb2name = cl.getArgs()[0].replaceAll("\"", "");
        File fb2file = new File(fb2name);

        if(!fb2file.exists()){
                println(String.format("Input file or directory %s not found.", fb2name));
                return;
        }

        println(String.format("Converting %s...\n", fb2name));

        if(fb2file.isDirectory()) {
                processDirectory(fb2file, stylesheetName);
        } else {
                String pdfname = cl.getArgs().length == 1 ? fb2name + ".pdf" : cl.getArgs()[1];
                if ((new File(pdfname)).isDirectory()) {
                    pdfname = pdfname + "/" + (new File(fb2name)).getName() + ".pdf";
                }
                translate(fb2name, pdfname, stylesheetName);

        }

        println(String.format("\nResults: succeeded: %s, failed: %s", succeeded, failed));

    }

    private static void processDirectory(File dir, String stylesheetName) throws FileNotFoundException, UnsupportedEncodingException {
                File[] files = dir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isFile() && 
                                (pathname.getPath().endsWith(".fb2") || pathname.getPath().endsWith(".fb2.zip"));
                    }
                });
                for (File file: files){
                    translate(file.getPath(), file.getPath() + ".pdf", stylesheetName);
                }

                if(cl.hasOption('r')) {
                    File[] subdirs = dir.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            return pathname.isDirectory();
                        }
                    });
                    for(File subdir: subdirs) {
                        processDirectory(subdir, stylesheetName);
                    }
                }
    }

    private static void translate(String fb2name, String pdfname, String stylesheetName) throws FileNotFoundException, UnsupportedEncodingException {
                FileInputStream stylesheet = new FileInputStream(stylesheetName);

                PrintStream saveOut = System.out;
                boolean createLog = cl.hasOption('l') ? Boolean.parseBoolean(cl.getOptionValue('l')) : true;
                if(createLog) {
                    FileOutputStream log = new FileOutputStream(String.format("%s.fb2pdf.log", fb2name));
                    PrintStream newOut = new PrintStream(log, true, "cp1251");
                    System.setOut(newOut);
                    System.setErr(newOut);
                }
                try
                {
                        FB2toPDF.translate(fb2name, pdfname, stylesheet);
                        saveOut.print(String.format("Success: %s\n", fb2name));
                        succeeded++;
                        if (cl.hasOption("t")){
                            TwoUp.execute(pdfname, pdfname+".booklet.pdf");
                        }
                }
                catch (Exception ex)
                {
                        saveOut.print(String.format("Failed:  %s\n", fb2name));
                        println(ex.toString());
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



