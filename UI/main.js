importPackage(org.apache.pivot.wtk);
importClass(java.io.File);
importClass(java.io.FileFilter);
importClass(java.io.PrintStream);
importClass(java.util.prefs.Preferences);
importClass(org.trivee.fb2pdf.CLIDriver);
importClass(org.trivee.fb2pdf.gui.Task);
importClass(org.trivee.fb2pdf.gui.TextAreaOutputStream);
importClass(org.apache.pivot.util.concurrent.TaskListener);
importClass(org.apache.pivot.util.Filter);
importClass(org.apache.pivot.wtk.TaskAdapter);
importClass(org.apache.pivot.wtk.Form);
importClass(org.apache.pivot.wtk.Alert);
importClass(org.apache.pivot.wtk.MessageType);
importClass(org.apache.pivot.wtk.FileBrowserSheet);
importClass(org.apache.pivot.wtk.SheetCloseListener);
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
            processFileIterator(dragContent.getFileList().iterator());
            dropAction = DropAction.COPY;
        }
 
        return dropAction;
    }
};

var srcBrowserDisabledFilter = new Filter() {
    include: function(itm) {
        return !(itm.isDirectory() || itm.getName().endsWith(".fb2") || itm.getName().endsWith(".fb2.zip"));
    }
};

function showActivityIndicator() {
    activityIndicator.setActive(true);
    cardPane.setSelectedIndex(1);
    dropLabel.setEnabled(false);
    fileBrowser.setEnabled(false);
    convertButton.setEnabled(false);
}

function hideActivityIndicator() {
    fileBrowser.clearSelection();
    convertButton.setEnabled(true);
    fileBrowser.setEnabled(true);
    dropLabel.setEnabled(true);    
    activityIndicator.setActive(false);
    cardPane.setSelectedIndex(0);
}

function mainWindowOpened(window) {
    var consoleStream = new TextAreaOutputStream(console);
    System.setOut(new PrintStream(consoleStream, true));
    System.out.println("fb2pdf-j UI started")
    main.title = "fb2pdf-j " + CLIDriver.getImplementationVersion();
    UIState.restore();
}

function mainWindowClosed(window) {
    UIState.save();
}

function processFileIterator(iterator) {
    var fileList = [];
    while (iterator.hasNext()) {
        fileList.push(iterator.next());
    }
    showActivityIndicator();
    System.out.println("Start time: " + new Date());
    processFileList(fileList);
}

function convertButtonPressed(button) {
    processFileIterator(fileBrowser.getSelectedFiles().iterator());
}

function parametersSaveButtonPressed(button) {
    try {
        if (!validateParameters()) return;
    } catch(ex) {
        System.out.println(ex);
    }
}

function settingsBrowseButtonPressed() {
    try {
        var fileBrowserSheet = new FileBrowserSheet(FileBrowserSheet.Mode.OPEN);
        fileBrowserSheet.setRootDirectory(fileBrowser.getRootDirectory());
        fileBrowserSheet.setDisabledFileFilter(new Filter() {
            include: function(itm) {
                return !(itm.isDirectory() || itm.getName().endsWith(".json"));
            }
        });
        fileBrowserSheet.open(main, new SheetCloseListener() {
            sheetClosed: function(sheet) {
                try {
                    if (sheet.getResult()) {
                        settingsFileParameter.text = sheet.getSelectedFile().getPath();
                    }
                } catch(ex) {
                    System.out.println(ex);
                }
            }
        });
    } catch(ex) {
        System.out.println(ex);
    }
}

function outdirBrowseButtonPressed() {
    try {
        var fileBrowserSheet = new FileBrowserSheet(FileBrowserSheet.Mode.SAVE_TO);
        fileBrowserSheet.getStyles().put("hideDisabledFiles", true); 
        fileBrowserSheet.open(main, new SheetCloseListener() {
            sheetClosed: function(sheet) {
                try {
                    if (sheet.getResult()) {
                        outdirParameter.text = sheet.getSelectedFile().getPath();
                    }
                } catch(ex) {
                    System.out.println(ex);
                }
            }
        });
    } catch(ex) {
        System.out.println(ex);
    }
}

function validateParameters() {
    var result = true;
    settingsForm.clearFlags();
    var settingsFilePath = settingsFileParameter.text || "";
    if (settingsFilePath != "") {
        var settingsFile = new File(settingsFilePath);
        if (settingsFile.isDirectory() || !settingsFile.exists()) {
            Form.setFlag(settingsFileParameterBox, new Form.Flag(MessageType.ERROR, "File Not Found"));
            result = false;
        }
    }

    return result;
}

var UIState = (function(){
    var NODE_NAME = "fb2pdf-j gui";
    var FILE_BROWSER_DIR = "file-browser-directory";
    var MAIN_TAB_SELECTED = "main-tab-selected";
    var MAIN_SPLIT_RATIO = "main-split-ratio";

    return {
        save: function() {
            try {
                var preferences = Preferences.userRoot().node(NODE_NAME);

                preferences.put(FILE_BROWSER_DIR, fileBrowser.getRootDirectory().getAbsolutePath());
                preferences.putInt(MAIN_TAB_SELECTED, mainTabPane.getSelectedIndex());
                preferences.putFloat(MAIN_SPLIT_RATIO, mainSplitPane.getSplitRatio());
                preferences.flush();
            } catch (ex) {
                System.out.println("Unable to save GUI state: " + ex);
            }
        },
        restore: function() {
            try {
                var preferences = Preferences.userRoot().node(NODE_NAME);

                var dir = new File(preferences.get(FILE_BROWSER_DIR, fileBrowser.getRootDirectory()));
                if (dir.exists()) {
                    fileBrowser.setRootDirectory(dir);
                }
                mainTabPane.setSelectedIndex(preferences.getInt(MAIN_TAB_SELECTED, mainTabPane.getSelectedIndex()));
                mainSplitPane.setSplitRatio(preferences.getFloat(MAIN_SPLIT_RATIO, mainSplitPane.getSplitRatio()));
            } catch (ex) {
                System.out.println("Unable to restore GUI state: " + ex);
            }
        }
    }
})();
