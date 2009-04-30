/**
 * 
 */
package uk.me.gumbley.xplp;

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
        Pattern.compile("^(.*?)\\$\\{([^}]+?)\\}(.*?)$").matcher("");


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
        while(true) {
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
                break;
            }
        }
        return s;
    }
}
