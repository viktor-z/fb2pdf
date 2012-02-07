importPackage(org.apache.pivot.wtk);
importClass(java.io.FileFilter);
importClass(org.trivee.fb2pdf.CLIDriver);
 
function mainWindowOpened(window) {
    println("fb2pdf-j UI started")
}

function processFile(file) {
    var path = file.getPath(); 
    if (path.endsWith(".fb2") || path.endsWith(".fb2.zip")) {
        CLIDriver.main([path]);
    } else {
        //println("Skipping " + path);
    }
}

function processDirectory(dir) {
    var path = dir.getPath(); 
    CLIDriver.main(["-r", path]);
}

function processFileOrDirectory(file) {
    if (file.isDirectory()) {
        processDirectory(file);
    } else {
        processFile(file);
    }
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