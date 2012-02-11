importPackage(org.apache.pivot.wtk);
importClass(java.io.FileFilter);
importClass(org.trivee.fb2pdf.CLIDriver);
importClass(org.trivee.fb2pdf.gui.Task);
importClass(org.apache.pivot.util.concurrent.TaskListener);
importClass(org.apache.pivot.wtk.TaskAdapter);

function mainWindowOpened(window) {
    println("fb2pdf-j UI started")
    main.title = "fb2pdf-j " + CLIDriver.getImplementationVersion();
}

function processFile(file) {
    var path = file.getPath(); 
    if (path.endsWith(".fb2") || path.endsWith(".fb2.zip")) {
        runFb2Pdf([path]);
    } else {
        //println("Skipping " + path);
    }
}

function processDirectory(dir) {
    var path = dir.getPath(); 
    runFb2Pdf(["-r", path]);
}

function processFileOrDirectory(file) {
    if (file.isDirectory()) {
        processDirectory(file);
    } else {
        processFile(file);
    }
}

function runFb2Pdf(params) {
    var task = new Task(function(){
        CLIDriver.main(params);
    });
    hideActivityIndicator();
    println("Start time: " + new Date());
    task.execute(new TaskAdapter(taskListener));
}

var dropTarget = new DropTarget() {
    dragEnter: function(component, dragContent, supportedDropActions, userDropAction) {
        return (dragContent.containsFileList()) ? DropAction.COPY : null;
    },
 
    dragMove: function(component, dragContent, supportedDropActions, x, y, userDropAction) {
        return (dragContent.containsFileList()) ? DropAction.COPY : null;
    },
 
    userDropActionChange: function(component, dragContent, supportedDropActions, x, y, userDropAction) {
        return (dragContent.containsFileList()) ? DropAction.COPY : null;
    },
 
    dragExit: function(component) {
        // No-op
    },
 
    drop: function(component, dragContent, supportedDropActions, x, y, userDropAction) {
        var dropAction = null;
 
        if (dragContent.containsFileList()) {
            var it = dragContent.getFileList().iterator();
            while (it.hasNext()) {
                var file = it.next();
                processFileOrDirectory(file);
            }
            dropAction = DropAction.COPY;
        }
 
        return dropAction;
    }
};

function showActivityIndicator() {
    activityIndicator.setActive(false);
    cardPane.setSelectedIndex(0);
    main.setEnabled(true);
}

function hideActivityIndicator() {
    main.setEnabled(false);    
    activityIndicator.setActive(true);
    cardPane.setSelectedIndex(1);
}

var taskListener = new TaskListener() {

    taskExecuted: function(task) {
        showActivityIndicator()
        println("End time: " + new Date());
    },
 
    executeFailed: function(task) {
        showActivityIndicator()
        println(task.getFault());
        println("End time: " + new Date());
    }
};