package com.mascill.keutrack.core.testing

import com.mascill.keutrack.core.common.utils.CommonDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher

/**
 * Builds a [CommonDispatcher] whose io/main/computation all point at [testDispatcher].
 * [CommonDispatcher] is a concrete class (not an interface), so this is a factory rather
 * than a subtype.
 */
fun testCommonDispatcher(
    testDispatcher: TestDispatcher = StandardTestDispatcher(),
): CommonDispatcher = CommonDispatcher(
    io = testDispatcher,
    main = testDispatcher,
    computation = testDispatcher,
)
