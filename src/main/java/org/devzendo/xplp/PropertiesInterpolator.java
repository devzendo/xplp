/**
 * Copyright (C) 2008-2010 Matt Gumbley, DevZendo.org <http://devzendo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * 
 */
package org.devzendo.xplp;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpolation of name=value pairs from a
 * Properties object into a String, using Ant-style references
 * to names - e.g ${name}.
 * @author matt
 *
 */
public final class PropertiesInterpolator {
    private final Properties mProps;
    private final Matcher variableReferenceMatcher = 
        Pattern.compile("^(.*?)\\$\\{([^}]+?)\\}(.*?)$", Pattern.DOTALL).matcher("");

    /**
     * Create an interpolator, given a set of properties to
     * interpolate inside incoming string data.
     * @param props the name=value pairs.
     * 
     */
    public PropertiesInterpolator(final Properties props) {
        mProps = props;
    }

    /**
     * Replace any ${names} with their ${values}, except in # lines
     * @param input the input string, can be null
     * @return the output, null iff input == null
     */
    public String interpolate(final String input) {
        //System.out.println("Interpolating '" + input + "'");
        if (input == null || input.length() == 0) {
            return input;
        }
        if (input.matches("^\\s*#.*$")) {
            return input;
        }
        String s = input;
        while (true) {
            //System.out.println("Finding variables in '" + s + "'");
            variableReferenceMatcher.reset(s);
            if (variableReferenceMatcher.find()) {
                //System.out.println("Found variable");
                final String before = variableReferenceMatcher.group(1);
                final String variableName = variableReferenceMatcher.group(2);
                final String after = variableReferenceMatcher.group(3);
                //System.out.println("Variable '" + variableName + "'");
                if (mProps.containsKey(variableName)) {
                    final String variableValue = mProps.getProperty(variableName);
                    //System.out.println("Replacement is '" + variableValue + "'");
                    s = before + variableValue + after;
                } else {
                    //System.out.println("Got no value for variable");
                    throw new IllegalStateException("The name '" + variableName + "' is not defined");
                }
            } else {
                //System.out.println("No more variables");
                break;
            }
        }
        //System.out.println("Output is '" + s + "'");
        return s;
    }
}
