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

package org.devzendo.xplp;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests correct interpolation of name=value pairs from a
 * Properties object into a String, using Ant-style references
 * to names - e.g ${name}.
 * 
 * @author matt
 *
 */
public final class TestPropertiesInterpolation {
    private final String lineSeparator = System.getProperty("line.separator");
    private Properties mProps;
    private PropertiesInterpolator mInterpolator;
    private String mLibsString;
    
    @Before
    public void getPrerequisites() {
        mProps = new Properties();
        mProps.put("key", "value");
        mProps.put("long.key.name", "long value");
        mProps.put("return", "line1" + lineSeparator + "line2");
        
        final StringBuilder sb = new StringBuilder();
        sb.append("            <string>$JAVAROOT/lib/BeanCounter-0.1.0-SNAPSHOT.jar</string>" + lineSeparator);
        sb.append("            <string>$JAVAROOT/lib/MiniMiser-0.1.0-SNAPSHOT.jar</string>" + lineSeparator);
        mLibsString = sb.toString();
        mProps.put("xplp.macosxclasspatharray", mLibsString);
        
        mInterpolator = new PropertiesInterpolator(mProps);
    }
    
    @Test
    public void nullAndEmptyPassedStraightThrough() {
        Assert.assertNull(mInterpolator.interpolate(null));
        Assert.assertEquals(0, mInterpolator.interpolate("").length());
    }
    
    @Test
    public void noInterpolation() {
        Assert.assertEquals("verbatim text", mInterpolator.interpolate("verbatim text"));
    }
    
    @Test
    public void replaceOneInstance() {
        Assert.assertEquals("check value test", mInterpolator.interpolate("check ${key} test"));
        Assert.assertEquals("check long value test", mInterpolator.interpolate("check ${long.key.name} test"));
    }

    @Test
    public void replaceAtStart() {
        Assert.assertEquals("value test", mInterpolator.interpolate("${key} test"));
    }

    @Test
    public void replaceAtEnd() {
        Assert.assertEquals("test value", mInterpolator.interpolate("test ${key}"));
    }

    @Test
    public void replaceMultipleOccurrences() {
        Assert.assertEquals("check value test value investigate", mInterpolator.interpolate("check ${key} test ${key} investigate"));
    }
    
    @Test
    public void replaceMultipleOccurrencesMultipleKeys() {
        Assert.assertEquals("check value test long value foo long value",
            mInterpolator.interpolate("check ${key} test ${long.key.name} foo ${long.key.name}"));
    }

    @Test
    public void replaceMultipleOccurrencesMultipleKeysRightNextToEachOther() {
        Assert.assertEquals("valuelong valuelong value",
            mInterpolator.interpolate("${key}${long.key.name}${long.key.name}"));
    }

    @Test
    public void replaceMultipleOccurrencesMultipleKeysRightNextToEachOtherIncludingNewlines() {
        Assert.assertEquals("valueline1" + lineSeparator + "line2long value",
            mInterpolator.interpolate("${key}${return}${long.key.name}"));
    }

    @Test(expected = IllegalStateException.class)
    public void variableNotFound() {
        mInterpolator.interpolate("wahey ${nonexistant} frugal!");
    }
    
    @Test
    public void replaceLongString() {
        Assert.assertEquals(mLibsString, mInterpolator.interpolate("${xplp.macosxclasspatharray}"));
    }

    @Test
    public void dontInterpolateInComments() {
        Assert.assertEquals("# ${env.HOME}", mInterpolator.interpolate("# ${env.HOME}"));
    }

    @Test
    public void doNotReplaceNoInterpolatedVariables() {
        mInterpolator.doNotInterpolate("VERBATIM");
        Assert.assertEquals("check value test value don't replace ${VERBATIM} investigate", mInterpolator.interpolate("check ${key} test ${key} don't replace ${VERBATIM} investigate"));
    }

    @Test
    public void janelSpecialVerbatimVariablesAreNotReplaced() {
        // it isn't cloned in the constructor, so can modify input props after construction :)
        mProps.put("FOUND_EXE_FOLDER", "${FOUND_EXE_FOLDER}");
        Assert.assertEquals("check value test value don't replace ${FOUND_EXE_FOLDER} investigate", mInterpolator.interpolate("check ${key} test ${key} don't replace ${FOUND_EXE_FOLDER} investigate"));
    }
}
