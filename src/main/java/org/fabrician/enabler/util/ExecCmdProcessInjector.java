/*
 * Copyright (c) 2014 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrician.enabler.util;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.fabrician.enabler.DockerContainer;

import com.datasynapse.fabric.container.ProcessWrapper;
import com.datasynapse.fabric.util.ContainerUtils;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;

/**
 * This utility class injects or "spawn" one or more helper processes into a running parent Docker container.
 * 
 * @see <a href="https://docs.docker.com/reference/commandline/cli/#exec">docker exec</a>
 */
public class ExecCmdProcessInjector {
    private static Logger logger = ContainerUtils.getLogger(ExecCmdProcessInjector.class);
    private final DockerContainer enabler;
    private final long cmdInjectionDelay;
    private final List<String> cmds;

    public ExecCmdProcessInjector(DockerContainer enabler, URL cmdUrl, long cmdInjectionDelay) throws IOException {
        this.enabler = enabler;
        this.cmdInjectionDelay = cmdInjectionDelay;
        this.cmds = Resources.readLines(cmdUrl, Charsets.UTF_8, new ExecCmdLineProcessor());
    }

    public static void exec(DockerContainer enabler, URL cmdUrl, long cmdInjectionDelay) throws Exception {
        ExecCmdProcessInjector injector = new ExecCmdProcessInjector(enabler, cmdUrl, cmdInjectionDelay);
        injector.exec();
    }

    public void exec() throws Exception {
        for (int i = 0; i < cmds.size(); i++) {
            String cmd=cmds.get(i);
            try {  
                ProcessWrapper p = enabler.getExecCmdProcessWrapper(cmd);
                p.setShowProcessCmdLog(true);
                p.exec();
                if (i < cmds.size() - 1) {
                    TimeUnit.SECONDS.sleep(cmdInjectionDelay);
                }
            } catch (InterruptedException ex) {
                logger.warning("while waiting for next 'docker exec' command to be injected, got thread interrupted.");
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "while injecting 'docker exec' command [" + cmd + "] into running docker container " + enabler.dockerContainerInfo(), ex);
                throw ex;
            }
        }
    }

    private static class ExecCmdLineProcessor implements LineProcessor<List<String>> {
        private final List<String> cmds = Lists.newArrayList();

        public boolean processLine(String line) throws IOException {
            String cmd = StringUtils.trimToEmpty(line);
            if (cmd.isEmpty() || cmd.startsWith("#")) {
                return true;
            }
            cmds.add(cmd);
            return true;
        }

        public List<String> getResult() {
            return Collections.unmodifiableList(cmds);
        }

    }

}
