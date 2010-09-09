/*
 * WriteRequest.java
 *
 * Copright (c) 2010  Kazuyoshi Aizawa <admin2@whiteboard.ne.jp>
 */ 
import java.io.IOException;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileStatus;
import java.io.FileNotFoundException;

/**
 * <p>Write リクエストを表すクラス</p>
 */
public class WriteRequest extends Request {

   public void process() {
       long offset = getOffset();
       long size = getSize();
       long filesize = 0;
       FileStatus fstat = null;       
       
        try {
            FileSystem hdfs = getFileSystem();

            // ファイルの属性を得る
            fstat = hdfs.getFileStatus(getFullPath());
            filesize = fstat.getLen();

            /*
             * この iumfscntl から受け取る write リクエストのオフセット値
             * は必ず PAGE 境界上。そして受け取るデータは PAGE 境界からの
             * データ。（既存データ含む)
             * 
             *        PAGESIZE              PAGESIZE
             *  |---------------------|---------------------|
             *  |<---------- filesize -------->|
             *  |<---- offset ------->|<-- size --->|
             *
             *  HDFS の append は filesize 直後からの追記しか許さないので
             *  iumfs から渡されたデータから、追記すべき分を算出し、
             *  HDFS に要求する。
             */ 
            if(offset + size < filesize) {
                // ファイルサイズ未満のデータ書き込み要求。すなわち変更。
                setResponseHeader(ENOTSUP, 0);
                return;
            }            
            FSDataOutputStream fsdos = hdfs.append(getFullPath());
            /*
             * ファイルの最後に/サイズのデータを書き込み用バッファに読み込む
             * 現在はオフセットの指定はできず Append だけ。
             */
            fsdos.write(getData(filesize - offset, size));
            fsdos.close();
            /*
             * レスポンスヘッダをセット
             */
            setResponseHeader(SUCCESS, 0);

        } catch (FileNotFoundException ex){
            logger.finer("FileNotFoundException happend when writing");
            ex.printStackTrace();
            setResponseHeader(ENOENT, 0);            
        } catch (IOException ex) {
            logger.info("IOException happend when writing");
            ex.printStackTrace();
            setResponseHeader(ENOTSUP, 0);
        }
    }
}

