package com.eyalbira.example;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.eyalbira.loadingdots.LoadingDots;

/**
 * Created by eyalbiran on 12/5/15.
 */
public class TestActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.test);

        final ViewGroup root = (ViewGroup) findViewById(R.id.root);

        Button startButton = (Button) findViewById(R.id.start);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAll(root);
            }
        });

        Button endButton = (Button) findViewById(R.id.stop);
        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopAll(root);
            }
        });

        Button addButton = (Button) findViewById(R.id.add_programmatically);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addProgrammatically(root);
            }
        });
    }

    private void startAll(ViewGroup root) {
        int count = root.getChildCount();
        for (int index = 0; index < count; index++) {
            View view = root.getChildAt(index);
            if (view instanceof LoadingDots) {
                ((LoadingDots)view).startAnimation();
            } else if (view instanceof ViewGroup) {
                startAll((ViewGroup) view);
            }
        }
    }

    private void stopAll(ViewGroup root) {
        int count = root.getChildCount();
        for (int index = 0; index < count; index++) {
            View view = root.getChildAt(index);
            if (view instanceof LoadingDots) {
                ((LoadingDots)view).stopAnimation();
            } else if (view instanceof ViewGroup) {
                stopAll((ViewGroup) view);
            }
        }
    }

    private void addProgrammatically(ViewGroup root) {
        root.removeAllViews();

        LoadingDots loadingDots = new LoadingDots(this);
        loadingDots.setDotsCount(3);
        loadingDots.setDotsSizeRes(R.dimen.LoadingDots_dots_size_default);
        loadingDots.setDotsColor(Color.BLUE);

        root.addView(loadingDots, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
}
