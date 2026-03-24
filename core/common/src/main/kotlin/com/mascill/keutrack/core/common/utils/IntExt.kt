package com.mascill.keutrack.core.common.utils

/**
 * Method to simplify nullable integer null value to 0 instead
 *
 * Note: this method implementation is like [orEmpty]
 */
fun Int?.orZero() = this ?: 0