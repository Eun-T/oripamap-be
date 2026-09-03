package org.scoula.common.util;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class UploadFiles {

    public static String upload(String baseDir, MultipartFile multipartFile) throws IOException {
        //baseDir : 저장하고 싶은 폴더 이름
        //MultipartFile : 첨부한 파일에 대한 정보를 다 가지고 있으면서 서버에서 받아서 처리할 수 있는 객체

        //폴더가 있는지 체크 --> 폴더를 인식시켜주어야함.(File객체)
        File base = new File(baseDir); //baseDir = c:/upload/board
        if(!base.exists()) {
            base.mkdirs(); //폴더 생성
        }

        //서버에서는 여러 사람이 파일 업로드하므로 파일이름이 겹침. --> 저장시 유니크한 이름으로 만들어서 저장함.
        String unique = UploadFileName.getUniqueName(multipartFile.getOriginalFilename());
        //apple-년월일시분초.png로 만들어줌.
        File dest = new File(baseDir, unique);//파일 생성(램에 만든 것)
        multipartFile.transferTo(dest); //파일 생성후 이동(c:/upload/board/apple-년월일시분초.png)
        return dest.getPath(); //결과를 db에 저장하기 위해서 return
    }

    public static void downloadImage(HttpServletResponse response, File file) {
        try {
            Path path = Path.of(file.getPath());
            String mimeType = Files.probeContentType(path); //파일의 타입을 알아온다.
            response.setContentType(mimeType); //http에 파일을 보낼 때는 파일의 타입을 함께 보내주어야함.
            response.setContentLength((int) file.length()); //파일의 크기도 함께 보냄.
            try (OutputStream os = response.getOutputStream();
                 BufferedOutputStream bos = new BufferedOutputStream(os)) {
                Files.copy(path, bos);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}


