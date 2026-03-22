package com.mascill.keutrack.core.common.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Class that provide dispatcher
 *
 * Note: dispatcher using this component is for unit testing purpose, because we need to set all
 * dispatcher to [Dispatchers.Unconfined] in unit testing
 */
class CommonDispatcher(
    val io: CoroutineDispatcher,
    val main: CoroutineDispatcher,
    val computation: CoroutineDispatcher
)