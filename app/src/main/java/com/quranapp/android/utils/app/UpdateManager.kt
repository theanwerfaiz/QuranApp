/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 1/3/2022.
 * All rights reserved.
 */
package com.quranapp.android.utils.app

import android.animation.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import com.peacedesign.android.utils.AppBridge
import com.peacedesign.android.utils.ColorUtils
import com.peacedesign.android.utils.ViewUtils
import com.peacedesign.android.utils.kotlin_utils.dp2px
import com.peacedesign.android.utils.kotlin_utils.removeView
import com.peacedesign.android.utils.kotlin_utils.visible
import com.peacedesign.android.widget.dialog.base.PeaceDialog
import com.quranapp.android.R
import com.quranapp.android.components.AppUpdateInfo
import com.quranapp.android.databinding.LytUpdateAppBinding
import com.quranapp.android.databinding.LytUpdateAppDialogBinding
import com.quranapp.android.utils.Logger
import kotlin.math.pow

class UpdateManager(private val ctx: Context, private val parent: ViewGroup?) {
    private val mIconAnimationHandler = Handler(Looper.getMainLooper())
    private var mIconAnimators = ArrayList<ObjectAnimator>()

    /**
     * Returns true if there is an update available
     */
    fun check4CriticalUpdate(): Boolean {
        if (AppUpdateInfo().getMostImportantUpdate().priority == AppUpdateInfo.CRITICAL) {
            Logger.print("UpdateManager:", "Critical update available")
            showUpdateAvailableDialog(true)
            return true
        }
        return false
    }

    /**
     * Returns true if there is an update available
     */
    fun check4NonCriticalUpdate(): Boolean {
        val priority = AppUpdateInfo().getMostImportantUpdate().priority
        Logger.print("Update priority = $priority")

        when (priority) {
            AppUpdateInfo.NONE -> {
                return false
            }

            AppUpdateInfo.CRITICAL -> {
                showUpdateAvailableDialog(true)
            }
            AppUpdateInfo.MAJOR -> {
                showUpdateAvailableDialog(false) { updateAvailable() }
            }
            else -> {
                updateAvailable()
            }
        }

        return priority == AppUpdateInfo.CRITICAL
    }

    private fun showUpdateAvailableDialog(isCritical: Boolean, runOnDismiss: Runnable? = null) {
        val binding = LytUpdateAppDialogBinding.inflate(LayoutInflater.from(ctx))
        binding.txt.setText(if (isCritical) R.string.strMsgUpdateAvailable2Continue else R.string.strMsgUpdateAvailable4Dialog)
        mIconAnimators.add(animateUpdateIcon(binding.icon))

        val builder = PeaceDialog.newBuilder(ctx)
        builder.setView(binding.root)
        builder.setCancelable(false)
        builder.setOnDismissListener { runOnDismiss?.run() }
        builder.setPositiveButton(R.string.strLabelUpdate, ColorUtils.DANGER) { _, _ ->
            AppBridge.newOpener(ctx).openPlayStore()
        }
        builder.setDismissOnPositive(!isCritical)

        if (!isCritical) {
            builder.setNeutralButton(R.string.strLabelLater, null)
        }

        builder.show()
    }

    private fun updateAvailable() {
        if (parent == null) return

        val mUpdateBinding = LytUpdateAppBinding.inflate(LayoutInflater.from(ctx), parent, false)

        mUpdateBinding.let {
            it.root.id = R.id.appUpdateLayout
            it.txt.visibility = View.VISIBLE
            it.button.visibility = View.VISIBLE
            it.icon.visibility = View.VISIBLE
            it.txt.setText(R.string.strMsgUpdateAvailable)
            it.button.setText(R.string.strLabelUpdate)
            it.button.setOnClickListener {
                AppBridge.newOpener(ctx).openPlayStore()
            }

            mIconAnimators.add(animateUpdateIcon(mUpdateBinding.icon))

            it.root.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                ViewUtils.setMargins(this, ctx.dp2px(5f))
            }

            if (it.root.parent == null) {
                it.root.removeView()
                parent.addView(it.root)
            }

            parent.visible()
        }

    }

    private fun animateUpdateIcon(iconView: View): ObjectAnimator {
        val pvhTransY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, 11f, -20f, 10f, -3f, 0f)
        val pvhScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.1f, .8f, 1.3f, 1.03f, 1f)
        val pvhScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, .8f, 1.1f, 0.9f, 1f, 1f)
        return ObjectAnimator.ofPropertyValuesHolder(iconView, pvhTransY, pvhScaleX, pvhScaleY).apply {
            interpolator = TimeInterpolator { v -> (1.toFloat() - (1 - v).toDouble().pow(2.0)).toFloat() }
            duration = 1000
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (iconView.isAttachedToWindow) {
                        mIconAnimationHandler.postDelayed({ start() }, 1500)
                    }
                }
            })
            start()
        }
    }

    fun onPause() {
        mIconAnimators.forEach { it.cancel() }
        mIconAnimationHandler.removeCallbacksAndMessages(null)
    }

    fun onResume() {
        mIconAnimators.forEach { it.start() }
    }
}