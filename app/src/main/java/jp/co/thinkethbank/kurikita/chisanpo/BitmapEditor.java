package jp.co.thinkethbank.kurikita.chisanpo;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import java.io.ByteArrayOutputStream;

/** ビットマップに対する加工を行う */
public class BitmapEditor {
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

    static byte[] compressJpeg(Bitmap bitmap, int quality){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        if(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)){
            return outputStream.toByteArray();
        }else{
            return null;
        }
    }
}
