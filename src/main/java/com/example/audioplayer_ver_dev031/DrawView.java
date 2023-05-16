package com.example.audioplayer_ver_dev031;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class DrawView extends View {
    long playbackPosition = 0L;
    long contentDuration = 100L;
    Paint paint = new Paint();

    public DrawView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.argb(255, 0, 160, 160));
        // canvas.drawRect(0F, 0F, getWidth() * ((float)playbackPosition / (float)contentDuration), getHeight(), paint);
        canvas.drawRect(0F, 0F, getWidth(), getHeight(), paint);
        invalidate();
    }

    public void setPlaybackPosition(long playbackPosition) {
        this.playbackPosition = playbackPosition;
    }

    public void setContentDuration(long duration) {
        this.contentDuration = duration;
    }
}
