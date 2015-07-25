package jp.co.thinkethbank.kurikita.chisanpo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

/** ビットマップに対する加工を行う */
public class BitmapEditor {
    private static final float THUMB_SIZE = 128f;
    /**
     * Bitmapをリサイズする
     * @param src スケーリングするBitmap
     * @param shortLength ソース画像の短い方の辺のスケーリング後のサイズ
     * @return リサイズ後のBitmap
     */
    static Bitmap resizePicture(Bitmap src, int shortLength){
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        float scaleRate;

        if(srcWidth < srcHeight){
            scaleRate = (float)shortLength / srcWidth;
        }else{
            scaleRate = (float)shortLength / srcHeight;
        }
        Matrix matrix = new Matrix();
        matrix.postScale(scaleRate, scaleRate);

        return Bitmap.createBitmap(src, 0, 0, srcWidth, srcHeight, matrix, true);
    }

    static Bitmap thumbnail(Bitmap src){
        int w = src.getWidth();
        int h = src.getHeight();
        int len;
        int xOffset;
        int yOffset;
        float scaleRate;
        Matrix matrix = new Matrix();

        if(w > h){
            len = h;
            scaleRate = THUMB_SIZE / h;
            xOffset = (w - h) / 2;
            yOffset = 0;
        }else{
            len = w;
            scaleRate = THUMB_SIZE / w;
            xOffset = 0;
            yOffset = (h - w) / 2;
        }
        matrix.postScale(scaleRate, scaleRate);

        return Bitmap.createBitmap(src, xOffset, yOffset, len, len, matrix, true);
    }

    static byte[] compressJpeg(Bitmap bitmap, int quality){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        if(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)){
            return outputStream.toByteArray();
        }else{
            return null;
        }
    }

    static Bitmap decodeThumbnail(String absolutePath){
        try {
            FileInputStream inputStream = new FileInputStream(absolutePath);
            int read;

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while((read = inputStream.read()) != -1){
                bos.write(read);
            }
            byte[] ba = bos.toByteArray();
            return BitmapFactory.decodeByteArray(ba, 0, ba.length);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
