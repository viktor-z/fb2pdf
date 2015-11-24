/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.trivee.fb2pdf;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.*;

/**
 *
 * @author vzeltser
 */
public class Log {
    private static final Logger logger = Logger.getLogger("fb2pdf");

    protected static void info(String msg, Object... params) {
        logger.log(Level.INFO, msg, params);
    }

    protected static void error(String msg, Object... params) {
        logger.log(Level.SEVERE, msg, params);
    }

    protected static void warning(String msg, Object... params) {
        logger.log(Level.WARNING, msg, params);
    }
    
    protected static void debug(String msg, Object... params) {
        logger.log(Level.FINE, msg, params);
    }
    
    protected static void setup(String fileName, String encoding) throws IOException {
        Handler fh = new FileHandler(fileName);
        fh.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                StringBuilder sb = new StringBuilder(record.getLevel().getLocalizedName());
                sb.append(": ");
                sb.append(MessageFormat.format(record.getMessage(), record.getParameters()));
                sb.append("\n");
                return sb.toString();
            }
        });
        fh.setEncoding(encoding);
        logger.setUseParentHandlers(false);
        while(logger.getHandlers().length > 0) {
            Handler h = logger.getHandlers()[0];
            h.close();
            logger.removeHandler(h);
        }
        logger.addHandler(fh);
    }
}
