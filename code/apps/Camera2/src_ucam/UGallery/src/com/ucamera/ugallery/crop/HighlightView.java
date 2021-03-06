/*
 * Copyright (C) 2011,2012 Thundersoft Corporation
 * All rights Reserved
 *
 *  Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ucamera.ugallery.crop;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.View;
import com.ucamera.ugallery.R;

// This class is used by CropImage to display a highlighted cropping rectangle
// overlayed with the image. There are two coordinate spaces in use. One is
// image, another is screen. computeLayout() uses mMatrix to map from image
// space to screen space.
class HighlightView {

    private static final String TAG = "HighlightView";
    View mContext;  // The View displaying the image.

    public static final int GROW_NONE        = (1 << 0);
    public static final int GROW_LEFT_EDGE   = (1 << 1);
    public static final int GROW_RIGHT_EDGE  = (1 << 2);
    public static final int GROW_TOP_EDGE    = (1 << 3);
    public static final int GROW_BOTTOM_EDGE = (1 << 4);
    public static final int MOVE             = (1 << 5);
    public static final int GROW_LEFT_TOP_CORNER = (1 << 6);
    public static final int GROW_RIGHT_TOP_CORNER = (1 << 7);
    public static final int GROW_LEFT_BOTTOM_CORNER = (1 << 8);
    public static final int GROW_RIGHT_BOTTOM_CORNER = (1 << 9);
    private Bitmap mLTbmp;
    private Bitmap mRTbmp;
    private Bitmap mLBbmp;
    private Bitmap mRBbmp;
    private int mWidth;

    public HighlightView(View ctx) {
        mContext = ctx;
        mLTbmp = ((BitmapDrawable)mContext.getResources().getDrawable(R.drawable.box_lift_above)).getBitmap();
        mRTbmp = ((BitmapDrawable)mContext.getResources().getDrawable(R.drawable.box_right_above)).getBitmap();
        mLBbmp = ((BitmapDrawable)mContext.getResources().getDrawable(R.drawable.box_lift_following)).getBitmap();
        mRBbmp = ((BitmapDrawable)mContext.getResources().getDrawable(R.drawable.box_right_following)).getBitmap();
        mWidth = mLTbmp.getWidth();
    }

    private void init() {
    }

    boolean mIsFocused = true;
    boolean mHidden;

    public boolean hasFocus() {
        return mIsFocused;
    }

    public void setFocus(boolean f) {
        mIsFocused = f;
    }

    public void setHidden(boolean hidden) {
        mHidden = hidden;
    }

    protected void draw(Canvas canvas) {
        if (mHidden) {
            return;
        }
        canvas.save();
        initLineRect();
        Path path = new Path();
        if (!hasFocus()) {
            mOutlinePaint.setColor(0xFF000000);
            canvas.drawRect(mDrawRect, mOutlinePaint);

        } else {
            Rect viewDrawingRect = new Rect();
            mContext.getDrawingRect(viewDrawingRect);
            if (mCircle) {
                float width  = mDrawRect.width();
                float height = mDrawRect.height();
                path.addCircle(mDrawRect.left + (width  / 2),
                               mDrawRect.top + (height / 2),
                               width / 2,
                               Path.Direction.CW);
                mOutlinePaint.setColor(0xFFEF04D6);
            } else {
                path.addRect(new RectF(mDrawRect), Path.Direction.CW);
                //mOutlinePaint.setColor(0xFFFF8A00);
                mOutlinePaint.setColor(Color.WHITE);
            }
            try{
                /*
                 * BUG FIX: 618
                 * FIX COMMENT: this method is not support in hardware acceleration mode
                 *   see: android.view.GLES20Canvas for more info
                 * Date: 2012-08-30
                 */
                //canvas.clipPath(path, Region.Op.DIFFERENCE);
            }catch(Exception e){
                Log.e(TAG,"Error calling 'clipPath' of class:" + canvas.getClass().getName());
            }
            /*
             * BUG FIX: 3903, 3892
             * BUG COMMENT: draw gray rect
             * FIX DATE: 2013-05-24
             * */
            canvas.drawRect(new Rect(viewDrawingRect.left, viewDrawingRect.top, viewDrawingRect.right, mDrawRect.top), hasFocus() ? mFocusPaint : mNoFocusPaint);
            canvas.drawRect(new Rect(viewDrawingRect.left, mDrawRect.top, mDrawRect.left, mDrawRect.bottom), hasFocus() ? mFocusPaint : mNoFocusPaint);
            canvas.drawRect(new Rect(mDrawRect.right, mDrawRect.top, viewDrawingRect.right, mDrawRect.bottom), hasFocus() ? mFocusPaint : mNoFocusPaint);
            canvas.drawRect(new Rect(viewDrawingRect.left, mDrawRect.bottom, viewDrawingRect.right, viewDrawingRect.bottom), hasFocus() ? mFocusPaint : mNoFocusPaint);
            /*canvas.drawRect(viewDrawingRect,
                    hasFocus() ? mFocusPaint : mNoFocusPaint);*/

            canvas.restore();
//            drawLineRect(canvas);
            drawCorner(canvas);
