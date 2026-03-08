package com.xiaomo.androidforclaw.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.draco.ladb.databinding.ActivityChatHistoryBinding
import com.xiaomo.androidforclaw.ui.adapter.ResultRecyclerAdapter
import com.xiaomo.androidforclaw.util.ResultUtil

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        val results = ResultUtil.getResults()
        binding.recyclerView.adapter = ResultRecyclerAdapter(results)

    }
}
