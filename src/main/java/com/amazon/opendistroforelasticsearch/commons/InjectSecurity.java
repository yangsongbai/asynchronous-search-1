/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazon.opendistroforelasticsearch.commons;

import static com.amazon.opendistroforelasticsearch.commons.ConfigConstants.INJECTED_USER;
import static com.amazon.opendistroforelasticsearch.commons.ConfigConstants.OPENDISTRO_SECURITY_INJECTED_ROLES;
import static com.amazon.opendistroforelasticsearch.commons.ConfigConstants.OPENDISTRO_SECURITY_USE_INJECTED_USER_FOR_PLUGINS;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;

public class InjectSecurity implements AutoCloseable {

    private String id;
    private ThreadContext.StoredContext ctx = null;
    private ThreadContext threadContext;
    private Settings settings;
    private final Logger log = LogManager.getLogger(this.getClass());

    /**
     * Create InjectSecurity object. This is auto-closeable. Id is used only for logging purpose.
     * @param id
     * @param settings
     * @param tc
     */
    public InjectSecurity(final String id, final Settings settings, final ThreadContext tc) {
        this.id = id;
        this.settings = settings;
        this.threadContext = tc;

        this.ctx = tc.newStoredContext(true);
        log.trace("{}, InjectSecurity constructor: {}", Thread.currentThread().getName(), id);
    }

    /**
     * Injects user or roles, based on opendistro_security_use_injected_user_for_plugins setting. By default injects roles.
     * @param user
     * @param roles
     */
    public void inject(final String user, final List<String> roles) {
        boolean injectUser = settings.getAsBoolean(OPENDISTRO_SECURITY_USE_INJECTED_USER_FOR_PLUGINS, false);
        if (injectUser)
            injectUser(user);
        else
            injectRoles(roles);
    }

    /**
     * Injects user.
     * @param user
     */
    public void injectUser(final String user) {
        if (Strings.isNullOrEmpty(user)) {
            return;
        }

        if (threadContext.getTransient(INJECTED_USER) == null) {
            threadContext.putTransient(INJECTED_USER, user);
            log.debug("{}, InjectSecurity - inject roles: {}", Thread.currentThread().getName(), id);
        } else {
            log.error("{}, InjectSecurity- most likely thread context corruption : {}", Thread.currentThread().getName(), id);
        }
    }

    /**
     * Injects roles. Comma separated roles.
     * @param roles
     */
    public void injectRoles(final List<String> roles) {

        if ((roles == null) || (roles.size() == 0)) {
            return;
        }

        final String rolesStr = String.join(",", roles);
        String injectStr = "plugin|" + rolesStr;
        if (threadContext.getTransient(OPENDISTRO_SECURITY_INJECTED_ROLES) == null) {
            threadContext.putTransient(OPENDISTRO_SECURITY_INJECTED_ROLES, injectStr);
            log.debug("{}, InjectSecurity - inject roles: {}", Thread.currentThread().getName(), id);
        } else {
            log.error("{}, InjectSecurity- most likely thread context corruption : {}", Thread.currentThread().getName(), id);
        }
    }

    @Override
    public void close() {
        if (ctx != null) {
            ctx.close();
            log.trace("{}, InjectSecurity close : {}", Thread.currentThread().getName(), id);
        }
    }
}