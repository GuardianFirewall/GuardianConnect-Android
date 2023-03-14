/*
 * Copyright © 2017-2021 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.guardianconnect.configStore

import android.content.Context
import android.util.Log
import com.guardianconnect.R
import com.wireguard.config.BadConfigException
import com.wireguard.config.Config
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Configuration store that uses a `wg-quick`-style file for each configured tunnel.
 */
class FileConfigStore(private val context: Context) : ConfigStore {
    @Throws(IOException::class)
    override fun create(name: String, config: Config): Config {
        Log.d(TAG, "Creating configuration for tunnel $name")
        val file = fileFor(name)
        if (!file.createNewFile()) {
            file.delete()
            file.createNewFile()
        }
        FileOutputStream(file, false).use {
            it.write(
                config.toWgQuickString().toByteArray(StandardCharsets.UTF_8)
            )
        }
        return config
    }

    @Throws(IOException::class)
    override fun delete(name: String) {
        Log.d(TAG, "Deleting configuration for tunnel $name")
        val file = fileFor(name)
        if (!file.delete())
            throw IOException(context.getString(R.string.config_delete_error, file.name))
    }

    fun fileFor(name: String): File {
        return File(context.filesDir, "$name.conf")
    }

    @Throws(BadConfigException::class, IOException::class)
    override fun load(name: String): Config {
        FileInputStream(fileFor(name)).use { stream -> return Config.parse(stream) }
    }

    @Throws(IOException::class)
    override fun save(name: String, config: Config): Config {
        Log.d(TAG, "Saving configuration for tunnel $name")
        val file = fileFor(name)
        if (!file.isFile)
            throw FileNotFoundException(
                context.getString(
                    R.string.config_not_found_error,
                    file.name
                )
            )
        FileOutputStream(file, false).use { stream ->
            stream.write(
                config.toWgQuickString().toByteArray(StandardCharsets.UTF_8)
            )
        }
        return config
    }

    companion object {
        private const val TAG = "FileConfigStore"
    }
}
