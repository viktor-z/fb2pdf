/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.trivee.fb2pdf.gui;

import java.io.IOException;
import org.apache.pivot.wtk.ApplicationContext;
import org.apache.pivot.wtk.TextArea;

/**
 *
 * @author vzeltser
 */
public class TextAreaOutputStream extends java.io.OutputStream {
    
    private TextArea textArea;
    
    public TextAreaOutputStream(TextArea textArea) {
        this.textArea = textArea;
    }

    synchronized private void append(final String string) {
        ApplicationContext.queueCallback(new Runnable() {

            @Override
            public void run() {
                int c = textArea.getCharacterCount();
                textArea.insertText(string, c);  
            }
        });
    }
    
    @Override  
    public void write(final int b) throws IOException {  
        append(String.valueOf((char) b));  
    }  

    @Override  
    public void write(byte[] b, int off, int len) throws IOException {  
        append(new String(b, off, len));  
    }  

    @Override  
    public void write(byte[] b) throws IOException {  
        write(b, 0, b.length);  
    }  

}
