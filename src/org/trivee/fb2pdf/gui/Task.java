package org.trivee.fb2pdf.gui;

import org.apache.pivot.util.concurrent.TaskExecutionException;

public class Task extends org.apache.pivot.util.concurrent.Task<String> {
    
    private Runnable runnable;
    
    public Task(Runnable runnable) {
        super();
        this.runnable = runnable;
    }

    @Override
    public String execute() throws TaskExecutionException {
        runnable.run();
        return "Done";
    }
    
}
