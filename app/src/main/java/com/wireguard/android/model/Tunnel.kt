/*
 * Copyright © 2018 Samuel Holland <samuel@sholland.org>
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.model

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.wireguard.android.BR
import com.wireguard.android.util.ExceptionLoggers
import com.wireguard.config.Config
import com.wireguard.util.Keyed
import java9.util.concurrent.CompletableFuture
import java9.util.concurrent.CompletionStage
import java.util.regex.Pattern

/**
 * Encapsulates the volatile and nonvolatile state of a WireGuard tunnel.
 */

class Tunnel internal constructor(
    private val manager: TunnelManager,
    private var name: String,
    private var config: Config?,
    private var state: State?
) : BaseObservable(), Keyed<String> {

    private var statistics: Statistics? = null

    val configAsync: CompletionStage<Config>
        get() = if (config == null) manager.getTunnelConfig(this) else CompletableFuture.completedFuture(config)

    val stateAsync: CompletionStage<State>
        get() = TunnelManager.getTunnelState(this)

    // FIXME: Check age of statistics.
    val statisticsAsync: CompletionStage<Statistics>
        get() = if (statistics == null) TunnelManager.getTunnelStatistics(this) else CompletableFuture.completedFuture(
            statistics
        )

    fun delete(): CompletionStage<Void> {
        return manager.delete(this)
    }

    @Bindable
    fun getConfig(): Config? {
        if (config == null)
            manager.getTunnelConfig(this).whenComplete(ExceptionLoggers.E)
        return config
    }

    override fun getKey(): String? {
        return name
    }

    @Bindable
    fun getName(): String {
        return name
    }

    @Bindable
    fun getState(): State? {
        return state
    }

    @Bindable
    fun getStatistics(): Statistics? {
        // FIXME: Check age of statistics.
        if (statistics == null)
            TunnelManager.getTunnelStatistics(this).whenComplete(ExceptionLoggers.E)
        return statistics
    }

    fun onConfigChanged(config: Config): Config {
        this.config = config
        notifyPropertyChanged(BR.config)
        return config
    }

    fun onNameChanged(name: String): String {
        this.name = name
        notifyPropertyChanged(BR.name)
        return name
    }

    fun onStateChanged(state: State?): State? {
        if (state != State.UP)
            onStatisticsChanged(null)
        this.state = state
        notifyPropertyChanged(BR.state)
        return state
    }

    fun onStatisticsChanged(statistics: Statistics?): Statistics? {
        this.statistics = statistics
        notifyPropertyChanged(BR.statistics)
        return statistics
    }

    fun setConfig(config: Config): CompletionStage<Config> {
        return if (config != this.config) manager.setTunnelConfig(this, config) else CompletableFuture.completedFuture(
            this.config
        )
    }

    fun setName(name: String): CompletionStage<String> {
        return if (name != this.name) manager.setTunnelName(
            this,
            name
        ) else CompletableFuture.completedFuture(this.name)
    }

    fun setState(state: State): CompletionStage<State> {
        return if (state != this.state) manager.setTunnelState(
            this,
            state
        ) else CompletableFuture.completedFuture(this.state)
    }

    enum class State {
        DOWN,
        TOGGLE,
        UP;

        companion object {

            fun of(running: Boolean): State {
                return if (running) UP else DOWN
            }
        }
    }

    class Statistics : BaseObservable()

    companion object {
        const val NAME_MAX_LENGTH = 15
        private val NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_=+.-]{1,15}")

        fun isNameInvalid(name: CharSequence): Boolean {
            return !NAME_PATTERN.matcher(name).matches()
        }
    }
}