package xyz.pokkst.pokket.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager


class ToggleViewPager(context: Context, attrs: AttributeSet?) :
    ViewPager(context, attrs) {
    private var pagingEnabled = true
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (pagingEnabled) {
            super.onTouchEvent(event)
        } else false
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return if (pagingEnabled) {
            super.onInterceptTouchEvent(event)
        } else false
    }

    fun setPagingEnabled(enabled: Boolean) {
        this.pagingEnabled = enabled
    }

    fun isPagingEnabled(): Boolean {
        return pagingEnabled
    }
}