package com.mascill.keutrack.core.designsystem.model

/**
 * Horizontal placement of the title region in `KeuTrackTopBar`.
 *
 * - **Start:** title follows `leading` inside the flexible start region.
 * - **Center:** title optically centered; leading / trailing overlay the sides and
 *   inset the title equally so it does not overlap.
 * - **End:** title is end-aligned in the flexible space before `trailing`.
 */
enum class KeuTrackTopBarTitleAlignment {
    Start,
    Center,
    End,
}
