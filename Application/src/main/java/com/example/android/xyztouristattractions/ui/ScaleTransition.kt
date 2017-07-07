package com.example.android.xyztouristattractions.ui

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.transition.TransitionValues
import android.transition.Visibility
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

/**
 * A simple scale transition class to allow an element to scale in or out.
 * This is used by the floating action button on the attraction detail screen
 * when it appears and disappears during the Activity transitions.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class ScaleTransition(context: Context, attrs: AttributeSet) : Visibility(context, attrs) {

    fun createAnimation(view: View, startScale: Float, endScale: Float): Animator {
        view.scaleX = startScale
        view.scaleY = startScale
        val holderX = PropertyValuesHolder.ofFloat("scaleX", startScale, endScale)
        val holderY = PropertyValuesHolder.ofFloat("scaleY", startScale, endScale)
        return ObjectAnimator.ofPropertyValuesHolder(view, holderX, holderY)
    }

    override fun onAppear(sceneRoot: ViewGroup, view: View, startValues: TransitionValues,
                          endValues: TransitionValues): Animator {
        return createAnimation(view, 0f, 1f)
    }

    override fun onDisappear(sceneRoot: ViewGroup, view: View, startValues: TransitionValues,
                             endValues: TransitionValues): Animator {
        return createAnimation(view, 1f, 0f)
    }
}