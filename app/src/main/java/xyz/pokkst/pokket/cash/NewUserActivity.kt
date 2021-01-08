package xyz.pokkst.pokket.cash

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import xyz.pokkst.pokket.cash.util.StatusBarHelper

class NewUserActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_user)
        StatusBarHelper.setStatusBarColor(this, R.color.purple)
    }
}