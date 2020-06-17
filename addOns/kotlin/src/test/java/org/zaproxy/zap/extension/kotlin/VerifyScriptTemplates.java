/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2020 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.zap.extension.kotlin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zaproxy.zap.extension.alert.ExtensionAlert;
import org.zaproxy.zap.testutils.AbstractVerifyScriptTemplates;

/** Verifies that the Kotlin script templates are parsed without errors. */
public class VerifyScriptTemplates extends AbstractVerifyScriptTemplates {

    private static ScriptEngine se;

    @BeforeAll
    public static void setUp() {
        se = new KotlinEngineWrapper(Thread.currentThread().getContextClassLoader()).getEngine();
    }

    @Override
    protected String getScriptExtension() {
        return ".kts";
    }

    @Override
    protected void parseTemplate(Path template) throws Exception {
        try (Reader reader = Files.newBufferedReader(template, StandardCharsets.UTF_8)) {
            Compilable c = (Compilable) se;
            CompiledScript cs = c.compile(reader);
            cs.eval();
        }
    }

    @Test
    public void shouldPrintToScriptContextWriter() throws Exception {
        String script = getScriptContents("printTest.kts");

        ByteArrayOutputStream console = new ByteArrayOutputStream();
        ScriptContext sc = se.getContext();
        Writer originalWriter = sc.getWriter();

        try {
            sc.setWriter(new PrintWriter(console));
            Compilable c = (Compilable) se;
            CompiledScript cs = c.compile(script);

            cs.eval();

            sc.getWriter().flush();
            assertEquals("ZAP" + System.lineSeparator(), console.toString());
        } finally {
            sc.setWriter(originalWriter);
        }
    }

    @Test
    public void shouldHaveSameHashCode() throws Exception {

        int jvmHashCode = ExtensionAlert.class.hashCode();

        String script = getScriptContents("hashcodeTest.kts");
        Compilable c = (Compilable) se;
        CompiledScript cs = c.compile(script);
        Object retVal = cs.eval();

        assertTrue(retVal instanceof Integer);

        Integer scriptHashCode = (Integer) retVal;
        assertEquals(jvmHashCode, scriptHashCode);
    }

    private String getScriptContents(String scriptName) throws IOException {
        return FileUtils.readFileToString(
                new File(this.getClass().getResource(scriptName).getFile()), "UTF-8");
    }
}
