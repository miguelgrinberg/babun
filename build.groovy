#!/usr/bin/env groovy
import static java.lang.System.*
import static java.lang.System.getenv

GROOVY = System.getenv()['GROOVY_HOME'] + "\\bin\\groovy.bat"
VERSION = new File("${getRoot()}/babun.version").text.trim()
TEN_MINUTES = 10
TWENTY_MINUTES = 20

execute()

def execute() {
    log "EXEC"
    checkArguments()
    String mode = this.args[0]
    if (mode == "clean") {
        doClean()
    } else if (mode == "cygwin") {
        doCygwin("x86")
    } else if (mode == "package") {
        doPackage("x86")
    } else if (mode == "release") {
        doRelease("x86")
    } else if (mode == "cygwin64") {
        doCygwin("x86_64")
    } else if (mode == "package64") {
        doPackage("x86_64")
    } else if (mode == "release64") {
        doRelease("x86_64")
    }
    log "FINISHED"
}

def checkArguments() {
    if (this.args.length != 1 || !this.args[0].matches("clean|cygwin|package|release|cygwin64|package64|release64")) {
        err.println "Usage: build.groovy <clean|cygwin|package|release>"
        exit(-1)
    }
}

def initEnvironment() {
    File target = getTarget()
    if (!target.exists()) {
        target.mkdir()
    }
}

def doClean() {
    log "EXEC clean"
    File target = getTarget()
    if (target.exists()) {
        if (!target.deleteDir()) {
            throw new RuntimeException("Cannot delete targe folder")
        }
    }
}

def doPackage(String bitVersion) {
    log "EXEC package"  
    executeBabunPackages(bitVersion)  
    executeBabunCygwin(bitVersion)
    executeBabunCore(bitVersion)
    executeBabunDist(bitVersion)
}

def doCygwin(String bitVersion) {    
    executeBabunPackages(bitVersion)
    boolean downloadOnly=true
    executeBabunCygwin(bitVersion, downloadOnly)
}

def doRelease(bitVersion) {
    log "EXEC release"
    doPackage()
    executeRelease(bitVersion)
}

def executeBabunPackages(String bitVersion) {    
    String module = "babun-packages"
    log "EXEC ${module}"
    if (shouldSkipModule(module, bitVersion)) return
    File workingDir = new File(getRoot(), module);
    String conf = new File(getRoot(), "${module}/conf/").absolutePath
    String out = new File(getTarget(), "${bitVersion}/${module}").absolutePath
    def command = [GROOVY, "packages.groovy", conf, out, bitVersion]
    executeCmd(command, workingDir, TEN_MINUTES)
}

def executeBabunCygwin(String bitVersion, boolean downloadOnly = false) {
    String module = "babun-cygwin"
    log "EXEC ${module}"
    File workingDir = new File(getRoot(), module);
    String input = workingDir.absolutePath
    String repo = new File(getTarget(), "${bitVersion}/babun-packages").absolutePath
    String out = new File(getTarget(), "${bitVersion}/${module}").absolutePath
    String pkgs = new File(getRoot(), "babun-packages/conf/cygwin.${bitVersion}.packages")
    String downOnly = downloadOnly as String
    println "Download only flag set to: ${downOnly}"
    def command = [GROOVY, "cygwin.groovy", repo, input, out, pkgs, bitVersion, downOnly]
    executeCmd(command, workingDir, TEN_MINUTES)
}

def executeBabunCore(String bitVersion) {
    String module = "babun-core"
    log "EXEC ${module}"
    if (shouldSkipModule(module, bitVersion)) return
    File workingDir = new File(getRoot(), module);
    String root = getRoot().absolutePath
    String cygwin = new File(getTarget(), "${bitVersion}/babun-cygwin/cygwin").absolutePath
    String out = new File(getTarget(), "${bitVersion}/${module}").absolutePath    
    String branch = getenv("babun_branch") ? getenv("babun_branch") : "release"
    println "Taking babun branch [${branch}]"
    def command = [GROOVY, "core.groovy", root, cygwin, out, branch]
    executeCmd(command, workingDir, TEN_MINUTES)
}

def executeBabunDist(String bitVersion) {
    String module = "babun-dist"
    log "EXEC ${module}"
    if (shouldSkipModule(module, bitVersion)) return
    File workingDir = new File(getRoot(), module);
    String input = workingDir.absolutePath
    String cygwin = new File(getTarget(), "${bitVersion}/babun-core/cygwin").absolutePath
    String out = new File(getTarget(), "${bitVersion}/${module}").absolutePath
    def command = [GROOVY, "dist.groovy", cygwin, input, out, VERSION, bitVersion]
    executeCmd(command, workingDir, TEN_MINUTES)
}

def executeRelease(String bitVersion) {
    log "EXEC release"
    assert getenv("bintray_user") != null
    assert getenv("bintray_secret") != null
    File artifact = new File(getTarget(), "${bitVersion}/babun-dist/babun-${VERSION}-dist.zip")
    def args = [GROOVY, "babun-dist/release/release.groovy", "babun", "babun-dist", VERSION,
            artifact.absolutePath, getenv("bintray_user"), getenv("bintray_secret")]
    executeCmd(args, getRoot(), TWENTY_MINUTES)
}

def shouldSkipModule(String module, String bitVersion) {
    File out = new File(getTarget(), "${bitVersion}/${module}")
    log "Checking if skip module ${module} -> folder ${out.absolutePath}"
    if (out.exists()) {
        log "SKIP ${module}"
        return true
    }
    log "DO NOT SKIP ${module}"
    return false
}

File getTarget() {
    return new File(getRoot(), "target")
}

File getRoot() {
    return new File(getClass().protectionDomain.codeSource.location.path).parentFile
}

def executeCmd(List<String> command, File workingDir, int timeout) {
    ProcessBuilder processBuilder = new ProcessBuilder(command)
    processBuilder.directory(workingDir)
    Process process = processBuilder.start()
    addShutdownHook { process.destroy() }
    process.consumeProcessOutput(out, err)
    process.waitForOrKill(timeout * 60000)
    assert process.exitValue() == 0
}

def getReleaseScript() {
    new File(getRoot(), "release.groovy")
}

def log(String msg) {
    println "[${new Date()}] ${msg}"
}
