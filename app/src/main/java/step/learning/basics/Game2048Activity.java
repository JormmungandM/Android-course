package step.learning.basics;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Vibrator;
import android.content.Context;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class Game2048Activity extends AppCompatActivity {
    private final int[][] tiles = new int[4][4];
    private final Random random = new Random();
    private final TextView[][] tvTiles = new TextView[4][4];
    private int score;
    private int bestScore;
    private TextView tvScore;
    private TextView tvBestScore;
    private boolean continuePlaying;
    private final String bestScoreFilename = "best_score.txt";
    private Animation spawnAnimation;
    private final Stack<int[][]> gameStateHistory = new Stack<>();
    private final Stack<Integer> gameScoreHistory = new Stack<>();
    private int[][] tilesId = {
            {R.id.tile00, R.id.tile01, R.id.tile02, R.id.tile03},
            {R.id.tile10, R.id.tile11, R.id.tile12, R.id.tile13},
            {R.id.tile20, R.id.tile21, R.id.tile22, R.id.tile23},
            {R.id.tile30, R.id.tile31, R.id.tile32, R.id.tile33},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game2048);

        findViewById(R.id.new_game_btn).setOnClickListener(this::restart);
        findViewById(R.id.undo_btn).setOnClickListener(this::undoLastMove);

        continuePlaying = false;

        score = 0;
        tvScore = findViewById(R.id.tv_score);
        tvBestScore = findViewById(R.id.tv_best_score);
        if(!loadBestScore()) bestScore = 0;
        tvBestScore.setText(getString(R.string.best_score_text, bestScore));

        spawnAnimation = AnimationUtils.loadAnimation(this, R.anim.spawn_tile);
        spawnAnimation.reset();

        for(int i = 0; i < 4; ++i) {
            for(int j = 0; j < 4; ++j) {
                tvTiles[i][j] = findViewById(getResources().getIdentifier("tile"+i+j, "id", getPackageName()));
            }
        }

        findViewById(R.id.layout_2048).setOnTouchListener(new OnSwipeListener(this)
        {
            @Override
            public void OnSwipeRight() {
                if(moveRight()) {
                    spawnTile();
                    saveGameState();
                }
                else vibrate();
            }
            @Override
            public void OnSwipeLeft() {
                if(moveLeft()) {
                    spawnTile();
                    saveGameState();
                }
                else vibrate();
            }
            @Override
            public void OnSwipeUp() {
                if(moveUp()) {
                    spawnTile();
                    saveGameState();
                }
                else vibrate();
            }
            @Override
            public void OnSwipeDown() {
                if(moveDown()) {
                    spawnTile();
                    saveGameState();
                }
                else vibrate();
            }
        });

        spawnTile();
        saveGameState();
    }

    private boolean saveBestScore() {
        try (FileOutputStream fos = openFileOutput(bestScoreFilename, Context.MODE_PRIVATE) ) {
            DataOutputStream writer = new DataOutputStream(fos);
            writer.writeInt(bestScore);
            writer.flush();
            writer.close();
        }
        catch(IOException ex) {
            Log.d("saveBestScore", ex.getMessage());
            return false;
        }
        return true;
    }

    private boolean loadBestScore() {
        try(FileInputStream fis = openFileInput(bestScoreFilename)) {
            DataInputStream reader = new DataInputStream(fis);
            bestScore = reader.readInt();
            reader.close();
        }
        catch (IOException ex) {
            Log.d("loadBestScore", ex.getMessage());
            return false;
        }
        return true;
    }

    private boolean spawnTile() {
        List<Integer> freeTiles = new ArrayList<>();
        for(int i = 0; i < 4; ++i) {
            for(int j = 0; j < 4; ++j) {
                if(tiles[i][j] == 0) {
                    freeTiles.add(i * 10 + j);
                }
            }
        }
        int cnt = freeTiles.size();
        if(cnt == 0) {
            return false;
        }
        int rnd = random.nextInt(cnt);
        int x = freeTiles.get(rnd) / 10;
        int y = freeTiles.get(rnd) % 10;
        tiles[x][y] = random.nextInt(10) == 0 ? 4 : 2;

        tvTiles[x][y].startAnimation(spawnAnimation);

        showField();
        return true;
    }

    private void showField() {
        Resources resources = getResources();
        for(int i = 0; i < 4; ++i) {
            for(int j = 0; j < 4; ++j) {
                tvTiles[i][j].setText(String.valueOf(tiles[i][j]));
                tvTiles[i][j].setTextAppearance(
                        resources.getIdentifier("Tile" + tiles[i][j], "style", getPackageName())
                );

                tvTiles[i][j].setBackgroundColor(
                        resources.getColor(
                                resources.getIdentifier("game_bg_" + tiles[i][j], "color", getPackageName()), getTheme())
                );
            }
        }

        tvScore.setText(getString(R.string.score_text, score));
        if(score > bestScore) {
            bestScore = score;
            saveBestScore();
            tvBestScore.setText(getString(R.string.best_score_text, bestScore));
        }

        if(isWin())  showDialog("You won","You got 2048");
        if(isLose()) showDialog("You lose","You have no moves left");
    }

    private boolean moveRight() {
        boolean result = false;
        for(int i = 0; i < 4; ++i) {
            boolean needRepeat = true;
            while(needRepeat) {
                needRepeat = false;
                for(int j = 3; j > 0; --j) {
                    if(tiles[i][j] == 0) {
                        for(int k = j - 1; k >= 0; --k) {
                            if(tiles[i][k] != 0) {
                                tiles[i][j] = tiles[i][k];
                                tiles[i][k] = 0;
                                needRepeat = true;
                                result = true;
                                break;
                            }
                        }
                    }
                }
            }

            //merge
            for(int j = 3; j > 0; --j) {
                if(tiles[i][j] != 0 && tiles[i][j] == tiles[i][j - 1]) {
                    tiles[i][j] *= 2;
                    for(int k = j - 1; k > 0; --k) {
                        tiles[i][k] = tiles[i][k-1];
                    }
                    tiles[i][0] = 0;

                    mergeTilesAnimation(tvTiles[i][j]);

                    result = true;
                    score += tiles[i][j];
                }
            }
        }

        return result;
    }

    private boolean moveLeft() {
        boolean result = false;
        for(int i = 0; i < 4; ++i) {
            boolean needRepeat = true;
            while(needRepeat) {
                needRepeat = false;
                for(int j = 0; j < 3; ++j) {
                    if(tiles[i][j] == 0) {
                        for(int k = j + 1; k < 4; ++k) {
                            if(tiles[i][k] != 0) {
                                tiles[i][k - 1] = tiles[i][k];
                                tiles[i][k] = 0;
                                needRepeat = true;
                                result = true;
                            }
                        }
                    }
                }
            }

            //merge
            for(int j = 0; j < 3; ++j) {
                if(tiles[i][j] != 0 && tiles[i][j] == tiles[i][j + 1]) {
                    tiles[i][j] *= 2;
                    for(int k = j + 1; k < 3; ++k) {
                        tiles[i][k] = tiles[i][k+1];
                    }
                    tiles[i][3] = 0;

                    mergeTilesAnimation(tvTiles[i][j]);

                    result = true;
                    score += tiles[i][j];
                }
            }
        }

        return result;
    }

    private boolean moveUp() {
        boolean result = false;
        for(int j = 0; j < 4; ++j) {
            boolean needRepeat = true;
            while(needRepeat) {
                needRepeat = false;
                for(int i = 0; i < 3; ++i) {
                    if(tiles[i][j] == 0) {
                        for(int k = i + 1; k < 4; ++k) {
                            if(tiles[k][j] != 0) {
                                tiles[i][j] = tiles[k][j];
                                tiles[k][j] = 0;
                                needRepeat = true;
                                result = true;
                                break;
                            }
                        }
                    }
                }
            }

            //merge
            for(int i = 0; i < 3; ++i) {
                if(tiles[i][j] != 0 && tiles[i][j] == tiles[i + 1][j]) {
                    tiles[i][j] *= 2;
                    for(int k = i + 1; k < 3; ++k) {
                        tiles[k][j] = tiles[k+1][j];
                    }
                    tiles[3][j] = 0;

                    mergeTilesAnimation(tvTiles[i][j]);

                    result = true;
                    score += tiles[i][j];
                }
            }
        }

        return result;
    }

    private boolean moveDown() {
        boolean result = false;
        for(int j = 0; j < 4; ++j) {
            boolean needRepeat = true;
            while(needRepeat) {
                needRepeat = false;
                for(int i = 3; i > 0; --i) {
                    if(tiles[i][j] == 0) {
                        for(int k = i - 1; k >= 0; --k) {
                            if(tiles[k][j] != 0) {
                                tiles[i][j] = tiles[k][j];
                                tiles[k][j] = 0;
                                needRepeat = true;
                                result = true;
                                break;
                            }
                        }
                    }
                }
            }

            //merge
            for(int i = 3; i > 0; --i) {
                if(tiles[i][j] != 0 && tiles[i][j] == tiles[i - 1][j]) {
                    tiles[i][j] *= 2;
                    for(int k = i - 1; k > 0; --k) {
                        tiles[k][j] = tiles[k-1][j];
                    }
                    tiles[0][j] = 0;

                    mergeTilesAnimation(tvTiles[i][j]);

                    result = true;
                    score += tiles[i][j];
                }
            }
        }

        return result;
    }

    private boolean isWin() {
        if(!continuePlaying) {
            for(int i = 0; i < 4; ++i) {
                for(int j = 0; j < 4; ++j) {
                    if(tiles[i][j] == 8) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void restart(View v) {
        restart();
    }

    private void restart() {
        score = 0;
        tvScore.setText(getString(R.string.score_text, score));
        for(int i = 0; i < 4; i++) {
            for(int j = 0; j < 4; j++) {
                tiles[i][j] = 0;
            }
        }
        gameScoreHistory.clear();
        gameStateHistory.clear();
        showField();
        spawnTile();
        saveGameState();
    }

    private void showDialog(String Title,String Message) {

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        builder.setTitle(Title);
        builder.setMessage(Message);

        if(Title.equals("You won")) {
            builder.setPositiveButton("Continue", (dialog, button) -> {
                continuePlaying = true;
            });
        }

        builder.setNegativeButton("Exit", (dialog, button) ->       { finish(); } );
        builder.setNeutralButton("Again", (dialog, button) ->       { restart(); } );
        builder.setCancelable(false);


        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.show();

        Button nButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        nButton.setTextColor(getColor(R.color.game_dialog_font));
        Button pButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        pButton.setTextColor(getColor(R.color.game_dialog_font));
        Button n2Button = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        n2Button.setTextColor(getColor(R.color.game_dialog_font));

    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            long duration = 200;
            vibrator.vibrate(duration);
        }
    }

    private boolean isLose() {
        for(int i = 0; i < 4; ++i) {
            for(int j = 0; j < 4; ++j) {
                if(tiles[i][j] == 0) {
                    return false;
                }
            }
        }

        for(int i = 0; i < 4; ++i) {
            for(int j = 0; j < 4; ++j) {
                if(j < 3 && tiles[i][j] == tiles[i][j+1]) {
                    return false;
                }
                if(i < 3 && tiles[i][j] == tiles[i+1][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    private void undoLastMove(View v) {
        if (gameStateHistory.size() > 1) {
            gameStateHistory.pop();
            int[][] prevState = gameStateHistory.peek();
            for (int i = 0; i < 4; ++i) {
                for (int j = 0; j < 4; ++j) {
                    tiles[i][j] = prevState[i][j];
                }
            }
            if(gameScoreHistory.size() > 1) {
                gameScoreHistory.pop();
                score = gameScoreHistory.peek();
            }
            showField();
        }
    }

    private void saveGameState() {
        int[][] state = new int[4][4];
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                state[i][j] = tiles[i][j];
            }
        }
        gameScoreHistory.add(score);
        gameStateHistory.add(state);
    }

    private void mergeTilesAnimation(TextView tile) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(1f, 1.5f, 1f, 1.5f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(100);
        tile.startAnimation(scaleAnimation);
    }
}