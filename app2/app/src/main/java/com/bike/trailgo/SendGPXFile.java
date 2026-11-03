package com.bike.trailgo;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.bike.trailgo.net.Server;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class SendGPXFile extends AppCompatActivity implements Runnable {

    String filepath = "";
    String fileName = "";
    public SendGPXFile(String filepath, String fileName) {
        this.filepath = filepath;
        this.fileName = fileName;
    }

    String url_address;

    String str; //결과를 저장할 변수

    @Override
    public void run() {

        try {
            url_address = Server.getUrl("app/fileUpload/gpx/" + fileName);
            Log.e("파일네임", fileName);
            FileInputStream fis = new FileInputStream(filepath + fileName);
            HttpURLConnection conn = Server.openUrlConnection(url_address);
            //웹서버를 통해 입출력 가능하도록 설정
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);//캐쉬 사용하지 않음
            conn.setRequestMethod("POST");
            //정해진 시간 내에 재접속할 경우 소켓을 새로 생성하지 않고 기존연결 사용
            conn.setRequestProperty("Connection", "Keep-Alive");
            //첨부파일에 대한 정보
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=files");


            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            //form-data; name=파일변수명; filename="첨부파일이름"
            dos.writeBytes("--files\r\n"); // --은 파일 시작 알림 표시
            dos.writeBytes("Content-Disposition: form-data;"
                    + " name=\"file1\";"
                    + " filename=\"" + fileName
                    + "\"" + "\r\n");

            dos.writeBytes("\r\n");//줄바꿈 문자
            int bytes = fis.available();
            int maxBufferSize = 1024;
            //Math.min(A, B)둘중 작은값;
            int bufferSize = Math.min(bytes, maxBufferSize);
            byte[] buffer = new byte[bufferSize];
            int read = fis.read(buffer, 0, bufferSize);
            while (read > 0) {
                //서버에 업로드
                dos.write(buffer, 0, bufferSize);
                bytes = fis.available();
                bufferSize = Math.min(bytes, maxBufferSize);
                //읽은 바이트 수
                read = fis.read(buffer, 0, bufferSize);
            }
            dos.writeBytes("\r\n");//줄바꿈 문자

           /*boundary=경계문자 => 경계문자의 이름
             --경계문자 => 첨부파일 전송 시작부분
             --경계문자--      => 첨부파일 전송 끝부분*/

            dos.writeBytes("--files--\r\n");
            fis.close();
            dos.flush();
            dos.close();

            //서버 응답 처리
            int ch;

            int status = conn.getResponseCode();
            Log.e("status", String.valueOf(status));
            Log.e("method", conn.getRequestMethod());
            InputStream is = conn.getInputStream();
            StringBuffer sb = new StringBuffer();
            while ((ch = is.read()) != -1) { // 내용이 없을 때까지 반복
                sb.append((char) ch); // 문자 읽어서 저장
            }
            // 스트링.trim() 스트링의 좌우 공백 제거
            str = sb.toString().trim();
            if (str.equals("success")) {
                str = "파일이 업로드되었습니다.";
            } else if (str.equals("fail")) {
                str = "파일 업로드 실패...";
            }

            is.close();
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //gpx 파일 웹서버로 업로드(백그라운드 스레드로 처리)
    public void upload() {
        //백그라운드 스레드 생성, 호출
        Thread th = new Thread(this);
        th.start(); //run() 가 실행됨.
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFilepath(String filepath){
        this.filepath = filepath;
    }
}
