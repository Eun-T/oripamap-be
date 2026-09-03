package org.scoula.common.util;

public class UploadFileName {
    //apple.png가 원래 첨부파일 이름. //MultipartFile를 가지고 추출
    public static String getUniqueName(String filename){
         int ix = filename.lastIndexOf("."); //5
        String name = filename.substring(0, ix); //ix-1까지 추출, 4m apple
        String ext = filename.substring(ix + 1); //ext
        //String total = name + "-" + System.currentTimeMillis() + "." + ext;
        String total = String.format("%s-%d.%s", name, System.currentTimeMillis(), ext);
        return total;
    }
}
