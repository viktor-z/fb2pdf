importPackage(org.apache.pivot.wtk);
importClass(java.io.FileFilter);
importClass(java.io.PrintStream);
importClass(org.trivee.fb2pdf.CLIDriver);
importClass(org.trivee.fb2pdf.gui.Task);
importClass(org.trivee.fb2pdf.gui.TextAreaOutputStream);
importClass(org.apache.pivot.util.concurrent.TaskListener);
importClass(org.apache.pivot.wtk.TaskAdapter);
importClass(java.lang.System);

function processFile(file, fileList) {
    var path = file.getPath(); 
    if (path.endsWith(".fb2") || path.endsWith(".fb2.zip")) {
        runFb2Pdf([path], fileList);
    } else {
        System.out.println("Skipping " + path);
        processFileList(fileList);
    }
}

function processDirectory(dir, fileList) {
    var path = dir.getPath(); 
    runFb2Pdf(["-r", path], fileList);
}

function processFileList(fileList) {
    if (!fileList || fileList.length < 1) {
        hideActivityIndicator();
        System.out.println("End time: " + new Date() + '\n');
        return; 
    }
    var file = fileList.pop();
    if (file.isDirectory()) {
        processDirectory(file, fileList);
    } else {
        processFile(file, fileList);
    }
}

function runFb2Pdf(params, fileList) {


    var taskListener = new TaskListener() {

        taskExecuted: function(task) {
            processFileList(fileList);
        },
     
        executeFailed: function(task) {
            System.out.println(task.getFault());
            processFileList(fileList);
        }
    };
    var task = new Task(function(){
        CLIDriver.main(params);
    });
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
            var fileList = [];
            var it = dragContent.getFileList().iterator();
            while (it.hasNext()) {
                fileList.push(it.next());
            }
           showActivityIndicator();
           System.out.println("Start time: " + new Date());
           processFileList(fileList);
           dropAction = DropAction.COPY;
        }
 
        return dropAction;
    }
};

function showActivityIndicator() {
    activityIndicator.setActive(true);
    cardPane.setSelectedIndex(1);
    dropLabel.setEnabled(false);
}

function hideActivityIndicator() {
    dropLabel.setEnabled(true);    
    activityIndicator.setActive(false);
    cardPane.setSelectedIndex(0);
}

function mainWindowOpened(window) {
    var consoleStream = new TextAreaOutputStream(console);
    System.setOut(new PrintStream(consoleStream, true));
    System.out.println("fb2pdf-j UI started")
    main.title = "fb2pdf-j " + CLIDriver.getImplementationVersion();
}

