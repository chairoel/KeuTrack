package com.mascill.keutrack.core.testing

import com.mascill.keutrack.core.common.utils.CommonDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

fun testCommonDispatcher(
    testDispatcher: TestDispatcher = StandardTestDispatcher(),
): CommonDispatcher = CommonDispatcher(
    io = testDispatcher,
    main = testDispatcher,
    computation = testDispatcher,
)
