package ru.hunkel.mgrouptracker.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.hunkel.mgrouptracker.R
import ru.hunkel.mgrouptracker.database.utils.DatabaseManager

class MainActivity : AppCompatActivity() {

    private lateinit var mDatabaseManager: DatabaseManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mDatabaseManager = DatabaseManager(this)

    }
}
