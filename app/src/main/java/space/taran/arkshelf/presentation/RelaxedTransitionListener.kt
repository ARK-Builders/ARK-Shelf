package space.taran.arkshelf.presentation

import androidx.constraintlayout.motion.widget.MotionLayout

class RelaxedTransitionListener(
    val onTransitionStarted: (() -> Unit)? = null,
    val onTransitionChange: (() -> Unit)? = null,
    val onTransitionCompleted: ((Int) -> Unit)? = null,
    val onTransitionTrigger: (() -> Unit)? = null
): MotionLayout.TransitionListener {
    override fun onTransitionStarted(
        motionLayout: MotionLayout?,
        startId: Int,
        endId: Int
    ) {
        onTransitionStarted?.invoke()
    }

    override fun onTransitionChange(
        motionLayout: MotionLayout?,
        startId: Int,
        endId: Int,
        progress: Float
    ) {
        onTransitionChange?.invoke()
    }

    override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
        onTransitionCompleted?.invoke(currentId)
    }

    override fun onTransitionTrigger(
        motionLayout: MotionLayout?,
        triggerId: Int,
        positive: Boolean,
        progress: Float
    ) {
        onTransitionTrigger?.invoke()
    }

}