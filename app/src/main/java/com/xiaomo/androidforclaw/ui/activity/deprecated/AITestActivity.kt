package com.xiaomo.androidforclaw.ui.activity

import android.app.Application
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.draco.ladb.R

class AITestActivity : AppCompatActivity() {
    companion object {
        lateinit var app: Application
    }

    //    viewModels()
//    private val agentViewModel: MobileOperationAgent by lazy { MobileOperationAgent(application, paraphraseAgent) }

    // Imagine we have a TextView to show logs or status
    private lateinit var statusView: TextView
    private lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_test) // Assume a layout with a Button and TextView
        app = this.application

        // statusView = findViewById(R.id.statusView)
        // startButton = findViewById(R.id.startButton)

        // Observe ViewModel live data (if any) for updates
        // agentViewModel.statusLiveData.observe(this) { status ->
        //    statusView.text = status
        // }

        // Start the agent on button click
//        findViewById<View>(R.id.tv_hello).setOnClickListener {
//            agentViewModel.runAgent(null)
//        }
    }
}