//            canvas.drawPath(path, mOutlinePaint);
        }
    }

    public void setMode(ModifyMode mode) {
        if (mode != mMode) {
            mMode = mode;
            mContext.invalidate();
        }
    }

    // Returns the cropping rectangle in image space.
    public Rect getCropRect() {
        return mDrawRect;
//        return new Rect((int) mCropRect.left, (int) mCropRect.top,(int) mCropRect.right, (int) mCropRect.bottom);
    }

    // Maps the cropping rectangle from image space to screen space.
    private Rect computeLayout() {
        RectF r = new RectF(mCropRect.left, mCropRect.top,
                            mCropRect.right, mCropRect.bottom);
        if(mMatrix != null)
        mMatrix.mapRect(r);
        return new Rect(Math.round(r.left), Math.round(r.top),
                        Math.round(r.right), Math.round(r.bottom));
    }

    public void invalidate() {
        mDrawRect = computeLayout();
    }

    public void setup(Matrix m/*, Rect imageRect*/, RectF cropRect,/* boolean circle,*/
                      boolean maintainAspectRatio/*, int width, int height*/) {
        /*mViewHeight = height;
        mViewWidth = width;*/
//        if (circle) {
//            maintainAspectRatio = true;
//        }
        mMatrix = new Matrix(m);

        mCropRect = cropRect;
//        mImageRect = new RectF(imageRect);
        mMaintainAspectRatio = maintainAspectRatio;
//        mCircle = circle;

        mInitialAspectRatio = mCropRect.width() / mCropRect.height();
        mDrawRect = computeLayout();

        mFocusPaint.setARGB(125, 50, 50, 50);
        mNoFocusPaint.setARGB(125, 50, 50, 50);
        mOutlinePaint.setStrokeWidth(5F);
        mOutlinePaint.setStyle(Paint.Style.STROKE);
        mOutlinePaint.setAntiAlias(true);

        mMode = ModifyMode.None;
        init();
    }
    private void initLineRect()
    {
        int left = mDrawRect.left + mDrawRect.width() / 3;
        int right = mDrawRect.left + mDrawRect.width() / 3 * 2;
        int top = mDrawRect.top + mDrawRect.height() / 3;
        int bottom = mDrawRect.top + mDrawRect.height() / 3 *2;
        mDrawLeftLineRect = new Rect(left, mDrawRect.top, left + 1, mDrawRect.bottom);
        mDrawRightLineRect = new Rect(right, mDrawRect.top, right + 1, mDrawRect.bottom);
        mDrawTopLineRect = new Rect(mDrawRect.left, top, mDrawRect.right, top + 1);
        mDrawBottomLineRect = new Rect(mDrawRect.left, bottom, mDrawRect.right, bottom + 1);
    }
    private void drawLineRect(Canvas canvas)
    {
        Paint linePaint = new Paint();
        linePaint.setColor(Color.WHITE);
        linePaint.setStrokeWidth(1);
        linePaint.setAntiAlias(true);
        PathEffect effects = new DashPathEffect(
                new float[]{50, 10}, 1);
        linePaint.setPathEffect(effects);

        canvas.drawRect(mDrawLeftLineRect, linePaint);
        canvas.drawRect(mDrawRightLineRect, linePaint);
        canvas.drawRect(mDrawTopLineRect, linePaint);
        canvas.drawRect(mDrawBottomLineRect, linePaint);
    }
    private void drawCorner(Canvas canvas) {
        Paint linePaint = new Paint();
        linePaint.setColor(Color.WHITE);
        linePaint.setStrokeWidth(5);
        linePaint.setAntiAlias(true);

        canvas.drawBitmap(mLTbmp, mDrawRect.left, mDrawRect.top, linePaint);
        canvas.drawBitmap(mRTbmp, mDrawRect.right - mWidth, mDrawRect.top, linePaint);
        canvas.drawBitmap(mLBbmp, mDrawRect.left, mDrawRect.bottom - mWidth, linePaint);
        canvas.drawBitmap(mRBbmp, mDrawRect.right - mWidth, mDrawRect.bottom - mWidth, linePaint);
    }
    public void clearCornerBitmap() {
        recycleBitmap(new Bitmap[] {mLBbmp, mLTbmp, mRBbmp, mRTbmp});
    }
    private void recycleBitmap(Bitmap[] bitmaps) {
        for(Bitmap bitmap : bitmaps) {
            if(bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                bitmap = null;
            }
        }
    }
    enum ModifyMode { None, Move, Grow }

    private ModifyMode mMode = ModifyMode.None;

    Rect mDrawRect;  // in screen space
    private RectF mImageRect;  // in image space
    RectF mCropRect;  // in image space
    Matrix mMatrix;
    private Rect mDrawLeftLineRect;
    private Rect mDrawRightLineRect;
    private Rect mDrawTopLineRect;
    private Rect mDrawBottomLineRect;

    private boolean mMaintainAspectRatio = false;
    private float mInitialAspectRatio;
    private boolean mCircle = false;

    private final Paint mFocusPaint = new Paint();
    private final Paint mNoFocusPaint = new Paint();
    private final Paint mOutlinePaint = new Paint();
}
