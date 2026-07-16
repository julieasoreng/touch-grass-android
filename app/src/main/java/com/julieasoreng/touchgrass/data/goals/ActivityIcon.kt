package com.julieasoreng.touchgrass.data.goals

import androidx.annotation.DrawableRes
import com.julieasoreng.touchgrass.R

/** Every icon a goal can be drawn with, backed by a line-art vector drawable (from the
 *  "Touchgrass app icons" design project) rather than an emoji, so goals render identically
 *  across devices/fonts and can be recolored to match the goal's chosen [ActivityStyle.color]. */
enum class ActivityIcon(@DrawableRes val drawableRes: Int) {
    READING(R.drawable.ic_activity_reading),
    EXERCISE(R.drawable.ic_activity_exercise),
    ART(R.drawable.ic_activity_art),
    MEDITATION(R.drawable.ic_activity_meditation),
    MUSIC(R.drawable.ic_activity_music),
    WALK(R.drawable.ic_activity_walk),
    JOURNALING(R.drawable.ic_activity_journaling),
    GARDENING(R.drawable.ic_activity_gardening),
    BREATHING(R.drawable.ic_activity_breathing),
    COOKING(R.drawable.ic_activity_cooking),
    PHOTOGRAPHY(R.drawable.ic_activity_photography),
    PUZZLE(R.drawable.ic_activity_puzzle),
    SLEEP(R.drawable.ic_activity_sleep),
    TEA(R.drawable.ic_activity_tea),
    CALL(R.drawable.ic_activity_call)
}
