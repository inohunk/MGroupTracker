package ru.hunkel.mgrouptracker.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ru.hunkel.mgrouptracker.R
import ru.hunkel.mgrouptracker.fragments.MainFragment
import java.util.*

class HostActivity : AppCompatActivity() {
    private var cBackButtonPressed = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host)
    }

    override fun onBackPressed() {
        val fragment =
            supportFragmentManager.findFragmentById(R.id.my_nav_host_fragment)!!.childFragmentManager.fragments[0]

        if (fragment is MainFragment) {
            if (MainFragment.mServiceBounded) {

            } else {

                Toast.makeText(this, "Для выхода нажмите еще раз", Toast.LENGTH_SHORT).show()
                cBackButtonPressed++
                if (cBackButtonPressed == 2) {
                    super.onBackPressed()
                } else {
                    Timer().schedule(object : TimerTask() {
                        override fun run() {
                            cBackButtonPressed = 0
                        }
                    }, 3000)
                }
            }
        } else {
            super.onBackPressed()
        }
    }
}
