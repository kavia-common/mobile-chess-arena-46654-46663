package org.example.app

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import org.example.app.game.GameController
import org.example.app.game.GameState
import org.example.app.ui.ChessBoardView
import org.example.app.ui.MoveHistoryAdapter

class MainActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var chessBoard: ChessBoardView
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: MoveHistoryAdapter
    private lateinit var btnUndo: Button
    private lateinit var btnReset: Button
    private lateinit var btnToggleMode: Button
    private lateinit var btnFlip: Button
    private lateinit var btnAiMove: Button
    private lateinit var controller: GameController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.toolbar_title)?.text = getString(R.string.app_title)

        chessBoard = findViewById(R.id.chessBoard)
        recycler = findViewById(R.id.recycler_history)
        btnUndo = findViewById(R.id.btn_undo)
        btnReset = findViewById(R.id.btn_reset)
        btnToggleMode = findViewById(R.id.btn_toggle_mode)
        btnFlip = findViewById(R.id.btn_flip)
        btnAiMove = findViewById(R.id.btn_ai_move)

        adapter = MoveHistoryAdapter()
        recycler.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recycler.adapter = adapter

        val state = GameState()
        controller = GameController(state,
            onUiUpdate = { onUiUpdate() },
            onDisableInput = { setInputsEnabled(false) },
            onEnableInput = { setInputsEnabled(true) },
            scope = scope
        )

        chessBoard.setGameController(controller)

        btnUndo.setOnClickListener {
            controller.undo()
        }
        btnReset.setOnClickListener {
            controller.reset()
        }
        btnToggleMode.setOnClickListener {
            controller.toggleMode()
            btnToggleMode.text = controller.modeText()
        }
        btnFlip.setOnClickListener {
            controller.flipBoard()
            chessBoard.invalidate()
        }
        btnAiMove.setOnClickListener {
            controller.forceAiMove()
        }

        btnToggleMode.text = controller.modeText()
        onUiUpdate()
    }

    private fun setInputsEnabled(enabled: Boolean) {
        val controls = arrayOf<View>(btnUndo, btnReset, btnToggleMode, btnFlip, btnAiMove)
        for (v in controls) v.isEnabled = enabled
        chessBoard.isEnabled = enabled
        chessBoard.invalidate()
    }

    private fun onUiUpdate() {
        // Update board and history
        chessBoard.setBoard(controller.currentBoard(), controller.currentHighlights())
        adapter.submitList(controller.historyPairs())
        recycler.post {
            recycler.scrollToPosition(adapter.itemCount - 1)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
