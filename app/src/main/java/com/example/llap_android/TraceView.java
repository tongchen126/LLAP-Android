package com.example.llap_android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by weiwang on 2/6/16.
 */
public class TraceView extends View {
    Paint mypaint= new Paint();
    Paint greenpaint= new Paint();
    private int pointsize=40;
    private int [] linex=new int[pointsize];
    private int [] liney=new int[pointsize];
    private int offset=0;
    private int step=10;
    private int minoroffset=0;
    private int toggle=0;

    public TraceView(Context context,AttributeSet attrs)
    {
        super(context,attrs);
        offset=0;
        mypaint.setColor(Color.BLUE);
        mypaint.setStrokeWidth(8);
        greenpaint.setColor(Color.GREEN);
        greenpaint.setStrokeWidth(8);
    }
    @Override
    public void onDraw(Canvas canvas)
    {   int pstart,pend;
        pstart=offset+1;
        pend=offset+2;
        canvas.drawLine(300,700,300,300,greenpaint);
        canvas.drawLine(300,300,700,300, greenpaint);
        canvas.drawLine(700,300,700,700, greenpaint);
        canvas.drawLine(700,700,300,700, greenpaint);
        for(int i=0;i<pointsize-1;i++) {
            if(pstart>=pointsize) {
                pstart=pstart-pointsize;
            }
            if(pend>=pointsize) {
                pend=pend-pointsize;
            }
            canvas.drawLine(linex[pstart], liney[pstart], linex[pend], liney[pend], mypaint);
            pstart++;
            pend++;
        }

    }
    public void setTrace(int [] x, int [] y, int numpoints)
    {
        for(int i=(step-minoroffset)%step;i<numpoints;i+=step)
        {
            offset++;
            if(offset>=pointsize)
            {
                offset=0;
            }
            liney[offset]=800-4*y[i];
            linex[offset]=4*x[i]+600;
        }
        minoroffset=(numpoints+minoroffset)%step;
        toggle+=numpoints;
        if(toggle>=1*step) {

            toggle=0;
            invalidate();
        }
    }
}
