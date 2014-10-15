/*
* Copyright (c) 2014 TIBCO Software Inc. All Rights Reserved.
*
* Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
* In most instances, the license terms are contained in a file named license.txt.
*/
package org.fabrician.enabler;

import com.datasynapse.fabric.domain.featureinfo.ApplicationLoggingInfo;

public class DockerLoggingInfo extends ApplicationLoggingInfo{
    private static final long serialVersionUID = -2544030600755386056L;
    public static final String[] DEFAULT_PATTERNS = { "docker/docker_logs/.*\\.log.*" };

    protected String[] getDefaultPatterns() {
        return DEFAULT_PATTERNS;
    }

}
