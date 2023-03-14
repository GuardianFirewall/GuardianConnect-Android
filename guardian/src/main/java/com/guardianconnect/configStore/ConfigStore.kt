/*
 * Copyright © 2017-2021 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.guardianconnect.configStore

import com.wireguard.config.Config

/**
 * Interface for persistent storage providers for WireGuard configurations.
 */
interface ConfigStore {
    /**
     * Create a persistent tunnel, which must have a unique name within the persistent storage
     * medium.
     *
     * @param name   The name of the tunnel to create.
     * @param config Configuration for the new tunnel.
     * @return The configuration that was actually saved to persistent storage.
     */
    @Throws(Exception::class)
    fun create(name: String, config: Config): Config

    /**
     * Delete a persistent tunnel.
     *
     * @param name The name of the tunnel to delete.
     */
    @Throws(Exception::class)
    fun delete(name: String)

    /**
     * Load the configuration for the tunnel given by `name`.
     *
     * @param name The identifier for the configuration in persistent storage (i.e. the name of the
     * tunnel).
     * @return An in-memory representation of the configuration loaded from persistent storage.
     */
    @Throws(Exception::class)
    fun load(name: String): Config

    /**
     * Save the configuration for an existing tunnel given by `name`.
     *
     * @param name   The identifier for the configuration in persistent storage (i.e. the name of
     * the tunnel).
     * @param config An updated configuration object for the tunnel.
     * @return The configuration that was actually saved to persistent storage.
     */
    @Throws(Exception::class)
    fun save(name: String, config: Config): Config
}
