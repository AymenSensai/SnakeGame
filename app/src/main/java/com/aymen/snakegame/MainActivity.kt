package com.aymen.snakegame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.aymen.snakegame.ui.theme.DarkGreen
import com.aymen.snakegame.ui.theme.LightGreen
import com.aymen.snakegame.ui.theme.Red
import com.aymen.snakegame.ui.theme.SnakeGameTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val game = Game(lifecycleScope)
            setContent {
                SnakeGameTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Snake(game)
                    }
                }
            }
        }
    }
}

data class State(val snake: List<Pair<Int, Int>>, val food: Pair<Int, Int>, val score: Int)

class Game(private val scope: CoroutineScope) {

    private val mutex = Mutex()
    private val mutableState =
        MutableStateFlow(State(food = Pair(5, 5), snake = listOf(Pair(7, 7)), score = 0))
    val state: Flow<State> = mutableState

    var move = Pair(1, 0)
        set(value) {
            scope.launch {
                mutex.withLock {
                    field = value
                }
            }
        }

    init {
        scope.launch {
            var snakeLength = 4

            while (true) {
                delay(120)
                mutableState.update {
                    val newPosition = it.snake.first().let { poz ->
                        mutex.withLock {
                            Pair(
                                (poz.first + move.first + BOARD_WIDTH) % BOARD_WIDTH,
                                (poz.second + move.second + BOARD_HEIGHT) % BOARD_HEIGHT
                            )
                        }
                    }

                    var snakePositions = listOf(newPosition) + it.snake.take(snakeLength - 1)
                    var score = it.score

                    if (newPosition == it.food) {
                        snakeLength++
                        score++
                    }

                    if (it.snake.contains(newPosition)) {
                        snakeLength = 4
                        snakePositions = listOf(Pair(8, 12))
                        score = 0
                    }

                    it.copy(
                        food = if (newPosition == it.food) Pair(
                            Random().nextInt(BOARD_WIDTH),
                            Random().nextInt(BOARD_HEIGHT)
                        ) else it.food,
                        snake = snakePositions,
                        score = score
                    )
                }
            }
        }
    }

    companion object {
        const val BOARD_WIDTH = 16
        const val BOARD_HEIGHT = 24
    }

}

@Composable
fun Snake(game: Game) {
    val state = game.state.collectAsState(initial = null)
    var currentDirection by remember { mutableStateOf(Pair(1, 0)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, _, _ ->
                    val newDirection = when {
                        pan.x > 25 -> Pair(1, 0)   // Right
                        pan.x < -25 -> Pair(-1, 0)  // Left
                        pan.y > 25 -> Pair(0, 1)    // Down
                        pan.y < -25 -> Pair(0, -1)  // Up
                        else -> currentDirection
                    }
                    if (!isOppositeDirection(currentDirection, newDirection)) {
                        currentDirection = newDirection
                        game.move = currentDirection
                    }
                }
            }
    ) {
        state.value?.let {
            Board(it)
        }
    }
}

@Composable
fun Board(state: State) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Score: ${state.score}", textAlign = TextAlign.Center, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        BoxWithConstraints {
            val tileWidth = maxWidth / Game.BOARD_WIDTH
            val tileHeight = maxHeight / Game.BOARD_HEIGHT

            Box(
                Modifier
                    .fillMaxSize()
                    .border(2.dp, DarkGreen)
            )

            Box(
                Modifier
                    .offset(x = tileWidth * state.food.first, y = tileHeight * state.food.second)
                    .size(width = tileWidth, height = tileHeight)
                    .background(
                        Red, CircleShape
                    )
            )

            state.snake.forEach {
                if (it == state.snake.first()) {
                    Box(
                        modifier = Modifier
                            .offset(x = tileWidth * it.first, y = tileHeight * it.second)
                            .size(width = tileWidth, height = tileHeight)
                            .background(
                                DarkGreen, RoundedCornerShape(4.dp)
                            )
                    )
                    Row(
                        modifier = Modifier
                            .size(width = tileWidth, height = tileHeight)
                            .padding(2.dp)
                            .offset(x = tileWidth * it.first, y = tileHeight * it.second),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    LightGreen, CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    LightGreen, CircleShape
                                )
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .offset(x = tileWidth * it.first, y = tileHeight * it.second)
                            .size(width = tileWidth, height = tileHeight)
                            .background(
                                DarkGreen, RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }
    }
}

private fun isOppositeDirection(dir1: Pair<Int, Int>, dir2: Pair<Int, Int>): Boolean {
    return dir1.first == -dir2.first && dir1.second == -dir2.second
}