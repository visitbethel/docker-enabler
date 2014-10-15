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

public enum BuildCmdOptions {
    USE_CACHE("--nocache", true, true), //
    REMOVE_SUCCESS("--rm", true, true), //
    REMOVE_ALWAYS("--force-rm", true, false), //
    BUILD_VERBOSE("-q", true, true);

    public static Optional<String> build(RuntimeContextVariable var) {
        try {
            BuildCmdOptions val = valueOf(var.getName());
            if (var.getTypeInt() != RuntimeContextVariable.OBJECT_TYPE) {
                String currentValue = StringUtils.trimToEmpty((String) var.getValue());
                // empty value means the option is not valid and would be skipped
                // so that we accept the default
                if (!currentValue.isEmpty()) {
                    if (val.isBooleanType) {
                        Boolean b = BooleanUtils.toBooleanObject(currentValue);
                        if (b != val.defaultBooleanValue) {
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

    private final String optionSwitch;
    private final boolean isBooleanType;
    private final boolean defaultBooleanValue; // default value if option type is boolean

    private BuildCmdOptions(String optionSwitch, boolean isBooleanType) {
        this(optionSwitch, isBooleanType, false);
    }

    private BuildCmdOptions(String optionSwitch, boolean isBooleanType, boolean defaultBooleanValue) {
        this.optionSwitch = optionSwitch;
        this.isBooleanType = isBooleanType;
        this.defaultBooleanValue = defaultBooleanValue;
    }
}
