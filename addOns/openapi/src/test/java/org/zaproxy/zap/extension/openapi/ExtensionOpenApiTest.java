package org.zaproxy.zap.extension.openapi;

import fi.iki.elonen.NanoHTTPD;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.List;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.extension.ExtensionLoader;
import org.zaproxy.zap.extension.spider.ExtensionSpider;
import org.zaproxy.zap.testutils.NanoServerHandler;


import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class ExtensionOpenApiTest extends AbstractServerTest {

    private ExtensionOpenApi classUnderTest;

    @BeforeEach
    public void setup() throws Exception {
        classUnderTest = new ExtensionOpenApi();

        Control control = mock(Control.class, withSettings().lenient());
        setControlSingleton(control);

        ExtensionLoader extensionLoader = mock(ExtensionLoader.class, withSettings().lenient());
        when(control.getExtensionLoader()).thenReturn(extensionLoader);

        given(extensionLoader.getExtension(ExtensionSpider.class)).willReturn(mock(ExtensionSpider.class));
    }

    @Test
    public void shouldFailWithInvalidOverrideUrl() throws IOException {
        // given
        String test = "/PetStoreJson/";
        String defnName = "defn.json";
        this.nano.addHandler(new DefnServerHandler(test, defnName, "PetStore_defn.json"));
        String fileContents = getHtml("PetStore_defn.json",
                new String[][] {{"PORT", String.valueOf(this.nano.getListeningPort())}});
        File tmpFile = File.createTempFile("foo", "bar");
        FileUtils.write(tmpFile, fileContents, Charset.defaultCharset());

        // when
        List<String> errors = classUnderTest.importOpenApiDefinition(tmpFile, "htp:/", false);

        //then
        assertThat("Should fail because of bad target override URL", !errors.isEmpty());
    }

    @Test
    public void shouldImportFile() throws IOException {
        // given
        String test = "/PetStoreJson/";
        String defnName = "defn.json";
        this.nano.addHandler(new DefnServerHandler(test, defnName, "PetStore_defn.json"));
        String fileContents = getHtml("PetStore_defn.json",
                new String[][] {{"PORT", String.valueOf(this.nano.getListeningPort())}});
        File tmpFile = File.createTempFile("foo", "bar");
        FileUtils.write(tmpFile, fileContents, Charset.defaultCharset());

        // when
        List<String> errors = classUnderTest.importOpenApiDefinition(tmpFile, false);

        //then
        assertThat("Should parse OK", errors.isEmpty());
    }

    @Test
    public void shouldFailNonOpenApiURL() throws URIException {

        // given
        String test = "/PetStoreJson/";
        String defnName = "defn.json";
        this.nano.addHandler(new DefnServerHandler(test, defnName, "PetStore_defn.json"));
        URI uri = new URI("http://localhost:" + this.nano.getListeningPort() + "/fake", false);

        // when
        List<String> errors = classUnderTest.importOpenApiDefinition(uri, null, false);

        //then
        assertThat("Should fail fake URL", errors != null && !errors.isEmpty());
    }

    @Test
    public void shouldFailNonExistentUrl() throws URIException {

        // given
        URI uri = new URI("http://foo", false);

        // when
        List<String> errors = classUnderTest.importOpenApiDefinition(uri, null, false);

        //then
        assertThat("Should fail fake URL", errors != null && !errors.isEmpty());
    }

    @Test
    public void shouldFailBadJson() throws IOException {
        // given
        File file = getResourcePath("bad-json.json").toFile();

        // when
        List<String> errors = classUnderTest.importOpenApiDefinition(file, false);

        //then
        assertThat("Should fail to parse bad json", !errors.isEmpty());
    }

    @Test
    public void shouldFailBadYaml() throws IOException {
        // given
        File file = getResourcePath("bad-yaml.yml").toFile();

        // when
        List<String> errors = classUnderTest.importOpenApiDefinition(file, false);

        //then
        assertThat("Should fail to parse bad yaml", !errors.isEmpty());
    }

    private static void setControlSingleton(Control control) throws Exception {
        Field field =
                ReflectionSupport.findFields(
                        Control.class,
                        f -> "control".equals(f.getName()),
                        HierarchyTraversalMode.TOP_DOWN)
                        .get(0);
        field.setAccessible(true);
        field.set(Control.class, control);
    }

    private class DefnServerHandler extends NanoServerHandler {

        private final String defnName;
        private final String defnFileName;
        private final String port;

        public DefnServerHandler(String name, String defnName, String defnFileName) {
            this(name, defnName, defnFileName, nano.getListeningPort());
        }

        public DefnServerHandler(String name, String defnName, String defnFileName, int port) {
            super(name);
            this.defnName = defnName;
            this.defnFileName = defnFileName;
            this.port = String.valueOf(port);
        }

        @Override
        protected NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
            String response;
            if (session.getUri().endsWith(defnName)) {
                response = getHtml(defnFileName, new String[][] {{"PORT", port}});
            } else {
                // We dont actually care about the response in this handler ;)
                response = getHtml("Blank.html");
            }
            return newFixedLengthResponse(response);
        }
    }
}
