/*
 * Copyright (c) 2014 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrician.enabler.util;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.datasynapse.fabric.common.RuntimeContext;
import com.datasynapse.fabric.common.RuntimeContextVariable;
import com.google.common.base.Optional;

/**
 * 
 * A class to build some useful auxiliary Docker CLI RUN command options from Runtime Context variables which represents them. The enums below represents some RUN options from the Enabler's
 * "container.xml" and they must matched and must have an "option switch". These are non port(-p) or volume(-v) mapping options or enviromental variables options(-e, --env-file).
 */
public enum RunCmdAuxiliaryOptions {
    CID_FILE("--cidfile",false),//
    ENTRY_POINT_OVERRIDE("--entrypoint", false), //
    MEMORY_LIMIT("-m", false), //
    PRIVILEDGED_MODE("--privileged", true,false), //
    USER_OVERRIDE("-u", false), //
    WORKDIR_OVERRIDE("-w", false);

    public static Optional<String> build(RuntimeContextVariable var) {
        try {
            RunCmdAuxiliaryOptions val = valueOf(var.getName());
            if (var.getTypeInt() != RuntimeContextVariable.OBJECT_TYPE) {
                String currentValue = StringUtils.trimToEmpty((String) var.getValue());
                // empty value means the option is not valid and would be skipped
                // so that we accept the default
                if (!currentValue.isEmpty()) {
                    if (val.isBooleanType) {
                        Boolean b = BooleanUtils.toBooleanObject(currentValue);
                        if (b !=val.defaultBooleanValue) {
                            return Optional.of(" " + val.optionSwitch + " ");
                        }
                    } else {
                        return Optional.of(" " + val.optionSwitch + " " + currentValue);
                    }
                }
            }

        } catch (Exception ex) {

        }
        return Optional.absent();
    }

    public static String buildAll(RuntimeContext rtc) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rtc.getVariableCount(); i++) {
            RuntimeContextVariable var = rtc.getVariable(i);
            Optional<String> option = build(var);
            if (option.isPresent()) {
                sb.append(option.get());
            }
        }
        return sb.toString();
    }

    private RunCmdAuxiliaryOptions(String optionSwitch, boolean isBooleanType) {
        this(optionSwitch,isBooleanType,false);
    }

    private RunCmdAuxiliaryOptions(String optionSwitch, boolean isBooleanType,boolean defaultBooleanValue){
        this.optionSwitch = optionSwitch;
        this.isBooleanType = isBooleanType;
        this.defaultBooleanValue=defaultBooleanValue;
    }
    private final String optionSwitch;
    private final boolean isBooleanType;
    private final boolean defaultBooleanValue; // default value if option type is boolean
}
