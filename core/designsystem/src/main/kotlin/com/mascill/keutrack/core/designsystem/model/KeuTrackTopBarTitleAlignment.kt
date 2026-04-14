package com.mascill.keutrack.core.designsystem.model

/**
 * Horizontal placement of the title region in `KeuTrackTopBar`.
 *
 * - **Start:** title follows `leading` inside the flexible start region.
 * - **Center:** title in the optical center column (balanced with leading / trailing widths).
 * - **End:** title is end-aligned in the flexible space before `trailing`.
 */
enum class KeuTrackTopBarTitleAlignment {
    Start,
    Center,
    End,
}
