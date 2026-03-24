package com.mascill.keutrack.core.common.utils

/**
 * Method to simplify nullable boolean null value to false instead
 *
 * Note: this method implementation is like [orEmpty]
 */
fun Boolean?.orFalse() = this ?: false