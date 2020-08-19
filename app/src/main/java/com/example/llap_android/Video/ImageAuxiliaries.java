package com.example.llap_android.Video;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

import com.example.llap_android.ScriptC_sumgreen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class ImageAuxiliaries {
    private RenderScript mRenderScript;
    private ScriptIntrinsicYuvToRGB mScriptIntrinsicYuvToRGB;
    private ScriptC_sumgreen mSumGreen;
    private static ImageAuxiliaries mSelf;
    private ImageAuxiliaries(Context context){
        mRenderScript = RenderScript.create(context);
        mScriptIntrinsicYuvToRGB = ScriptIntrinsicYuvToRGB.create(mRenderScript,Element.U8(mRenderScript));
        mSumGreen = new ScriptC_sumgreen(mRenderScript);
    }
    public static void init(Context context){
        mSelf = new ImageAuxiliaries(context);
    }
    public static ImageAuxiliaries getInstance(){
        return mSelf;
    }
    private static byte[] convertYUV420888ToNV21(Image img){
        byte[] nv21;
        ByteBuffer yBuffer = img.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = img.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = img.getPlanes()[2].getBuffer();
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21,0,ySize);
        vBuffer.get(nv21,ySize,vSize);
        uBuffer.get(nv21,ySize + vSize, uSize);
        return  nv21;
    }
    private static Allocation imageRGBA_Allocation(RenderScript renderScript,ScriptIntrinsicYuvToRGB scriptIntrinsicYuvToRGB,Image img){
        final byte[] nv21 = convertYUV420888ToNV21(img);
        Type.Builder itype = new Type.Builder(renderScript, Element.U8(renderScript));
        itype.setX(nv21.length);
        Allocation iallocation = Allocation.createTyped(renderScript,itype.create());
        Type.Builder otype = new Type.Builder(renderScript,Element.RGBA_8888(renderScript)).setX(img.getWidth()).setY(img.getHeight());
        Allocation oallocation = Allocation.createTyped(renderScript,otype.create());
        iallocation.copyFrom(nv21);
        scriptIntrinsicYuvToRGB.setInput(iallocation);
        scriptIntrinsicYuvToRGB.forEach(oallocation);
        return oallocation;
    }
    synchronized public double __averageGreen(Image img) throws Exception {
        Allocation rgba = imageRGBA_Allocation(mRenderScript,mScriptIntrinsicYuvToRGB,img);
        /*
        int _i = rgba.getBytesSize();
        byte[] rgba_buffer = new byte[_i];
        rgba.copyTo(rgba_buffer);

        int len = rgba_buffer.length;
        long sum = 0;
        for (int i = 1; i < len; i+=4){
            sum += (rgba_buffer[i] & 0xff);
        }
        sum = sum;
        */
       //     Allocation rgba = Allocation.createSized(mRenderScript,Element.U8_4(mRenderScript),);
    //    rgba.copyFrom(rgba_buffer);

        Type.Builder guint_type = new Type.Builder(mRenderScript,Element.U32(mRenderScript)).setX(img.getWidth()).setY(img.getHeight());
        Allocation guint = Allocation.createTyped(mRenderScript,guint_type.create());
        ScriptC_sumgreen sumgreen = mSumGreen;
        sumgreen.forEach_extractGreenChannel(rgba,guint);
        long gsum = sumgreen.reduce_sumGreen(guint).get();
        double dgsum = (double)gsum / (img.getHeight() * img.getWidth());
        return dgsum;
    }
    synchronized public double averageGreen(Image img) throws Exception {
        Allocation _rgba = imageRGBA_Allocation(mRenderScript,mScriptIntrinsicYuvToRGB,img);

        int _i = _rgba.getBytesSize();
        byte[] rgba_buffer = new byte[_i];
        _rgba.copyTo(rgba_buffer);
        /*
        int len = rgba_buffer.length;
        long sum = 0;
        for (int i = 1; i < len; i+=4){
            sum += (rgba_buffer[i] & 0xff);
        }
        sum = sum;
        */
        //     Allocation rgba = Allocation.createSized(mRenderScript,Element.U8_4(mRenderScript),);
        //    rgba.copyFrom(rgba_buffer);
        Allocation rgba = Allocation.createSized(mRenderScript,Element.U8_4(mRenderScript),rgba_buffer.length / 4);
        rgba.copyFrom(rgba_buffer);
        Type.Builder guint_type = new Type.Builder(mRenderScript,Element.U32(mRenderScript)).setX(rgba_buffer.length / 4);
        Allocation guint = Allocation.createTyped(mRenderScript,guint_type.create());
        ScriptC_sumgreen sumgreen = mSumGreen;
        sumgreen.forEach_extractGreenChannel(rgba,guint);
        long gsum = sumgreen.reduce_sumGreen(guint).get();
        double dgsum = (double)gsum / (img.getHeight() * img.getWidth());
        return dgsum;
    }
    public void testKernel(){
        byte[] idata = {-127,-1,1,127};
        int[] odata = new int[idata.length / 4];
        Allocation input = Allocation.createSized(mRenderScript,Element.U8_4(mRenderScript),idata.length / 4);
        Allocation output = Allocation.createSized(mRenderScript,Element.U32(mRenderScript),idata.length / 4);
        input.copyFrom(idata);
        mSumGreen.forEach_extractGreenChannel(input,output);
        output.copyTo(odata);
        long sum = 0;
        for (int i = 0; i < idata.length; i++){
            sum += (idata[i] & 0xff);
        }
    }
    public Bitmap image2Bitmap(Image img){
        Allocation rgba = imageRGBA_Allocation(mRenderScript,mScriptIntrinsicYuvToRGB,img);
        Bitmap bitmap = Bitmap.createBitmap(img.getWidth(),img.getHeight(), Bitmap.Config.ARGB_8888);
        rgba.copyTo(bitmap);
        return bitmap;
    }
    public static void saveBitmap(String file,Bitmap bitmap) throws FileNotFoundException {
        FileOutputStream fos = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,fos);
    }
}
