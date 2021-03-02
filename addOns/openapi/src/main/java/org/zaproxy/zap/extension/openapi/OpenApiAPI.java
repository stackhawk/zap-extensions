/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2017 The ZAP Development Team
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
package org.zaproxy.zap.extension.openapi;

import java.io.File;
import java.util.List;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.zaproxy.zap.extension.api.ApiAction;
import org.zaproxy.zap.extension.api.ApiException;
import org.zaproxy.zap.extension.api.ApiImplementor;
import org.zaproxy.zap.extension.api.ApiResponse;
import org.zaproxy.zap.extension.api.ApiResponseElement;
import org.zaproxy.zap.extension.api.ApiResponseList;
import org.zaproxy.zap.extension.openapi.converter.swagger.InvalidUrlException;

public class OpenApiAPI extends ApiImplementor {

    private static final String PREFIX = "openapi";
    static final String ACTION_IMPORT_FILE = "importFile";
    static final String ACTION_IMPORT_URL = "importUrl";
    static final String ACTION_GET_STRUCTURAL_NODE_MODIFIERS = "getStructuralNodeModifiers";
    static final String PARAM_URL = "url";
    static final String PARAM_FILE = "file";
    static final String PARAM_TARGET = "target";

    private static final String PARAM_HOST_OVERRIDE = "hostOverride";
    private ExtensionOpenApi extension = null;

    /** Provided only for API client generator usage. */
    public OpenApiAPI() {
        this(null);
    }

    public OpenApiAPI(ExtensionOpenApi ext) {
        extension = ext;
        this.addApiAction(
                new ApiAction(
                        ACTION_IMPORT_FILE,
                        new String[] {PARAM_FILE},
                        new String[] {PARAM_TARGET}));
        this.addApiAction(
                new ApiAction(
                        ACTION_IMPORT_URL,
                        new String[] {PARAM_URL},
                        new String[] {PARAM_HOST_OVERRIDE}));
        this.addApiAction(
                new ApiAction(
                        ACTION_GET_STRUCTURAL_NODE_MODIFIERS,
                        new String[] {PARAM_FILE},
                        new String[] {PARAM_TARGET}));
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    @Override
    public ApiResponse handleApiAction(String name, JSONObject params) throws ApiException {
        if (ACTION_IMPORT_FILE.equals(name)) {
            File file = handleFile(params);
            List<String> errors;
            String target = params.optString(PARAM_TARGET, "");
            try {
                errors = extension.importOpenApiDefinition(file, target, false);
            } catch (InvalidUrlException e) {
                throw new ApiException(ApiException.Type.ILLEGAL_PARAMETER, PARAM_TARGET);
            }

            checkErrors(errors);
            return new ApiResponseList(name);

        } else if (ACTION_IMPORT_URL.equals(name)) {
            try {
                String override = params.optString(PARAM_HOST_OVERRIDE, "");

                List<String> errors =
                        extension.importOpenApiDefinition(
                                new URI(params.getString(PARAM_URL), false), override, false);

                checkErrors(errors);
                return new ApiResponseList(name);
            } catch (URIException e) {
                throw new ApiException(ApiException.Type.ILLEGAL_PARAMETER, PARAM_URL);
            } catch (InvalidUrlException e) {
                throw new ApiException(ApiException.Type.ILLEGAL_PARAMETER, PARAM_HOST_OVERRIDE);
            }

        } else if (ACTION_GET_STRUCTURAL_NODE_MODIFIERS.equals(name)) {
            File file = handleFile(params);
            String target = params.optString(PARAM_TARGET, "");
            ApiResponse[] responses;
            try {
                responses =
                        extension.getStructuralNodeModifiers(file, target).stream()
                                .map(
                                        (node) ->
                                                new ApiResponseElement(
                                                        node.getName(),
                                                        node.getPattern().toString()))
                                .toArray(ApiResponse[]::new);
            } catch (InvalidUrlException e) {
                throw new ApiException(ApiException.Type.ILLEGAL_PARAMETER, PARAM_TARGET);
            }

            return new ApiResponseList(name, responses);
        } else {
            throw new ApiException(ApiException.Type.BAD_ACTION);
        }
    }

    private File handleFile(JSONObject params) throws ApiException {
        File file = new File(params.getString(PARAM_FILE));
        if (!file.exists() || !file.canRead()) {
            throw new ApiException(ApiException.Type.DOES_NOT_EXIST, file.getAbsolutePath());
        }

        if (!file.isFile()) {
            throw new ApiException(
                    ApiException.Type.ILLEGAL_PARAMETER,
                    "Not a regular file " + file.getAbsolutePath());
        }
        return file;
    }

    private void checkErrors(List<String> errors) throws ApiException {
        if (errors != null && !errors.isEmpty()) {
            String msg = String.join(";", errors);
            throw new ApiException(ApiException.Type.ILLEGAL_PARAMETER, msg);
        }
    }
}
