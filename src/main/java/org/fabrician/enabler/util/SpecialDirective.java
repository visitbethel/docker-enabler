/*
 * Copyright (c) 2014 TIBCO Software Inc. All Rights Reserved.
 *
 * Use is subject to the terms of the TIBCO license terms accompanying the download of this code.
 * In most instances, the license terms are contained in a file named license.txt.
 */
package org.fabrician.enabler.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.datasynapse.fabric.container.AbstractContainer;

/**
 * This class is used to resolve any special directive runtime context variable substitution tokens embedded in the string value of another runtime context variable. <code>
 * Example: HTTP_PORT=9000, !PORT_EXPOSE_xyz=80
 * !PORT_MAP_http1=${HTTP_PORT}:${!PORT_EXPOSE_xyz}
 * </code>
 * <p>
 * Above code will not be substituted for the substitution token ${!PORT_EXPOSE_xyz} because of the character (!) when using normal variables resolution. Instead, we use the following call to resolve
 * the embedded any special directive runtime context variable substitution tokens:
 * </p>
 * <code>
 * DockerContainer enabler =....
 * String http_port_map=SpecialDirective.resolveStringValue(enabler,"${HTTP_PORT}:${!PORT_EXPOSE_xyz}");
 * 
 * which should give us String http_port_map="9000:80"
 * </code>
 * 
 * Note: By convention, context variables that are prefixed by-
 * 
 * <pre>
 * "!PORT_EXPOSE_" are reckoned to be a private container ports to be exposed
 * "!PORT_MAP_" are reckoned to be a host-to-container ports mapping
 * "!VOL_MAP_" are reckoned to be a host-to-container volume mapping
 * "!ENV_VAR" are reckoned to be container enviromental variables mapping
 * "!ENV_FILE_" are reckoned to be container enviromental variables mapping
 * "!SEC_OPT_" are reckoned to be container security options
 * </pre>
 */
public enum SpecialDirective {
    PORT_EXPOSE("!PORT_EXPOSE_", "\\$\\{\\!PORT_EXPOSE_[a-zA-Z0-9_]+[a-zA-Z0-9_-]*\\}"), //
    PORT_MAP("!PORT_MAP_", "\\$\\{\\!PORT_MAP_[a-zA-Z0-9_]+[a-zA-Z0-9_-]*\\}"), //
    VOL_MAP("!VOL_MAP_", "\\$\\{\\!VOL_MAP_[a-zA-Z0-9_]+[a-zA-Z0-9_-]*\\}"), //
    ENV_VAR("!ENV_VAR_", "\\$\\{\\!ENV_VAR_[a-zA-Z0-9_]+[a-zA-Z0-9_-]*\\}"), //
    ENV_FILE("!ENV_FILE_", "\\$\\{\\!ENV_FILE_[a-zA-Z0-9_]+[a-zA-Z0-9_-]*\\}"), //
    SEC_OPT("!SEC_OPT_", "\\$\\{\\!SEC_OPT_[a-zA-Z0-9_]+[a-zA-Z0-9_-]*\\}");

    /**
     * Check if a string is prefixed by a special directive
     * 
     * @param str
     *            the string to check
     * @return true if prefixed; false otherwise
     */
    public boolean prefix(String str) {
        return str.startsWith(prefix);
    }

    /**
     * Return a list of prefixes represented by this enum
     * 
     * @return a list of prefixes
     */
    public static List<String> getPrefixes() {
        List<String> prefixes = new ArrayList<String>();
        for (SpecialDirective directive : values()) {
            prefixes.add(directive.prefix);
        }
        return Collections.unmodifiableList(prefixes);
    }

    /**
     * Resolve a string value including the special directive runtime context variables substitution tokens.
     * 
     * @param enabler
     *            the enabler use
     * @param str
     *            the string value
     * @return a fully resolved string value
     * @throws Exception
     */
    public static String resolveStringValue(AbstractContainer enabler, String str) throws Exception {
        if (StringUtils.trimToEmpty(str).isEmpty()) {
            return str;
        }
        String resolvedStr = enabler.resolveVariables(str);
        List<String> substitution_tokens = extractSubstitutionTokens(resolvedStr);
        if (substitution_tokens.isEmpty()) {
            return resolvedStr;
        }
        Map<String, String> resolutionMap = resolveSubstitutionTokens(enabler, substitution_tokens);
        resolvedStr = StringUtils.replaceEachRepeatedly(resolvedStr, substitution_tokens.toArray(new String[] {}), getReplacementValues(substitution_tokens, resolutionMap));
        return resolvedStr;
    }

    /**
     * Extract the list of 'Special Directive' runtime context variable substitution tokens embedded in a String
     * 
     * @param str
     *            the string to extract from
     * @return a List of substitution tokens.
     */
    public static List<String> extractSubstitutionTokens(String str) {
        List<String> list = new ArrayList<String>();
        if (StringUtils.trimToEmpty(str).isEmpty()) {
            return Collections.unmodifiableList(list);
        }
        for (SpecialDirective directive : list()) {
            Matcher m = directive.substitutionPattern.matcher(str);
            if (m.find()) {
                String substring = m.group(0);
                if (substring != null && !list.contains(substring)) {
                    list.add(substring);
                }
            }
        }
        return Collections.unmodifiableList(list);
    }

    private static List<SpecialDirective> list() {
        return Arrays.asList(values());
    }

    private static Map<String, String> resolveSubstitutionTokens(AbstractContainer enabler, List<String> substitutionTokens) {
        Map<String, String> resolvedMap = new HashMap<String, String>();
        for (String subToken : substitutionTokens) {
            try {
                String contextVar = subToken.substring(2, subToken.length() - 1); // assume ${XYZ} where XYZ == the contextVar
                String contextVarValue = enabler.getStringVariableValue(contextVar);
                String resolvedVarValue = enabler.resolveVariables(contextVarValue);
                List<String> specialSubstitutions2 = extractSubstitutionTokens(resolvedVarValue);
                if (specialSubstitutions2.isEmpty()) {
                    resolvedMap.put(subToken, resolvedVarValue);
                } else {
                    Map<String, String> resolvedMap2 = resolveSubstitutionTokens(enabler, specialSubstitutions2);
                    String resolvedVarValue2 = StringUtils.replaceEachRepeatedly(resolvedVarValue, specialSubstitutions2.toArray(new String[] {}),
                            getReplacementValues(specialSubstitutions2, resolvedMap2));
                    resolvedMap.put(subToken, resolvedVarValue2);
                }
            } catch (Exception ex) {

            }
        }
        return Collections.unmodifiableMap(resolvedMap);
    }

    private static String[] getReplacementValues(List<String> substitutionTokens, Map<String, String> substitutionTokenValueMap) {
        List<String> values = new ArrayList<String>();
        for (String token : substitutionTokens) {
            values.add(substitutionTokenValueMap.get(token));
        }
        return values.toArray(new String[] {});
    }

    private SpecialDirective(final String prefix, final String substitutionPattern) {
        this.prefix = prefix;
        this.substitutionPattern = Pattern.compile(substitutionPattern);
    }

    private final String prefix;
    private final Pattern substitutionPattern;

}
